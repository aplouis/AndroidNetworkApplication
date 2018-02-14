package com.fogoa.networkapplication.network.models;

import android.util.Log;

import com.fogoa.networkapplication.misc.Constants;
import com.fogoa.networkapplication.misc.Utilities;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.UUID;

public class AuthToken {
    private static final String TAG = AuthToken.class.getSimpleName();
    public String userid = "";
    public String access_token = "";
    public String refresh_token = "";
    public String token_type = "";
    public long expire_in_sec = 0;
    public long local_expire_time = 0;
    public String uuid = "";

    public AuthToken() { }

    public AuthToken(String jsonString) {
        ParseJson(jsonString);
    }

    public AuthToken(JSONObject jsonObj) {
        ParseJson(jsonObj);
    }

    public void ParseJson(String jsonString) {
        if (jsonString != null && jsonString.length() > 0) {
            try {
                Object objResp = new JSONTokener(jsonString).nextValue();
                if (objResp instanceof JSONObject) {
                    JSONObject jsonObj = (JSONObject)objResp;
                    ParseJson(jsonObj);
                }
            }
            catch (JSONException e) {
                //json is invlid
                if (Constants.DEBUG) Log.e(TAG, "json parse exception : " + e.toString());
            }

        }

    }

    public void ParseJson(JSONObject jsonObj) {
        try {
            if (jsonObj.has("user_id")) {
                userid = jsonObj.getString("user_id");
            }
            if (jsonObj.has("access_token")) {
                access_token = jsonObj.getString("access_token");
            }
            if (jsonObj.has("refresh_token")) {
                refresh_token = jsonObj.getString("refresh_token");
            }
            if (jsonObj.has("token_type")) {
                token_type = jsonObj.getString("token_type");
            }
            if (jsonObj.has("expires_in")) {
                expire_in_sec = Utilities.safeParseLong(jsonObj.getString("expires_in"));
                //sets the expire time in milliseconds to 5 seconds before the expire time
                local_expire_time = System.currentTimeMillis() + ((expire_in_sec - 5) * 1000);
            }
            if (jsonObj.has("local_expire_time")) {
                //if the json has a local expire time use that
                local_expire_time = Utilities.safeParseLong(jsonObj.getString("local_expire_time"));
            }
            if (jsonObj.has("uuid")) {
                uuid = jsonObj.getString("uuid");
                if (uuid==null || uuid.isEmpty()) {
                    uuid = UUID.randomUUID().toString();
                }
            }
            else {
                uuid = UUID.randomUUID().toString();
            }
        }
        catch (JSONException e) {
            //json is invlid
            if (Constants.DEBUG) Log.e(TAG, "json parse exception : " + e.toString());
        }

    }

    public String GetJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("user_id", userid);
            json.put("access_token", access_token);
            json.put("token_type", token_type);
            json.put("expires_in", expire_in_sec);
            json.put("refresh_token", refresh_token);
            json.put("locale_expire_time", local_expire_time);
            json.put("uuid", uuid);

            return json.toString();
        }
        catch (JSONException e) {
            return "{\"user_id\":\""+userid+"\",\"access_token\":\""+access_token+"\",\"token_type\":\""+token_type+"\",\"expires_in\":"+expire_in_sec+",\"refresh_token\":\""+refresh_token+"\",\"local_expire_time\":\"" + local_expire_time + "\",\"uuid\":\"" + uuid + "\"}";
        }
    }

    public String toString() {
        return GetJson();
    }

}
