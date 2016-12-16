package annoyingapps.com.phonebook;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by Fatih on 17.8.2015.
 */
public class Converter {
    public JSONObject getJsonFromUserObject(List<User> argUsers) throws JSONException {
        JSONObject lcResponseDetailsJson = new JSONObject();
        JSONArray lcJsonArray = new JSONArray();

        for (int i = 0; i < argUsers.size(); i++)
        {
            JSONObject lcUserDetailsJson = new JSONObject();
            lcUserDetailsJson.put("UN", argUsers.get(i).Username);
            lcUserDetailsJson.put("PN", argUsers.get(i).PhoneNumber);

            lcJsonArray.put(lcUserDetailsJson);
        }
        lcResponseDetailsJson.put("USERS", lcJsonArray);
        return lcResponseDetailsJson;
    }
}
