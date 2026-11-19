package ru.truetv.bridge.android;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import java.io.*;

/** Legacy Downloads read access, only through exact user-granted URIs. No directory or write access. */
public final class ReceivedFiles extends ContentProvider {
 public boolean onCreate(){return true;}
 private static File root()throws IOException{return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getCanonicalFile();}
 static Uri uri(Context c,File file)throws IOException{if(!file.getCanonicalFile().getParentFile().equals(root()))throw new IOException("Outside Downloads");return new Uri.Builder().scheme("content").authority(c.getPackageName()+".received").appendPath(file.getName()).build();}
 private File file(Uri uri)throws FileNotFoundException{try{if(uri.getPathSegments().size()!=1)throw new IOException();String name=uri.getLastPathSegment();if(name==null||name.isEmpty()||name.equals(".")||name.equals("..")||name.contains("/")||name.contains("\\"))throw new IOException();File file=new File(root(),name);if(!file.getCanonicalFile().getParentFile().equals(root())||!file.isFile())throw new IOException();return file;}catch(IOException|RuntimeException bad){throw new FileNotFoundException("Received file unavailable");}}
 public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("Read only");return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
 public String getType(Uri uri){String name=uri.getLastPathSegment();int dot=name==null?-1:name.lastIndexOf('.');String extension=dot<0?"":name.substring(dot+1).toLowerCase(java.util.Locale.ROOT);String type=MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);return type==null?"application/octet-stream":type;}
 public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){try{File f=file(uri);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor cursor=new MatrixCursor(columns);Object[] row=new Object[columns.length];for(int i=0;i<columns.length;i++)row[i]=OpenableColumns.DISPLAY_NAME.equals(columns[i])?f.getName():OpenableColumns.SIZE.equals(columns[i])?f.length():null;cursor.addRow(row);return cursor;}catch(FileNotFoundException unavailable){return new MatrixCursor(projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection);}}
 public Uri insert(Uri u,ContentValues v){throw new UnsupportedOperationException();}public int update(Uri u,ContentValues v,String s,String[] a){throw new UnsupportedOperationException();}public int delete(Uri u,String s,String[] a){throw new UnsupportedOperationException();}
}
