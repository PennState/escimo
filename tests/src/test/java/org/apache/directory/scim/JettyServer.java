package org.apache.directory.scim;


import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.ContextEntry;
import org.apache.directory.server.core.annotations.CreateDS;
import org.apache.directory.server.core.annotations.CreateIndex;
import org.apache.directory.server.core.annotations.CreatePartition;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.factory.DSAnnotationProcessor;
import org.apache.directory.server.factory.ServerAnnotationProcessor;
import org.apache.directory.server.ldap.LdapServer;
import org.apache.directory.server.ldap.replication.provider.SyncReplRequestHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

/**
 * Class for starting Jetty server with the escimo-server war
 */
public class JettyServer
{
    private static Server server;

    private static LdapServer ldapServer;

    private static CoreSession adminSession;
    
    public static void start() throws Exception
    {
        if ( ( server != null ) && server.isRunning() )
        {
            return;
        }
        
        startLdapServer();
        
        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath( "/" );
        webapp.setWar( getEscimoWar().getAbsolutePath() );
        webapp.setParentLoaderPriority( true );
        String cpath = System.getProperty("java.class.path");
        
        //checkForJdk6Compliance( cpath );
        
        cpath = cpath.replaceAll( ":", ";" );
//        webapp.setExtraClasspath( cpath );

        server = new Server( 8080 );
        server.setHandler( webapp );
        server.start();
    }


    public static void stop()
    {
        if ( server != null )
        {
            try
            {
                server.stop();
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
        }
        
        if( ldapServer != null )
        {
            ldapServer.stop();
        }
    }


    private static File getEscimoWar()
    {
        String msg = "No escimo-server war file found, please build escimo-server project first and  then either set the escimo.test.version property or run the test from 'tests' folder";

        FilenameFilter ff = new FilenameFilter()
        {
            public boolean accept( File dir, String name )
            {
                return ( name.startsWith( "escimo" ) && name.endsWith( ".war" ) );
            }
        };

        String testVersion = System.getProperty( "escimo.test.version" );

        if ( testVersion == null )
        {
            String pwd = System.getProperty( "user.dir" );
            File file = new File( pwd );

            File serverFolder = new File( file.getParentFile(), "server/target" );

            if ( serverFolder.exists() )
            {
                File[] files = serverFolder.listFiles( ff );

                if ( files.length == 1 )
                {
                    return files[0];
                }
            }
            else
            {
                throw new RuntimeException( msg );
            }
        }

        String m2repoLoc = System.getProperty( "user.home" )
            + "/.m2/repository/org/apache/directory/scim/escimo-server/" + testVersion;
        String war = m2repoLoc + "/escimo-server-" + testVersion + ".war";
        File warFile = new File( war );

        if ( !warFile.exists() )
        {
            throw new RuntimeException( msg );
        }

        return warFile;
    }

    
    
    @CreateDS(
        allowAnonAccess = true,
        name = "provider-replication",
        enableChangeLog = false,
        partitions =
            {
                @CreatePartition(
                    name = "example",
                    suffix = "dc=example,dc=com",
                    indexes =
                        {
                            @CreateIndex(attribute = "objectClass"),
                            @CreateIndex(attribute = "dc"),
                            @CreateIndex(attribute = "ou")
                    },
                    contextEntry = @ContextEntry(entryLdif =
                        "dn: dc=example,dc=com\n" +
                            "objectClass: domain\n" +
                            "dc: example"))
        })
    @CreateLdapServer(transports =
        { @CreateTransport(port = 10389, protocol = "LDAP") })
    public static void startLdapServer() throws Exception
    {
        DirectoryService provDirService = DSAnnotationProcessor.getDirectoryService();

        ldapServer = ServerAnnotationProcessor.getLdapServer( provDirService );
        ldapServer.setReplicationReqHandler( new SyncReplRequestHandler() );
        ldapServer.startReplicationProducer();

        Runnable r = new Runnable()
        {

            public void run()
            {
                try
                {
                    adminSession = ldapServer.getDirectoryService().getAdminSession();
                }
                catch ( Exception e )
                {
                    throw new RuntimeException( e );
                }
            }
        };

        Thread t = new Thread( r );
        t.setDaemon( true );
        t.start();
        t.join();
    }
    
    /**
     * @return the adminSession
     */
    public static CoreSession getAdminSession()
    {
        return adminSession;
    }


    private static void checkForJdk6Compliance( String classpath ) throws Exception
    {
        String[] files = classpath.split( ":" );
        StringBuilder offendingJars = new StringBuilder();
        
        for( String item : files )
        {
            //System.out.println("Processing " + item);
            boolean valid = false;
            if( item.endsWith( ".jar" ) )
            {
                valid = isValidJar( item );
                if( !valid )
                {
                    offendingJars.append( item )
                    .append( "\n" );
                }
            }
            else if( item.endsWith( ".war" ) )
            {
                
            }
        }
        
        if( offendingJars.length() > 0 )
        {
            System.out.println("Offending jars:\n" + offendingJars);
            System.exit( 0 );
        }
    }

    private static boolean isValidJar( String jarFilePath ) throws Exception
    {
        byte[] majorVersion = new byte[8];
        
        JarFile jar = new JarFile( jarFilePath );
        Enumeration<JarEntry> en = jar.entries();
        while( en.hasMoreElements() )
        {
            JarEntry je = en.nextElement();
            if( je.isDirectory() )
            {
                continue;
            }
            
            if( !je.getName().endsWith( ".class" ) )
            {
                continue;
            }
            
            //System.out.println("Processing entry : " + je.getName());
            InputStream in = jar.getInputStream( je );
            //System.out.println(in.available());
            
            in.read( majorVersion, 0, 8 );
            in.close();
            
            int ver = ( majorVersion[6] & 0xFF ) + ( majorVersion[7] & 0xFF );

            return ver <= 50;
            //System.out.println( ver );
            // completed processing for the jar, break out
        }
        
        return false;
    }
    
    private static void recurseRepo( File folder ) throws Exception
    {
        File[] files = folder.listFiles();
        for(File f : files )
        {
            if( f.isDirectory() )
            {
                recurseRepo( f );
            }
            else if( f.getName().endsWith( ".jar" ) )
            {
                System.out.println("Processing : " + f);
                boolean valid = isValidJar( f.getAbsolutePath() );
                if( !valid )
                {
                    System.out.println("Not valid: " + f.getAbsolutePath() );
                }
            }
        }
    }
    
    public static void main( String[] args ) throws Exception
    {
        //recurseRepo( new File("/Users/dbugger/.m2/repository") );
        System.out.println( getEscimoWar() );
        start();
    }
}
