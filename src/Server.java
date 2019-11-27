import java.io.IOException;
import java.net.*;

public class Server {

	public static final int SERVER_PORT		= 80;
	public static final String SERVER_STRING = "My Little Pony HTTP Server";
	public static final String SERVER_HTTP_VERSION = "0.9";
	public static final String CRLF = "\r\n";
	public static final String documentRoot = ".\\wwwroot";
	private static boolean logging = true;
	private static LoggingThread loggingThread;
	protected static String sharedLogString = "";



	public static void main(String [] args)
	{
		try {
			ServerSocket srvSocket = new ServerSocket(Server.SERVER_PORT);
			System.out.println("HTTP server running at port: " + Server.SERVER_PORT + ", documentRoot = " + documentRoot );
			//Create logging Thread if logging activated
			if (logging) {
				loggingThread = new LoggingThread();
				System.out.println("protocol started");
				new Thread(loggingThread).start();
			}
			while(true){
				try{
					Socket ConnectionSocket = srvSocket.accept();
					System.out.println("Server got new request from client with IP: " + ConnectionSocket.getRemoteSocketAddress() + " and port: " + ConnectionSocket.getPort());
					if(ConnectionSocket != null) new Thread(new ServerThread(ConnectionSocket, loggingThread, documentRoot, logging )).start();

				}
				catch (IOException e) {
					e.printStackTrace();
				}

			}
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

}
