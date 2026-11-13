package ru.truetv.launcher;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.*;

/** Read-only public radio states and the owner-selected Wi-Fi screen. */
public final class RadioControls {
 public static final int UNKNOWN=-1,ABSENT=-2,OFF=0,ON=1,CHANGING=2;
 private RadioControls(){}
 static BluetoothAdapter adapter(Context c){BluetoothManager m=(BluetoothManager)c.getSystemService(Context.BLUETOOTH_SERVICE);return m==null?null:m.getAdapter();}
 public static boolean bluetoothAllowed(Context c){return Build.VERSION.SDK_INT<31||c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED;}
 public static int wifiState(Context c){try{WifiManager w=(WifiManager)c.getSystemService(Context.WIFI_SERVICE);if(w==null)return ABSENT;switch(w.getWifiState()){case WifiManager.WIFI_STATE_ENABLED:return ON;case WifiManager.WIFI_STATE_DISABLED:return OFF;case WifiManager.WIFI_STATE_ENABLING:case WifiManager.WIFI_STATE_DISABLING:return CHANGING;default:return UNKNOWN;}}catch(RuntimeException denied){return UNKNOWN;}}
 public static int bluetoothState(Context c){try{if(!bluetoothAllowed(c))return UNKNOWN;BluetoothAdapter a=adapter(c);if(a==null)return ABSENT;switch(a.getState()){case BluetoothAdapter.STATE_ON:return ON;case BluetoothAdapter.STATE_OFF:return OFF;case BluetoothAdapter.STATE_TURNING_ON:case BluetoothAdapter.STATE_TURNING_OFF:return CHANGING;default:return UNKNOWN;}}catch(RuntimeException denied){return UNKNOWN;}}
 public static void wifiSettings(Activity a){RadioScreens.open(a,true);}
 /** Physical Wi-Fi association; internet validation and VPN routing do not determine the icon. */
 public static int wifiConnection(Context c){try{android.net.ConnectivityManager manager=(android.net.ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);android.net.NetworkInfo network=manager==null?null:manager.getNetworkInfo(android.net.ConnectivityManager.TYPE_WIFI);return manager==null?UNKNOWN:network!=null&&network.isConnected()?1:0;}catch(RuntimeException denied){return UNKNOWN;}}
}
