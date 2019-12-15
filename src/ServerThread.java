import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import static java.lang.Thread.sleep;


public class ServerThread implements Runnable {

	public static final String headerServer = "Server: Java HTTP Server "+ Server.SERVER_HTTP_VERSION;
	public static final String headerContentLength = "Content-Length: ";
	public static final String headerContentLang = "Content-Language: de";
	public static final String headerConnection = "Connection: close";
	public static final String headerContentType = "Content-Type: ";
	private Path documentRoot;
	private boolean logging;
	private boolean connected = true;
	private static LoggingThread loggingThread;

	private Socket ClientSocket;
	private BufferedReader is;
	private OutputStream os;
	private PrintWriter out = null;

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
				//get character output stream to client (for headers)
				out = new PrintWriter(ClientSocket.getOutputStream());

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

			//Switch read in Command from Tokens
			switch (cmd.get(0)){
				case "GET":// in case of GET Command
				case "HEADER": //in case of HEADER Command
					//get the file's MIME content type

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
								confirmation(out, content, fileLength);
								// readFIle for Streaming
								byte[] fileData = readFileData(file, fileLength);
								os.write(fileData, 0, fileLength);
								os.flush();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}else {
							//inform client file doesn't exist
							fileNotFound(out, cmd.get(1));
							closeConnection();
						}
					}
					break; // Ending GET
				default:notImplemented(out, cmd.get(0)); // If Request-method is not Implemented
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
			out.close();
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
	 * @param out Client output stream
	 * @param file File requested by client
	 */
	private void fileNotFound(PrintWriter out, String file)
	{
		//send file not found HTTP headers
		out.println("HTTP/1.0 404 File Not Found");
		out.println(headerServer);
		out.println("Date: " + new Date());
		out.println(headerContentType +"text/html");
		out.println();
		out.println("<HTML>");
		out.println("<HEAD><TITLE>File Not Found</TITLE>" +
				"</HEAD>");
		out.println("<BODY>");
		out.println("<H2>404 File Not Found: " + file + "</H2>");
		out.println("</BODY>");
		out.println("</HTML>");
		out.flush();

	}

	private void notImplemented(PrintWriter out, String method){
		//send Not Implemented message to client
		out.println("HTTP/1.0 501 Not Implemented");
		out.println(headerServer);
		out.println("Date: " + new Date());
		out.println(headerContentType + "text/html");
		out.println(); //blank line between headers and content
		out.println("<HTML>");
		out.println("<HEAD><TITLE>Not Implemented</TITLE>" +
				"</HEAD>");
		out.println("<BODY>");
		out.println("<H2>501 Not Implemented: " + method +
				" method.</H2>");
		out.println("</BODY></HTML>");
		out.flush();
	}

	private void confirmation(PrintWriter out, String content, int fileLength){
		//send HTTP headers
		out.println("HTTP/1.0 200 OK");
		out.println(headerServer);
		out.println("Date: " + new Date());
		out.println(headerContentType + content);
		out.println(headerContentLength + fileLength);
		out.println(); //blank line between headers and content
		out.flush(); //flush character output stream buffer

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
