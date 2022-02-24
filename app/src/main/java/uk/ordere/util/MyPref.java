package uk.ordere.util;

import android.content.Context;
import android.content.SharedPreferences;

public class MyPref {

    private static String PREF_NAME = "launcher_pref";
    private static String BRIGHTNESS_VALUE = "brightness_value";
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
   private static MyPref myPref;

    private MyPref(Context context){
        sharedPreferences = context.getSharedPreferences(PREF_NAME,Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public static MyPref getInstance(Context context){
        if(myPref==null){
            myPref = new MyPref(context);
        }

        return myPref;
    }


    public void saveBrightness(int value){
        editor.putInt(BRIGHTNESS_VALUE,value);
        editor.apply();
    }

    public int getBrightness(){
        return sharedPreferences.getInt(BRIGHTNESS_VALUE,50);
    }


}
