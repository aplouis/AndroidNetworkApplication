package com.fogoa.networkapplication.network.models;

/**
 * Wrapper class that serves as a union of a result value and an exception. When the download
 * task has completed, ResultValue can either be an error message or the returned value from the server.
 * This allows you to pass exceptions to the UI thread that were thrown during doInBackground().
 */
public class NetworkRequestResult {
    public String sResultValue = "";
    public Exception eException = null;
    public int iResponseCode = -1;

    public NetworkRequestResult () {}
    public NetworkRequestResult (String pResultValue) {
        sResultValue = pResultValue;
    }
    public NetworkRequestResult (Exception pException) {
        eException = pException;
    }

    public String getErrorMsg() {
        String errMsg = "Server Error!";
        if (sResultValue!=null && !sResultValue.isEmpty()) {
            errMsg = sResultValue;
        }
        else if (eException!=null) {
            errMsg = eException.getMessage();
        }
        return errMsg;
    }

    public String toString() {
        String resp = "Response Code: " + iResponseCode + "  Result: " + sResultValue;
        if (eException!= null) {
            resp += " Exception: "+ eException.toString() + eException.getMessage();
        }
        return resp;
    }

}
