package de.uni_freiburg.es.sensorrecordingtool.autodiscovery;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings;

import java.util.ArrayList;

public class ConnectionTechnology {

    private String identifier = null;
    private Type type;

    public ConnectionTechnology(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getIdentifier() {
        return identifier;
    }

    public ConnectionTechnology setIdentifier(String identifier) {
        this.identifier = identifier;
        return this;
    }

    @Override
    public String toString() {
        return type.name() + "<" + identifier + ">";
    }

    public static ArrayList<ConnectionTechnology> gatherConnectionList(Context context) {
        ArrayList<ConnectionTechnology> connectionTechList = new ArrayList<>();

        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH))
            connectionTechList.add(new ConnectionTechnology(ConnectionTechnology.Type.WEAR)); // no need for identifier as it uses serving instead of polling

        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            connectionTechList.add(new ConnectionTechnology(ConnectionTechnology.Type.BT_CLASSIC).setIdentifier(getLocalBTAddress(context)));
        }

        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI) && isWifiConnected(context))
            connectionTechList.add(new ConnectionTechnology(ConnectionTechnology.Type.TCP_OVER_WIFI).setIdentifier(getLocalWifiAddress(context)));

        return connectionTechList;
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi.isConnected();
    }

    public static String getLocalBTAddress(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), "bluetooth_address");
    }

    @SuppressLint("DefaultLocale")
    public static String getLocalWifiAddress(Context context) {
        WifiManager connManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int ipAddress = connManager.getConnectionInfo().getIpAddress();
        return String.format("%d.%d.%d.%d",
                (ipAddress & 0xff),
                (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff),
                (ipAddress >> 24 & 0xff));
    }

    public static ConnectionTechnology pickBestConnectionTechnology(ArrayList<ConnectionTechnology> technologies) {
        return technologies.get(0);
    }

    public enum Type {
        TCP_OVER_WIFI, WEAR, LOCAL, BT_CLASSIC
    }
}
