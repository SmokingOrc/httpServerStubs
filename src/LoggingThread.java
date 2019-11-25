import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class LoggingThread extends Thread {

    public void run(){
        //Create Logfile if not exists
        if(!Files.exists(Paths.get("protocol.txt"))){
            try {
                Files.createFile(Paths.get("protocol.txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //Write to file all 5 seconds
        while(true){
            try {
                sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (Server.sharedLogString){
                writeLogToFile(Server.sharedLogString);
                Server.sharedLogString = "";
            }
        }
    }

    /**
     * @param logString
     * Appends logString to LogFile
     */
    private void writeLogToFile(String logString){
        try {
            Files.write(Paths.get("protocol.txt"), logString.getBytes(), StandardOpenOption.APPEND);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}