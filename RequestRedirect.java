import java.net.*;
import java.io.*;
import java.util.*;

public class RequestRedirect implements Runnable{
    private static String webserver ="192.168.74.141";
    private static LinkedList queue = new LinkedList();
    private static LinkedList ip_queue = new LinkedList();
    private File homeDir;
    public RequestRedirect(File homeDir){
	this.homeDir = homeDir;
    }
    
    public static void reqProc(Socket s, int cnt){
	synchronized (queue){
	    queue.addLast(s);
	    System.out.println("connect:" + cnt + " " + queue);
	    queue.notifyAll();
	}
    }
    
    public static void registerIP(String ip){
	ip_queue.addLast(ip);
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
		OutputStream outs = new BufferedOutputStream(sock.getOutputStream());
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outs));
		InputStream in = sock.getInputStream();
		BufferedReader rd;
		rd = new BufferedReader(new InputStreamReader(in));
		String get = rd.readLine();
		String header = rd.readLine();
		header = rd.readLine();
		StringTokenizer st2 = new StringTokenizer(header);
		String browser = st2.nextToken();
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
		    String ipstr = ip.toString().substring(1);
		    if(flg){
			if(version.startsWith("HTTP/")){
			    //if(indxname.equals("index.html") && busy){ //if busy
			    if(indxname.equals("index.html") && ipstr.equals("192.168.74.141")){ //check IP
				Socket sock2 = new Socket(webserver,8000); // connect to web server
				OutputStream outs2 = new BufferedOutputStream(sock2.getOutputStream());
				BufferedWriter out2 = new BufferedWriter(new OutputStreamWriter(outs2)); 
				
				out2.write("GET /index.html HTTP/1.1 \r\n");
				out2.flush();
				
				InputStream in2 = sock2.getInputStream();
				BufferedReader rd2;
 				rd2 = new BufferedReader(new InputStreamReader(in2));
 				String messege;
				while((messege = rd2.readLine()) != null){
 				    //System.out.println(messege);
				    out.write(messege);
				    out.newLine(); //abstraction of CR/LF code
				    out.flush();
				}
				
				sock2.close();
				
			    }else if(indxname.equals("dummy.html")){
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

			    }else{
				out.write("HTTP/1.1 302 Found\r\n");
				out.write("Date: " + now + "\r\n");
				out.write("Server: HTTP 1.1\r\n");
				out.write("Location: http://"+webserver+":8080/dummy.html\r\n");
				out.flush();
			    }
			}
		    }
		    else{
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
