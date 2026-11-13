package ru.truetv.launcher;
import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import ru.truetv.bridge.*;

/** Explicit Save TXT uses its own journal so reception of another file cannot delete its stage. */
final class TextDownloads {
 private TextDownloads(){}
 static final class Saved {final Uri uri;final String name;Saved(Uri u,String n){uri=u;name=n;}}
 static synchronized void cleanup(Context c)throws IOException {new BridgeDownloads(c,null,"totv_text_pending");}
 static synchronized Saved save(Context c,String value)throws IOException {
  byte[] bytes=SharedText.encode(value);String name="Text-"+new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.ROOT).format(new Date())+".txt";Saved[] saved=new Saved[1];
  Wire.Target target=new BridgeDownloads(c,(u,n,m)->saved[0]=new Saved(u,n),"totv_text_pending").create(new Wire.Item(name,"text/plain",bytes.length));
  try{try(OutputStream out=target.stream()){out.write(bytes);}target.publish();return saved[0];}catch(IOException|RuntimeException e){target.discard();throw e;}
 }
}
