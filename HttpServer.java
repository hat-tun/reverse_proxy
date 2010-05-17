import java.net.*;
import java.io.*;

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
	
	System.out.println("port" + servsock.getLocalPort());
	
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
