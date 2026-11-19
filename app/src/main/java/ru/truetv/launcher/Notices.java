package ru.truetv.launcher;

import android.app.*;
import android.content.*;
import android.os.*;
import android.provider.Settings;
import android.service.notification.*;
import java.lang.ref.WeakReference;
import java.util.*;

/** Android keeps the live list. No notification history, disk storage, networking or polling. */
public final class Notices extends NotificationListenerService {
    interface Listener { void noticesChanged(); }
    static final class Entry {
        final PendingIntent intent;final boolean clearable;final int id;
        final String key,pkg,title,text,group;
        final long time;
        final boolean summary;
        Entry(StatusBarNotification value){
            clearable=value.isClearable();id=value.getId();key=value.getKey();pkg=value.getPackageName();time=value.getPostTime();group=value.getGroupKey();
            Notification n=value.getNotification();intent=n.contentIntent;summary=(n.flags&Notification.FLAG_GROUP_SUMMARY)!=0;
            String heading="",body="";
            try{if(n.extras!=null){heading=plain(n.extras.getCharSequence(Notification.EXTRA_TITLE),256);body=plain(n.extras.getCharSequence(Notification.EXTRA_BIG_TEXT),16384);if(body.isEmpty())body=plain(n.extras.getCharSequence(Notification.EXTRA_TEXT),16384);}}catch(RuntimeException ignored){}
            title=heading;text=body;
        }
        private static String plain(CharSequence text,int max){if(text==null)return "";String s=text.toString();if(s.length()<=max)return s;int end=max;if(Character.isHighSurrogate(s.charAt(end-1)))end--;return s.substring(0,end)+"…";}
    }
    private static final Object lock=new Object();
    private static final LinkedHashMap<String,Entry> live=new LinkedHashMap<>();
    private static final ArrayList<WeakReference<Listener>> listeners=new ArrayList<>();
    private static final Handler main=new Handler(Looper.getMainLooper());
    private static Notices active;
    static boolean granted(Context c){
        ComponentName component=new ComponentName(c,Notices.class);
        try{if(Build.VERSION.SDK_INT>=27)return ((NotificationManager)c.getSystemService(NOTIFICATION_SERVICE)).isNotificationListenerAccessGranted(component);
            String raw=Settings.Secure.getString(c.getContentResolver(),"enabled_notification_listeners");if(raw!=null)for(String value:raw.split(":"))if(component.equals(ComponentName.unflattenFromString(value)))return true;
        }catch(RuntimeException ignored){}return false;
    }
    static boolean connected(Context c){synchronized(lock){if(active==null)return false;}return granted(c);}
    static int count(Context c){return connected(c)?rows(false).size():-1;}
    static List<Entry> snapshot(Context c){return connected(c)?rows(true):Collections.emptyList();}
    private static ArrayList<Entry> rows(boolean sort){
        ArrayList<Entry> source;synchronized(lock){source=new ArrayList<>(live.values());}
        HashSet<String> children=new HashSet<>();for(Entry e:source)if(!e.summary)children.add(e.group);
        ArrayList<Entry> result=new ArrayList<>();for(Entry e:source)if(!e.summary||!children.contains(e.group))result.add(e);
        if(sort)Collections.sort(result,(a,b)->Long.compare(b.time,a.time));return result;
    }
    static void listen(Listener value){synchronized(lock){unlisten(value);listeners.add(new WeakReference<>(value));}value.noticesChanged();}
    static void unlisten(Listener value){synchronized(lock){for(Iterator<WeakReference<Listener>> it=listeners.iterator();it.hasNext();){Listener other=it.next().get();if(other==null||other==value)it.remove();}}}
    private static void changed(){synchronized(lock){if(listeners.isEmpty())return;}main.post(()->{ArrayList<Listener> copy=new ArrayList<>();synchronized(lock){for(Iterator<WeakReference<Listener>> it=listeners.iterator();it.hasNext();)if(it.next().get()==null)it.remove();for(WeakReference<Listener> ref:listeners){Listener value=ref.get();if(value!=null)copy.add(value);}}for(Listener value:copy)value.noticesChanged();});}
    public void onListenerConnected(){synchronized(lock){active=this;live.clear();}try{StatusBarNotification[] values=getActiveNotifications();if(values!=null)for(StatusBarNotification value:values)put(value);}catch(RuntimeException ignored){disconnected();return;}changed();}
    public void onNotificationPosted(StatusBarNotification value){if(value!=null){put(value);changed();}}
    private void put(StatusBarNotification value){try{Entry entry=new Entry(value);synchronized(lock){if(active==this)live.put(entry.key,entry);}}catch(RuntimeException ignored){}}
    public void onNotificationRemoved(StatusBarNotification value){if(value!=null){synchronized(lock){if(active==this)live.remove(value.getKey());}changed();}}
    public void onListenerDisconnected(){disconnected();}
    public void onDestroy(){disconnected();super.onDestroy();}
    private void disconnected(){synchronized(lock){if(active==this){active=null;live.clear();}}changed();}
    static void dismiss(Entry value){Notices service;synchronized(lock){service=active;}if(service!=null&&value.clearable)try{service.cancelNotification(value.key);}catch(RuntimeException ignored){}}
    static void reconnect(Activity c){if(!granted(c)||connected(c))return;if(Build.VERSION.SDK_INT>=24)try{requestRebind(new ComponentName(c,Notices.class));return;}catch(RuntimeException ignored){}settings(c);}
    static void settings(Activity c){try{c.startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));}catch(RuntimeException unavailable){Ui.toast(c,"Откройте в системных настройках доступ к уведомлениям и разрешите True TV Launcher.","Open Notification access in system settings and allow True TV Launcher.");}}
}
