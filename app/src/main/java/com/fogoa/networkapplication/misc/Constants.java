package com.fogoa.networkapplication.misc;


import com.fogoa.networkapplication.BuildConfig;

public class Constants {
    public static final boolean DEBUG = true;
    public static final boolean DEV_BUILD = true;
    public static final boolean STAGE_BUILD = false;

    /* user level storage */
    public static final String USER_FILE = "USERINFO";
    public static final String PREF_AUTH_JSON = "AUTHJSON";
    public static final String PREF_USER_JSON = "USERJSON";

    /* activty intent parm keys */
    public static final String INTENT_KEY_EMAIL = BuildConfig.APPLICATION_ID + ".email";
    public static final String INTENT_KEY_MENU_ID = BuildConfig.APPLICATION_ID + ".menu_id";
    public static final String INTENT_KEY_MEAL_ID = BuildConfig.APPLICATION_ID + ".meal_id";

    /* activty result ids */
    public static final int INTENT_RESULT_REFRESH = 1;

}
