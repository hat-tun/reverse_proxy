import java.net.*;
import java.io.*;
import java.util.*;

public class RequestPassThrough implements Runnable{
    private static String webserver ="192.168.74.141";
    private static LinkedList queue = new LinkedList();
    private File homeDir;

    public RequestPassThrough(File homeDir){
	this.homeDir = homeDir;
    }
    
    public static void reqProc(Socket s, int cnt){
	synchronized (queue){
	    queue.addLast(s);
	    System.out.println("connect:" + cnt + " " + queue);
	    queue.notifyAll();
	}
    }
    
    public void run(){
	while(true){
	    Socket sock;
	    synchronized (queue){
		while(queue.isEmpty()){
		    try{
			queue.wait();
		    }
		    catch (InterruptedException e){						
		    }
		}
		sock = (Socket) queue.removeFirst();
	    }
	    try{
		
		String filename;
		String indxname;
		String contentType;
		String version;
		
		BufferedOutputStream outs = new BufferedOutputStream(sock.getOutputStream());
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outs));
		BufferedInputStream in = new BufferedInputStream(sock.getInputStream());
		BufferedReader rd;
				
		Socket sock2 = new Socket(webserver,8000); // connect to web server
		
		BufferedOutputStream outs2 = new BufferedOutputStream(sock2.getOutputStream());
		
		byte[] buf3 = new byte[1024];
		byte[] buf4 = new byte[1024];

		// Request Transfer
  		in.read(buf3);//std input stream has no terminal???  So once read.
	    
		buf4 = buf3;
		
		String req = new String(buf3);
		String[] request = req.split("\r\n");
		String get = request[0]; //first line fetch
		
		StringTokenizer st = new StringTokenizer(get);
		String method = st.nextToken();
		if(method.equals("GET")){
		    filename = st.nextToken();
		    indxname = filename.substring(1, filename.length());
		    contentType = "text/html";
		    version = st.nextToken();
		    //String adr = homeDir + "\\" + indxname;
		    String adr = homeDir + "/" + indxname;
		    File file = new File(adr);
		    boolean flg = file.canRead();
		    Date now = new Date();
		    InetAddress ip = sock.getLocalAddress();

		    IpTable iptab = new IpTable(ip,adr);

		    String ipstr = ip.toString().substring(1);
		    if(flg){
			if(version.startsWith("HTTP/")){
			    if(indxname.equals("dummy.html")){ // if dummy request
				FileInputStream fin = new FileInputStream(file);
				byte[] buf = new byte[(int) file.length()];

				fin.read(buf);
				fin.close();
				
				out.write("HTTP/1.1 200 OK\r\n");
				out.write("Date: " + now + "\r\n");
				out.write("Server: HTTP 1.1\r\n");
				out.write("Content-length: " + buf.length + "\r\n");
				out.write("Content-type: " + contentType + "\r\n\r\n");
				out.flush();
				
				outs.write(buf);
				outs.flush();
			    }else{ // other files including image
				outs2.write(buf4);
				outs2.flush();
				
				int len = 0;
				
				byte[] buf2 = new byte[1024];
				BufferedInputStream in2 = new BufferedInputStream(sock2.getInputStream());
				
				// Response Transfer
				while((len = in2.read(buf2)) != -1){
				    outs.write(buf2);
				    outs.flush();
				}

				//iptab register
				if(iptab.size() > 3){
				    iptab.removeFirst();
				}
				iptab.addLast(iptab);

				
				in2.close();
			    }
				
			}else{
			    if(version.startsWith("HTTP/")){
				out.write("HTTP/1.1 404 File Not Found\r\n");
				out.write("Date: " + now + "\r\n");
				out.write("Server: HTTP 1.1\r\n");
				out.write("Content-type: text/html\r\n\r\n");
			    }
			    out.write("<HTML>\r\n");
			    out.write("<HEAD><TITLE>File Not Found</TITLE>\r\n");
			    out.write("</HEAD>\r\n");
			    out.write("<BODY>");
			    out.write("<H1>HTTP Error 404: File Not Fond</H1>\r\n");
			    out.write("</BODY></HTML>\r\n");
			    out.flush();
			}
		    }
		    //close sockets
		    outs2.close();
		    sock2.close();
		    
		    in.close();
		    out.close();
		    outs.close();
		    sock.close();
		    
		}
	    }
	    catch(IOException e) {
	    }
	    finally{
		try{
		    sock.close();
		}
		catch (IOException e){
		}
	    }
	}
    }
}

