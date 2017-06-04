package com.zulip.android.util;

/**
 * List of all Constants used in the projects
 */

public class Constants {
    public final static String IS_CONTENT_EDIT_PARAM_SAVED = "isContentEditParamSaved";
    public final static String IS_EDITING_ALLOWED = "isEditingAllowed";
    public final static String MAXIMUM_CONTENT_EDIT_LIMIT = "maximumContentEditLimit";
    //Default maximum time limit for editing message(Same as server)
    public final static int DEFAULT_MAXIMUM_CONTENT_EDIT_LIMIT = 600;
    public final static boolean DEFAULT_EDITING_ALLOWED = true;
    public final static String SERVER_URL = "SERVER_URL";
    public static final String CANCEL = "CANCEL";
    public static int MILLISECONDS_IN_A_MINUTE = 1000;
    public static String DATE_FORMAT = "dd/MM/yyyy";
    public static final int REQUEST_PICK_FILE = 3;
    public static final int HIDE_FAB_AFTER_SEC = 5;
    // row number which is used to differentiate the '@-mentions' in people drawer
    public static int MENTIONS = -2;
    public static int REACTION_MARGIN = 64;
    //end points
    public static String END_POINT_TERMS_OF_SERVICE = "terms";
    public static String END_POINT_PRIVACY = "privacy";
    public static String END_POINT_REGISTER = "register";
    //if two continuous messages are from same sender and time difference is less than this then hide it
    public static long HIDE_TIMESTAMP_THRESHOLD = 60 * 1000;// 1 minute
    public static String ACTION_MESSAGE_PUSH_NOTIFICATION = "ACTION_MESSAGE_PUSH_NOTIFICATION";
    public static String NIGHT_THEME = "NIGHT_THEME";
    public static String AUTO_NIGHT_THEME = "AUTO_NIGHT_THEME";
}
