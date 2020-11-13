import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.IOException;
import java.lang.InterruptedException;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

import java.io.File;
import java.io.FileInputStream;

/**
 * <h1>Client Class</h1>
 *
 * <p>
 * Implements Runnable. Used to create a thread that broadcasts the rover's routing table
 * to its neighbors (every 5 seconds).
 * </p>
 *
 * @author: Alberto Serrano-Calva (axs4986)
 */
public class Client implements Runnable
{
    private static final int BROADCAST_INTERVAL = 1;
    private static final int CHUNK_SIZE         = 10;

    private InetAddress    src_address;
    private InetAddress    dest_addr;
    private DatagramSocket socket;
    private int            port;

    // State of current object.
    private String         filename;
    private List<Packet>   packets;
    private long           ack_num;
    private long           seq_num;

    /**
     * Constructor: Creates a broadcasting object and set up socket and group prior
     * to executing thread.
     *
     * @throws IOException
     *
     * @param broadcast_addr Source IPv4 address.
     * @param port           Port from which to broadcast.
     *
     * @return Nothing.
     */
    public Client(String dest_addr, int port, String filename) throws IOException
    {
        this.dest_addr    = InetAddress.getByName(dest_addr);
        this.port         = port;
        this.src_address  = InetAddress.getLocalHost();
        this.packets      = new ArrayList<>();
        this.filename     = filename;
        
        // TODO: Read in file and setup byte chunks.
        //getByteChunksFromFile(msg_file);

        // Socket setup.
        this.socket       = new DatagramSocket(this.port);
    }

    private void initHandshake()
    {
        Packet init_p = new Packet();
        this.seq_num  = ThreadLocalRandom.current().nextLong(1, 4000);

        init_p.setSeqNum(this.seq_num);
        init_p.setSynFlag();

        byte[] init_bytes = init_p.toByteArray();

        // TODO: repeat request for connection until we get a response
        try
        {
            DatagramPacket send_packet = new DatagramPacket(
                init_bytes, init_bytes.length, this.dest_addr, this.port);

            this.socket.send(send_packet);

            System.out.println("\nSENDING syn with seq: " + this.seq_num);

        } catch( IOException ex ) 
        {
            ex.printStackTrace();
        }

        while( true )
        {
            byte[] buffer = new byte[Router.BUFFER_SIZE];

            try
            {
                DatagramPacket recv_packet = new DatagramPacket(buffer, buffer.length);
                this.socket.receive(recv_packet);
                
                Packet p = new Packet(recv_packet);

                if( p.getAckNum() == (this.seq_num + 1) )
                {

                    Packet res = new Packet();

                    this.seq_num = p.getAckNum();
                    res.setSeqNum(this.seq_num);
                    //res.setSynFlag(); // TODO: Check if we need to set syn flag again.

                    this.ack_num = p.getSeqNum() + 1; 
                    res.setAckNum(this.ack_num);
                    res.setAckFlag();

                    // Set request file command.
                    res.setCommand(1);
                    res.setData(this.filename);

                    byte[] res_bytes = res.toByteArray();
                    DatagramPacket send_packet = new DatagramPacket(
                        res_bytes, res_bytes.length, 
                        recv_packet.getAddress(), recv_packet.getPort());

                    // Send response to SYN+ACK to finalize connection.
                    this.socket.send(send_packet);

                    // We're done here.
                    System.out.println("CLIENT: Handshake completed");
                    break;
                }                                     
            } catch( IOException ex )
            {
                ex.printStackTrace();
            }
        }
        System.out.println("CLIENT: Connection established!");
    }

    /**
     * Starts the broadcast process/thread. If the size of the rover's RIP table is
     * empty, it will make a RIP request to its neighbors for routing information.
     * If there is something in the RIP routing table, then broadcasts those routes
     * to all its neighbors. Does this every 5 seconds.
     *
     * @return Nothing.
     */
    @Override
    public void run()
    {
        // Initiate handshake.
        initHandshake();

        // Assume message will be request unless router has routes to send.
        byte[] buffer = new byte[Router.BUFFER_SIZE];

        while( true )
        {
            try
            {
                // Receive packets.
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

            } catch( IOException ex )
            {
                ex.printStackTrace();
            }
        }
    }

    private void getByteChunksFromFile(File file)
    {
        FileInputStream fileInputStream = null;
        byte[] bFile = new byte[(int) file.length()];

        try
        {
            fileInputStream = new FileInputStream(file);
            fileInputStream.read(bFile);
            fileInputStream.close();
        }

        catch( Exception ex )
        {
            ex.printStackTrace();
        }

        // Setup packet header values.
        int  syn     = 0, 
             ack     = 0, 
             fin     = 0;
        long seq_num = 0,
             ack_num = 0;

        List<Packet> packets_to_send = new ArrayList<>();

        for( int i = 0; i < bFile.length; i += CHUNK_SIZE )
        {
            byte[] byte_chunk = Arrays.copyOfRange(bFile, i, Math.min(bFile.length, i+CHUNK_SIZE));
            this.packets.add(new Packet(ack_num, seq_num, byte_chunk, syn, ack, fin));

            System.out.println("====================== Sending ==================");
            System.out.println(new String(byte_chunk, StandardCharsets.UTF_8));
        }
        System.out.println("Chunk Number: " + this.packets.size());
    }
}
