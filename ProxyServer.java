import java.net.*;
import java.io.*;
import java.util.*;

public class ProxyServer{


    private static int numTasks = 0;


    private static class CheckTime extends TimerTask{
	public void run(){
	    numTasks= 0;
	}
    }


    public static void main(String[] args) throws IOException{
	int HTTP_PORT = 8080;
	//File dir = new File("c:\\workspace\\Intern");
	File dir = new File("/home/hat-tun/intern/reverse_proxy");


	int numThreads = 5;
	int cnt = 0;

	ServerSocket servsock = new ServerSocket(HTTP_PORT);
	
      	for(int i = 1; i < numThreads; i++){
	    RequestPassThrough req = new RequestPassThrough(dir);
	    Thread t = new Thread(req);
	    t.start();
	}

	for(int i = 1; i < numThreads; i++){
	    RequestRedirect req2 = new RequestRedirect(dir);
	    Thread t2 = new Thread(req2);
	    t2.start();
	}

	
	System.out.println("port" + servsock.getLocalPort());
	
	TimerTask task = new CheckTime();
	Timer timer = new Timer();

	timer.schedule(task,0,5000);// init in 5sec interval
	
	while(true){
	    try{
		Socket sock = servsock.accept();
		cnt++;
		numTasks++;
		System.out.println("task = "+numTasks);
		if(numTasks > 3){
		    System.out.println("Overflow");
		    RequestRedirect.reqProcRedirect(sock, cnt);
		}else{
		    RequestPassThrough.reqProc(sock, cnt);
		}
	    }
	    catch (IOException e){
		e.printStackTrace();
		System.exit(1);
	    }
	}
    }
    
}															
