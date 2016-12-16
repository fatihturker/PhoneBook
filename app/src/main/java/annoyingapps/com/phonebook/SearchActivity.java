package annoyingapps.com.phonebook;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class SearchActivity extends Activity {
    ArrayList<User> mUserList;
    ListView mListView;
    private Context mAppContext = this;
    private String mDeviceId;
    private EditText mtxtSearchKey;
    InterstitialAd mInterstitialAd;
    private boolean mIsPhoneNumber;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        mtxtSearchKey = (EditText) findViewById(R.id.txtNameOrNumber);
        mtxtSearchKey.setHint(getResources().getString(R.string.eg_username));

        final Button btnSearch = (Button) findViewById(R.id.btnSearch);
        btnSearch.setText(getResources().getString(R.string.search_button));

        RadioButton rSearchByNumber = (RadioButton) findViewById(R.id.rbtnByNumber);
        rSearchByNumber.setText(getResources().getString(R.string.number_search));

        RadioButton rSearchByName = (RadioButton) findViewById(R.id.rbtnByName);
        rSearchByName.setText(getResources().getString(R.string.person_search));

        mIsPhoneNumber = false;
        RadioGroup radGrp = (RadioGroup) findViewById(R.id.rgrpSearch);
        radGrp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup arg0, int id) {
                mtxtSearchKey.setText("");
                switch (id) {
                    case -1:
                        Log.v("SearchActivity", "Choices cleared!");
                        break;
                    case R.id.rbtnByName:
                        mtxtSearchKey.setHint(getResources().getString(R.string.eg_username));
                        mIsPhoneNumber = false;
                        break;
                    case R.id.rbtnByNumber:
                        mtxtSearchKey.setHint(getResources().getString(R.string.eg_number));
                        mIsPhoneNumber = true;
                        break;
                    default:
                        Log.v("SearchActivity", "Huh?");
                        break;
                }
            }
        });
        mDeviceId = Security.GetDeviceID(mAppContext);
        final Button btnContinue = (Button) findViewById(R.id.btnSearch);
        btnContinue.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (mtxtSearchKey.getText().length() > 0) {
                    new FindUserTask().execute();
                } else {
                    CreateNewAlert(getResources().getString(R.string.verification_problem), getResources().getString(R.string.search_key_alert), false);
                }
            }
        });
        mUserList = new ArrayList<User>();
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-4788682465554858/9822537723");

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
        AdView adView = (AdView)this.findViewById(R.id.adView2);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();
        adView.loadAd(adRequest);
    }

    private void requestNewInterstitial() {
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .build();

        mInterstitialAd.loadAd(adRequest);
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

    private String GetUsers(){
        HashMap<String,String> lcParameters = new HashMap<String,String>();
        lcParameters.put(Constants.getUsersRequesterParameterName, mDeviceId);
        lcParameters.put(Constants.getUsersIsPhoneNumberParameterName, mIsPhoneNumber ? "1" : "0");
        lcParameters.put(Constants.getUsersSearchKeyParameterName, mtxtSearchKey.getText().toString());
        Log.w("PhoneBook", "GetUsers SearchKey:  " + Constants.getUsersSearchKeyParameterName + " " + mtxtSearchKey.getText().toString());
        lcParameters.put(Constants.getUsersSecurityTokenParameterName, new Security().GetSecurityToken(Enums.SecurityTokenType.GETUSERS, mDeviceId));
        String lcUrl = Constants.getUsersServiceUrl;
        String lcResponse = new Request().performCallRequest(lcUrl, lcParameters);
        Log.w("PhoneBook", "GetUsers Response: " + lcResponse);
        return lcResponse;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_search, menu);
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

    private void FindUser(){
        String lcUsers = GetUsers();
        JSONObject lcUsersJson = null;
        try {
            lcUsersJson = new JSONObject(lcUsers);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(lcUsersJson != null){
            JSONArray lcJsonArray = null;
            try {
                lcJsonArray = lcUsersJson.getJSONArray("Users");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mUserList = new ArrayList<User>();
            for(int i = 0 ; i < lcJsonArray.length() ; i++){
                try {
                    mUserList.add(new User(lcJsonArray.getJSONObject(i).getString("U"), lcJsonArray.getJSONObject(i).getString("P")));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void FillTheList(){
        if(mUserList!=null){
            mListView = (ListView) findViewById(R.id.lwUsers);
            MyCustomAdapter lcAdapter = new MyCustomAdapter(SearchActivity.this, R.layout.list, mUserList);
            mListView.setAdapter(lcAdapter);
            AdapterView.OnItemClickListener lcOcl = new AdapterView.OnItemClickListener()
            {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    User lcUser = (User) mListView.getItemAtPosition(position);
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + lcUser.PhoneNumber));
                    startActivity(callIntent);
                }
            };
            mListView.setOnItemClickListener(lcOcl);
        }else{
            CreateNewAlert(getResources().getString(R.string.no_record),getResources().getString(R.string.no_record_alert),false);
        }
    }


    private class FindUserTask extends AsyncTask<Void, Void, String[]> {
        private ProgressDialog progressDialog;

        @Override
        protected String[] doInBackground(Void... voids) {
            FindUser();
            return new String[0];
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(mAppContext, "", getResources().getString(R.string.searching));
        }

        @Override
        protected void onPostExecute(String[] employees) {
            progressDialog.dismiss();
            FillTheList();
            requestNewInterstitial();
        }
    }

    public class MyCustomAdapter extends ArrayAdapter<User> {

        public MyCustomAdapter(Context argContext, int textViewResourceId, ArrayList<User> argUserList)
        {
            super(argContext, textViewResourceId, argUserList);
        }

        @Override
        public int getCount(){
            return mUserList.size();
        }

        /* (non-Javadoc)
         * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
         */
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if(row==null){
                LayoutInflater inflater= getLayoutInflater();
                row=inflater.inflate(R.layout.list, parent, false);
            }
            User lcUser = (User) mListView.getItemAtPosition(position);

            TextView lcUserName = (TextView) row.findViewById(R.id.lblName);
            lcUserName.setText(lcUser.Username);

            TextView lcPhoneNumber = (TextView) row.findViewById(R.id.lblNumber);
            lcPhoneNumber.setText(lcUser.PhoneNumber);

            return row;
        }
    }
}
