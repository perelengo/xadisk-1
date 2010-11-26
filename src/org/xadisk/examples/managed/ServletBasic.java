package org.xadisk.examples.managed;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import org.xadisk.connector.outbound.XADiskConnectionFactory;
import org.xadisk.connector.outbound.XADiskConnection;

/*This is a very basic example to outline the usage of XADisk in managed (J2EE) environment.*/

/*
 * This class is a Servlet and can be deployed inside a web-application (.war) to any J2EE 5(or above) server (I tested it on JBoss 5.1.0).
 * One also needs to deploy the XADisk.rar (https://xadisk.dev.java.net/files/documents/10793/148852/XADisk.rar) as a Resource Adapter
 * and configure a connection-factory (as per the documentation of J2EE server you are using) named "xadiskcf"
 * (or you can keep a name of your choice and change the argument to the "lookup" call below).
 *
 * Before running this sample, please change the local file-system paths used below according to your environment.
 */

/* This example is only intended to show a usage for XADisk. The other programming practices can vary among programmers.*/


public class ServletBasic extends HttpServlet {

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        UserTransaction utx = null;
        XADiskConnection connection = null;
        String TEST_ROOT = "C:";
        try {
            out.println("<html><head><title>Servlet ServletBasic</title></head><body>");
            out.println("<h1>Servlet ServletBasic at " + request.getContextPath () + "</h1>");

            out.println("Looking up the global transaction object...<br>");
            utx = lookUpGlobalTransaction();

            out.println("Beginning a global transaction...<br>");
            utx.begin();

            out.println("Looking up a Connection Factory instance for XADisk...<br>");
            XADiskConnectionFactory cf = (XADiskConnectionFactory)new InitialContext().lookup("java:xadiskcf");

            out.println("Retrieveing a Connection to interact with XADisk....<br>");
            connection = cf.getConnection();

            out.println("Will do some operations on the local file-system via XADisk...<br>");
            out.println("[We could also have done some work on database, JMS etc and all of the work " +
                    "will then become part of the same global transaction.]<br>");

            File f = new File(TEST_ROOT + "\\servlet.txt");
            if(connection.fileExists(f, true)) { //"true" for pessimistic locking over the file.
                connection.moveFile(f, new File(TEST_ROOT + "\\backup.txt_" + System.currentTimeMillis()));
            } else {
                connection.createFile(f, false);//"false" for creating normal files, "true" for directories.
            }


            /* ...
             * ...
             * ...
             *
             * You can do more file operations here, by calling APIs for reading/writing/creating/deleting/updating files and directories.
             * Please refer to the XADisk User Guide, which mentions the kind of APIs for these file-system operations.
             *
             * ...
             * ...
             * ...
             */


            out.println("Done with my work, committing the global transaction now...<br>");
            utx.commit();
            out.println("Cool...everything went well. GoodBye for now.<br>");

            out.println("</body></html>");
            
        } catch(Exception e) {
            out.println("Servel Encountered an Exception...<br><br>");
            e.printStackTrace(out);
        } finally {
            cleanUp(utx, connection, out);
            out.close();
        }
    }

    private UserTransaction lookUpGlobalTransaction() throws NamingException {
        return (UserTransaction)new InitialContext().lookup("java:comp/UserTransaction");
    }

    private void cleanUp(UserTransaction utx, XADiskConnection connection, PrintWriter out) {
        try {
            if(utx != null && utx.getStatus() == Status.STATUS_ACTIVE) {
                    utx.rollback();
            }
            if(connection != null) {
                connection.close();
            }
        } catch(Throwable t) {
            t.printStackTrace(out);
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
    throws ServletException, IOException {
        processRequest(request, response);
    }

    public String getServletInfo() {
        return "Short description";
    }
}
