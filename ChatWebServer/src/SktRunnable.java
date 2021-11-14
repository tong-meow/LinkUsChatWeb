import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;


//////////////////   SOCKET RUNNABLE CLASS  //////////////////
//  - create runnable based on every socket, in order to let the thread handle it
//  - determine if the request is an HTTP request or a web socket request
//  - handle these 2 types of requests


public class SktRunnable implements Runnable{
    // member variables (final: every client's socket should remain constant)
    private final Socket socket_;


    // constructor: assign the socket member variable to the socket which is accepted from the client
    public SktRunnable(Socket socket){
        socket_ = socket;
    }


    // override the run() method, let the thread in the MainServer class to execute it
    @Override
    public void run(){
        // define input stream of the socket
        InputStream input = null;
        try {
            input = socket_.getInputStream();
        } catch (IOException e) {
            System.out.println("A socket input stream error occurred: " + e.getMessage());
        }
        assert input != null;
        Scanner scanner = new Scanner(input);

        // read the header of the request
        String requestMethod = scanner.next();
        String requestFile = scanner.next();
        String requestProtocol = scanner.next();
        // print the reading result to check if the header is right
        System.out.println(">>>>>>>>> Client's request:");
        System.out.println(requestMethod + " " + requestFile + " " + requestProtocol);

        // read the rest lines of request
        Map<String,String> requestMap = new HashMap<>();
        scanner.nextLine();
        String requestLine = scanner.nextLine();
        while (!requestLine.equals("")) {
            String[] arrLine = requestLine.split(": ", 2);
            System.out.println(arrLine[0] + ": " + arrLine[1]);
            requestMap.put(arrLine[0], arrLine[1]);
            requestLine = scanner.nextLine();
        }
        System.out.println();

        // determine the request type
        // if it is a web socket request, create a WebSocketResponse
        if (requestMap.containsKey("Upgrade")){
            if (requestMap.containsKey("Sec-WebSocket-Key")){
                WSResponse wsResponse = new WSResponse(socket_, input);
                wsResponse.handShake(requestMap);
                wsResponse.keepListening();
            }
        }
        // if it is an HTTP request, create an HTTP response
        else{
            HTTPResponse httpRp = new HTTPResponse(socket_, requestFile);
            httpRp.sendResponse();
        }
    }
}