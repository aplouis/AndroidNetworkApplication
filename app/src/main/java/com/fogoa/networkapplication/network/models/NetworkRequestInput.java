package com.fogoa.networkapplication.network.models;

import com.fogoa.networkapplication.BuildConfig;

/**
 * This class incapsolates all the information needed to make a netowrk request
 */

public class NetworkRequestInput {
    public String urlString = "";
    public String authToken = "";
    public String postData = "";
    public String requestMethod = "GET";
    public String apiAppName = "";
    public String apiUID = "";
    public String apiLocale = "";
    public boolean useClientAuth = false;

    public NetworkRequestInput () {apiAppName=getAppName();}

    public static String getAppName() {
        return BuildConfig.VERSION_NAME;
    }

}
