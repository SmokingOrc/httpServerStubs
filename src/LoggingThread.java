import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class LoggingThread extends Thread {

    public void run(){
        //Write to file all 5 seconds
        while(true){
            try {
                sleep(300000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (Server.sharedLogString){
                writeLogToFile(Server.sharedLogString);
                System.out.println(Server.sharedLogString);
               // Server.sharedLogString = "";
            }
        }
    }

    /**
     * @param logString
     * Appends logString to LogFile
     */
    private void writeLogToFile(String logString){
        //Create Logfile if not exists
        if(!Files.exists(Paths.get("protocol.txt"))){
            try {
                Files.createFile(Paths.get("protocol.txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            Files.write(Paths.get("protocol.txt"), logString.getBytes(), StandardOpenOption.APPEND);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void showProtocol(){
        //show Protocol all 5 seconds
        if (Files.exists(Paths.get("protocol.txt"))) {
            try {
                sleep(300000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                List<String> readProtocol = Files.readAllLines(Paths.get("protocol.txt"));
                for(String log : readProtocol){
                    System.out.println(log);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }



}