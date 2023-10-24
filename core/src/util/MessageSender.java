package util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MessageSender implements Runnable
{
	private MessageSenderParams messageParms = null;
	private String encryptedResponseString = null;
	private int currentRetries = 0;
	private AbstractClient client = null;
	
	/**
	 * Constructor where message params are passed in directly. Used when sending synchronously.
	 */
	public MessageSender(AbstractClient client, MessageSenderParams messageWrapper)
	{
		this.client = client;
		this.messageParms = messageWrapper;
	}
	
	/**
	 * Constructor just containing the client. When this runnable gets kicked off, we'll get the next 
	 * messageWrapper to send off of the client.
	 */
	public MessageSender(AbstractClient client)
	{
		this.client = client;
	}
	
	@Override
	public void run()
	{
		if (messageParms == null)
		{
			//We're picking up off the queue, so we should synchronise
			synchronized (client)
			{
				this.messageParms = client.getNextMessageToSend();
				sendMessage();
			}
		}
		else
		{
			sendMessage();
		}
	}
	
	public String sendMessage()
	{
		int portNumber = MessageUtil.getRandomPortNumber();
		InetAddress address = MessageUtil.factoryInetAddress(MessageUtil.SERVER_IP);
		
		sleepWithCatch();
		
		BufferedReader in = null;
		String messageString = messageParms.getMessageString();
		
		try (Socket socket = new Socket(address, portNumber);
		  PrintWriter out = new PrintWriter(socket.getOutputStream(), true);)
		{
			client.setLastSentMessageMillis(System.currentTimeMillis());
			
			int soTimeOut = messageParms.getReadTimeOut();
			socket.setSoTimeout(soTimeOut);
			
			out.write(messageParms.getEncryptedMessageString() + "\n");
			out.flush();
			
			//If we're not expecting a response (e.g. for a disconnect), just return out.
			if (!messageParms.getExpectResponse())
			{
				return null;
			}

			//Read in the response
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			encryptedResponseString = in.readLine();
			if (encryptedResponseString == null)
			{
				return retryOrStackTrace(new Throwable("NULL responseString"));
			}

			//Handle the response if we're not ignoring it
			if (!messageParms.getIgnoreResponse())
			{
				client.handleResponse(messageString, encryptedResponseString);
			}
			
			return encryptedResponseString;
		}
		catch (SocketException t)
		{
			return retryOrStackTrace(t);
		}
		catch (SocketTimeoutException ste)
		{
			if (!isResponseIgnored(messageString))
			{
				return retryOrStackTrace(ste);
			}
			
			return null;
		}
		catch (Throwable t)
		{
			MessageUtil.stackTraceAndDumpMessages(t, messageParms.getCreationStack(), messageString, encryptedResponseString);
			return null;
		}
		finally
		{
			if (in != null)
			{
				try {in.close();} catch (Throwable t){}
			}
		}
	}
	
	private void sleepWithCatch()
	{
		try
		{
			Thread.sleep(messageParms.getMillis());
			if (MessageUtil.millisDelay > 0)
			{
				Thread.sleep(MessageUtil.millisDelay);
			}
		}
		catch (InterruptedException ie)
		{
			Debug.stackTrace(ie);
		}
	}
	
	private String retryOrStackTrace(Throwable t)
	{
		if (!AbstractClient.getInstance().isOnline())
		{
			return null;
		}
		
		String messageName = messageParms.getMessageName();
		if (t instanceof SocketTimeoutException
		  && messageParms.getAlwaysRetryOnSoTimeout())
		{
			//Always retry, don't bother logging a line
			Debug.append("Had SocketTimeoutException for " + messageName + ", retrying", !AbstractClient.devMode);
			return sendMessage();
		}
		
		int retries = messageParms.getRetries();
		if (currentRetries < retries)
		{
			currentRetries++;
			messageParms.setMillis(0);
			Debug.append(t.getMessage() + " for " + messageName + ", will retry (" + currentRetries + "/" + retries + ")");
			return sendMessage();
		}
		else
		{
			Debug.append("Failed to send message after " + retries + " retries.");
			Debug.append("Message: " + messageParms.getMessageString());
			Debug.stackTraceSilently(t);
			Debug.append("Previous stack:");
			Debug.stackTraceSilently(messageParms.getCreationStack());
			
			if (client.isCommunicatingWithServer())
			{
				client.unableToConnect();
			}
			else
			{
				client.finishServerCommunication();
				client.goOffline();
				
				if (!messageParms.getIgnoreResponse())
				{
					client.connectionLost();
				}
			}
			
			return null;
		}
	}
	
	private boolean isResponseIgnored(String xmlStr)
	{
		Document document = XmlUtil.getDocumentFromXmlString(xmlStr);
		if (document == null)
		{
			//I've had this be null here on Live. Just return false so we re-send
			Debug.stackTrace("NULL document when checking if response is ignored for message " + xmlStr);
			return false;
		}
		
		Element rootElement = document.getDocumentElement();
		
		String name = rootElement.getTagName();
		
		return name.equals(XmlConstants.ROOT_TAG_NEW_CHAT)
		    || name.equals(XmlConstants.ROOT_TAG_DISCONNECT_REQUEST);
	}
}
