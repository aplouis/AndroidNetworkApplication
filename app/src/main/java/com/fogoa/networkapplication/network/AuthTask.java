package com.fogoa.networkapplication.network;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import com.fogoa.networkapplication.extensions.BaseApplication;
import com.fogoa.networkapplication.misc.Constants;
import com.fogoa.networkapplication.network.listeners.AsyncNetworkThread;
import com.fogoa.networkapplication.network.models.AuthToken;
import com.fogoa.networkapplication.network.models.NetworkRequestInput;
import com.fogoa.networkapplication.network.models.NetworkRequestResult;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class AuthTask {
    private static final String TAG = AuthTask.class.getSimpleName();

    static final String CLIENT_ID = "";
    static final String CLIENT_SECRET = "";
    private static String CLIENT_ENDPOINT = "";

    private WeakReference<BaseApplication> appReference;
    private boolean bWorking = false;
    //String apiName = "";

    public AuthTask(Context ctx) {
        appReference = new WeakReference<>((BaseApplication)ctx);

        /* maybe set different end points based on dev stage builds
        if (Constants.STAGE_BUILD) {
            CLIENT_ENDPOINT = "https://oauth2staging.sparkpeople.com/auth/oauth2";
        }
        else if (Constants.DEV_BUILD) {
            CLIENT_ENDPOINT = "https://Oauth2dev.sparkpeople.com/auth/oauth2";
        }
        */
    }

    public void getUserAuthorzation(String uname, String pass, final AsyncNetworkThread regListener) {
        try {
            uname = URLEncoder.encode(uname, "UTF-8");
            pass = URLEncoder.encode(pass,"UTF-8");
        }
        catch(Exception e) {
            //if we get an encoding exception we just won't encode the values
        }

        String urlAuthorize = CLIENT_ENDPOINT + "?grant_type=password&username="+uname+"&password="+pass;
        BaseApplication appContext = appReference.get();

        NetworkRequestInput nrInput = new NetworkRequestInput();
        nrInput.requestMethod = "POST";
        nrInput.urlString = urlAuthorize;
        nrInput.useClientAuth = true;
        if (appContext != null ) {
            if (appContext.authToken != null && appContext.authToken.uuid != null) {
                nrInput.apiUID = appContext.authToken.uuid;
            }
            if (appContext.getLocale()!=null) {
                nrInput.apiLocale = appContext.getLocale().toString();
            }
        }

        AuthorizeUser(nrInput, new AsyncNetworkThread() {
            @Override
            public void OnRequestComplete(NetworkRequestResult nrResp) {
                //if (Constants.DEBUG) Log.d(TAG, "AuthorizeUser resp: "+resp);
                if (nrResp.iResponseCode == HttpURLConnection.HTTP_OK) {
                    try {
                        Object objResp = new JSONTokener(nrResp.sResultValue).nextValue();
                        if (objResp instanceof JSONObject) {
                            JSONObject joResp = (JSONObject)objResp;

                            BaseApplication appContext = appReference.get();
                            if (appContext!=null) {
                                if (appContext.authToken==null) {
                                    //no content token create one
                                    appContext.authToken = new AuthToken(joResp);
                                }
                                else {
                                    //update with new value
                                    appContext.authToken.ParseJson(joResp);
                                }

                                //save to persist storage
                                SharedPreferences settings = appContext.getSharedPreferences(Constants.USER_FILE, 0);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString(Constants.PREF_AUTH_JSON, appContext.authToken.GetJson());
                                //editor.commit();
                                editor.apply();
                            }
                        }
                    }
                    catch (JSONException e) {
                        //json is invlid
                        if (Constants.DEBUG) Log.e(TAG, "json parse exception : " + e.toString());
                    }
                }

                if (regListener!=null) {
                    //bubble up response
                    regListener.OnRequestComplete(nrResp);
                }
            }
        });
    }

    public void getRefreshToken(String refresh_token, final AsyncNetworkThread regListener) {
        try {
            refresh_token = URLEncoder.encode(refresh_token, "UTF-8");
        }
        catch(Exception e) {
            //if we get an encoding exception we just won't encode the values
        }

        String urlAuthorize = CLIENT_ENDPOINT + "?grant_type=refresh_token&refresh_token="+refresh_token;

        BaseApplication appContext = appReference.get();
        NetworkRequestInput nrInput = new NetworkRequestInput();
        nrInput.requestMethod = "POST";
        nrInput.urlString = urlAuthorize;
        nrInput.useClientAuth = true;
        if (appContext != null ) {
            if (appContext.authToken != null && appContext.authToken.uuid != null) {
                nrInput.apiUID = appContext.authToken.uuid;
            }
            if (appContext.getLocale()!=null) {
                nrInput.apiLocale = appContext.getLocale().toString();
            }
        }

        RefreshToken(nrInput, new AsyncNetworkThread() {
            @Override
            public void OnRequestComplete(NetworkRequestResult nrResp) {
                //if (Constants.DEBUG) Log.d(TAG, "RefreshToken resp: "+resp);
                if (nrResp.iResponseCode == HttpURLConnection.HTTP_OK) {
                    try {
                        Object objResp = new JSONTokener(nrResp.sResultValue).nextValue();
                        if (objResp instanceof JSONObject) {
                            JSONObject joResp = (JSONObject)objResp;

                            BaseApplication appContext = appReference.get();
                            if (appContext!=null) {
                                if (appContext.authToken==null) {
                                    //no content token create one
                                    appContext.authToken = new AuthToken(joResp);
                                }
                                else {
                                    //update with new value
                                    appContext.authToken.ParseJson(joResp);
                                }

                                //save to persist storage
                                SharedPreferences settings = appContext.getSharedPreferences(Constants.USER_FILE, 0);
                                SharedPreferences.Editor editor = settings.edit();
                                editor.putString(Constants.PREF_AUTH_JSON, appContext.authToken.GetJson());
                                //editor.commit();
                                editor.apply();

                            }
                        }
                    }
                    catch (JSONException e) {
                        //json is invlid
                        if (Constants.DEBUG) Log.e(TAG, "json parse exception : " + e.toString());
                    }
                }
                if (regListener!=null) {
                    //bubble up response
                    regListener.OnRequestComplete(nrResp);
                }
            }
        });
    }

    private static void AuthorizeUser(NetworkRequestInput nrInput, AsyncNetworkThread regListener) {
        AuthAsyncTask authNetworkTask = new AuthAsyncTask(regListener);
        authNetworkTask.execute(nrInput);
    }

    private static void RefreshToken(NetworkRequestInput nrInput, AsyncNetworkThread regListener) {
        AuthAsyncTask authNetworkTask = new AuthAsyncTask(regListener);
        authNetworkTask.execute(nrInput);
    }


    private static class AuthAsyncTask extends AsyncTask<NetworkRequestInput, Void, NetworkRequestResult> {
        private AsyncNetworkThread regListener;

        AuthAsyncTask(AsyncNetworkThread listener) {
            regListener = listener;
        }

        @Override
        protected NetworkRequestResult doInBackground(NetworkRequestInput... params) {
            NetworkRequestResult nrResp = new NetworkRequestResult();
            if (!isCancelled()) {
                NetworkRequestInput nrInput = new NetworkRequestInput();
                if (params != null && params.length > 0) {
                    nrInput = (NetworkRequestInput) params[0];
                }
                HttpURLConnection conn = null;
                InputStream stream = null;
                try {
                    //if (Constants.DEBUG) Log.d(TAG, "AuthAsyncTask reqUrl: "+nrInput.urlString);
                    URL url = new URL(nrInput.urlString);
                    conn = (HttpURLConnection) url.openConnection();
                    //conn.addRequestProperty("client_id", CLIENT_ID);
                    //conn.addRequestProperty("client_secret", CLIENT_SECRET);
                    //conn.setRequestProperty("Authorization", "OAuth " + token);

                    conn.setRequestMethod("POST");
                    conn.setDoInput(true);
                    conn.setRequestProperty("charset", "utf-8");
                    conn.setRequestProperty("accept-charset", "UTF-8");

                    conn.setRequestProperty("Authorization", "Basic " + Base64.encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes("UTF-8"), Base64.NO_WRAP));
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    //add custom headers
                    if (!nrInput.apiAppName.isEmpty()) {
                        conn.setRequestProperty("JCAPI_UA", nrInput.apiAppName);
                    }
                    if (!nrInput.apiUID.isEmpty()) {
                        conn.setRequestProperty("JCAPI_UID", nrInput.apiUID);
                    }
                    if (!nrInput.apiLocale.isEmpty()) {
                        conn.setRequestProperty("JCAPI_LOCALE", nrInput.apiLocale);
                    }


                    int respCode = conn.getResponseCode();
                    nrResp.iResponseCode = respCode;
                    if (respCode == HttpURLConnection.HTTP_OK) {
                        stream = conn.getInputStream();
                        BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        nrResp.sResultValue = response.toString();
                    }
                    else {
                        nrResp.eException = new Exception("Auth Network Error: request "+nrInput.urlString+" returned error code " + respCode + " " + conn.getResponseMessage());

                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        String inputLine;
                        StringBuilder response = new StringBuilder();
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();

                        JSONObject joResp = (JSONObject) new JSONTokener(response.toString()).nextValue();
                        //try to parse out error message
                        if (joResp.has("error_description")) {
                            String errorMsg = joResp.getString("error_description");
                            if (errorMsg!=null && !errorMsg.isEmpty()) {
                                nrResp.sResultValue = errorMsg;
                            }
                        }

                    }

                } catch (Exception e) {
                    //if (Constants.DEBUG) Log.e(TAG, "server req exception : " + e.getMessage() + " - " + e.toString());
                    nrResp.eException = e;
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        }
                        catch (Exception ioe) {
                            //not sure we care about io exception on close
                            nrResp.eException = ioe;
                        }
                    }
                    if (conn != null) {
                        conn.disconnect();
                    }
                }
            } //cancelled check
            //if (Constants.DEBUG) Log.d(TAG, "AuthAsyncTask HttpURLConnection return resp: "+nrResp.toString());
            return nrResp;
        }

        @Override
        protected void onPostExecute(NetworkRequestResult result) {
            super.onPostExecute(result);

            //if (Constants.DEBUG) Log.d(TAG, "onPostExecute result: "+result);
            if (regListener!=null) {
                //if (Constants.DEBUG) Log.d(TAG, "regListener.OnRequestComplete result: "+result);
                regListener.OnRequestComplete(result);
            }
        }

    }


}
