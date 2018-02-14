package com.fogoa.networkapplication.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Base64;
import android.util.Log;

import com.fogoa.networkapplication.extensions.BaseApplication;
import com.fogoa.networkapplication.misc.Constants;
import com.fogoa.networkapplication.network.listeners.AsyncNetworkThread;
import com.fogoa.networkapplication.network.listeners.NetworkCallback;
import com.fogoa.networkapplication.network.models.NetworkRequestInput;
import com.fogoa.networkapplication.network.models.NetworkRequestResult;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

/**
 * Implementation of headless Fragment that runs an AsyncTask to fetch data from the network.
 */

public class NetworkFragment extends Fragment {
    public static final String TAG = NetworkFragment.class.getSimpleName();;

    private static final String URL_KEY = "";
    public static String API_ENDPOINT = "";

    private WeakReference<BaseApplication> appReference;

    private ArrayList<NetworkTask> listNetworkTask = new ArrayList<NetworkTask>();

    private int authErrorCnt = 0;

    /**
     * Static initializer for NetworkFragment that sets the URL of the host it will be downloading
     * from.
     */
    public static NetworkFragment getInstance(FragmentManager fragmentManager, Context context) {
        // Recover NetworkFragment in case we are re-creating the Activity due to a config change.
        // This is necessary because NetworkFragment might have a task that began running before
        // the config change occurred and has not finished yet.
        // The NetworkFragment is recoverable because it calls setRetainInstance(true).
        NetworkFragment networkFragment = (NetworkFragment) fragmentManager.findFragmentByTag(NetworkFragment.TAG);
        if (networkFragment == null) {
            networkFragment = new NetworkFragment();
            fragmentManager.beginTransaction().add(networkFragment, TAG).commit();
        }
        networkFragment.authErrorCnt = 0;
        if (context!=null) {
            networkFragment.appReference = new WeakReference<>((BaseApplication) context.getApplicationContext());
            //if (Constants.DEBUG) Log.d(TAG, "getInstance set appReference ");
        }

        return networkFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Retain this Fragment across configuration changes in the host Activity.
        setRetainInstance(true);

        /*  may want to motify end point based on build
        if (Constants.STAGE_BUILD) {
            API_ENDPOINT = "https://apistaging.sparkpeople.com/api/v2/";
        }
        else if (Constants.DEV_BUILD) {
            API_ENDPOINT = "https://apidev.sparkpeople.com/api/v2/";
        }
        */

        if (getActivity()!=null) {
            appReference = new WeakReference<>((BaseApplication) getActivity().getApplicationContext());
            //if (Constants.DEBUG) Log.d(TAG, "onCreate set appReference ");
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context!=null) {
            appReference = new WeakReference<>((BaseApplication) context.getApplicationContext());
            //if (Constants.DEBUG) Log.d(TAG, "onAttach set appReference ");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        // Clear reference to host Activity to avoid memory leak.
        //mCallback = null;
        if (listNetworkTask!=null) {
            for (NetworkTask netTask : listNetworkTask) {
                listNetworkTask.remove(netTask);
                if (netTask!=null) {
                    netTask = null;
                }
            }
            listNetworkTask = null;
        }

    }

    @Override
    public void onDestroy() {
        // Cancel task when Fragment is destroyed.
        cancelRequest();
        super.onDestroy();
    }

    /**
     * Start non-blocking execution of DownloadTask.
     */
    public void startRequest(String reqUrl, final AsyncNetworkThread reqListener) {
        startRequest(reqUrl, false, "GET", null, reqListener);
    }
    public void startRequest(String reqUrl, final boolean isApiReq, final AsyncNetworkThread reqListener) {
        startRequest(reqUrl, isApiReq, "GET", null, reqListener);
    }
    public void startRequest(final String reqUrl, final boolean isApiReq, final String requestMethod, final String postData, final AsyncNetworkThread reqListener) {
        String irUrl = reqUrl;
        if (isApiReq) {
            //add the api end point to the request
            irUrl = API_ENDPOINT + irUrl;
        }
        BaseApplication appContext = appReference.get();
        String accessToken = "";
        if (appContext.authToken!=null && appContext.authToken.access_token!=null) {
            accessToken = appContext.authToken.access_token;
        }
        NetworkRequestInput nrInput = new NetworkRequestInput();
        nrInput.requestMethod = requestMethod;
        nrInput.urlString = irUrl;
        nrInput.authToken = accessToken;
        nrInput.postData = postData;
        if (appContext.authToken != null && appContext.authToken.uuid != null) {
            nrInput.apiUID = appContext.authToken.uuid;
        }
        if (appContext.getLocale()!=null) {
            nrInput.apiLocale = appContext.getLocale().toString();
        }
        startRequest(nrInput, reqListener);
    }
    public void startRequest(final NetworkRequestInput nrInput, final AsyncNetworkThread reqListener) {

        //check content weak reference
        if (appReference==null && getActivity()!=null) {
            //if (Constants.DEBUG) Log.d(TAG, "startRequest appReference null getActivity() not null ");
            appReference = new WeakReference<>((BaseApplication)getActivity().getApplicationContext());
        }
        if (appReference==null) {
            //if (Constants.DEBUG) Log.d(TAG, "startRequest appReference null why? ");
            if (reqListener!=null) {
                reqListener.OnRequestComplete(new NetworkRequestResult(new Exception("Current context is not valid.")));
                return;
            }
        }

        final BaseApplication appContext = appReference.get();
        if (appContext!= null) {
            final String frUrl = nrInput.urlString;
            //this version allows for mutiple asyctask
            final NetworkTask nNetworkTask = new NetworkTask();
            NetworkCallback<NetworkRequestResult> nNetworkCallback = new NetworkCallback<NetworkRequestResult>() {
                @Override
                public void updateFromDownload(NetworkRequestResult result) {
                    //if (Constants.DEBUG) Log.d(TAG, "startRequest updateFromDownload result: "+result.toString());
                    if (result.iResponseCode == HttpURLConnection.HTTP_UNAUTHORIZED || result.iResponseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                        //if (Constants.DEBUG) Log.d(TAG, "updateFromDownload resp fail get new refersh token authErrorCnt: "+authErrorCnt);
                        if (authErrorCnt == 0 && appContext!= null && appContext.authToken!=null && appContext.authToken.refresh_token!=null && !appContext.authToken.refresh_token.isEmpty()) {
                            AuthTask auth = new AuthTask(appContext);
                            //refresh the token - pass null because we don't need a call back
                            auth.getRefreshToken(appContext.authToken.refresh_token,new AsyncNetworkThread() {
                                @Override
                                public void OnRequestComplete(NetworkRequestResult nrResp) {
                                    //if (Constants.DEBUG) Log.d(TAG, "updateFromDownload getRefreshToken OnRequestComplete resp: "+nrResp.toString());
                                    if ( nrResp.iResponseCode == HttpURLConnection.HTTP_OK) {
                                        authErrorCnt++;
                                        //try task again
                                        nrInput.authToken = appContext.authToken.access_token;
                                        startRequest(nrInput, reqListener);
                                    }
                                    else {
                                        if (reqListener != null) {
                                            reqListener.OnRequestComplete(nrResp);
                                        }
                                    }
                                }
                            });
                        }
                        else if (reqListener != null) {
                            //if (Constants.DEBUG) Log.d(TAG, "updateFromDownload appContext or soemthing is null");
                            reqListener.OnRequestComplete(result);
                        }
                    }
                    else {
                        // Update your UI here based on result of download.
                        if (reqListener != null) {
                            reqListener.OnRequestComplete(result);
                        }
                    }
                }

                @Override
                public void onProgressUpdate(int progressCode, int percentComplete) {
                }

                @Override
                public void finishDownloading() {
                    //mDownloading = false;
                    //if (mNetworkFragment != null) {
                    //    mNetworkFragment.cancelDownload();
                    //}
                    nNetworkTask.cancel(true);
                    listNetworkTask.remove(nNetworkTask);
                }
            };
            nNetworkTask.setCallback(nNetworkCallback);

            //check for expired token
            if (appContext.authToken!=null && appContext.authToken.refresh_token!=null && !appContext.authToken.refresh_token.isEmpty() && appContext.authToken.local_expire_time < System.currentTimeMillis()) {
                //if we have a refresh token and the local exire time is less then the current time first refresh the token
                AuthTask auth = new AuthTask(appContext);
                auth.getRefreshToken(appContext.authToken.refresh_token,new AsyncNetworkThread() {
                    @Override
                    public void OnRequestComplete(NetworkRequestResult nrResp) {
                        //if (Constants.DEBUG) Log.d(TAG, "NetworkFragment getRefreshToken response: " + nrResp.toString());
                        if ( nrResp.iResponseCode == HttpURLConnection.HTTP_OK) {
                            listNetworkTask.add(nNetworkTask);
                            //if (Constants.DEBUG) Log.d(TAG, "startRequest execute req after refresh token ");
                            nrInput.authToken = appContext.authToken.access_token;
                            nNetworkTask.execute(nrInput);
                        }
                        else {
                            if (reqListener!=null) {
                                reqListener.OnRequestComplete(nrResp);
                            }
                        }
                    }
                });

            }
            else {
                listNetworkTask.add(nNetworkTask);
                //if (Constants.DEBUG) Log.d(TAG, "startRequest execute req ");
                nNetworkTask.execute(nrInput);
            }
        }
        else {
            if (reqListener!=null) {
                reqListener.OnRequestComplete(new NetworkRequestResult(new Exception("Not yet authorized to access server api.")));
            }
        }

    }

    /**
     * Cancel (and interrupt if necessary) any ongoing DownloadTask execution.
     */
    public void cancelRequest() {
        if (listNetworkTask!=null) {
            for (NetworkTask netTask : listNetworkTask) {
                if (netTask!=null) {
                    netTask.cancel(true);
                }
            }
        }
    }

    public NetworkInfo getActiveNetworkInfo() {
        NetworkInfo networkInfo = null;
        BaseApplication appContext = appReference.get();
        if (appContext!=null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) appContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager!=null) {
                networkInfo = connectivityManager.getActiveNetworkInfo();
            }
        }
        return networkInfo;
    }

    /**
     * Implementation of AsyncTask designed to fetch data from the network.
     */
    private class NetworkTask extends AsyncTask<NetworkRequestInput, Integer, NetworkRequestResult> {

        private NetworkCallback<NetworkRequestResult> mCallback;

        NetworkTask() {
        }

        NetworkTask(NetworkCallback<NetworkRequestResult> callback) {
            setCallback(callback);
        }

        public void setCallback(NetworkCallback<NetworkRequestResult> callback) {
            mCallback = callback;
        }

        /**
         * Cancel background network operation if we do not have network connectivity.
         */
        @Override
        protected void onPreExecute() {
            NetworkInfo networkInfo = getActiveNetworkInfo();
            if (networkInfo == null || !networkInfo.isConnected() ||
                    (networkInfo.getType() != ConnectivityManager.TYPE_WIFI
                            && networkInfo.getType() != ConnectivityManager.TYPE_MOBILE)) {
                // If no connectivity, cancel task and update Callback with null data.
                mCallback.updateFromDownload(null);
                cancel(true);
            }
        }

        /**
         * Defines work to perform on the background thread.
         */
        @Override
        protected NetworkRequestResult doInBackground(NetworkRequestInput... params) {
            NetworkRequestResult result = new NetworkRequestResult();
            if (!isCancelled() && params != null && params.length > 0) {
                String token = "";
                String postData = "";
                String requestMethod = "GET";
                NetworkRequestInput nrInput = (NetworkRequestInput) params[0];
                String urlString = nrInput.urlString;
                if (nrInput.authToken!=null && !nrInput.authToken.isEmpty()) {
                    token = nrInput.authToken;
                }
                if (nrInput.requestMethod!=null && !nrInput.requestMethod.isEmpty()) {
                    requestMethod = nrInput.requestMethod;
                }
                if (nrInput.postData!=null && !nrInput.postData.isEmpty()) {
                    postData = nrInput.postData;
                }

                try {
                    URL url = new URL(urlString);

                    InputStream stream = null;
                    HttpsURLConnection connection = null;
                    try {
                        connection = (HttpsURLConnection) url.openConnection();
                        // Timeout for reading InputStream arbitrarily set to 3000ms.
                        connection.setReadTimeout(3000);
                        // Timeout for connection.connect() arbitrarily set to 3000ms.
                        connection.setConnectTimeout(3000);
                        // For this use case, set HTTP method to GET.
                        //connection.setRequestMethod("GET");
                        connection.setRequestMethod(requestMethod);
                        //connection.setRequestMethod("POST");
                        // Already true by default but setting just in case; needs to be true since this request
                        // is carrying an input (response) body.
                        connection.setDoInput(true);
                        connection.setUseCaches(false);
                        connection.setRequestProperty("charset", "utf-8");
                        connection.setRequestProperty("accept-charset", "UTF-8");


                        //connection.addRequestProperty("client_id", SPoAuth.CLIENT_ID);
                        //connection.addRequestProperty("client_secret", SPoAuth.CLIENT_SECRET);
                        //connection.setRequestProperty("Authorization", "OAuth " + token);
                        //connection.setRequestProperty("Authorization", "Bearer " + Base64.encodeToString(token.getBytes("UTF-8"), Base64.NO_WRAP));
                        if (token!=null && !token.isEmpty() && !nrInput.useClientAuth) {
                            if (Constants.DEBUG) Log.e(TAG, "Authorizationn Bearer " + token);
                            connection.setRequestProperty("Authorization", "Bearer " + token);
                        }
                        else {
                            connection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString((AuthTask.CLIENT_ID + ":" + AuthTask.CLIENT_SECRET).getBytes("UTF-8"), Base64.NO_WRAP));
                        }
                        connection.setRequestProperty("Content-Type", "application/json");

                        //add custom headers
                        if (!nrInput.apiAppName.isEmpty()) {
                            connection.setRequestProperty("API_UA", nrInput.apiAppName);
                        }
                        if (!nrInput.apiUID.isEmpty()) {
                            connection.setRequestProperty("API_UID", nrInput.apiUID);
                        }
                        if (!nrInput.apiLocale.isEmpty()) {
                            connection.setRequestProperty("API_LOCALE", nrInput.apiLocale);
                        }

                        //post and put need to send data
                        if (postData!=null && !postData.isEmpty()) {
                            connection.setDoOutput(true);

                            OutputStream os = connection.getOutputStream();

                            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                            writer.write(postData);
                            writer.flush();
                            writer.close();

                            os.flush();
                            os.close();
                        }

                        // Open communications link (network traffic occurs here).
                        //connection.connect();
                        publishProgress(NetworkCallback.Progress.CONNECT_SUCCESS);

                        int respCode = connection.getResponseCode();
                        result.iResponseCode = respCode;
                        if (respCode == HttpURLConnection.HTTP_NO_CONTENT) {
                            result.sResultValue = "";
                        }
                        else if (respCode == HttpURLConnection.HTTP_OK || respCode == HttpURLConnection.HTTP_CREATED) {
                            stream = connection.getInputStream();
                            publishProgress(NetworkCallback.Progress.GET_INPUT_STREAM_SUCCESS, 0);
                            BufferedReader in = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
                            String inputLine;
                            StringBuilder response = new StringBuilder();
                            while ((inputLine = in.readLine()) != null) {
                                response.append(inputLine);
                            }
                            in.close();

                            result.sResultValue = response.toString();
                        }
                        else {
                            result.eException = new Exception("Network Error: request "+urlString+" returned error code " + respCode + " " + connection.getResponseMessage());

                            try {
                                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
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
                                        result.sResultValue = errorMsg;
                                    }
                                }
                            }
                            catch (Exception e) {
                                //json is invlid
                                //if (Constants.DEBUG) Log.e(TAG, "json parse exception : " + e.toString());
                            }


                        }

                    } finally {
                        // Close Stream and disconnect HTTPS connection.
                        if (stream != null) {
                            try {
                                stream.close();
                            }
                            catch (Exception ioe) {
                                //not sure we care about io exception on close
                                result.eException = ioe;
                            }
                        }
                        if (connection != null) {
                            connection.disconnect();
                        }
                    }

                } catch(Exception e) {
                    result.eException = e;
                }
            }
            if (Constants.DEBUG) Log.d(TAG, "NetworkTask HttpURLConnection return resp: "+result.toString());
            return result;
        }

        /**
         * Updates the DownloadCallback with the result.
         */
        @Override
        protected void onPostExecute(NetworkRequestResult result) {
            if (result != null && mCallback != null) {
                if (result.eException != null) {
                    mCallback.updateFromDownload(result);
                } else if (result.sResultValue != null) {
                    mCallback.updateFromDownload(result);
                }
                mCallback.finishDownloading();
            }
        }

        /**
         * Override to add special behavior for cancelled AsyncTask.
         */
        @Override
        protected void onCancelled(NetworkRequestResult result) {
        }
    }

    /**
     * Converts the contents of an InputStream to a String.
     */
    public String readStream(InputStream stream, int maxReadSize)
            throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] rawBuffer = new char[maxReadSize];
        int readSize;
        StringBuffer buffer = new StringBuffer();
        while (((readSize = reader.read(rawBuffer)) != -1) && maxReadSize > 0) {
            if (readSize > maxReadSize) {
                readSize = maxReadSize;
            }
            buffer.append(rawBuffer, 0, readSize);
            maxReadSize -= readSize;
        }
        return buffer.toString();
    }

}
