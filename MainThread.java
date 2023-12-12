/***
 * Marco Martinez
 * Computer Networks I
 * Programming Project 2: Building a Web Server
 * MainThread.java
 * 
 * Implementation of the server's main thread that manages
 * the three worker threads.
 * 
 * Sources:
 * BlockingQueue help: https://www.youtube.com/watch?v=d3xb1Nj88pw
 */
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainThread implements Runnable {

    private boolean debug = true;

    /***
     * Thread-safe blocking queue. (This data structure makes it so that producer thread
     * blocks when inserting into queue, and consumer blocks when taking from head of queue.)
     * Example: if queue is full, then producer will block until item has been taken from
     * the front (ie theres space now). If queue is empty, consumers will block until there is
     * an item in the queue.
     * 
     * Also BlockingQueue is an interface, so we need to create an instance of a class that
     * implements the BlockingQueue interface. In this case, we use the LinkedList impl.
     */
    public static BlockingQueue<Socket> queue;
    // Used to kill server gracefully.
    public static AtomicBoolean die;
    // Server socket.
    public static ServerSocket ss;

    // Constructor.
    public MainThread(ServerSocket ss, AtomicBoolean die) {
        MainThread.queue = new LinkedBlockingQueue<Socket>();
        MainThread.die = die;
        MainThread.ss = ss;
    }

    @Override
    public void run() {
        // Listen for new requests.
        // Generate worker threads that consume from blocking queue.
        if (debug) {
            System.out.println("Main thread has been initialized!");
        }

        // Do a system call to find out how many cores the CPU has.
        // Generate a thread for every core in the CPU.
        int cores = Runtime.getRuntime().availableProcessors();

        if (debug) {
            System.out.println("Cores: " + cores);
        }

        // Create list of workers.
        List<Worker> workers = new ArrayList<Worker>();
        
        int[] init_worker_number = {1};
        for (int i = 0; i < cores; i++) {
            workers.add(new Worker(MainThread.queue, MainThread.die, init_worker_number));
        }

        // Create and start worker threads.
        for (int i = 0; i < workers.size(); i++) {
            new Thread(workers.get(i)).start();
        }

        // Used to store current request IP address.
        String currentRequestIP = "";
        while (!die.get()) {
            // Grab request from ss and put them in BlockingQueue.
            try {
                // Accept new connection. (Times out after 30 seconds.)
                Socket sock = ss.accept();
                currentRequestIP = sock.getInetAddress().getHostAddress();

                try {
                    queue.offer(sock);
                } catch (Exception e) {
                    System.out.println("Queue Exception: " + e.getMessage());
                }

            } catch (Exception e) {
                System.out.println("Socket exception MainThread: " + e.getMessage());
                System.out.println("Last socket connection was to: " + currentRequestIP);
            }
        }

        System.out.println("Main thread has killed itself!");

    }
    
}
