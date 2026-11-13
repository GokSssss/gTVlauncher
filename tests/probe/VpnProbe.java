package ru.truetv.probe;
import android.app.*;import android.content.*;import android.net.*;import android.os.*;
/** Disposable VM black-hole VPN. No owner device use, no forwarding or packet capture. */
public final class VpnProbe extends VpnService {
 private ParcelFileDescriptor tunnel;
 public int onStartCommand(Intent i,int f,int id){if(!Build.FINGERPRINT.contains("generic")&&!Build.MODEL.contains("Android SDK")){stopSelf();return START_NOT_STICKY;}try{NotificationManager n=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=26)n.createNotificationChannel(new NotificationChannel("vpn-fixture","VPN fixture",NotificationManager.IMPORTANCE_LOW));Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"vpn-fixture"):new Notification.Builder(this);startForeground(9,b.setSmallIcon(android.R.drawable.ic_lock_lock).setContentTitle("VM test VPN").build());Builder vpn=new Builder().setSession("VM-only blocked default route").addAddress("10.77.0.1",32).addRoute("0.0.0.0",0).addAllowedApplication("ru.truetv.totv").addAllowedApplication("ru.truetv.launcher");if(i.getBooleanExtra("bypass",false))vpn.allowBypass();tunnel=vpn.establish();if(tunnel==null)throw new IllegalStateException("VPN not authorized");}catch(Exception e){android.util.Log.e("VpnFixture236","establish",e);stopSelf();}return START_NOT_STICKY;}
 public void onDestroy(){try{if(tunnel!=null)tunnel.close();}catch(Exception ignored){}super.onDestroy();}
}
