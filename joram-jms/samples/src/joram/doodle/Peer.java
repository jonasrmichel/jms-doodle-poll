package doodle;

import java.net.ConnectException;
import java.net.UnknownHostException;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.objectweb.joram.client.jms.Queue;
import org.objectweb.joram.client.jms.admin.AdminException;
import org.objectweb.joram.client.jms.admin.AdminModule;

/**
 * Peers are the networked objects in our doodle system. The peer class defines
 * the bare minimum functionality of an independent communicating process in our
 * system.
 * 
 * Most importantly, each peer exposes a "listening" queue, which may be used by
 * other peers to send this peer notifications, messages, etc.
 * 
 * @author Jonas Michel
 * 
 */
public abstract class Peer implements MessageListener {
	/** The name that identifies this peer. */
	protected String name;

	/** This peer's role. */
	protected Role role;

	/** Possible roles a peer object could take on. */
	public enum Role {
		USER, POLL
	};

	/** Persistent JMS communication constructs. */
	protected InitialContext context;
	protected QueueConnection connection;

	/** A peer receives messages on a dedicated queue. */
	protected QueueSession listeningSession;
	protected Queue listeningQueue;
	protected QueueReceiver listeningReceiver;

	/** The listening queue naming prefix. */
	public static final String QUEUE_PREFIX = "doodle_queue";

	public Peer(Role role) {
		this.role = role;
	}
	
	public Peer(String name, Role role) {
		this.name = name;
		this.role = role;

		setup();
	}
	
	public void start(String name) {
		this.name = name;
		
		setup();
	}

	/**
	 * Initializes the peer's JMS constructs and listening queue.
	 */
	protected void setup() {
		try {
			AdminModule.connect();

			context = new InitialContext();
			QueueConnectionFactory qcf = (QueueConnectionFactory) context
					.lookup(DoodleAdmin.QUEUE_CONNECTION_FACTORY);

			connection = qcf.createQueueConnection();

			listeningSession = connection.createQueueSession(false,
					QueueSession.AUTO_ACKNOWLEDGE);

			listeningQueue = Queue.create(getQueueName(name, role));
			listeningQueue.setFreeReading();
			listeningQueue.setFreeWriting();

			context.bind(getQueueName(name, role), listeningQueue);

			listeningReceiver = listeningSession.createReceiver(listeningQueue);
			listeningReceiver.setMessageListener(this);

			connection.start();

			AdminModule.disconnect();

		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ConnectException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (AdminException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Shuts down the peer.
	 */
	protected void stop() {
		try {
			listeningReceiver.close();
			connection.stop();
			listeningSession.close();
			connection.close();
			context.close();

		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NamingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Sends a message to a peer with the provided name and role.
	 * 
	 * @param name
	 *            the name of the receiver.
	 * @param role
	 *            the role of the receiver.
	 * @param payload
	 *            the payload to send.
	 * @return true if the message was delivered successfully, false otherwise.
	 */
	protected boolean send(String name, Role role, Payload payload) {
		try {
			// create a session to send the message
			QueueSession session = connection.createQueueSession(false,
					QueueSession.AUTO_ACKNOWLEDGE);

			// lookup the receiving peer's listening queue
			Queue queue = (Queue) context.lookup(getQueueName(name, role));

			// make sender
			QueueSender sender = session.createSender(queue);

			Message message = session.createObjectMessage(payload);
			sender.send(message);

			// close the session
			session.close();

			return true;

		} catch (NamingException e) {
			// the receiving peer's queue does not exist
			return false;

		} catch (JMSException e) {
			// some other error occurred
			e.printStackTrace();

			return false;
		}
	}

	/**
	 * Creates the appropriate doodle queue name given a peer's name and role.
	 * 
	 * @param name
	 *            a peer name.
	 * @param role
	 *            a peer role
	 * @return the doodle queue name.
	 */
	public static String getQueueName(String name, Role role) {
		return QUEUE_PREFIX + "_" + role.toString() + "_" + name;
	}

	/**
	 * This method will be called when a message arrives on the peer's listening
	 * queue.
	 */
	@Override
	public abstract void onMessage(Message message);

}
