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
    private int            dest_port;

    // State of current object.
    private long           ack_num;
    private long           seq_num;
    private long           init_seq_num;
    
    private InputStream    file_stream;
    private int            last_command;

    public Server(String dest_addr, int port) throws IOException
    {
        //this.dest_addr    = InetAddress.getByName(dest_addr);
        this.dest_addr    = null;
        this.port         = port;
        this.dest_port    = port; // temp assignment
        this.src_address  = InetAddress.getLocalHost();
        this.file_stream  = null;
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
                    // Get destination address and port.
                    this.dest_addr = recv_packet.getAddress();
                    this.dest_port = recv_packet.getPort();

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
                            this.dest_addr, this.dest_port);
                    
                    // Send SYN+ACK response.
                    this.socket.send(res_packet);
                }

                // Get final response from client, if server sequence num (+1) matches
                // ack number sent, connection is established.
                else if( p.isAckFlagSet() && (p.getAckNum() == ++this.seq_num) )
                {
                    System.out.println("SERVER: Handshake complete");
                    this.last_command = p.getCommand();
                    this.init_seq_num = this.seq_num;

                    // Check for file request.
                    // No other scenarios to include.
                    if( this.last_command == 1 )
                    {
                        // Save file if file does exist.
                        File file_temp;
                        if( checkFile(file_temp = new File(p.getDataStr())) )
                        {
                            System.out.format("SERVER: File %s exists!\n", file_temp);
                            this.file_stream  = new FileInputStream(file_temp);
                        }
                    } break;
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

    private boolean checkFile(File file_req)
    {
        return (file_req.exists() && file_req.isFile());
    }

    private void sendFileChunk()
    {

    }

    @Override
    public void run()
    {
        // Wait for handshake before proceeding to accept packets.
        awaitHandshake();

        // TODO Check what command is
        // If file transfer, check if 

        byte[] buffer = new byte[Router.BUFFER_SIZE];

        //
        // Receive
        // Parse commands / ACK (potentially will have re-send)
        //     If triple ack resend.
        while( true )
        {
            // Allocate buffer to send.
            byte[] send_buffer = new byte[Router.CHUNK_SIZE];

            try
            {
                if( this.file_stream.read(send_buffer) != -1 )
                {
                    Packet file_packet = new Packet();

                    // Set command to file response.
                    file_packet.setCommand(2);

                    // Set ack and seq nums.
                    file_packet.setSeqNum(this.seq_num);
                    
                    // Add data.
                    file_packet.setData(send_buffer);

                    byte[] file_packet_bytes = file_packet.toByteArray();

                    // Receive packets.
                    DatagramPacket recv_packet = new DatagramPacket(
                            file_packet_bytes, file_packet_bytes.length,
                            this.dest_addr, this.dest_port);
                    socket.send(recv_packet);

                    this.seq_num += file_packet.getData().length;
                }

                DatagramPacket ack_packet = new DatagramPacket(buffer, buffer.length);
                this.socket.receive(ack_packet);
                Packet ack = new Packet(ack_packet);

                this.ack_num = ack.getAckNum();
                
            } catch( IOException ex )
            {
                ex.printStackTrace();
            }
        }
    }
}
