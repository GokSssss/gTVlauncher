package ru.truetv.bridge.android;
import android.content.Context;
import android.net.*;
import android.net.wifi.WifiManager;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.SocketFactory;

/** Select one physical route for one LAN transfer; never changes the process default or VPN. */
public final class LanNetwork {
 private LanNetwork(){}
 private static final class Link {final Network network;final LinkProperties properties;final boolean lan;Link(Network n,LinkProperties p,boolean l){network=n;properties=p;lan=l;}}
 public static final class Route {public final InetAddress host;public final SocketFactory sockets;Route(InetAddress h,SocketFactory f){host=h;sockets=f;}}
 private static List<Link> links(Context c){ArrayList<Link> links=new ArrayList<>();ConnectivityManager manager=(ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);if(manager==null)return links;for(Network network:manager.getAllNetworks()){NetworkCapabilities cap=manager.getNetworkCapabilities(network);LinkProperties properties=manager.getLinkProperties(network);if(cap!=null&&properties!=null&&!cap.hasTransport(NetworkCapabilities.TRANSPORT_VPN))links.add(new Link(network,properties,cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)||cap.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)));}return links;}
 private static int match(Link link,InetAddress host){int best=0;byte[] address=host.getAddress();for(LinkAddress own:link.properties.getLinkAddresses()){byte[] local=own.getAddress().getAddress();int bits=own.getPrefixLength();if(local.length!=address.length||bits<(address.length==4?8:32))continue;boolean same=true;for(int i=0;i<bits;i++)if((local[i/8]&(128>>(i%8)))!=(address[i/8]&(128>>(i%8)))){same=false;break;}if(same)best=Math.max(best,bits);}return best;}
 private static InetAddress ipv4(String value){if(value==null||value.length()>15)return null;String[] parts=value.split("\\.",-1);if(parts.length!=4)return null;byte[] bytes=new byte[4];try{for(int i=0;i<4;i++){if(!parts[i].matches("[0-9]{1,3}"))return null;int n=Integer.parseInt(parts[i]);if(n>255)return null;bytes[i]=(byte)n;}InetAddress ip=InetAddress.getByAddress(bytes);return ip.isLoopbackAddress()||ip.isAnyLocalAddress()||ip.isMulticastAddress()?null:ip;}catch(Exception ignored){return null;}}
 public static Route route(Context c,InetAddress discovered,String hints)throws IOException {
  if(discovered.isLoopbackAddress())return new Route(discovered,SocketFactory.getDefault());
  List<Link> links=links(c);Link selected=null;InetAddress host=discovered;int best=0;
  if(hints!=null&&hints.length()<=80)for(String hint:hints.split(",")){InetAddress address=ipv4(hint);if(address==null)continue;for(Link link:links){int bits=match(link,address);if(bits>best){best=bits;selected=link;host=address;}}}
  if(selected==null)for(Link link:links){int bits=match(link,discovered);if(bits>best){best=bits;selected=link;}}
  if(selected==null)for(Link link:links)if(link.lan&&!link.properties.getLinkAddresses().isEmpty()){selected=link;break;}
  if(selected==null)throw new IOException("No local Wi-Fi/Ethernet route. Allow LAN access in VPN settings.");
  return new Route(host,new PreferredSockets(selected.network.getSocketFactory()));
 }
 // Some VPNs forbid explicit network binding while permitting LAN via their normal routing rules.
 private static final class PreferredSockets extends SocketFactory {
  private final SocketFactory physical;PreferredSockets(SocketFactory p){physical=p;}
  public Socket createSocket()throws IOException{try{return physical.createSocket();}catch(SocketException denied){return new Socket();}}
  private Socket connect(InetAddress h,int p,InetAddress local,int lp)throws IOException{Socket s=createSocket();try{if(local!=null)s.bind(new InetSocketAddress(local,lp));s.connect(new InetSocketAddress(h,p),8000);return s;}catch(IOException e){try{s.close();}catch(IOException ignored){}throw e;}}
  public Socket createSocket(String h,int p)throws IOException{return connect(InetAddress.getByName(h),p,null,0);}
  public Socket createSocket(String h,int p,InetAddress l,int q)throws IOException{return connect(InetAddress.getByName(h),p,l,q);}
  public Socket createSocket(InetAddress h,int p)throws IOException{return connect(h,p,null,0);}
  public Socket createSocket(InetAddress h,int p,InetAddress l,int q)throws IOException{return connect(h,p,l,q);}
 }
 /** IPv4 hints avoid VPN-interface addresses from older OEM mDNS implementations. */
 public static String advertise(Context c){LinkedHashSet<String> addresses=new LinkedHashSet<>();try{for(Link link:links(c))if(link.lan)for(LinkAddress own:link.properties.getLinkAddresses()){InetAddress ip=own.getAddress();if(ip instanceof Inet4Address&&!ip.isLoopbackAddress()&&!ip.isAnyLocalAddress())addresses.add(ip.getHostAddress());if(addresses.size()>=4)break;}}catch(RuntimeException ignored){}StringBuilder out=new StringBuilder();for(String address:addresses){if(out.length()>0)out.append(',');out.append(address);if(out.length()>60)break;}return out.toString();}
 public static final class Multicast implements AutoCloseable {private WifiManager.MulticastLock lock;public Multicast(Context context){try{WifiManager wifi=(WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);if(wifi!=null){lock=wifi.createMulticastLock("ToTV-visible");lock.setReferenceCounted(false);lock.acquire();}}catch(RuntimeException unavailable){lock=null;}}public void close(){WifiManager.MulticastLock held=lock;lock=null;if(held!=null)try{if(held.isHeld())held.release();}catch(RuntimeException ignored){}}}
}
