import java.net.*;
import java.io.*;
import java.util.*;

public class IpTable{

    private static LinkedList ip_queue = new LinkedList();

    private InetAddress ip;
    private String path;
    
    public IpTable(InetAddress ip, String path){
	this.ip = ip;
	this.path = path;
    }
	
    public static void addLast(IpTable ips){
	ip_queue.addLast(ips);
    }

    public static int size(){
	return(ip_queue.size());
    }

    public static void removeFirst(){
	ip_queue.removeFirst();
    }

    public static IpTable getFirst(){
	return((IpTable)ip_queue.getFirst());
    }

    public static void printTab(IpTable iptab){
	System.out.println(iptab.ip + " " + iptab.path);
    }

}															
