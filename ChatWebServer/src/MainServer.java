import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


/*
 *********************   OVERVIEW   *********************
 * THIS SERVER CAN LISTEN TO THE REQUEST FROM CHAT WEB CLIENTS:
 *  - receive socket from clients, and send back webpage files;
 *  - receive connection request from clients;
 *  - receive requests from clients and add clients into rooms;
 *  - receive requests from clients and broadcast the message that clients entered to the room;
 *  - handle the leaving of the room of clients.
 ********************************************************
 */



/////////////////////   MAIN CLASS  //////////////////////
//  - server socket keeps listening and accept sockets from client
//  - generate threads for every socket from every client
//  - handle exceptions at the highest level


public class MainServer {

    static int PORT = 8081;
    static ServerSocket svSocket_;

    public static void main(String[] args) {
        try {
            // set the port
            svSocket_ = new ServerSocket(PORT);
            System.out.println("Server launched. Listening for connections to port: " + PORT + "...\n");

            // keep the active status and wait for connecting...
            while (true){
                // accept client socket
                Socket cltSocket = svSocket_.accept();
                // create a runnable ---> class: sktRunnable
                SktRunnable runnable = new SktRunnable (cltSocket);
                // create a thread to handle a single runnable
                Thread thread = new Thread(runnable);
                thread.start();
            }

        // catch exceptions
        } catch (IOException e) {
            System.err.println("A server connection error occurred: " + e.getMessage());
        } catch (Exception e){
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }
    }
}
