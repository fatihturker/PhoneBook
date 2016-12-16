package annoyingapps.com.phonebook;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.ContactsContract;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends Activity {

    private Context mAppContext = this;
    private String mDeviceId;
    private SharedPreferences mSharedPrefs;
    private SharedPreferences.Editor mPrefsEditor;
    private LocationProvider mLocationListener;
    InterstitialAd mInterstitialAd;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLocationListener = new LocationProvider(mAppContext);
        mDeviceId = Security.GetDeviceID(mAppContext);
        CreateContentRequestAlert(getResources().getString(R.string.permission_request),getResources().getString(R.string.contact_permission));
    }

    private void CreateContentRequestAlert(String argTitle, String argMessage){
        new AlertDialog.Builder(mAppContext)
                .setTitle(argTitle)
                .setMessage(argMessage)
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        System.exit(0);
                    }
                })
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mSharedPrefs = getSharedPreferences("uFile",
                                MODE_PRIVATE);
                        mPrefsEditor = mSharedPrefs.edit();

                        final EditText lcEditText = (EditText) findViewById(R.id.txtUsername);
                        lcEditText.setHint(getResources().getString(R.string.eg_username));

                        TextView lcTextView = (TextView) findViewById(R.id.lblUsername);
                        lcTextView.setText(getResources().getString(R.string.enter_text));

                        final Button btnContinue = (Button) findViewById(R.id.btnContinue);
                        btnContinue.setText(getResources().getString(R.string.continue_button));
                        mInterstitialAd = new InterstitialAd(MainActivity.this);
                        mInterstitialAd.setAdUnitId("ca-app-pub-4788682465554858/8345804520");

                        mInterstitialAd.setAdListener(new AdListener() {
                            @Override
                            public void onAdLoaded() {
                                mInterstitialAd.show();
                            }

                            @Override
                            public void onAdClosed() {
                                // Proceed to the next level.
                            }
                        });
                        if (BuildConfig.DEBUG) {
                            mPrefsEditor.putInt("uSaved", 0);
                            mPrefsEditor.commit();
                        }
                        if(isNetworkAvailable()){
                            new UsersTask().execute();
                            int lcIsAlreadySaved = mSharedPrefs.getInt("uSaved", 0);
                            if(lcIsAlreadySaved == 1){
                                Intent lcSearchActivity = new Intent(MainActivity.this, SearchActivity.class);
                                startActivity(lcSearchActivity);
                            }else {
                                requestNewInterstitial();
                                btnContinue.setOnClickListener(new View.OnClickListener() {
                                    public void onClick(View v) {
                                        if(lcEditText.getText().length() > 0){
                                            new SaveMeTask().execute();
                                        }else{
                                            CreateNewAlert(getResources().getString(R.string.verification_problem), getResources().getString(R.string.name_alert), false);
                                        }

                                    }
                                });
                            }
                        } else {
                            CreateNewAlert(getResources().getString(R.string.network_problem),getResources().getString(R.string.network_alert), true);
                        }

                        AdView adView = (AdView)MainActivity.this.findViewById(R.id.adView);
                        AdRequest adRequest = new AdRequest.Builder()
                                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                                .build();
                        adView.loadAd(adRequest);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();

        mInterstitialAd.loadAd(adRequest);
    }

    private void Save(){
        try {
            saveAllContactList();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveUsersToDB(JSONObject argJsonObject) {
        HashMap<String,String> lcParameters = new HashMap<String,String>();
        try {
            lcParameters.put(Constants.insertUserAllUsersParameterName, argJsonObject.getString("USERS"));
            Log.w("PhoneBook", "saveUsersToDB JSON OBJECT:" + argJsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String lcLongitude = "";
        String lcLatitude = "";
        if(mLocationListener != null){
            lcLongitude = mLocationListener.Longitude == null ? "" : String.format("%f",mLocationListener.Longitude);
            lcLatitude = mLocationListener.Latitude == null ? "" : String.format("%f",mLocationListener.Latitude);
        }
        lcParameters.put(Constants.insertUserRequesterParameterName, mDeviceId);
        lcParameters.put(Constants.insertUserLongitudeParameterName, lcLongitude);
        lcParameters.put(Constants.insertUserLatitudeParameterName, lcLatitude);
        lcParameters.put(Constants.insertUserSecurityTokenParameterName, new Security().GetSecurityToken(Enums.SecurityTokenType.INSERTUSER, mDeviceId));
        String lcUrl = Constants.insertUserServiceUrl;
        String lcResponse = new Request().performCallRequest(lcUrl, lcParameters);
        Log.w("PhoneBook", "saveUsersToDB Response:" + lcResponse);
    }

    private void saveAllContactList() throws IOException {
        JSONObject lcJsonObject = null;
        try {
            lcJsonObject = new Converter().getJsonFromUserObject(getUserList());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(lcJsonObject != null){
            saveUsersToDB(lcJsonObject);
        }
    }

    private void CreateNewAlert(String argTitle, String argMessage, final boolean argIfExit){
        new AlertDialog.Builder(mAppContext)
                .setTitle(argTitle)
                .setMessage(argMessage)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (argIfExit == true){
                            finish();
                            System.exit(0);
                        }
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private List<User> getUserList(){
        List<User> lcUsers = new ArrayList<User>();
        ContentResolver lcContentResolver = getContentResolver();
        Cursor lcCursor = lcContentResolver.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);
        Log.w("PhoneBook", "getUserList lcCursor Count:" + lcCursor.getCount());
        if (lcCursor.getCount() > 0) {
            while (lcCursor.moveToNext()) {
                String lcId = lcCursor.getString(lcCursor.getColumnIndex(ContactsContract.Contacts._ID));
                String lcName = lcCursor.getString(lcCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                if (Integer.parseInt(lcCursor.getString(
                        lcCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor lcPCur = lcContentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                            new String[]{lcId}, null);
                    while (lcPCur.moveToNext()) {
                        String lcPhoneNo = lcPCur.getString(lcPCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        lcUsers.add(new User(lcName, lcPhoneNo));
                    }
                    lcPCur.close();
                }
            }
        }

        return lcUsers;
    }

    private void SaveMe(){
        List<User> lcUsers = new ArrayList<User>();
        TelephonyManager lcTMgr = (TelephonyManager)mAppContext.getSystemService(Context.TELEPHONY_SERVICE);
        String lcPhoneNumber = lcTMgr.getLine1Number();
        EditText txtUserName = (EditText)findViewById(R.id.txtUsername);

        String lcUserName = txtUserName.getText().toString();
        lcUsers.add(new User(lcUserName,lcPhoneNumber));

        JSONObject lcJsonObject = null;
        try {
            lcJsonObject = new Converter().getJsonFromUserObject(lcUsers);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(lcJsonObject != null){
            saveUsersToDB(lcJsonObject);
            mPrefsEditor.putInt("uSaved", 1);
            mPrefsEditor.commit();
            Intent lcSearchActivity = new Intent(MainActivity.this, SearchActivity.class);
            startActivity(lcSearchActivity);
        }
    }
    private class UsersTask extends AsyncTask<Void, Void, String[]> {
        private ProgressDialog progressDialog;

        @Override
        protected String[] doInBackground(Void... voids) {
            Save();
            return new String[0];
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(mAppContext, "", getResources().getString(R.string.loading));
        }

        @Override
        protected void onPostExecute(String[] employees) {
            progressDialog.dismiss();
        }
    }
    private class SaveMeTask extends AsyncTask<Void, Void, String[]> {
        private ProgressDialog progressDialog;

        @Override
        protected String[] doInBackground(Void... voids) {
            SaveMe();
            return new String[0];
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(mAppContext, "", getResources().getString(R.string.preparing));
        }

        @Override
        protected void onPostExecute(String[] employees) {
            progressDialog.dismiss();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
