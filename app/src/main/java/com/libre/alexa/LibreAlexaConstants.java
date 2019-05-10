package com.libre.alexa;

/**
 * Created by bhargav on 21/6/17.
 */
public class LibreAlexaConstants {
    public static final String PRODUCT_ID = "productID";
    public static final String ALEXA_ALL_SCOPE = "alexa:all";
    public static final String DEVICE_SERIAL_NUMBER = "deviceSerialNumber";
    public static final String PRODUCT_INSTANCE_ATTRIBUTES = "productInstanceAttributes";
    public static final String[] APP_SCOPES = {ALEXA_ALL_SCOPE};

    public static final int ALEXA_INTENT_FOR_RESULT = 900;
    public static final String BUNDLE_FRIENDLY_NAME = "_FRIENDLY_NAME";
    public static final String BUNDLE_ENDPOINT_ID = "_ENDPOINT_ID";
    public static final String BUNDLE_HIT_COUNT = "_HIT_COUNT";
    public static final String BUNDLE_ALEXA_USERNAME = "_ALEXA_USERNAME";
    public static final String BUNDLE_DESCRIPTION = "_ALEXA_DESCRIPTION";
    public static final String BUNDLE_IPADDRESS = "_ALEXA_IPADDRESS";
    public static final int HIT_COUNT_LIMIT = 3;

    public static final String INTENT_FROM_ACTIVITY = "_ALEXA_FROM_ACTIVITY";
    public static final String INTENT_FROM_ACTIVITY_SPLASH_SCREEN = "_ALEXA_FROM_ACTIVITY_SPLASH_SCREEN";
    public static final String INTENT_FROM_ACTIVITY_ADVANCED_SETTINGS = "_ALEXA_FROM_ACTIVITY_ADVANCED_SETTINGS";
    public static final String INTENT_FROM_ACTIVITY_ACTIVE_SCENES = "_ALEXA_FROM_ACTIVITY_ACTIVE_SCENES";

    public static final String ALEXA_EXCEPTION_SKIPPED_LOGIN = "_EXCEPTION_SKIPPED_LOGIN";
    public static final String ALEXA_REFRESH_TOKEN = "Atzr|IwEBIBNft-yrKz-uz5t8Np80mCLEPrjN557u0SeLMGBfaoOjdOzb84RZittu2CAI0OaEAMJyRtT4_QVWDiMjyLOxNAWnIQbKyQMe2zks1YEHlX_pBOY-jm2qBqgWFdVcxWOJXgYGTkvOwN5fEU2s2xkAyGY36voSrCJre4B7oeP7kqx4j4Ooh1Xldw3UEBDM3pWTxcCkjEVh2OkvsLQ0hrYhisGAA5f4giHk7QTpxIhpHao5DW0o2MHSxbK2tqlJIEgSrsN4ABR84w2dM0eSlOJ4aY-2jBJj59QqGcS0LdcKakXqRXKrgHqVstr2c_eFfntA9GfjZTl-SboqUDzlYb21T-kVwcZ54YsBH-ux9Cl32G74dNDLMmbJGJXoGPETPrj3hcbQBwsLeKlxH7O1hf9JzVF07Y7owMjZ_crjrgEtnpJ1B3NOyFJqLx0ixbgeOZEFse7o4VlPQEbf6-dZmxX0q0C5WmBRrcO0BWNAbwF_hluVBg";

    public static final String ACCESS_TOKENS_STATUS ="ACCESS_TOKENS_STATUS";
    public interface Handlers{
        public static final int LOADER = 0x401;
    }
    public interface TimerValues{
        public static final int LOADING_TIME = 5000;
    }
    public interface Languages{
        public static final String ENG_US = "en-US";
        public static final String ENG_GB = "en-GB";
        public static final String ENG_IN = "en-IN";
        public static final String DE = "de-DE";
        public static final String JP = "ja-JP";
    }
}