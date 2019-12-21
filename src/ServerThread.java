import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.*;

import static java.nio.file.Files.probeContentType;


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
	private String uri;

	public ServerThread(Socket ClientSocket, LoggingThread loggingThread, String documentRoot, boolean logging) {
		this.ClientSocket = ClientSocket;
		this.documentRoot = Paths.get(documentRoot);
		this.logging = logging;
		this.loggingThread = loggingThread;
	}
	
	public synchronized void run() {
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

			StringTokenizer st = new StringTokenizer(readCommand);

			String methode = st.nextToken();
			uri = st.nextToken();
			String version = st.nextToken();




			System.out.println(holdConnection);



			//Switch read in Command from Tokens
			switch (methode){
				case "GET":// in case of GET Command
				case "HEAD": //in case of HEADER Command

					/*
					Read in Header fields
					 */

					header = new HashMap<String, String>();
					String line = null;
					try {
						while((line = is.readLine()).length() != 0){
							System.out.println(line);
							int p = line.indexOf(':');
							String key, value;
							key = line.substring(0, p + 1).trim().toLowerCase();
							value = line.substring(p + 1).trim();
							header.put(key, value);
							System.out.println(line + " | Key: " + key+ " Value: " + value);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					// find Header field connection and read value for keep-alive
					for (String key : header.keySet()){
						if (key.equals("connection:")){
							if (header.get(key).equals("keep-alive")){
								holdConnection = true;
							}else{
								holdConnection = false;
							}
						}
					}
					if (uri.equals("/")){ // If request is index.html
						File file = new File(String.valueOf(documentRoot), "\\Index.html");
						int fileLength = (int) file.length();
						protocol(methode +" "+ documentRoot+uri); // Logging for Protocol
                        try {
							String contentType = probeContentType(file.toPath());
							fileResponseHandler(HTTP_OK, contentType, file.toString(), fileLength);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
					} else if(methode.equals("GET")){ //Checking GET plus requested FILEPATH

						File file = new File(String.valueOf(documentRoot), uri);
						int fileLength = (int) file.length();

						protocol(methode +" "+ file.toString()); //Logging for Protocol
						if (isValidFile(file.toString()) && !uri.endsWith("/")) {		// isValid für HTTP 1.0
							try {
								String contentType = probeContentType(file.toPath());

								fileResponseHandler(HTTP_OK, contentType, file.toString(), fileLength);
							} catch (IOException e) {
								e.printStackTrace();
							}
						}else {
							//inform client file doesn't exist
							try {
								String contentType = probeContentType(file.toPath());

								fileResponseHandler(HTTP_NOTFOUND, contentType, file.toString(), fileLength);
							} catch (IOException e) {
								e.printStackTrace();
							}

						}
					}
					break; // Ending GET
				case "POST":

					try {
						String headerLine = null;
						while((headerLine = is.readLine()).length() != 0){
							System.out.println(headerLine);
							// find Header field connection and read value for keep-alive

								if (headerLine.equals("Connection: keep-alive")){
									holdConnection = true;
								}
							System.out.println(holdConnection);

						}

						StringBuilder payload = new StringBuilder();
						while(is.ready()){
							payload.append((char) is.read());
						}


						System.out.println(payload);
						System.out.println("Payload data is: "+payload.toString());
						String postData = payload.toString();
						String[] stringArray = postData.split("=");

						String responsePOST = "<html><body>";
						String firstName = stringArray[0];
						String lastNameReal = stringArray[2];

						String combo = stringArray[1];

						String[] stringArray2 = combo.split("&");
						String firstNameReal = stringArray2[0];
						String lastName = stringArray2[1];

						System.out.println(firstName);
						System.out.println(firstNameReal);
						System.out.println(lastName);
						System.out.println(lastNameReal);

						responsePOST += "<p> " + "Recieved form variable with name [" + firstName + "] and value [" + firstNameReal + "]"+ Server.CRLF;
						responsePOST += "<p> " + "Recieved form variable with name [" + lastName + "] and value [" + lastNameReal + "]"+ Server.CRLF;
						responsePOST += "</body></html>" + Server.CRLF;
						os.write(responsePOST.getBytes());





						break; // ending Post request

					} catch (IOException e) {
						e.printStackTrace();
					}
				default:
					try {
						notImplemented(HTTP_NOTIMPLEMENTED, methode); // If Request-method is not Implemented
					} catch (IOException e) {
						e.printStackTrace();
					}
			}
			if (!holdConnection) {
				closeConnection();
			}
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
