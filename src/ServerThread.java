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
	public static final String HTTP_OK = "200 OK",  HTTP_NOTFOUND = "404 Not Found",
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
	
	public void run() {
		String readCommand="";

		System.out.println("Client " + ClientSocket.getInetAddress() + " on port " + ClientSocket.getPort() + " connected.");

		//Doing the request/response mechanism
		while (connected){
            holdConnection = false;
			try {
				//BufferedReader for reading the incoming command
				is = new BufferedReader(new InputStreamReader(ClientSocket.getInputStream()));
				//DataOutputStream for sending binary files (Byte[])
				os = new DataOutputStream(ClientSocket.getOutputStream());

				readCommand=is.readLine();
				System.out.println(readCommand);

                StringTokenizer st = new StringTokenizer(readCommand);

                String method = st.nextToken(); // get Request method
                uri = st.nextToken(); // get uri
                String version = st.nextToken(); // Get HTTP Version


                //Switch read in method from Tokens
                switch (method){
                    case "GET":// in case of GET Command
                    case "HEAD": //in case of HEADER Command

					/*
					Read in Header fields
					 */

                        header = new HashMap<String, String>();
                        String line = null;
                            while((line = is.readLine()).length() != 0){
                                int p = line.indexOf(':');
                                String key, value;
                                key = line.substring(0, p + 1).trim().toLowerCase();
                                value = line.substring(p + 1).trim();
                                header.put(key, value);
                                // System.out.println(line + " | Key: " + key+ " Value: " + value);
                            }

                        // find Header field connection and read value for keep-alive
                        if (version.equals("HTTP/1.1")) {
                            for (String key : header.keySet()) {
                                if (key.equals("connection:")) {
                                    if (header.get(key).equals("keep-alive")) {
                                        holdConnection = true;

                                    } else {
                                        holdConnection = false;
                                    }
                                    System.out.println(holdConnection + " is http 1.1");

                                }
                            }
                        }

                        if (uri.equals("/")){ // If request is index.html
                            File file = new File(String.valueOf(documentRoot), "\\Index.html");
                            int fileLength = (int) file.length();
                            protocol(method +" "+ documentRoot+uri); // Logging for Protocol
                                String contentType = probeContentType(file.toPath());
                                fileResponseHandler(HTTP_OK, contentType, file.toString(), fileLength, holdConnection);

                        } else if(method.equals("GET")){ //Checking GET plus requested FILEPATH

                            File file = new File(String.valueOf(documentRoot), uri);
                            int fileLength = (int) file.length();

                            protocol(method +" "+ file.toString()); //Logging for Protocol

                            if (isValidFile(file.toString()) && !uri.endsWith("/")) {		// isValid für HTTP 1.0
                                    String contentType = probeContentType(file.toPath());

                                    fileResponseHandler(HTTP_OK, contentType, file.toString(), fileLength, holdConnection);
                            }else {
                                //inform client file doesn't exist
                                    String contentType = probeContentType(file.toPath());

                                    fileResponseHandler(HTTP_NOTFOUND, contentType, file.toString(), fileLength, holdConnection);

                            }
                        }
                        break; // Ending GET
                    case "POST":

                            String headerLine = null;
                            while((headerLine = is.readLine()).length() != 0){
                                System.out.println(headerLine);
                                // find Header field connection and read value for keep-alive

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

                            responsePOST += "<p> " + "Recieved form variable with name [" + firstName + "] and value [" + firstNameReal + "] </p>"+ Server.CRLF;
                            responsePOST += "<p> " + "Recieved form variable with name [" + lastName + "] and value [" + lastNameReal + "]</p>"+ Server.CRLF;
                            responsePOST += "</body></html>" + Server.CRLF;
                            os.write(responsePOST.getBytes());

                            break; // ending Post request

                    default:
                            notImplemented(HTTP_NOTIMPLEMENTED, method); // If Request-method is not Implemented
                            holdConnection = false;
                }

            } catch (IOException e) {

                holdConnection = false;
                e.printStackTrace();
			}

            if (!holdConnection) {
                closeConnection();
            }

        }
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

	/**
		IsValid -Function checking Filepath
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

	private void fileResponseHandler(String HTTP, String content,String file, int fileLength, boolean holdConnection ) throws IOException{
		File f = new File(file);

		String response= "";
		response += "HTTP/"+ Server.SERVER_HTTP_VERSION+" "+ HTTP + Server.CRLF;
		response += headerServer+Server.CRLF;
		response += "Date: "+ new Date() + Server.CRLF;
		if (holdConnection){// for keep-alive
		    response += "connection: keep-alive" + Server.CRLF;
        }else {
            response += headerConnection + Server.CRLF;
        }
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
        response += headerConnection + Server.CRLF;
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
