package com.fogoa.networkapplication.extensions;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;


import com.fogoa.networkapplication.LoginActivity;
import com.fogoa.networkapplication.data.models.UserItem;
import com.fogoa.networkapplication.misc.Constants;
import com.fogoa.networkapplication.network.NetworkFragment;
import com.fogoa.networkapplication.network.listeners.AsyncNetworkThread;
import com.fogoa.networkapplication.network.models.AuthToken;
import com.fogoa.networkapplication.network.models.NetworkRequestResult;

import java.net.HttpURLConnection;

public class BaseActivity extends AppCompatActivity {
    private static final String TAG =BaseActivity.class.getSimpleName();

    //vars used by all activities
    public BaseApplication appContext;
    public boolean isSaveInstanceState = false;
    public boolean isLoginReq = false;

    // Keep a reference to the NetworkFragment, which owns the AsyncTask object
    // that is used to execute network ops.
    public NetworkFragment mNetworkFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        appContext = ((BaseApplication) getApplicationContext());

        //all activities require login currently
        if (isLoginReq) {
            if (!isLoggedIn()) {
                //  send to not loged in screen
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                getActivity().startActivity(intent);
                getActivity().finish();
            }
        }
    }

    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                if (!isSaveInstance()) {
                    onBackPressed();
                    return true;
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    */


    @Override
    public void onResume() {
        super.onResume();
        isSaveInstanceState = false;
    }

    @Override
    public void onSaveInstanceState(Bundle stateBundle) {
        isSaveInstanceState = true;
        super.onSaveInstanceState(stateBundle);
    }


    //add getActivity() so the same methods can be used in fragments and activities
    public BaseActivity getActivity() {
        return this;
    }

    public boolean isSaveInstance() {
        //not sure whu something like this doesn't already exist but there are many ui items that should not be done after the activity onSaveInstanceState has been called
        if (isFinishing() || isDestroyed()) {
            isSaveInstanceState = true;
        }
        return isSaveInstanceState;
    }

    public boolean isLoggedIn() {
        //if (Constants.DEBUG) Log.e(TAG, "isLoggedIn() called");
        boolean bLoggedIn = false;
        //logic to check the auth token when app is created
        //get persist storage
        SharedPreferences settings = getSharedPreferences(Constants.USER_FILE, 0);

        if (appContext.authToken==null) {
            String authJson = settings.getString(Constants.PREF_AUTH_JSON, "");
            if (!authJson.isEmpty()) {
                //if (Constants.DEBUG) Log.d(TAG, "isLoggedIn() authJson: "+authJson);
                appContext.authToken = new AuthToken(authJson);
            }
        }

        if (appContext.authToken!=null && appContext.authToken.access_token!=null && !appContext.authToken.access_token.isEmpty()) {
            bLoggedIn = true;

            //if we are logged in check for the user object
            /* APL 1/25/18 - not sure we want to persist the user - maybe get it each application launch
            String userJson = settings.getString(Constants.PREF_USER_JSON, "");
            if (!userJson.isEmpty()) {
                if (Constants.DEBUG) Log.d(TAG, "isLoggedIn() userJson: "+userJson);
                appContext.userItem = new UserItem(userJson);
            }
            */

            if (appContext.userItem==null) {
                //if (Constants.DEBUG) Log.e(TAG, "isLoggedIn() user item null calling getUserHttpReq()");
                getUserHttpReq();
            }
            else {
                //if (Constants.DEBUG) Log.e(TAG, "isLoggedIn() user item not null?");
            }
        }

        return bLoggedIn;
    }

    /* moved to application content
    public Locale getLocale() {
        Resources resources = getResources();
        Locale locale = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            locale = resources.getConfiguration().getLocales().getFirstMatch(resources.getAssets().getLocales());
            if (locale == null) {
                locale = resources.getConfiguration().getLocales().get(0);
            }
        }
        else {
            locale = resources.getConfiguration().locale;
        }
        return locale;
    }
    */

    public void showAlert(String message, boolean bSnack) {
        if (!isSaveInstance()) {
            // && !Constants.DEBUG
            if (bSnack) {
                Snackbar.make(getActivity().findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG).show();
            }
            else {
                AlertDialog.Builder bld = new AlertDialog.Builder(getActivity());
                bld.setMessage(message);
                bld.setNeutralButton("OK", null);
                bld.create().show();
            }
        }
    }

    public void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            view = new View(getActivity());
        }
        if (view != null) {
            view.clearFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm!=null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    protected void getUserHttpReq() {
        if (mNetworkFragment==null) {
            mNetworkFragment = NetworkFragment.getInstance(getSupportFragmentManager(), appContext);
        }
        if (mNetworkFragment != null && !isSaveInstance()) {
            String apiReq = "users";
            mNetworkFragment.startRequest(apiReq, true,new AsyncNetworkThread() {
                @Override
                public void OnRequestComplete(NetworkRequestResult nrResp) {
                    if (!isSaveInstance() && nrResp!=null) {
                        //if the activty is being closed out no need to move
                        if (nrResp.iResponseCode == HttpURLConnection.HTTP_OK) {
                            //if (Constants.DEBUG) Log.e(TAG, "getUserHttpReq() resp  json: "+nrResp.sResultValue);
                            if (appContext.userItem==null) {
                                //no content token create one
                                appContext.userItem = new UserItem(nrResp.sResultValue);
                            }
                            else {
                                //update with new value
                                appContext.userItem.ParseJson(nrResp.sResultValue);
                            }
                            //save to persist storage
                            /*APL - 1/25/18 - not sure we want to persist the user data - maybe pull it every luanch
                            SharedPreferences settings = appContext.getSharedPreferences(Constants.USER_FILE, 0);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putString(Constants.PREF_USER_JSON, appContext.userItem.GetJson());
                            editor.apply();
                            */

                            onUserUpdate(appContext.userItem);
                        }
                        else {
                            showAlert("There was an issue verifying your information, you may not be able to interact with the application until it is resolved. Error: "+nrResp.getErrorMsg(), false);
                        }
                    }
                }
            });
        }
    }

    protected void onUserUpdate(UserItem userItem) {
        //methd for updating user data
    };

}
