package util;

import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.Locale;

import static utils.InjectedThings.logger;

/**
 * Interface used by Entropy Android & Desktop for anything to do with the online session
 */
public abstract class AbstractClient implements OnlineConstants
{
	public static boolean devMode = false;
	public static String operatingSystem = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
	public static boolean justUpdated = false;
	public static int instanceNumber = 1;
	
	//Instance
	private static AbstractClient client = null;
	
	//Properties on the instance
	private long lastSentMessageMillis = -1;
	private ArrayList<MessageSenderParams> pendingMessages = new ArrayList<>();
	
	/**
	 * Abstract methods
	 */
	public abstract void init();
	public abstract String getUsername();
	public abstract boolean isOnline();
	public abstract void sendAsyncInSingleThread(MessageSenderParams message);
	public abstract String sendSyncOnDevice(MessageSender runnable);
	public abstract void handleResponse(String message, String encryptedResponse) throws Throwable;
	public abstract void checkForUpdates();
	
	/**
	 * Use these to show waiting dialogs/info to the user
	 */
	public abstract boolean isCommunicatingWithServer();
	public abstract void finishServerCommunication();
	public abstract void unableToConnect();
	public abstract void connectionLost();
	public abstract void goOffline();
	
	/**
	 * Helpers during startup
	 */
	public static void parseProgramArguments(String[] args)
	{
		for (int i=0; i<args.length; i++)
		{
			String arg = args[i];
			parseProgramArgument(arg);
		}
	}
	private static void parseProgramArgument(String arg)
	{
		if (arg.equals("justUpdated"))
		{
			justUpdated = true;
			logger.info("justUpdated", "Running in justUpdated mode");
		}
		else if (arg.equals("devMode"))
		{
			devMode = true;
			logger.info("devMode", "Running in devMode");
		}
		else
		{
			logger.info("unexpectedArgument", "Unexpected program argument: " + arg);
		}
	}
	public static boolean isAppleOs()
	{
		return operatingSystem.contains("mac") || operatingSystem.contains("darwin");
	}
	public static boolean isWindowsOs()
	{
		return operatingSystem.contains("windows");
	}
	
	public void checkForUpdatesIfRequired()
	{
		if (justUpdated)
		{
			logger.info("justUpdated", "Just updated - not checking for updates");
			return;
		}
		
		checkForUpdates();
	}
	
	public long getLastSentMessageMillis()
	{
		return lastSentMessageMillis;
	}
	public void setLastSentMessageMillis(long lastSentMessageMillis)
	{
		this.lastSentMessageMillis = lastSentMessageMillis;
	}
	
	public static AbstractClient getInstance()
	{
		return client;
	}
	public static void setInstance(AbstractClient client)
	{
		AbstractClient.client = client;
		client.init();
	}
	
	public String sendSync(Document message, boolean encrypt)
	{
		return sendSync(message, encrypt, MessageSenderParams.SO_TIMEOUT_MILLIS, false);
	}
	public String sendSync(Document message, boolean encrypt, int readTimeOut, boolean alwaysRetryOnSoTimeout)
	{
		String messageString = XmlUtil.getStringFromDocument(message);
		String encryptedMessageString = messageString;
		if (encrypt)
		{
			encryptedMessageString = EncryptionUtil.encrypt(encryptedMessageString, MessageUtil.symmetricKey);
		}
		
		MessageSenderParams wrapper = new MessageSenderParams(messageString, 0, 5);
		wrapper.setEncryptedMessageString(encryptedMessageString);
		wrapper.setIgnoreResponse(true);
		wrapper.setReadTimeOut(readTimeOut);
		wrapper.setAlwaysRetryOnSoTimeout(alwaysRetryOnSoTimeout);
		
		MessageSender sender = new MessageSender(this, wrapper);
		return sendSyncOnDevice(sender);
	}
	
	public void startNotificationThreads()
	{
		startNotificationThread(XmlConstants.SOCKET_NAME_GAME);
		startNotificationThread(XmlConstants.SOCKET_NAME_CHAT);
		startNotificationThread(XmlConstants.SOCKET_NAME_LOBBY);
	}
	
	private void startNotificationThread(String socketType)
	{
		ClientNotificationRunnable runnable = new ClientNotificationRunnable(this, socketType);
		Thread notificationThread = new Thread(runnable, socketType + "Thread");
		notificationThread.start();
	}
	
	public void addToPendingMessages(MessageSenderParams message)
	{
		pendingMessages.add(message);
	}
	public MessageSenderParams getNextMessageToSend()
	{
		return pendingMessages.remove(0);
	}
}
