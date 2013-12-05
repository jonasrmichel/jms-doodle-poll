package doodle;

import java.net.ConnectException;
import java.net.UnknownHostException;

import javax.jms.QueueConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.objectweb.joram.client.jms.admin.AdminException;
import org.objectweb.joram.client.jms.admin.AdminModule;
import org.objectweb.joram.client.jms.admin.User;
import org.objectweb.joram.client.jms.tcp.QueueTcpConnectionFactory;

/**
 * Administers an agent server for the doodle poll system. This must be executed
 * prior to creating doodle poll users.
 * 
 * @author Jonas Michel
 * 
 */
public class DoodleAdmin {

	public static final String QUEUE_CONNECTION_FACTORY = "qcf";

	public static void main(String[] args) {
		try {
			System.out.println();
			System.out.println("Doodle poll administration configuring...");
			
			// clear out the shared file of available users
			UsersMonitor.reset();

			AdminModule.connect();

			User.create("anonymous", "anonymous");

			QueueConnectionFactory qcf = QueueTcpConnectionFactory.create();

			Context context = new InitialContext();
			context.bind(QUEUE_CONNECTION_FACTORY, qcf);
			context.close();

			AdminModule.disconnect();
			System.out.println("Doodle poll administration closed.");

		} catch (ConnectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AdminException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
