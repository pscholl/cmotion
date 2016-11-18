package de.uni_freiburg.es.intentforwarder;

import android.content.Intent;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

/**
 * Created by phil on 4/29/16.
 */
public class ForwardedUtils {
    public static final String RECORD_ACTION = "senserec";
    private static final String JSON_KEY_ACTION = "action";
    private static final String JSON_KEY_EXTRAS = "extras";
    public static final String READY_ACTION = "senserec_ready";
    public static final String STEADY_ACTION = "senserec_steady";

    public static Intent fromJson(byte[] arr) throws JSONException {
        JSONObject o = new JSONObject(new String(arr));
        return fromJson(o);
    }

    public static Intent fromJson(JSONObject s) {
        Intent intent = new Intent();
        Bundle bundle = new Bundle();

        try { // to load the extras bundle
            JSONObject extras = s.getJSONObject(JSON_KEY_EXTRAS);

            for (Iterator<String> it = extras.keys(); it.hasNext(); ) {
                String key = it.next();
                JSONArray arr = extras.optJSONArray(key);
                Double num = extras.optDouble(key);
                String str = extras.optString(key);

                if (arr != null && arr.length() <= 0)
                    bundle.putStringArray(key, new String[]{});

                else if (arr != null && !Double.isNaN(arr.optDouble(0))) {
                    double[] newarr = new double[arr.length()];
                    for (int i = 0; i < arr.length(); i++)
                        newarr[i] = arr.optDouble(i);
                    bundle.putDoubleArray(key, newarr);
                } else if (arr != null && arr.optString(0) != null) {
                    String[] newarr = new String[arr.length()];
                    for (int i = 0; i < arr.length(); i++)
                        newarr[i] = arr.optString(i);
                    bundle.putStringArray(key, newarr);
                } else if (!num.isNaN())
                    bundle.putDouble(key, num);

                else if (str != null)
                    bundle.putString(key, str);

                else
                    System.err.println("unable to transform json to bundle " + key);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        try { // try to load the action
            intent.setAction(s.getString(JSON_KEY_ACTION));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        intent.putExtras(bundle);
        return intent;
    }

    public static JSONObject toJson(Intent i) {
        Bundle bundle = i.getExtras();
        JSONObject extras = new JSONObject();

        if (bundle != null)
            for (String key : bundle.keySet())
                try {
                    extras.put(key, JSONObject.wrap(bundle.get(key)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

        JSONObject json = new JSONObject();
        try {
            json.put(JSON_KEY_ACTION, i.getAction());
            json.put(JSON_KEY_EXTRAS, extras);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }
}
