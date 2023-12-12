/**
 * Marco Martinez
 * Computer Networks I
 * Programming Project 2: Building a Web Server
 * WebServerMain.java
 * 
 * Main Server class. This is where the server gets started and managed. 
 * 11/09/2023
 */

// I/O exception.
import java.io.IOException;

// Server socket waits for requests to come in over the 
// network.
import java.net.ServerSocket;

// Thread safe variable used to kill server gracefully.
import java.util.concurrent.atomic.AtomicBoolean;

// Used for user input.
import java.util.Scanner;

public class WebServerMain {

    // Variable used for debugging purposes only.
    private static boolean debug = true;

    // Use port 8080.
    private static final int PORT = 8080;

    // ServerSocket used for incoming connections.
    public static ServerSocket ss;

    // Used to stop server gracefully.
    public static AtomicBoolean die = new AtomicBoolean(false);

    // Create the threads and start them.
    static MainThread mainWorker;
    // Create main thread and start it.
    static Thread mainThread;

    /**
     * Start server pool of threads.
     * Precondition: ServerSocket has been initialized and is waiting for 
     * Postcondition: pool of threads has been started. The number of threads
     *                is determined by the number of cores in the cpu.
     * @param ss a server socket.
     */
    public static void startThreads(ServerSocket ss) {

        // Create the threads and start them.
        mainWorker = new MainThread(ss, die);
        // Create main thread and start it.
        mainThread = new Thread(mainWorker);
        mainThread.start();
    }

    public static void main(String[] args) throws IOException {

        Scanner scan = new Scanner(System.in);

        // Bind ServerSocket with port 8080.
        try {
            ss = new ServerSocket(PORT);
            // Set timeout to 30 seconds.
            ss.setSoTimeout(30000);

            if (debug) {
                System.out.println("Server started on 127.0.0.1:8080");
            }

            // Start threads.
            startThreads(ss);

            String userInput = new String();

            while (true) {
                // Kill server when user enters: "<die>".
                System.out.println("Let me know when you want server to die. Enter <die>: ");
                userInput = scan.nextLine();

                if (userInput.equals("<die>")) {

                    // Tell all threads to die.
                    die.set(true);

                    // Close ServerSocket.
                    try {
                        ss.close();
                    } catch (IOException e) {
                        System.out.println("Error closing ServerSocket! Message: " + e.getMessage());
                    }
                    
                    break;
                }
            }
        
            System.out.println("WebServerMain thread is about to die. Server should be completely dead. Use ps aux to check.");
            System.out.println("Workers may take up to 10 seconds to die. Patience is a virtue.");


        } catch (IOException e) {

            System.out.println("Error binding ServerSocket to port " + PORT + ".");
            System.out.println(e.toString());
            System.exit(1);
        }

        // Close scanner.
        scan.close();
    }
}