package ru.truetv.launcher;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.nsd.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import ru.truetv.bridge.*;
import ru.truetv.bridge.android.*;

/** A visible receive window owns the socket. Leaving it closes discovery, consent and payload. */
public final class ShareSettingsActivity extends Activity {
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final Object lock=new Object();
 private final TransferWake wake=new TransferWake();
 private volatile Transfer.Receiver receiver;
 private volatile boolean visible;
 private volatile int generation;
 private LanNetwork.Multicast multicast;private NsdManager nsd;private NsdManager.RegistrationListener registration;
 private TextView state,details;private CheckBox automatic;private ProgressBar progress;
 private Button accept,reject,cancel,open,location,saveText,copyText;private Decision decision;
 private final ArrayList<Received> received=new ArrayList<>();
 private int textRevision;private boolean permissionAsked,savingText,textSaved;private String receivedText,pendingTextSave;
 private static final class Received {final Uri uri;final String name,mime;Received(Uri u,String n,String m){uri=u;name=n;mime=m;}}
 private String w(String ru,String en){return BridgeUi.w(this,ru,en);}
 protected void onCreate(Bundle saved){Ui.prepare(this);super.onCreate(saved);requestWindowFeature(Window.FEATURE_NO_TITLE);BridgeUi.top(this);
  // Old versions offered permanent background reception. It no longer exists.
  getSharedPreferences("totv_receiver",0).edit().clear().apply();getSharedPreferences("totv_trust",0).edit().clear().apply();
  LinearLayout body=BridgeUi.box(this);body.addView(BridgeUi.label(this,w("Принять файл или текст по Wi-Fi","Receive a file or text over Wi-Fi"),23));
  body.addView(BridgeUi.label(this,w("На телефоне: Отправить → To TV → выберите телевизор. Файлы сохраняются в Downloads. Приём работает только пока это окно открыто.","On phone: Share → To TV → choose this TV. Files go to Downloads. Reception works only while this window is open."),16));
  automatic=new CheckBox(this);automatic.setText(w("Принимать без подтверждения","Accept without confirmation"));automatic.setTextColor(android.graphics.Color.WHITE);automatic.setTextSize(17);automatic.setChecked(getSharedPreferences("totv_window",0).getBoolean("automatic",false));automatic.setOnCheckedChangeListener((b,on)->getSharedPreferences("totv_window",0).edit().putBoolean("automatic",on).apply());body.addView(automatic);
  state=BridgeUi.label(this,w("Подготовка…","Preparing…"),19);body.addView(state);details=BridgeUi.label(this,"",16);body.addView(details);
  progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(1000);progress.setVisibility(View.GONE);body.addView(progress,new LinearLayout.LayoutParams(-1,BridgeUi.dp(this,14)));
  accept=BridgeUi.button(this,w("Принять","Accept"),()->answer(true));reject=BridgeUi.button(this,w("Отклонить","Decline"),()->answer(false));cancel=BridgeUi.button(this,w("Отменить передачу","Cancel transfer"),this::cancelTransfer);open=BridgeUi.button(this,w("Открыть файл","Open file"),this::openFile);location=BridgeUi.button(this,w("Показать в папке","Show in folder"),()->{startActivity(new Intent(this,FileActivity.class).putExtra("downloads",true));finish();});
  saveText=BridgeUi.button(this,w("Сохранить в TXT","Save as TXT"),this::saveText);copyText=BridgeUi.button(this,w("Скопировать в буфер обмена","Copy to clipboard"),this::copyText);
  for(Button b:new Button[]{accept,reject,cancel,open,location,saveText,copyText}){b.setVisibility(View.GONE);body.addView(b);}body.addView(BridgeUi.button(this,w("Закрыть","Close"),this::finish));ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.addView(body);setContentView(scroll);
 }
 protected void onResume(){super.onResume();visible=true;if(storage()||permissionAsked)startReceiver();else if(!permissionAsked){permissionAsked=true;requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"},40);}else state.setText(w("Для сохранения файлов разрешите доступ к хранилищу.","Allow storage access to save files."));}
 protected void onPause(){visible=false;stopReceiver();super.onPause();}
 protected void onDestroy(){stopReceiver();worker.shutdownNow();super.onDestroy();}
 private boolean storage(){return Build.VERSION.SDK_INT<23||Build.VERSION.SDK_INT>=29||checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE")==PackageManager.PERMISSION_GRANTED;}
 public void onRequestPermissionsResult(int code,String[] names,int[] values){super.onRequestPermissionsResult(code,names,values);if(code==40&&visible)startReceiver();else if(code==41){String text=pendingTextSave;pendingTextSave=null;if(storage()&&text!=null)writeText(text);else Ui.toast(this,"Для сохранения TXT разрешите доступ к хранилищу","Allow storage access to save TXT");}}
 private void post(int g,Runnable action){main.post(()->{if(visible&&generation==g&&!isFinishing())action.run();});}
 private void startReceiver(){final int g;synchronized(lock){if(receiver!=null||registration!=null)return;g=++generation;}
  automatic.setEnabled(true);accept.setVisibility(View.GONE);reject.setVisibility(View.GONE);cancel.setVisibility(View.GONE);cancel.setEnabled(true);progress.setVisibility(View.GONE);state.setText(w("Запуск приёма…","Starting reception…"));worker.execute(()->{try{
   Identity identity=Identity.load(new File(getFilesDir(),"to-tv-identity"));
   if(storage()){new BridgeDownloads(this);TextDownloads.cleanup(this);}String label=BridgeUi.device(this);Transfer.Receiver next=new Transfer.Receiver(identity,label,item->destination(g,item),(offer,fp,code)->consent(g,offer),new Transfer.Events(){public void update(int index,String name,long bytes,long total){post(g,()->{state.setText(w("Приём: ","Receiving: ")+name);details.setText(BridgeUi.bytes(ShareSettingsActivity.this,bytes)+(total<0?"":" / "+BridgeUi.bytes(ShareSettingsActivity.this,total)));progress.setIndeterminate(total<0);if(total>=0)progress.setProgress(total==0?1000:(int)Math.min(1000,1000d*bytes/total));});}public void finished(boolean success,String detail){post(g,()->complete(success,detail));}});
   synchronized(lock){if(!visible||g!=generation){next.close();return;}receiver=next;}
   String fingerprint=identity.fingerprint(),lan=LanNetwork.advertise(this);post(g,()->advertise(g,fingerprint,next.port(),label,lan));next.accept();
  }catch(Exception e){post(g,()->{state.setText(BridgeUi.error(this,e));stopReceiver();});}});
 }
 private void advertise(int g,String fingerprint,int port,String label,String lan){multicast=new LanNetwork.Multicast(this);nsd=(NsdManager)getSystemService(NSD_SERVICE);NsdServiceInfo info=new NsdServiceInfo();info.setServiceType(Wire.TYPE);info.setServiceName("TrueTV-"+fingerprint.substring(0,12));info.setPort(port);info.setAttribute("id",fingerprint);info.setAttribute("name",label);info.setAttribute("text","1");if(!lan.isEmpty())info.setAttribute("lan4",lan);registration=new NsdManager.RegistrationListener(){public void onServiceRegistered(NsdServiceInfo i){post(g,()->state.setText(w("Готов к приёму · ","Ready to receive · ")+label));}public void onServiceUnregistered(NsdServiceInfo i){}public void onRegistrationFailed(NsdServiceInfo i,int error){post(g,()->{state.setText(w("Не удалось объявить приёмник в сети: ","Could not advertise receiver: ")+error);stopReceiver();});}public void onUnregistrationFailed(NsdServiceInfo i,int error){}};try{nsd.registerService(info,NsdManager.PROTOCOL_DNS_SD,registration);}catch(RuntimeException e){state.setText(BridgeUi.error(this,e));stopReceiver();}}
 private Wire.Target destination(int g,Wire.Item item)throws IOException {if(SharedText.MIME.equals(item.mime))return SharedText.target(item,value->post(g,()->receivedText=value));if(!storage())throw new IOException(w("Для файлов разрешите доступ к хранилищу","Allow storage access to receive files"));return new BridgeDownloads(this,(uri,name,mime)->post(g,()->received.add(new Received(uri,name,mime)))).create(item);}
 private boolean consent(int g,Wire.Offer offer)throws Exception {final boolean text=SharedText.isText(offer);Decision answer=new Decision();main.post(()->{if(!visible||g!=generation||isFinishing()){answer.answer(0);return;}received.clear();textRevision++;receivedText=null;textSaved=false;saveText.setVisibility(View.GONE);copyText.setVisibility(View.GONE);decision=answer;automatic.setEnabled(false);open.setVisibility(View.GONE);location.setVisibility(View.GONE);details.setMaxLines(6);details.setText((text?w("Текст · ","Text · "):offer.files.size()+w(" файлов · "," files · "))+BridgeUi.bytes(this,offer.size)+(text?"":"\n"+offer.files.get(0).name));state.setText(w("Принять от ","Accept from ")+offer.device+"?");if(automatic.isChecked())answer(true);else{accept.setVisibility(View.VISIBLE);reject.setVisibility(View.VISIBLE);accept.requestFocus();}});return answer.await(120)==1&&visible&&generation==g;}
 private void answer(boolean yes){Decision d=decision;if(d==null)return;decision=null;accept.setVisibility(View.GONE);reject.setVisibility(View.GONE);if(yes){wake.start(this);state.setText(w("Приём файла…","Receiving file…"));progress.setIndeterminate(true);progress.setVisibility(View.VISIBLE);cancel.setVisibility(View.VISIBLE);cancel.requestFocus();}d.answer(yes?1:0);}
 private void cancelTransfer(){if(decision!=null)answer(false);Transfer.Receiver r=receiver;if(r!=null)r.cancel();wake.stop();cancel.setEnabled(false);}
 private void complete(boolean success,String detail){wake.stop();decision=null;automatic.setEnabled(true);accept.setVisibility(View.GONE);reject.setVisibility(View.GONE);progress.setVisibility(View.GONE);cancel.setVisibility(View.GONE);cancel.setEnabled(true);if(success&&receivedText!=null){state.setText(w("Текст получен","Text received"));details.setMaxLines(6);details.setEllipsize(android.text.TextUtils.TruncateAt.END);details.setText(preview(receivedText));saveText.setVisibility(View.VISIBLE);saveText.setEnabled(!savingText&&!textSaved);copyText.setVisibility(View.VISIBLE);saveText.requestFocus();return;}state.setText(success?w("Файл получен","File received"):detail.contains("REJECTED")?w("Приём отклонён","Reception declined"):w("Передача не завершена","Transfer incomplete"));StringBuilder names=new StringBuilder();for(Received f:received){if(names.length()>0)names.append("\n");names.append(f.name);}details.setText((success?"":BridgeUi.error(this,new IOException(detail))+"\n")+names+ (received.isEmpty()?"":"\nDownloads"));open.setVisibility(received.isEmpty()?View.GONE:View.VISIBLE);location.setVisibility(received.isEmpty()?View.GONE:View.VISIBLE);if(!received.isEmpty())open.requestFocus();}
 private static String preview(String text){if(text.length()<=4096)return text;int end=4096;if(Character.isHighSurrogate(text.charAt(end-1)))end--;return text.substring(0,end)+"…";}
 private void copyText(){if(receivedText==null)return;try{((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("To TV",receivedText));Ui.toast(this,"Скопировано в буфер обмена","Copied to clipboard");}catch(RuntimeException e){Ui.toast(this,"Не удалось скопировать текст","Could not copy text");}}
 private void saveText(){if(receivedText==null||savingText||textSaved)return;if(!storage()){pendingTextSave=receivedText;requestPermissions(new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"},41);return;}writeText(receivedText);}
 private void writeText(String text){if(savingText)return;final int revision=textRevision;savingText=true;saveText.setEnabled(false);new Thread(()->{try{TextDownloads.Saved file=TextDownloads.save(getApplicationContext(),text);main.post(()->{savingText=false;if(textRevision==revision)textSaved=true;if(visible&&!isFinishing()){if(textRevision==revision)state.setText(w("Сохранено в Downloads: ","Saved to Downloads: ")+file.name);saveText.setEnabled(receivedText!=null&&!textSaved);}});}catch(Exception e){main.post(()->{savingText=false;if(visible&&!isFinishing()){if(textRevision==revision)state.setText(BridgeUi.error(this,e));saveText.setEnabled(receivedText!=null&&!textSaved);}});}},"ToTV-save-text").start();}
 private void openFile(){if(received.isEmpty())return;if(received.size()==1){open(received.get(0));return;}String[] names=new String[received.size()];for(int i=0;i<names.length;i++)names[i]=received.get(i).name;new AlertDialog.Builder(this).setTitle(w("Открыть файл","Open file")).setItems(names,(d,i)->open(received.get(i))).show();}
 private void open(Received file){Intent view=new Intent(Intent.ACTION_VIEW).setDataAndType(file.uri,file.mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);view.setClipData(ClipData.newRawUri(file.name,file.uri));try{startActivity(view);finish();}catch(RuntimeException e){new AlertDialog.Builder(this).setMessage(w("Нет приложения для открытия этого файла.","No app can open this file.")).setPositiveButton("OK",null).show();}}
 private void stopReceiver(){Transfer.Receiver old;synchronized(lock){generation++;old=receiver;receiver=null;}if(decision!=null){decision.answer(0);decision=null;}wake.stop();if(old!=null)try{old.close();}catch(IOException ignored){}if(nsd!=null&&registration!=null)try{nsd.unregisterService(registration);}catch(RuntimeException ignored){}registration=null;if(multicast!=null){multicast.close();multicast=null;}}
}
