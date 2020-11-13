import java.net.DatagramPacket;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.nio.ByteBuffer;

/**
 * <h1>Packet Class</h1>
 *
 * <p>
 * </p>
 *
 * @author: Alberto Serrano-Calva (axs4986)
 */
public class Packet
{
    public  static final int HEADER_SIZE = 12;
    private static final boolean VERBOSE = true;

    private int syn_flag;
    private int ack_flag;
    private int fin_flag;

    private long    acknowledgement_num;
    private long    sequence_num;
    private byte[]  payload;

    // Commands:
    //   0 - Nothing, used for connection establishment and closing, and ACKs
    //   1 - File requests
    //   2 - File responses
    //   3 - Error: file does not exist
    private int     command;

    // TODO
    private int     checksum;
    private int     window;
    
    /**
     * Constructor. Create empty packet.
     */
    public Packet()
    {
        this.syn_flag            = 0;
        this.ack_flag            = 0;
        this.fin_flag            = 0;
        this.command             = 0;
        this.acknowledgement_num = 0l;
        this.sequence_num        = 0l;
        this.payload             = new byte[0];
    }

    /**
     * Constructor. Creates Packet object from data and options.
     */
    public Packet(long ack_num, long seq_num, byte[] data, int syn_f, int ack_f, 
            int fin_f)
    {
        this.syn_flag            = syn_f;
        this.ack_flag            = ack_f;
        this.fin_flag            = fin_f;
        this.acknowledgement_num = ack_num;
        this.sequence_num        = seq_num;
        this.payload             = data;
    }

    /**
     * Constructor. Creates Packet object by parsing received bytes.
     */
    public Packet(DatagramPacket recv_packet)
    {
        // Parse fields and populate object.
        parseBytes(recv_packet);
    }

    // Get single bit value (1 or 0) at specific position.
    private int getBit(int val, int pos)
    {
        return (val >>> pos) & 1;
    }
        
    private void printPacketContents()
    {
        System.out.println("\n=============== Parsing ===============");
        System.out.format("SYN: %d\nACK: %d\nFIN: %d\n", 
                this.syn_flag, this.ack_flag, this.fin_flag);

        System.out.format("Command   : %d\nACK Number: %d\nSEQ Number: %d\nMessage:\n",
                this.command, this.acknowledgement_num, this.sequence_num);

        System.out.println(new String(this.payload, StandardCharsets.UTF_8));
        System.out.println("=======================================\n");
    }

    private void parseBytes(DatagramPacket recv_packet)
    {
        // Get message and size of packet (including header and data).
        int data_size = recv_packet.getLength();
        byte[] msg    = recv_packet.getData();

        // Parse header and actual data.
        byte[] header = Arrays.copyOfRange(msg, 0, HEADER_SIZE);
        byte[] data   = Arrays.copyOfRange(msg, HEADER_SIZE, data_size);

        // Extract header.
        ByteBuffer bb = ByteBuffer.wrap(header);

        // Get header options.
        int options   = bb.get();
        this.syn_flag = getBit(options, 6);
        this.ack_flag = getBit(options, 5);
        this.fin_flag = getBit(options, 4);

        // Get command.
        this.command  = bb.get() & 0xFF;

        // Skip three bytes of header.
        bb.position(bb.position() + 2);

        // Get ack and seq numbers.
        this.acknowledgement_num = bb.getInt() & 0xFFFFFFFFL;
        this.sequence_num        = bb.getInt() & 0xFFFFFFFFL;

        // Extract payload.
        this.payload = data;

        // Print contents of received packet.
        if( VERBOSE ) printPacketContents();
    }

    public byte[] toByteArray()
    {
        byte[] byte_packet = new byte[HEADER_SIZE + this.payload.length];
        int cursor = 0;

        /* Setup header */
        
        // Setup options
        byte options = 0x00;
        options |= this.syn_flag << 6;
        options |= this.ack_flag << 5;
        options |= this.fin_flag << 4;
        byte_packet[cursor++] = options;

        byte_packet[cursor++] = (byte) this.command;

        // Make next 2 bytes 0.
        for( int i = 0; i < 2; i++ )
            byte_packet[cursor++] = (byte) 0;

        // Add acknowledgement number.
        byte_packet[cursor++] = (byte) (this.acknowledgement_num >> 24);
        byte_packet[cursor++] = (byte) (this.acknowledgement_num >> 16);
        byte_packet[cursor++] = (byte) (this.acknowledgement_num >> 8 );
        byte_packet[cursor++] = (byte)  this.acknowledgement_num;

        // Add sequence number.
        byte_packet[cursor++] = (byte) (this.sequence_num >> 24);
        byte_packet[cursor++] = (byte) (this.sequence_num >> 16);
        byte_packet[cursor++] = (byte) (this.sequence_num >> 8 );
        byte_packet[cursor++] = (byte)  this.sequence_num;

        // Copy data into byte array.
        for( int i = 0; i < payload.length; i++ )
        {
            byte_packet[cursor++] = this.payload[i];
        }

        return byte_packet;
    }

    public boolean isSynFlagSet()
    {
        return this.syn_flag == 1;
    }
    
    public boolean isAckFlagSet()
    {
        return this.ack_flag == 1;
    }

    public boolean isFinFlagSet()
    {
        return this.fin_flag == 1;
    }

    public long getSeqNum()
    {
        return this.sequence_num;
    }

    public long getAckNum()
    {
        return this.acknowledgement_num;
    }

    public byte[] getData()
    {
        return this.payload;
    }

    public String getDataStr()
    {
        return new String(this.payload, StandardCharsets.UTF_8);
    }

    public int getCommand()
    {
        return this.command;
    }

    public void setSynFlag()
    {
        this.syn_flag = 1;
    }

    public void setAckFlag()
    {
        this.ack_flag = 1;
    }

    public void setFinFlag()
    {
        this.fin_flag = 1;
    }

    public void setCommand(int cmd)
    {
        this.command = cmd;
    }

    public void setAckNum(long ack_num)
    {
        this.acknowledgement_num = ack_num;
    }

    public void setSeqNum(long seq_num)
    {
        this.sequence_num = seq_num;
    }

    public void setData(byte[] msg)
    {
        this.payload = msg;
    }

    public void setData(String msg)
    {
        this.payload = msg.getBytes();
    }
}
