package ru.truetv.bridge.android;
import android.content.Context;
import android.net.Uri;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import ru.truetv.bridge.*;

/** Explicit Save TXT uses its own journal so reception of another file cannot delete its stage. */
public final class TextFiles {
 private TextFiles(){}
 public static final class Saved {public final Uri uri;public final String name;Saved(Uri u,String n){uri=u;name=n;}}
 public static synchronized void cleanup(Context c,PublicDownloads.LegacyUri uri)throws IOException {new PublicDownloads(c,null,"totv_text_pending",uri);}
 public static synchronized Saved save(Context c,String value,PublicDownloads.LegacyUri uri)throws IOException {
  byte[] bytes=SharedText.encode(value);String name="Text-"+new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.ROOT).format(new Date())+".txt";Saved[] saved=new Saved[1];
  Wire.Target target=new PublicDownloads(c,(u,n,m)->saved[0]=new Saved(u,n),"totv_text_pending",uri).create(new Wire.Item(name,"text/plain",bytes.length));
  try{try(OutputStream out=target.stream()){out.write(bytes);}target.publish();return saved[0];}catch(IOException|RuntimeException e){target.discard();throw e;}
 }
}
