
public class LoggingThread extends Thread {

    public void run(){
        //Write to console all 5 min
        while(true){
            try {
                LoggingThread.sleep(300000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            synchronized (Server.sharedLogString){
                System.out.println(Server.sharedLogString);
                Server.sharedLogString = "";
            }
        }
    }


}