package smServer;
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
import java.util.logging.Level;
import java.util.logging.Logger;

public class InnoApi implements ShortMessageSender {

	private String userName;
	private String password;
	private String encoding;
	private Logger log;

	InnoApi (Logger log, String userName, String password) {
		encoding = "UTF-8";
		setUserName(userName);
		setPassword(password);
		this.log=log;
	}
	
	public void send(ShortMessage message) {
		
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
		String[] parms = new String[2];
		parms[0] = senderNo;
		parms[1] = receiverNo;
		int msgType = 4;

		log.log(Level.INFO, "Sending SM from {0} to {1}", parms);

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

	String getUserName() {
		return userName;
	}

	void setUserName(String userName) {
		this.userName = userName;
	}

	String getPassword() {
		return password;
	}

	void setPassword(String password) {
		this.password = password;
	}

}
