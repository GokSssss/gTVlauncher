package ru.truetv.launcher;
import android.content.Context;
import android.net.Uri;
import java.io.*;
import ru.truetv.bridge.Wire;
import ru.truetv.bridge.android.PublicDownloads;
/** TV keeps its existing document URIs; storage is shared with the phone receiver. */
final class BridgeDownloads implements Wire.Store {
 interface Published {void saved(Uri uri,String name,String mime);}
 private final PublicDownloads store;
 BridgeDownloads(Context c)throws IOException {this(c,null);}
 BridgeDownloads(Context c,Published callback)throws IOException {this(c,callback,"totv_pending");}
 BridgeDownloads(Context c,Published callback,String journal)throws IOException {store=new PublicDownloads(c,callback==null?null:callback::saved,journal,file->FileStore.get(c).uri(FileStore.get(c).identify(file)));}
 public Wire.Target create(Wire.Item item)throws IOException {return store.create(item);}
}
