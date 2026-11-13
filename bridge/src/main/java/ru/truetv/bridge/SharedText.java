package ru.truetv.bridge;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;

/** Plain shared text is distinct from a user-selected .txt file; never saved automatically. */
public final class SharedText {
 public static final String MIME="application/vnd.truetv.shared-text",EXTRA="totv.shared_text";
 public static final int MAX_BYTES=256*1024;
 private SharedText(){}
 public static byte[] encode(String value)throws IOException {
  if(value==null||value.length()>MAX_BYTES)throw new IOException("Shared text is too large (maximum 256 KB)");
  ByteBuffer encoded=StandardCharsets.UTF_8.newEncoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(value));
  if(encoded.remaining()>MAX_BYTES)throw new IOException("Shared text is too large (maximum 256 KB)");
  byte[] bytes=new byte[encoded.remaining()];encoded.get(bytes);return bytes;
 }
 public static Wire.Item item(byte[] bytes)throws IOException {if(bytes.length>MAX_BYTES)throw new IOException("Shared text too large");return new Wire.Item("Shared text.txt",MIME,bytes.length);}
 public static boolean isText(Wire.Offer offer)throws IOException {
  for(Wire.Item item:offer.files)if(MIME.equals(item.mime)){if(offer.files.size()!=1||item.size<0||item.size>MAX_BYTES)throw new IOException("Invalid shared text offer");return true;}
  return false;
 }
 public interface Received {void text(String value);}
 public static Wire.Target target(Wire.Item item,Received received)throws IOException {
  if(!MIME.equals(item.mime)||item.size<0||item.size>MAX_BYTES)throw new IOException("Invalid shared text size");
  return new Wire.Target(){final ByteArrayOutputStream bytes=new ByteArrayOutputStream((int)item.size);
   public OutputStream stream(){return new OutputStream(){public void write(int b)throws IOException {if(bytes.size()>=MAX_BYTES)throw new IOException("Shared text too large");bytes.write(b);}public void write(byte[] b,int off,int length)throws IOException {if(length<0||length>MAX_BYTES-bytes.size())throw new IOException("Shared text too large");bytes.write(b,off,length);}};}
   public String publish()throws IOException {if(bytes.size()!=item.size)throw new IOException("Incomplete shared text");String value=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())).toString();received.text(value);return "text";}
   public void discard(){bytes.reset();}
  };
 }
}
