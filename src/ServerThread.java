import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static java.lang.Thread.sleep;


public class ServerThread implements Runnable {

	public static final String headerServer = "Server: ";
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

			//Switch read in Command from Tokens
			switch (cmd.get(0)){
				case "GET":// in case of GET Command
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
					} else{ //Checking GET plus requested FILEPATH
						File file = new File(String.valueOf(documentRoot), cmd.get(1));
						int fileLength = (int) file.length();

						protocol(cmd.get(0) +" "+ file.toString()); //Logging for Protocol
						//if (isValidFile(file.toString())) {		// isValid für HTTP >0.9
							try {
								// readFIle for Streaming
								byte[] fileData = readFileData(file, fileLength);
								os.write(fileData, 0, fileLength);
								os.flush();
							} catch (IOException e) {
								e.printStackTrace();
							}
						//}else {
						//	closeConnection();
						//}
					}
					closeConnection();
					break; // Ending GET
				default: closeConnection();// always closing Connection

			}

		} while (connected);    //do until connection is closed
	}

	/**
	 * Method to close Readers, Writers and Connection Socket
	 */
	private void closeConnection() {
		try {
			ClientSocket.close();
			is.close();
			os.close();
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
