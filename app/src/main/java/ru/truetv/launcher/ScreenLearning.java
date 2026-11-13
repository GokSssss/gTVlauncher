package ru.truetv.launcher;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import java.util.concurrent.*;

/** Explicit two-minute window capture. Keeps one component in memory; never records a process history or screen content. */
final class ScreenLearning {
    private static final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<Runnable>(8),new ThreadPoolExecutor.DiscardOldestPolicy());
    private static Result pending;private static boolean startNotice;
    private final ControlService service;private final Runnable idle;private final Handler ui=new Handler(Looper.getMainLooper());
    private volatile int generation;private long expires;private boolean capturing,finishing,wifi,backDown;private String seen;private volatile ComponentName candidate;
    private final Runnable timeout=()->finish(true);
    private static final class Result {final boolean wifi;final Intent intent;final String error;Result(boolean wifi,Intent intent,String error){this.wifi=wifi;this.intent=intent;this.error=error;}}
    ScreenLearning(ControlService service,Runnable idle){this.service=service;this.idle=idle;}
    boolean active(){return capturing||finishing;}
    static void start(Activity owner,boolean wifi){ControlService service=ControlService.connected();if(service==null){ActionSheet.show(owner,Ui.word(owner,"Нужны специальные возможности","Accessibility is required"),new ActionSheet.Item(Ui.word(owner,"Включить службу управления лаунчера","Enable the launcher control service"),"access",()->Controls.openAccessibility(owner)));return;}service.beginLearning(owner,wifi);}
    boolean begin(Activity owner,boolean kind){AccessibilityServiceInfo info=service.getServiceInfo();if(info==null||(info.getCapabilities()&AccessibilityServiceInfo.CAPABILITY_CAN_REQUEST_FILTER_KEY_EVENTS)==0){Ui.toast(owner,"Отключите и снова включите службу лаунчера в специальных возможностях, затем повторите.","Turn the launcher Accessibility service off and back on, then try again.");return false;}
        java.util.ArrayList<String> packages=new java.util.ArrayList<>();packages.add(service.getPackageName());try{for(ApplicationInfo app:service.getPackageManager().getInstalledApplications(0))if((app.flags&ApplicationInfo.FLAG_SYSTEM)!=0)packages.add(app.packageName);}catch(RuntimeException unavailable){Ui.toast(owner,"Android не предоставил список системных приложений.","Android did not expose system applications.");return false;}
        synchronized(this){generation++;candidate=null;}capturing=true;finishing=false;wifi=kind;expires=SystemClock.elapsedRealtime()+120000;backDown=false;seen=null;candidate=null;pending=null;startNotice=true;
        info.eventTypes=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;info.packageNames=packages.toArray(new String[0]);info.flags|=AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;service.setServiceInfo(info);ui.postDelayed(timeout,120000);
        owner.startActivity(HomeActivity.homeIntent(owner));return true;
    }
    void window(AccessibilityEvent event){if(capturing&&SystemClock.elapsedRealtime()>=expires){finish(true);return;}if(!capturing||event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED||event.getPackageName()==null||event.getClassName()==null)return;String pkg=event.getPackageName().toString(),name=event.getClassName().toString();if(pkg.equals(service.getPackageName())||pkg.equals("android"))return;String key=pkg+"/"+name;if(key.equals(seen))return;seen=key;final int ticket=generation;final ComponentName component=new ComponentName(pkg,name.startsWith(".")?pkg+name:name);
        worker.execute(()->{if(ticket!=generation)return;try{ActivityInfo info=service.getPackageManager().getActivityInfo(component,0);synchronized(this){if(ticket==generation)candidate=(info.applicationInfo.flags&ApplicationInfo.FLAG_SYSTEM)!=0?component:null;}}catch(PackageManager.NameNotFoundException|RuntimeException ignored){}});
    }
    boolean key(KeyEvent event){if(capturing&&SystemClock.elapsedRealtime()>=expires){finish(true);return false;}if(!capturing||event.getKeyCode()!=KeyEvent.KEYCODE_BACK)return false;if(event.getAction()==KeyEvent.ACTION_DOWN){backDown=true;return true;}if(event.getAction()==KeyEvent.ACTION_UP&&backDown){finish(false);return true;}return false;}
    boolean finish(boolean expired){if(!capturing)return finishing;capturing=false;finishing=true;backDown=false;ui.removeCallbacks(timeout);idle.run();final int ticket=generation;final boolean kind=wifi,timedOut=expired||SystemClock.elapsedRealtime()>=expires;
        worker.execute(()->{ComponentName last=candidate;Intent intent=last==null?null:new Intent(kind?"android.settings.WIFI_SETTINGS":"android.settings.BLUETOOTH_SETTINGS").setComponent(last);if(intent!=null&&!RadioScreens.valid(service,intent))intent=null;final Intent selected=timedOut?null:intent;final String error=timedOut?Ui.word(service,"Время запоминания истекло. Попробуйте ещё раз.","Capture timed out. Try again."):selected==null?Ui.word(service,"Android не предоставил доступный переход к этому окну.","Android did not expose an accessible route to this window."):null;
            ui.post(()->{if(ticket!=generation)return;finishing=false;candidate=null;seen=null;startNotice=false;pending=new Result(kind,selected,error);service.startActivity(HomeActivity.homeIntent(service));});
        });return true;
    }
    void cancel(){synchronized(this){generation++;candidate=null;}capturing=finishing=backDown=false;seen=null;pending=null;startNotice=false;ui.removeCallbacks(timeout);idle.run();}
    static void offer(HomeActivity home){if(home.header==null||!home.hasWindowFocus()||home.isFinishing()||home.isDestroyed())return;if(startNotice){startNotice=false;home.showHeaderMessage(Ui.word(home,"Откройте нужный раздел настроек. Нажмите «Назад», чтобы закончить запоминание. Максимум 2 минуты.","Open the settings screen you need. Press Back to finish capture. Two-minute limit."));}
        Result result=pending;if(result==null)return;pending=null;String kind=result.wifi?"Wi-Fi":"Bluetooth";
        if(result.intent==null){ActionSheet.show(home,result.error,new ActionSheet.Item(Ui.word(home,"Выбрать экран из списка","Choose a screen from the list"),"settings",()->RadioScreens.configure(home,result.wifi)));return;}
        ActionSheet.show(home,Ui.word(home,"Проверка окна ","Test ")+kind,new ActionSheet.Item(Ui.word(home,"Проверить запомненный экран","Test the captured screen"),"check",()->RadioScreens.testCaptured(home,result.wifi,result.intent)),new ActionSheet.Item(Ui.word(home,"Выбрать другой экран из списка","Choose another screen from the list"),"replace",()->RadioScreens.configure(home,result.wifi)));
    }
}
