package de.uni_freiburg.es.sensorrecordingtool;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class WearPositionManager {

    private static final String KEY_POSITION = "wear_position";

    public static void setPosition(Context context, Position p) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(KEY_POSITION, p.name()).commit();
    }

    public static Position getPosition(Context context) {
        String s = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_POSITION, Position.RIGHT_WRIST.name());
        return Position.valueOf(s);
    }

    public enum Position {
        RIGHT_WRIST, LEFT_WRIST, RIGHT_UPPER_ARM, LEFT__UPPER_ARM, RIGHT_LEG, LEFT_LEG, NECK
    }
}
