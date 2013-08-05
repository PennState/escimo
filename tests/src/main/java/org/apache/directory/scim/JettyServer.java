package org.apache.directory.scim;


import java.io.File;
import java.io.FilenameFilter;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;


/**
 * Hello world!
 *
 */
public class JettyServer
{
    private static Server server;


    public static void start() throws Exception
    {
        if ( ( server != null ) && server.isRunning() )
        {
            return;
        }
        
        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath( "/" );
        webapp.setWar( getEscimoWar().getAbsolutePath() );
        webapp.setParentLoaderPriority( true );
        String cpath = System.getProperty("java.class.path");
        cpath = cpath.replaceAll( ":", ";" );
//        webapp.setExtraClasspath( cpath );

        server = new Server( 8080 );
        server.setHandler( webapp );
        server.start();
        server.join();
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
    }


    private static File getEscimoWar()
    {
        String msg = "No escimo-server war file found, please build escimo-server project first and  then either set the escimo.test.version property or run the test from 'tests' folder";

        FilenameFilter ff = new FilenameFilter()
        {

            @Override
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


    public static void main( String[] args ) throws Exception
    {
        System.out.println( getEscimoWar() );
        start();
    }
}
