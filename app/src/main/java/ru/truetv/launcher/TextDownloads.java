package ru.truetv.launcher;
import android.content.Context;
import android.net.Uri;
import java.io.*;
import ru.truetv.bridge.android.TextFiles;
/** Existing TV API, backed by the shared explicit TXT saver. */
final class TextDownloads {
 static final class Saved {final Uri uri;final String name;Saved(Uri u,String n){uri=u;name=n;}}
 static void cleanup(Context c)throws IOException {TextFiles.cleanup(c,file->FileStore.get(c).uri(FileStore.get(c).identify(file)));}
 static Saved save(Context c,String text)throws IOException {TextFiles.Saved saved=TextFiles.save(c,text,file->FileStore.get(c).uri(FileStore.get(c).identify(file)));return new Saved(saved.uri,saved.name);}
}
