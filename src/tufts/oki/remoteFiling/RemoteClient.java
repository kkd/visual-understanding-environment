/*
 * -----------------------------------------------------------------------------
 *
 * <p><b>License and Copyright: </b>The contents of this file are subject to the
 * Mozilla Public License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at <a href="http://www.mozilla.org/MPL">http://www.mozilla.org/MPL/.</a></p>
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.</p>
 *
 * <p>The entire file consists of original code.  Copyright &copy; 2003, 2004 
 * Tufts University. All rights reserved.</p>
 *
 * -----------------------------------------------------------------------------
 */

/*
 * RemoteClient.java
 *
 * Created on November 11, 2003, 11:32 AM
 */

package tufts.oki.remoteFiling;
import org.apache.commons.net.ftp.*;
import java.io.*;
import java.util.*;

/**
 *  The RemoteClient class encapsulates an FTP-client session.  A host name, username,
 *  and password is required to establish the connection.  checkClient() can be used
 *  to check to see if the connection is still present, and if not, re-establish it.
 *  <p>
 *  Since RemoteClient uses static variables and methods, only one FTP session can be
 *  active at a particular time.  Usually, this is not a limitation.
 *
 *  @author  Mark Norton
 */
public class RemoteClient {
    private static String server = null;        //  Cached remote server name.
    private static String username = null;      //  Cached user name for remote server.
    private static String password = null;      //  Cached password for remote server.
    private static String rootBase = null;      //  Cached root base.
    private static FTPClient client = null;     //  Current FTP client and session.
    
    /** 
     *  Creates a new instance of RemoteClient.  Client is opened based on host, username,
     *  and password provided.  These are cached in order to re-establishe the connection
     *  should it be dropped.
     */
    public RemoteClient(String host, String username, String password) throws osid.filing.FilingException {
        try {
            FTPClientFactory factory = new FTPClientFactory(host,username,password);
            this.client = factory.createClient();
            this.rootBase = client.printWorkingDirectory();
        }
        catch (java.io.IOException ex1) {
            throw new osid.filing.FilingException (osid.filing.FilingException.OPERATION_FAILED);
        }
        catch (osid.filing.FilingException ex2) {
            throw new osid.filing.FilingException (osid.filing.FilingException.OPERATION_FAILED);
        }
        server = host;             //  Cache the server name.
        username = username;       //  Cache the user name.
        password = password;       //  Cache the password.
    }

    /**
     *  Close the client connection.
     */
    public static void disconnect() throws osid.filing.FilingException {
        if (client != null) {
            try {
                client.disconnect();
                client = null;
            }
            catch (java.io.IOException ex2) {
                throw new osid.filing.FilingException (osid.filing.FilingException.IO_ERROR);
            }
        }
    }
    
    /**
     *  Checks if the filing manager has an ftp client. 
     */
    public static boolean hasClient() {
        return (client != null);
    }
     
    
    /**
     *  Gets an FTP client.
     *  Why would classes outside of the FTPFilingManager need access to this?  All FTP
     *  Functions should be encapsulated in the FTPFilingManager.  This is a candidate for
     *  removal, but is left in place for testing purposes.
     */
    public static FTPClient getClient() throws osid.filing.FilingException {
        checkClient();  //  Check to make sure connection is still active.
        return client;
    }
    
    /**
     *  Get the rootBase for the current FTP session.
     *
     *  author Mark Norton
     */
    public static String getRootBase() {
        return rootBase;
    }

    
    /**
     *  Some FTP servers limit the amount of time that a client may stay connected to it.
     *  This function checks to see if the session is still active by pinging the remote
     *  server.  If connection has been dropped, it is re-established.  No change is
     *  made to the initialization of root, or the current working directory.
     */
    private static void checkClient() throws osid.filing.FilingException {
        if (client == null)
            throw new osid.filing.FilingException (osid.filing.FilingException.ITEM_DOES_NOT_EXIST);
        
        //  Check the status to see if connection is still active.
        try {
            client.getStatus();
        }
        catch (java.io.IOException ex1) {
            //  If there is no status, then re-establish the connection based on cached names.
            try {
                FTPClientFactory factory = new FTPClientFactory(server,username,password);
                client = factory.createClient();
            }
            catch (osid.filing.FilingException ex2) {
                throw new osid.filing.FilingException (osid.filing.FilingException.OPERATION_FAILED);
            }
        }
    }
    
    public static String getServerName () {
        return server;
    }

}
