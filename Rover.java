import java.net.*;
import java.io.*;
import java.util.*;
import java.net.InetAddress;
import java.io.File;

/**
 * <h1>Rover Class</h1>
 *
 * <p>
 * Class is used to start single rover process and spin up its router, along with
 * other processes such as the garbage collector, router, etc.
 * </p>
 *
 * @author: Alberto Serrano-Calva (axs4986)
 *
 */
public class Rover
{
    private static final int PORT = 63002;


    /**
     * Main method for starting a rover.
     *
     * @param args Must be of size one to run and should contain a single integer, which will
     *             become rover's node number.
     *
     * @return Nothing.
     */
    public static void main(String args[])
    {
        String dest_addr = null;
        String msg_path  = null;
        //File   msg_file  = null;
        Thread client    = null;

        System.out.println();

        // Read arguments for sender mode.
        if( args.length == 2 )
        {
            // Extract node number.
            dest_addr = args[0];
            msg_path  = args[1];
            
            //msg_file  = new File(msg_path);
            
            // Check if destination IP address is valid.
            if( !validIP(dest_addr) )
            {
                System.out.format("Invalid destination IP address: %s\n", dest_addr);
                return;
            }

            System.out.println("Rover Mode  : CLIENT");
            System.out.format("Destination : %s\nFile Request: %s\n", dest_addr, msg_path);
        }

        // Read arguments for receiver mode.
        else if( args.length == 0 )
        {
            System.out.println("Rover Mode  : SERVER");
        }

        // Exit if number of arguments is incorrect.
        else
        {
            System.out.println("sendr usage: java Rover <dest-address> <file>");
            System.out.println("recv  usage: java Rover");
            return;
        }

        try
        {
            // NOTE: Do we even need local IP?
            InetAddress localhost   = InetAddress.getLocalHost();
            String address          = (localhost.getHostAddress()).trim();
            System.out.println("Rover IP    : " + address);

            // Start router on thead.
            if( args.length == 2 )
                client = new Thread(new Router(dest_addr, PORT, address, msg_path));
            else
                client = new Thread(new Router(PORT, address));

            client.start();

            // Keep program alive.
            while( true ) { Thread.sleep(1000); }

        } catch( Exception ex )
        {
            ex.printStackTrace();
        }
    }

    /**
     * Validates a string representing an IPv4 address.
     *
     * @param ip String representation of IPv4 address.
     *
     * @return boolean Denotes if address is valid.
     */
    public static boolean validIP(String ip) {
        try 
        {
            if ( ip == null || ip.isEmpty() )
                return false;

            String[] parts = ip.split( "\\." );
            if ( parts.length != 4 )
                return false;

            for ( String s : parts ) 
            {
                int i = Integer.parseInt( s );
                if ( (i < 0) || (i > 255) )
                    return false;
            }

            return true;

        } catch (NumberFormatException nfe) 
        {
            return false;
        }
    }
}
