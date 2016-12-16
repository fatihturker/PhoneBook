package annoyingapps.com.phonebook;

import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * Created by Fatih on 5.7.2015.
 */
public class Security {

    public String GetSecurityToken(Enums.SecurityTokenType argSecurityTokenType, String argDeviceId){
        switch (argSecurityTokenType){
            case INSERTUSER:
               return GetInsertUserSecurityToken(argDeviceId);
            case GETUSERS:
                return GetSelectUsersSecurityToken(argDeviceId);
        }
        return Constants.emptyString;
    }

    private String GetSelectUsersSecurityToken(String argDeviceId) {
        HashMap<String,String> lcParameters = new HashMap<String,String>();
        lcParameters.put(Constants.getSecurityTokenRequesterParameterName,argDeviceId);
        lcParameters.put(Constants.getSecurityTokenRequestTypeParameterName,Constants.getUsersSecurityTokenRequestType);
        String lcUrl = Constants.getSecurityTokenServiceUrl;
        String lcResponse = new Request().performCallRequest(lcUrl, lcParameters);
        Log.w("PhoneBook", "GetSelectUsersSecurityToken Response:" + lcResponse);
        return getMD5(Constants.getUsersSecurityTokenPrefix+lcResponse);
    }

    private String GetInsertUserSecurityToken(String argDeviceId){
        HashMap<String,String> lcParameters = new HashMap<String,String>();
        lcParameters.put(Constants.getSecurityTokenRequesterParameterName,argDeviceId);
        lcParameters.put(Constants.getSecurityTokenRequestTypeParameterName,Constants.insertUsersSecurityTokenRequestType);
        String lcUrl = Constants.getSecurityTokenServiceUrl;
        String lcResponse = new Request().performCallRequest(lcUrl, lcParameters);
        Log.w("PhoneBook", "GetInsertUserSecurityToken Response:" + lcResponse);
        return getMD5(Constants.insertUserSecurityTokenPrefix+lcResponse);
    }

    public static String GetDeviceID(Context argContext){
        return Settings.Secure.getString(argContext.getContentResolver(),
                Settings.Secure.ANDROID_ID);
    }

    public static final String getMD5(final String argKey) {
        final String MD5 = "MD5";
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest
                    .getInstance(MD5);
            digest.update(argKey.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (byte aMessageDigest : messageDigest) {
                String h = Integer.toHexString(0xFF & aMessageDigest);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
}
