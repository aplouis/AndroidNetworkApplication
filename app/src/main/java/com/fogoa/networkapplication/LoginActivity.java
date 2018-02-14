package com.fogoa.networkapplication;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.fogoa.networkapplication.data.models.UserItem;
import com.fogoa.networkapplication.extensions.BaseActivity;
import com.fogoa.networkapplication.network.AuthTask;
import com.fogoa.networkapplication.network.listeners.AsyncNetworkThread;
import com.fogoa.networkapplication.network.models.NetworkRequestResult;

import java.net.HttpURLConnection;

public class LoginActivity extends BaseActivity {

    TextInputLayout tilUsername;
    EditText etUsername;
    TextInputLayout tilPassword;
    EditText etPassword;
    Button btnLogin;

    UserItem userItem = new UserItem();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tilUsername = (TextInputLayout) findViewById(R.id.tilUsername);
        etUsername = (EditText) findViewById(R.id.etUsername);
        tilPassword = (TextInputLayout) findViewById(R.id.tilPassword);
        etPassword = (EditText) findViewById(R.id.etPassword);
        btnLogin = (Button) findViewById(R.id.btnLogin);
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (validateLoginForm()) {
                    loginUserHttpReq();
                }
            }
        });


    }


    private boolean validateLoginForm() {
        boolean isError = false;

        tilUsername.setErrorEnabled(false);
        String strUsername = etUsername.getText().toString().trim();
        if (strUsername.length() == 0 ) {
            isError = true;
            tilUsername.setErrorEnabled(true);
            tilUsername.setError("Username is required");
        }
        else {
            userItem.username = strUsername;
        }

        tilPassword.setErrorEnabled(false);
        String strPassword = etPassword.getText().toString().trim();
        if (strPassword.length() == 0) {
            isError = true;
            tilPassword.setErrorEnabled(true);
            tilPassword.setError("Password is required");
        }
        else {
            userItem.password = strPassword;
        }

        return !isError;
    }

    private void loginUserHttpReq() {
        if (!isSaveInstance() && userItem!= null) {
            AuthTask authTask = new AuthTask(appContext);
            final ProgressDialog progressDialog = ProgressDialog.show(getActivity(), "", "Saving...", true, false);
            authTask.getUserAuthorzation(userItem.username, userItem.password,new AsyncNetworkThread() {
                @Override
                public void OnRequestComplete(NetworkRequestResult nrResp) {
                    //if (Constants.DEBUG) Log.d(TAG, "Resp from user auth: "+nrResp.sResultValue);
                    if (nrResp.iResponseCode == HttpURLConnection.HTTP_OK) {
                        //if the response is okay we should be good to move onto the main page
                        //send to main page
                        Intent intent = new Intent(getActivity(), MainActivity.class);
                        getActivity().startActivity(intent);
                        getActivity().finish();

                    }
                    else {
                        showAlert(nrResp.getErrorMsg(), true);
                    }
                    progressDialog.dismiss();
                }
            });

        }
    }


}
