import java.net.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.ThreadLocalRandom;


public class Server implements Runnable
{
    private DatagramSocket socket;
    private InetAddress    src_address;
    private InetAddress    dest_addr;
    private int            port;

    // State of current object.
    private List<Packet>   packets;
    private long           ack_num;
    private long           seq_num;
    
    private File         file_request;
    private int            last_command;

    public Server(String dest_addr, int port) throws IOException
    {
        this.dest_addr    = InetAddress.getByName(dest_addr);
        this.port         = port;
        this.src_address  = InetAddress.getLocalHost();
        this.packets      = new ArrayList<>();
        this.file_request = null;
        this.last_command = 0;
        
        // Socket setup.
        this.socket       = new DatagramSocket(this.port);
    }

    private void awaitHandshake()
    {
        byte[] buffer = new byte[Router.BUFFER_SIZE];
        
        while( true )
        {
           try
           {
                // Receive packets.
                DatagramPacket recv_packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(recv_packet);
                
                Packet p = new Packet(recv_packet);
                
                // Wait for packet trying to establish connection, SYN will be set.
                if( p.isSynFlagSet() )
                {
                    Packet res = new Packet();

                    // Set acknowledgement.
                    this.ack_num = p.getSeqNum() + 1;
                    res.setAckNum(this.ack_num);
                    res.setAckFlag();

                    // Set syn and seq.
                    this.seq_num = ThreadLocalRandom.current().nextLong(1, 4000);
                    res.setSeqNum(this.seq_num);
                    res.setSynFlag();

                    byte[] res_bytes = res.toByteArray();

                    // Create packet to send.
                    DatagramPacket res_packet = new DatagramPacket(
                            res_bytes, res_bytes.length, 
                            recv_packet.getAddress(), recv_packet.getPort());
                    
                    // Send SYN+ACK response.
                    this.socket.send(res_packet);
                }

                // Get final response from client, if server sequence num (+1) matches
                // ack number sent, connection is established.
                else if( p.isAckFlagSet() && (p.getAckNum() == ++this.seq_num) )
                {
                    System.out.println("SERVER: Handshake complete");
                    this.last_command = p.getCommand();

                    // Check for file request.
                    // No other scenarios to include.
                    if( this.last_command == 1 )
                    {
                        this.file_request = new File(p.getDataStr());
                    }
                    break;
                }

                // If nothing matches, something might be breaking.
                else
                    System.out.println(
                        "Something went wrong while trying to establish connection..");

            } catch( IOException ex )
            {
                ex.printStackTrace();
            }
        }

        // We did it!
        System.out.println("SERVER: Connection established!");
    }

    private boolean checkFile()
    {
        return (this.file_request.exists() && this.file_request.isFile());
    }

    @Override
    public void run()
    {
        // Wait for handshake before proceeding to accept packets.
        awaitHandshake();

        if( checkFile() ) System.out.format("SERVER: File '%s' exists!\n", this.file_request);
        // TODO Check what command is
        // If file transfer, check if 

        byte[] buffer = new byte[Router.BUFFER_SIZE];
        List<Packet> packet_arr = new ArrayList<>();
        int counter   = 0;
        
        //FileOutputStream fos = null;
        //try
        //{
        //    fos = new FileOutputStream("out.txt");
        //} catch( Exception ex )
        //{
        //    ex.printStackTrace();
        //}
        //fos.write(temp);
        //fos.flush();
        //
        
        while( true )
        {
            try
            {
                // Receive packets.
                DatagramPacket recv_packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(recv_packet);
                
                packet_arr.add(new Packet(recv_packet));
                
            } catch( IOException ex )
            {
                ex.printStackTrace();
            }
        }
    }
}
