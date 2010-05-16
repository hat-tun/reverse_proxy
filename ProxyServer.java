import java.net.*;
import java.io.*;

public class ProxyServer{
    public static void main(String[] args) throws IOException{
	int HTTP_PORT = 8080;
	//File dir = new File("c:\\workspace\\Intern");
	File dir = new File("/home/hat-tun/intern");
	int numThreads = 5;
	int cnt = 0;
	ServerSocket servsock = new ServerSocket(HTTP_PORT);
	
	for(int i = 1; i < numThreads; i++){
	    RequestRedirect req = new RequestRedirect(dir);
	    Thread t = new Thread(req);
	    t.start();
	}
	
	System.out.println("port" + servsock.getLocalPort());
	
	while(true){
	    try{
		Socket sock = servsock.accept();
		cnt++;
		RequestRedirect.reqProc(sock, cnt);
	    }
	    catch (IOException e){
		e.printStackTrace();
		System.exit(1);
	    }
	}
    }
}															
