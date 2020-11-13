import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.MulticastSocket;
import java.io.IOException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * <h1>Router Class</h1>
 *
 * <p>
 * Router class's purpose is to listen to incoming packets, broadcast this rover's routes or
 * request for routes, timeout routes appropriately, and parse received packets.
 * </p>
 *
 * @author: Alberto Serrano-Calva (axs4986)
 */
public class Router implements Runnable
{
    public static final int BUFFER_SIZE = 1024;
    public static final int CHUNK_SIZE  = 10;

    private DatagramSocket socket;
    private InetAddress    group;
    private String         destination_address;
    private int            port;
    private String         source_address;

    /**
     * Constructor: Creates router broadcast object and spawns a new thread, such that
     *              a RIP request/response is sent out every 5 seconds. Also uses the
     *              global garbage collector object and starts a new thread to time
     *              out any neighbors that have been silent for too long.
     *
     * @param dest_addr
     * @param port
     * @param address
     *
     * @throws IOException
     */
    public Router(String dest_addr, int port, String src_addr, String filename)
            throws IOException
    {
        this.destination_address = dest_addr;
        this.port                = port;
        this.source_address      = src_addr;

        // Start client thread.
        Thread client_th = new Thread(new Client(this.destination_address, this.port, 
                    filename));
        client_th.start();
    }

    public Router(int port, String src_addr) throws IOException 
    {
        this.port           = port;
        this.source_address = src_addr;

        // Start server thread
        Thread server_th = new Thread(new Server(this.destination_address, this.port));
        server_th.start();
    }

    /**
     * Run method: Loops forever attempting to receive packet. Once a packet has been received,
     * a new RouterParse object is created and is run using a thread. The process of 
     * receiving packets then continues as normal until another packet is received.
     *
     * @return Nothing.
     */
    @Override
    public void run()
    {
        return;
    }
}
