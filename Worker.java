/***
 * Marco Martinez
 * Computer Networks I
 * Programming Project 2: Building a Web Server
 * Worker.java
 * 
 * Worker class (consumes from blocking queue). Handles HTTP requests.
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class Worker implements Runnable {

    BlockingQueue<Socket> queue;
    public static AtomicBoolean die;
    public static int[] check;
    int workerNumber;

    // Used for debugging purposes only.
    private boolean debug = true;

    Worker(BlockingQueue<Socket> queue, AtomicBoolean die, int[] check) {
        this.queue = queue;
        Worker.die = die;
        workerNumber = check[0];
        check[0]++;
    }

    /***
     * Answers HTTP request.
     * @param requestHeaderString header
     * @param requestBody could be null, be careful!
     * @param sock network socket being used.
     */
    public void answerHttpRequest(String requestHeaderString, char[] requestBody, Socket sock) {

        // Tokenize request (by line, hence the "\r\n").
        String[] requestTokenizedByLine = requestHeaderString.split("\r\n");

        if (debug) {
            // Print request.
            System.out.println("Worker " + workerNumber + " request:");
            System.out.println("******************************");
            for (String line : requestTokenizedByLine) {
                System.out.println(line);
            }
            System.out.println("******************************");
        }

        // Grab first line of header.
        // Contains the request line (Request method, URI, HTTP version).
        String requestLine = requestTokenizedByLine[0];
        // Tokenize request line by whitespace.
        String[] requestLineTokenized = requestLine.split(" ");

        try {
            // Init output stream to build HTTP response.
            OutputStream httpResponse = sock.getOutputStream();
            switch (requestLineTokenized[0]) {
                case "GET":
                    // Handle GET request.
                    if (debug) {
                        System.out.println("Worker " + workerNumber + " got a GET request!");
                    }
                    handleGetRequest(requestLine, httpResponse);
                    break;
                
                case "POST":
                    // Handle POST request.

                    if (debug) {
                        System.out.println("Worker " + workerNumber + " got a POST request!");
                    }
                    handlePostRequest(requestLine, requestBody, httpResponse);
                    break;
                
                case "DELETE":
                    // Handle DELETE request.
                    if (debug) {
                        System.out.println("Worker " + workerNumber + " got a DELETE request!");
                    }
                    handleDeleteRequest(requestLine, httpResponse);
                    break;
            
                default:
                    // Return a 405 Method Not Allowed response code.
                    buildResponseHeader(httpResponse, "405 Method Not Allowed", "text/html", 0, null);
                    break;
            }
            // Force buffered data to be written to OutputStream immediately.
            httpResponse.flush();
        } catch (Exception e) {
            System.out.println("Problem in worker grabbing the output stream! Message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Build response header.
     * @param appendMoreHeaders: used to add more headers that are not part of the hard coded ones.
     * Precondition:
     * Postcondition: HTTP response header has been written to socket output stream.
     */
    public void buildResponseHeader(OutputStream httpResponse, String statusCode, 
                                    String contentType, int contentLength,
                                    List<String> addMoreHeaders) {
        // Used to put date on response. 
        Date d = new Date();
        // Used to format data object.
        DateFormat df = DateFormat.getDateTimeInstance();
        df.setTimeZone(TimeZone.getTimeZone("GMT"));

        try {

            // Response line.
            httpResponse.write("HTTP/1.1 ".getBytes());
            httpResponse.write(statusCode.getBytes());
            httpResponse.write("\r\n".getBytes());

            // Date line.
            httpResponse.write("Date: ".getBytes());
            httpResponse.write((df.format(d)).getBytes());
            httpResponse.write("\r\n".getBytes());

            // Server line.
            httpResponse.write("Server: Marco's HTTP server.\r\n".getBytes());

            // Connection line.
            httpResponse.write("Connection: close\r\n".getBytes());

            // Content-Type line.
            httpResponse.write("Content-Type: ".getBytes());
            httpResponse.write(contentType.getBytes());
            httpResponse.write("\r\n".getBytes());

            // Add the other headers (if any).
            if (addMoreHeaders != null) {
                for (int i = 0; i < addMoreHeaders.size(); i++) {
                    httpResponse.write(addMoreHeaders.get(i).getBytes());
                    httpResponse.write("\r\n".getBytes());
                }
            }

            // HTTP header ends with 2 newlines.
            httpResponse.write("\r\n".getBytes());
        } catch (Exception e) {
            System.out.println("Error writing HTTP response header in worker code! Message: " + e.getMessage());
        }
    }

    /**
     * Handles GET request. 
     * @param requestLine first line of HTTP request.
     * @param httpResponse socket output stream to be written to.
     * Precondition:
     * Postcondition: handles GET request appropriately.
     */
    public void handleGetRequest(String requestLine, OutputStream httpResponse) {

        // Used to hold the three tokens of the request line.
        String[] requestLineTokens = requestLine.split(" ");

        // Grab URI from request line.
        String uri = requestLineTokens[1];

        switch (uri) {
            case "/":
            case "/index.html":
                // Return a simple HTML page and a 200 OK.

                // Create file pointer.
                File requestFile = new File("./cool.html");
                // Used to store contents of file in memory.
                StringBuilder fileContent = new StringBuilder();

                try {
                    
                    // Create file reader.
                    BufferedReader br = new BufferedReader(new FileReader(requestFile));

                    try {
                        // Read file contents.
                        String line;
                        while ((line = br.readLine()) != null) {
                            fileContent.append(line);
                        }
                    } catch (Exception e) {
                        System.out.println("Error reading file " + requestFile.getName() + ". Message: " + e.getMessage());
                    }
                    // Close file reader.
                    br.close();
                } catch (Exception e) {
                    System.out.println("Request file not found! Message: " + e.getMessage());
                }
                
                // Convert file contents to String.
                String fileContentString = fileContent.toString();
                int fileContentSizeInBytes = fileContentString.getBytes().length;

                // Build response header.
                buildResponseHeader(httpResponse, "200 OK", "text/html", fileContentSizeInBytes, null);
                
                // Write HTTP response body to OutputStream.
                try {
                    httpResponse.write(fileContentString.getBytes());
                } catch (Exception e) {
                    System.out.println("Error writing file contents to HTTP response body in Worker! Message: " + e.getMessage());
                }

                break;

            case "/google":
                // Redirect them to google.com.
                
                // Create a new redirect header line for google.com.
                List<String> headers = new ArrayList<>();
                headers.add("location: https://google.com");

                // Return a redirect 301 header to google.com.
                buildResponseHeader(httpResponse, "301 Moved Permanently", "text/html", 0, headers);
                break;
        
            default:

                // GET request URI doesn't exist.
                checkIfWrongHttpMethodUsed(uri, httpResponse);
                break;
        }
    }


    /**
     * Used to respond to request that is used incorrectly or doesn't exist.
     * @param uri
     * @param httpResponse
     * Precondition:
     * Postcondition: invalid request has been handled accordingly.
     */
    public void checkIfWrongHttpMethodUsed(String uri, OutputStream httpResponse) {

        // If the endpoint exists, then the wrong HTTP method was used.
        // Else, the endpoint doesn't exist.
        switch (uri) {
            case "/":
            case "/index.html":
            case "/google":
            case "/multiply":
            case "/database.php":
            case "/database.php?data=all":

                // Return a 405 Method Not Allowed response code.
                buildResponseHeader(httpResponse, "405 Method Not Allowed", "text/html", 0, null);
                break;
        
            default:

                // Return a 404 Not Found response code.
                buildResponseHeader(httpResponse, "404 Not Found", "text/html", 0, null);
                break;
        }
    }

    /**
     * Handles DELETE request.
     * @param requestLine
     * Precondition:
     * Postcondition: responds to DELETE request appropriately.
     */
    public void handleDeleteRequest(String requestLine, OutputStream httpResponse) {
        
        // Used to hold the three tokens of the request line.
        String[] requestLineTokens = requestLine.split(" ");

        // Grab URI from request line.
        String uri = requestLineTokens[1];

        switch (uri) {
            case "/database.php":
            case "/database.php?data=all":
                buildResponseHeader(httpResponse, "403 Forbidden", "text/html", 0, null);
                break;
        
            default:
                // DELETE request URI doesn't exist.
                checkIfWrongHttpMethodUsed(uri, httpResponse);
                break;
        }
    }

    /**
     * Handles POST request.
     * Precondition:
     * Postcondition: responds to POST request accordingly.
     */
    public void handlePostRequest(String requestLine, char[] requestBody, OutputStream httpResponse) {

        // Used to hold the three tokens of the request line.
        String[] requestLineTokens = requestLine.split(" ");

        // Grab URI from request line.
        String uri = requestLineTokens[1];

        switch (uri) {
            case "/multiply":

                // Check if request body exists.
                if (requestBody == null) {
                    // Return a 400 Bad Request status code.
                    buildResponseHeader(httpResponse, "400 Bad Request", "text/html", 0, null);
                    break;
                }

                // Grab response body as a String.
                String requestBodyAsString = "";
                for (int i = 0; i < requestBody.length; i++) {
                    requestBodyAsString += requestBody[i];
                }

                if (debug) {
                    System.out.println("Request body: *" + requestBodyAsString + "*.");
                }

                // Check if request body matches pattern agreed upon: "a=¡integer¿&b=¡integer¿".
                Pattern pattern = Pattern.compile("a=[-]?[0-9]+&b=[-]?[0-9]+");
                Matcher matcher = pattern.matcher(requestBodyAsString);

                // Check if body fits format we specified.
                if (!matcher.matches()) {

                    if (debug) {
                        System.out.println("Bad body! Pattern doesn't match!");
                    }
                    // Return a 400 Bad Request status code.
                    buildResponseHeader(httpResponse, "400 Bad Request", "text/html", 0, null);
                } else {
                    // Format is correct.

                    String requestAnswer = handleMultiplyRequestBody(requestBodyAsString);

                    // Build header, then attach body.
                    buildResponseHeader(httpResponse, "200 OK", "text/html", requestAnswer.getBytes().length, null);

                    // Try to write the answer to the request body.
                    try {
                        httpResponse.write(requestAnswer.getBytes());
                    } catch (IOException e) {
                        System.out.println("Error writing answer to response body in Worker! Message: " + e.getMessage());
                    }
                }
                
                break;
        
            default:
                // POST request URI doesn't exist.
                checkIfWrongHttpMethodUsed(uri, httpResponse);
                break;
        }
    }

    /***
     * Handles the /multiply request body.
     * Precondition: the body is of the form: "a=¡integer¿&b=¡integer¿"
     * Postcondition: returns the result of the multiplication.
     */
    public String handleMultiplyRequestBody(String requestBodyAsString) {
        String requestBodyAnswer = "";

        if (debug) {
            System.out.println("Pattern matches!");
        }
        
        // Parse integers out and multiply them.
        // Example input: "a=123&b=456"

        String[] splitOnAmpersandSymbol = requestBodyAsString.split("&");
        String[] splitOnFirstEqualSign = splitOnAmpersandSymbol[0].split("=");
        String[] splitOnSecondEqualSign = splitOnAmpersandSymbol[1].split("=");
        
        int a = Integer.parseInt(splitOnFirstEqualSign[1]);
        int b = Integer.parseInt(splitOnSecondEqualSign[1]);
        int multipyResult = a * b;

        if (debug) {
            System.out.println("a=" + a);
            System.out.println("b=" + b);
            System.out.println("Mult res=" + multipyResult);
        }

        // Convert answer to String.
        requestBodyAnswer = "" + multipyResult;
        
        return requestBodyAnswer;
    }

    @Override
    public void run() {
        System.out.println("Worker " + workerNumber + " has been initialized!");
        

        while (!die.get()) {

            // Take from queue until it's time to die.
            Socket sock = null;

            try {
                // Take from queue, block (wait) for up to 10 seconds.
                sock = queue.poll(10, TimeUnit.SECONDS);     
            } catch (InterruptedException e) {
                System.out.println("Error in Worker code Interrruptedd while waiting for queue! Message: " + e.getMessage());
            }

            if (sock != null) {

                if (debug) {
                    System.out.println("Worker " + workerNumber + " got work!");
                }
                    // Answer HTTP request.

                    // Initialize InputStreamReader to read HTTP request contents.
                    InputStreamReader inputStreamReader = null;
                    try {
                        inputStreamReader = new InputStreamReader(sock.getInputStream());
                    } catch (IOException e) {
                        System.out.println("Error initializing InputStreamReader in Worker code! Message: " + e.getMessage());
                    }

                    if (inputStreamReader != null) {
                        // Initialize BufferedReader to read HTTP request easily.
                        BufferedReader reader = new BufferedReader(inputStreamReader);

                        // Use StringBuilder to build and store HTTP header in memory (to be read later).
                        StringBuilder requestHeaderBuilder = new StringBuilder();

                        // Use character array to read and store HTTP body in memory (to be read later).
                        int contentLength = -1;
                        char[] httpBody = null;

                        // READ IN THE HTTP HEADER.
                        String line;
                        try {
                            // Read the HTTP request header line by line until an empty line is reached.
                            while ((line = reader.readLine()) != null && !line.isEmpty()) {

                                // Check if request contains Content-Length line (used to read in HTTP body).
                                if (line.contains("Content-Length")) {
                                    String[] lineParsed = line.split(": ");

                                    try {
                                        contentLength = Integer.parseInt(lineParsed[1]);
                                        if (debug)
                                            System.out.println("Content-Length: " + contentLength);
                                    } catch (Exception e) {
                                        System.out.println("Error trying to read in content-length of body.");
                                        System.out.println("Message: " + e.getMessage());
                                    }
                                }

                                // Append "\r\n" to end to keep HTTP format.
                                requestHeaderBuilder.append(line + "\r\n");
                            }
                        } catch (IOException e) {
                            System.out.println("Error reading request HEADER in Worker code! Message: " + e.getMessage());
                        }

                        // READ IN HTTP BODY (if any).
                        if (contentLength > 0) {
                            try {
                                // Initialize array.
                                httpBody = new char[contentLength];
                                // Put HTTP body in character array.
                                reader.read(httpBody, 0, contentLength);
                            } catch (Exception e) {
                                System.out.println("Error reading in HTTP body in Worker code! Message: " + e.getMessage());
                            }
                        }

                        // Turn request HEADER into an immutable String so that it can't be changed accidently.
                        String httpRequestHeader = requestHeaderBuilder.toString();

                        // Handle HTTP request.
                        answerHttpRequest(httpRequestHeader, httpBody, sock);

                        // Close socket, HTTP request handled.
                        try {
                            sock.close();
                        } catch (Exception e) {
                            System.out.println("Error closing socket! Message: " + e.getMessage());
                        }
                    }
            }
        }

        if (debug) {
            System.out.println("Worker " + workerNumber + " has killed itself!");
        }
    }

}
