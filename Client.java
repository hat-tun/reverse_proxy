import java.net.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;


public class Client{
    public static void main(String[] args) throws IOException{

	//File dir = new File("c:\\workspace\\Intern");
	File dir = new File("/home/hat-tun/intern/reverse_proxy/cache");
	int numThreads = 5;
	int cnt = 0;
	int HTTP_PORT = 8008;
	ServerSocket servsock = new ServerSocket(HTTP_PORT);
	
	for(int i = 1; i < numThreads; i++){
	    ClientProcessor req = new ClientProcessor(dir);
	    Thread t = new Thread(req);
	    t.start();
	}
	
	ClientRequester req2 = new ClientRequester();
	Thread t2 = new Thread(req2);
	t2.start();
	

	

	System.out.println("ip" + InetAddress.getLocalHost().getHostAddress() + " port" + servsock.getLocalPort());
	
	while(true){
	    try{
		Socket sock = servsock.accept();
		cnt++;
		ClientProcessor.reqProc(sock, cnt);
	    }
	    catch (IOException e){
		e.printStackTrace();
		System.exit(1);
	    }
	}
	
	
	
    }
}


class ClientRequester implements Runnable{
    
    private File cacheDir = new File("/home/hat-tun/intern/reverse_proxy/cache");

    public void run(){
	while(true){
	    try{
		BufferedReader r = new BufferedReader(new InputStreamReader(System.in), 1); // standard input
		URL url;
		
		System.out.print("URL > ");
		String input = r.readLine();
		url = new URL(input);

		HttpURLConnection urlconn = (HttpURLConnection)url.openConnection();

		urlconn.setRequestMethod("GET");
		urlconn.connect();
		

		

		String host = url.getHost();
		int port = url.getPort();
		String path = url.getPath();

		int delm = path.lastIndexOf("/");
		String directory = "";
		String file_name;
		if(delm != -1 && delm != 0){// if path file does not exist in homeDir
		    directory = path.substring(1,delm);
		    file_name = path.substring(delm+1);
		}else{
		    file_name = path.substring(1);
		}

		File html_dir = new File(cacheDir + "/" + directory);
		boolean flg = html_dir.canRead();
		if(!flg){ // if a directory does not exist
		    html_dir.mkdirs();
		}
		File html_file = new File(html_dir+ "/" + file_name);
		
		PrintWriter pw = new PrintWriter(new OutputStreamWriter (new FileOutputStream(html_file)));
		BufferedReader rd = new BufferedReader(new InputStreamReader(urlconn.getInputStream(),"JISAutoDetect")); //auto charset
		
		String[] objs = new String[100];
		int i = 0;
		while(true){
		    String line = rd.readLine();
		    if(line == null){
			break;
		    }
		    Pattern p = Pattern.compile("<img src.*\"(.*\\.(gif|jpeg|png))\".*>");//img files
		    Pattern p2 = Pattern.compile("<link rel.*=.*\"stylesheet\".* href=\"(.*\\.css)\".*>");//css files
		    Matcher m = p.matcher(line);
		    Matcher m2 = p2.matcher(line);
		    if(m.find()){
			objs[i] = m.group(1); //saving img file name
			i++;
		    }
		    if(m2.find()){
			objs[i] = m2.group(1); //saving css file name
			i++;
		    }
		    System.out.println(line);
		    pw.println(line);
		}
		
		pw.close();
		
		while(true){ // download object files
		    if(i==0){
			break;
		    }else{	
			String obj_path = objs[i-1];
			i--;

			URL url_obj = new URL("http://"+host+":"+port +"/"+directory+"/"+ obj_path);
			HttpURLConnection urlconn2 = (HttpURLConnection)url_obj.openConnection();
			
			urlconn2.setRequestMethod("GET");
			urlconn2.connect();
			
			BufferedInputStream in = new BufferedInputStream(urlconn2.getInputStream());
			
			delm = obj_path.lastIndexOf("/");
			String sub_directory = "";
			if(delm != -1 && delm != 0){
			    sub_directory = obj_path.substring(0,delm);
			    file_name = obj_path.substring(delm+1);
			}else{
			    file_name = obj_path.substring(0);
			}
			
			File obj_dir = new File(cacheDir + "/" + directory + "/" +sub_directory);
			flg = obj_dir.canRead();
			if(!flg){ // if a directory does not exist
			    obj_dir.mkdirs();
			}
			File obj_file = new File(cacheDir + "/" + directory + "/" +sub_directory + "/" + file_name);
			
			BufferedOutputStream out = new BufferedOutputStream (new FileOutputStream(obj_file));
			int k;
			byte[] buf = new byte[1024];
			while(true){
			    k=in.read(buf);
			    if(k == -1){
				break;
			    }
			    out.write(buf,0,k);
			    out.flush();
			}
			out.close();
		    }
		}
		

	    }catch(UnknownHostException e){
	    }catch(IOException e){
	    }catch(IllegalArgumentException e){
	    }
	}	    
    }
}

class ClientProcessor implements Runnable{
    private static LinkedList queue = new LinkedList();
    private File homeDir;
    public ClientProcessor(File homeDir){
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
		rd = new BufferedReader(new InputStreamReader(in));
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
