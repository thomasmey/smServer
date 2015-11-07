package smServer.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import smServer.ShortMessage;
import smServer.ShortMessageSender;

public class InnoApi implements ShortMessageSender, Runnable {

	private final String encoding;
	private final Logger log;

	private String userName;
	private String password;

	private static final Queue<ShortMessage> messageQueue = new LinkedList<ShortMessage>();

	public InnoApi (String userName, String password) {
		this.encoding = "ISO-8859-15";
		this.log = Logger.getLogger(InnoApi.class.getName());

		setUserName(userName);
		setPassword(password);
	}

	@Override
	public void run() {

		log.log(Level.INFO, "Starting sending thread {0}.", Thread.currentThread().getName());

		ShortMessage sm = null;
		while(true) {

			if(Thread.currentThread().isInterrupted())
				return;

			synchronized (messageQueue) {
				try {
					messageQueue.wait();
				} catch (InterruptedException e) {
					return;
				}
				sm = messageQueue.poll();
			}

			assert sm != null;
			send0(sm);
		}
	}

	/** sends a SMS, by putting it on the Queue */
	public void send(ShortMessage message) {
		synchronized (messageQueue) {
			messageQueue.offer(message);
			messageQueue.notify();
		}
	}

	private void send0(ShortMessage message) {

		URI apiUri = null;
		String userName = null;
		String passWord = null;
		String text = null;
		String sendDate = message.getSendDate();

		try {
			userName = URLEncoder.encode(this.userName, encoding);
			passWord = URLEncoder.encode(this.password, encoding);
			text = URLEncoder.encode(message.getText(), encoding);
			if(sendDate != null) {
				sendDate = URLEncoder.encode(sendDate,encoding);
			}
		} catch (UnsupportedEncodingException e) {
			log.log(Level.SEVERE, "Error while urlencoding", e);
			return;
		}
		
		String senderNo = "0" + message.getSenderNo();
		String receiverNo = "0" + message.getReceiverNo();
		int msgType = 4;

		log.log(Level.INFO, "Sending SM from {0} to {1}", new String[] {senderNo, receiverNo});

		try {
			String uri = "https://www.innosend.de/gateway/sms.php" + "?id=" + userName +
																	 "&pw=" + passWord +
																	 "&type=" + msgType +
																	 "&text=" + text +
																	 "&empfaenger=" + receiverNo +
																	 "&absender=" + senderNo;
			
			if(sendDate != null) {
				uri = uri + "&termin=" + sendDate;
			}
			apiUri = new URI(uri);
		} catch (URISyntaxException e) {
			log.log(Level.SEVERE, "Error while creating api URI!",e);
			return;
		}

		URL apiUrl;
		try {
			apiUrl = apiUri.toURL();
		} catch (MalformedURLException e) {
			log.log(Level.SEVERE, "Error while creating api URL!",e);
			return;
		}

		InputStream is;
		byte[] content = new byte[16];
		URLConnection conn = null;
		int status;
		Charset cs;
		try {
			conn = apiUrl.openConnection();
			conn.connect();
			is = conn.getInputStream();
			int len=is.read(content);
			is.close();
			String encoding = conn.getContentEncoding();
			if(encoding != null)
				cs=Charset.forName(encoding);
			else
				cs=Charset.forName("US-ASCII");

			String rc = cs.decode(ByteBuffer.wrap(Arrays.copyOfRange(content, 0, len))).toString();
			status = Integer.valueOf(rc);
			log.log(Level.INFO, "SMS send. Return code {0}", status);

		} catch (IOException e) {
			log.log(Level.SEVERE, "IO error while opening connection!", e);
			return;
		}
	}

	private void setUserName(String userName) {
		this.userName = userName;
	}

	private void setPassword(String password) {
		this.password = password;
	}

}
