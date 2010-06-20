import java.net.*;
import java.io.*;
import java.util.*;

public class HttpServer{
    public static void main(String[] args) throws IOException{
	int HTTP_PORT = 8000;
	//File dir = new File("c:\\workspace\\Intern");
	File dir = new File("/home/hat-tun/intern/reverse_proxy");
	int numThreads = 5;
	int cnt = 0;
	ServerSocket servsock = new ServerSocket(HTTP_PORT);
	
	for(int i = 1; i < numThreads; i++){
	    RequestProcessor req = new RequestProcessor(dir);
	    Thread t = new Thread(req);
	    t.start();
	}
	
	System.out.println("ip" + InetAddress.getLocalHost().getHostAddress() + " port" + servsock.getLocalPort());
	
	while(true){
	    try{
		Socket sock = servsock.accept();
		cnt++;
		RequestProcessor.reqProc(sock, cnt);
	    }
	    catch (IOException e){
		e.printStackTrace();
		System.exit(1);
	    }
	}
    }
}

class RequestProcessor implements Runnable{
    private static LinkedList queue = new LinkedList();
    private File homeDir;
    public RequestProcessor(File homeDir){
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
		OutputStream outs = new BufferedOutputStream(sock.getOutputStream());
		Writer out = new OutputStreamWriter(outs);
		InputStream in = sock.getInputStream();
		BufferedReader rd;
		rd = new BufferedReader(new InputStreamReader(in,"JISAutoDetect"));
		String get = rd.readLine();
		StringTokenizer st = new StringTokenizer(get);
		String method = st.nextToken();
		if(method.equals("GET")){
		    filename = st.nextToken();
		    indxname = filename.substring(1, filename.length());
		    contentType = "text/html";
		    version = st.nextToken();
		    //String adr = homeDir + "\\" + indxname;
		    String adr = homeDir + "/" + indxname;
		    //System.out.println("adr:" + adr);
		    File file = new File(adr);
		    boolean flg = file.canRead();
		    Date now = new Date();
		    if(flg){
			FileInputStream fin = new FileInputStream(file);
			byte[] buf = new byte[(int) file.length()];
			fin.read(buf);
			fin.close();
			if(version.startsWith("HTTP/")){
			    out.write("HTTP/1.1 200 OK\r\n");
			    out.write("Date: " + now + "\r\n");
			    out.write("Server: HTTP 1.1\r\n");
			    out.write("Content-length: " + buf.length + "\r\n");
			    out.write("Content-type: " + contentType + "\r\n\r\n");
			    out.flush();
			}
			outs.write(buf);
			outs.flush();
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
			out.write("<BODY>\r\n");
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
