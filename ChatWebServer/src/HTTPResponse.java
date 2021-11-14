import java.io.*;
import java.net.Socket;
import java.util.Date;


//////////////////   HTTP RESPONSE CLASS  //////////////////
//  - analyze the request
//  - create the response for an HTTP request depending on whether the requested file exists or not
//  - send the response


public class HTTPResponse {

    // member variables
    private final Socket socket_;
    private String fileName_;
    private File requestFile_;
    private FileInputStream fis_;


    // constructor: create an HTTPResponse Object
    // assign the member variables of a single HTTP response
    public HTTPResponse(Socket socket, String fileName) {

        socket_ = socket;
        fileName_ = fileName;

        // set up the requested file's path
        String filePath = "./resources" + fileName_;
        if (fileName_.equals("/")) {
            filePath = "./resources/homepage.html";
        }

        // read the requested file
        try {
            requestFile_ = new File(filePath);
            fis_ = new FileInputStream(requestFile_);
        }

        // if the request file doesn't exist
        // set the request file to "404.html"
        catch (FileNotFoundException fnfe) {
            fileName_ = "404.html";
            requestFile_ = new File("./resources/404.html");
            try {
                fis_ = new FileInputStream(requestFile_);
            } catch (FileNotFoundException e404) {
                System.out.println("Unable to find the '404 NOT FOUND' html file.");
            }
        }

        // handle other exceptions
        catch (Exception e) {
            System.out.println("Unexpected error occurred: " + e.getMessage());
        }
    }


    // method to get the file type
    // called in the 'handleResponse' method
    private synchronized String getFileContentType(){
        String type;
        if (fileName_.endsWith(".html") || fileName_.endsWith(".htm")){
            type = "text/html";
        }else if (fileName_.endsWith(".css")){
            type = "text/css";
        }else if (fileName_.endsWith(".jpg") || fileName_.endsWith(".jpeg")){
            type = "image/jpg";
        }else if (fileName_.endsWith(".png")){
            type = "image/png";
        }else{
            type = "unknown";
        }
        return type;
    }


    // method to handle response (regular HTTP request)
    // generate the header
    // use the print writer to flush and send the header
    private synchronized void handleResponse(PrintWriter pw) {
        pw.println("HTTP/1.1 200 OK");
        pw.println("Server: Jarvia Server");
        pw.println("Date: " + new Date());
        String fileContentType = getFileContentType();
        pw.println("Content-type: " + fileContentType);
        pw.println("Content-length: " + (int) requestFile_.length());
        pw.println();
        pw.flush();
    }


    // method to handle response (404 not found)
    // generate the header
    // use the print writer to flush and send the header
    private synchronized void handle404Response(PrintWriter pw) {
        pw.println("HTTP/1.1 404 NOT FOUND");
        pw.println("Server: Jarvia Server");
        pw.println("Date: " + new Date());
        pw.println("Content-type: text/html");
        pw.println("Content-length: " + (int) requestFile_.length());
        pw.println();
        pw.flush();
    }


    // method to send response to an HTTP request
    // send the response to the client, including the file requested
    // close the socket after sending
    public synchronized void sendResponse(){
        // create an output stream
        OutputStream output = null;
        try {
            output = socket_.getOutputStream();
        }catch (IOException e) {
            System.out.println("An output stream error occurred: " + e.getMessage());
        }
        assert output != null;
        PrintWriter pw = new PrintWriter(output);

        // response differently depends on the request message
        if (fileName_.equals("404.html")){
            handle404Response(pw);
        }else{
            handleResponse(pw);
        }

        // send the file
        int fileLength = (int) requestFile_.length();
        byte[] fileByteArr = new byte [(int)fileLength];
        BufferedInputStream bis = new BufferedInputStream(fis_);
        try {
            bis.read(fileByteArr, 0, fileLength);
            output.write(fileByteArr, 0, fileLength);
            output.flush();
            output.close();
        } catch (IOException e) {
            System.err.println("An 'file not found' exception error occurred: " + e.getMessage());
        }catch (Exception e){
            System.err.println("An unexpected error occurred: " + e.getMessage());
        }
        pw.close();

        // response succeed, close the socket
        try {
            socket_.close();
        } catch (IOException e) {
            System.out.println("Socket closing error occurred: " + e.getMessage());
        }
    }
}