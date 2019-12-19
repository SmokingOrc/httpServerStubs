import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;


public class ServerThread implements Runnable {

	public static final String headerServer = "Server: Java HTTP Server "+ Server.SERVER_HTTP_VERSION;
	public static final String headerContentLength = "Content-Length: ";
	public static final String headerContentLang = "Content-Language: de";
	public static final String headerConnection = "Connection: close";
	public static final String headerContentType = "Content-Type: ";
	//Some HTTP response status codes
	public static final String HTTP_OK = "200 OK", HTTP_REDIRECT = "301 Moved Permanently",
			HTTP_FORBIDDEN = "403 Forbidden", HTTP_NOTFOUND = "404 Not Found",
			HTTP_BADREQUEST = "400 Bad Request", HTTP_INTERNALERROR = "500 Internal Server Error",
			HTTP_NOTIMPLEMENTED = "501 Not Implemented";
	private Path documentRoot;
	private boolean logging;
	private boolean connected = true;
	private static LoggingThread loggingThread;
	private HashMap<String,String> header;
	boolean holdConnection = false;

	private Socket ClientSocket;
	private BufferedReader is;
	private OutputStream os;

	public ServerThread(Socket ClientSocket, LoggingThread loggingThread, String documentRoot, boolean logging) {
		this.ClientSocket = ClientSocket;
		this.documentRoot = Paths.get(documentRoot);
		this.logging = logging;
		this.loggingThread = loggingThread;
	}
	
	public void run() {
		// TODO Implement HTTP v0.9, just GET - the available files to the server are given by ServerFiles.files
		String readCommand="";

		System.out.println("Client " + ClientSocket.getInetAddress() + " on port " + ClientSocket.getPort() + " connected.");
		//Doing the request/response mechanism
		do {

			try {
				//BufferedReader for reading the incoming command
				is = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));
				//DataOutputStream for sending binary files (Byte[])
				os = new DataOutputStream(ClientSocket.getOutputStream());

				readCommand=is.readLine();
				System.out.println(readCommand);

			} catch (IOException e) {
				e.printStackTrace();
			}
			//StringTokenizer to get tokens into Array
			StringTokenizer st = new StringTokenizer(readCommand, " ");
			// create ArrayList object
			List<String> cmd = new ArrayList<String>();

			while(st.hasMoreTokens()) {

				cmd.add(st.nextToken());
			}
			//System.out.println(cmd.get(0));
			//System.out.println(cmd.get(1));
			/*
			header = new HashMap<String, String>();
			String line = null;
			int c = 0;
			while (c < 30 ) {
				try {
					line = is.readLine();
				} catch (IOException e) {
					e.printStackTrace();
				}
//				System.out.println(line.length());
				if (line.length() >= 1){
					int p = line.indexOf(':');
					String key, value;
					key = line.substring(0, p + 1).trim().toLowerCase();
					value = line.substring(p + 1).trim();
					header.put(key, value);
					System.out.println(line + " | Key: " + key+ " Value: " + value);
					c++;
				}
				c++;
			}
			for (String key : header.keySet()){
				if (key.equals("connection:")){
					if (header.get(key).equals("keep-alive")){
						holdConnection = true;
					}
				}
			}
				*/
			//Switch read in Command from Tokens
			switch (cmd.get(0)){
				case "GET":// in case of GET Command
				case "HEAD": //in case of HEADER Command

					if (cmd.get(1).equals("/")){ // If request is index.html
						File file = new File(String.valueOf(documentRoot), "\\Index.html");
						int fileLength = (int) file.length();
						protocol(cmd.get(0) +" "+ documentRoot+cmd.get(1)); // Logging for Protocol

						try {
							// readFIle for Streaming
							byte[] fileData = readFileData(file, fileLength);
							os.write(fileData, 0, fileLength);
							os.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else if(cmd.get(0).equals("GET")){ //Checking GET plus requested FILEPATH
						String content = getContentType(cmd.get(1));

						File file = new File(String.valueOf(documentRoot), cmd.get(1));
						int fileLength = (int) file.length();

						protocol(cmd.get(0) +" "+ file.toString()); //Logging for Protocol
						if (isValidFile(file.toString()) && !cmd.get(1).endsWith("/")) {		// isValid für HTTP 1.0
							try {
								fileResponseHandler(HTTP_OK, content, file.toString(), fileLength);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}else {
							//inform client file doesn't exist
							try {
								fileResponseHandler(HTTP_NOTFOUND, content, file.toString(), fileLength);
							} catch (IOException e) {
								e.printStackTrace();
							}

						}
					}
					break; // Ending GET
				case "POST":




					try {

						if (is.ready()) {
							String data = is.readLine();
								System.out.println(data);
								//byte[] data = is.;
								String responsePOST = "<html><body>";
									//if (content.equals("application/x-www-form-urlencoded")){
									StringTokenizer inputs = new StringTokenizer(data,"&");
									while (inputs.hasMoreTokens()){
										StringTokenizer input = new StringTokenizer(inputs.nextToken(),"=");
										responsePOST += "<p> " + "Recieved form variable with name [" + input.nextToken() + "] and value [" + input.nextToken() + "]"+ Server.CRLF;
									}
									responsePOST += "</body></html>" + Server.CRLF;

									os.write(responsePOST.getBytes());




						}else{
							notImplemented(HTTP_INTERNALERROR, cmd.get(1));
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println(cmd.get(0) + cmd.get(1)+ cmd.get(2));
					/*try {
						handlePOST(cmd.get(1), cmd.get(1).length());
					} catch (IOException e) {
						e.printStackTrace();
					}*/
				break;
				default:
					try {
						notImplemented(HTTP_NOTIMPLEMENTED, cmd.get(0)); // If Request-method is not Implemented
					} catch (IOException e) {
						e.printStackTrace();
					}
			}

			closeConnection();
		} while (connected);    //do until connection is closed
	}

	/**
	 * Method to close Readers, Writers and Connection Socket
	 */
	private void closeConnection() {
		try {
			is.close();
			os.close();
			ClientSocket.close();
			System.out.println("connection closed");
		} catch (IOException e) {
			e.printStackTrace();
		}
		connected = false;
	}

		// CASE "GET" und "Head" --> "/" ist index html...
		// uri mit " " durch %20
	/**
	 * Method to send Response
	 */

	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];

		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null)
				fileIn.close();
		}

		return fileData;
	}
	/**
		IsValid -Funktion für beginn HTTP: 1.0// isValid funktion --> bsp. "/images/bild.png" ersetzen / durch "\\"
	*/
	 public boolean isValidFile (String valid) {
		if (valid == null) return false;
			if(Files.exists(Paths.get(valid))) {
			return true;
			}
		return false;
	}

	/*
	Upgrade for HTTP 1.0

	 */
	/**
	 * getContentType returns the proper MIME content type
	 * according to the requested file's extension.
	 *
	 * @param fileRequested File requested by client
	 */
	private String getContentType(String fileRequested)
	{
		if (fileRequested.endsWith(".htm") ||
				fileRequested.endsWith(".html"))
		{
			return "text/html";
		}
		else if (fileRequested.endsWith(".gif"))
		{
			return "image/gif";
		}
		else if (fileRequested.endsWith(".jpg") ||
				fileRequested.endsWith(".jpeg"))
		{
			return "image/jpeg";
		}
		else if (fileRequested.endsWith(".class") ||
				fileRequested.endsWith(".jar"))
		{
			return "applicaton/octet-stream";
		}
		else
		{
			return "text/plain";
		}
	}

	/**
	 * fileNotFound informs client that requested file does not
	 * exist.
	 *
	 * @param file File requested by client
	 */

	private void fileResponseHandler(String HTTP, String content,String file, int fileLength ) throws IOException{
		File f = new File(file);

		String response= "";
		response += "HTTP/"+ Server.SERVER_HTTP_VERSION+" "+ HTTP + Server.CRLF;
		response += headerServer+Server.CRLF;
		response += "Date: "+ new Date() + Server.CRLF;
		response += headerContentType + content + Server.CRLF;
		response += headerContentLength + fileLength +Server.CRLF;
		response += Server.CRLF;
		os.write(response.getBytes());
		Files.copy(f.toPath(), os);
		os.flush();

	}
	private void notImplemented(String HTTP, String method) throws IOException{
		//send Not Implemented message to client
		String response= "";
		response += "HTTP/"+ Server.SERVER_HTTP_VERSION+" "+ HTTP + Server.CRLF;
		response += headerServer+Server.CRLF;
		response += "Date: "+ new Date() + Server.CRLF;
		response += headerContentType + "text/html" + Server.CRLF;
		response += Server.CRLF;
		response+="<HTML><HEAD><TITLE>Not Implemented</TITLE></HEAD><BODY><H2>501 Not Implemented: " + method +
				" method.</H2>";
		os.write(response.getBytes());
		os.flush();
	}
	private void handlePOST(String content, int lenght) throws IOException{
		char[] data = new char[lenght];
		String responsePOST = "<html><body>";
		if (is.ready()){
			is.read(data, 0, lenght);

			//if (content.equals("application/x-www-form-urlencoded")){
				StringTokenizer inputs = new StringTokenizer(String.valueOf(data),"&");
				while (inputs.hasMoreTokens()){
					StringTokenizer input = new StringTokenizer(inputs.nextToken(),"=");
					responsePOST += "<p> " + "Recieved form variable with name [" + input.nextToken() + "] and value [" + input.nextToken() + "]"+ Server.CRLF;
					responsePOST += "</body></html>" + Server.CRLF;
				}
			/*} else{
				notImplemented(HTTP_INTERNALERROR, content);
			}
			*/
		};
	}

	/**
	 * @param logString Synchronized method to write String into logfile
	 */
	private void protocol(String logString) {
		System.out.println(logString);
		if (logging) {
			Timestamp timestamp = new Timestamp(System.currentTimeMillis());
			//Logging timestamp, incoming request, InetAdress, Port
			String writeString = timestamp + " " + logString + " " + ClientSocket.getInetAddress() + " " + ClientSocket.getPort();
			//Synchronized since sharedLogString is used by all Server Threads
			synchronized (Server.sharedLogString) {
				//Append new line to sharedLogString
				Server.sharedLogString = Server.sharedLogString + "\n" + writeString;
			}
		}
	}


}
