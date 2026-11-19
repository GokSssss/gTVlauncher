package ru.truetv.launcher;
import android.content.Intent;
import android.net.Uri;
import java.io.*;
import ru.truetv.bridge.android.ReceiveActivity;
/** TV entry uses the same visible-window receiver as To TV on phones. */
public final class ShareSettingsActivity extends ReceiveActivity {
 protected void prepareAppearance(){Ui.prepare(this);}
 protected Uri legacyUri(File file)throws IOException {return FileStore.get(this).uri(FileStore.get(this).identify(file));}
 protected void showDownloads(){startActivity(new Intent(this,FileActivity.class).putExtra("downloads",true));finish();}
}
