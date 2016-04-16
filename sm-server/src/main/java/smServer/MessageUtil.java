package smServer;

import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

import smServer.api.ShortMessageSender;

public class MessageUtil {

	public static void sendMessage(AppContext ctx, ShortMessage msg) {
		if(msg == null) return;

		String rnSplit[] = msg.getProperty(ShortMessage.RECEIVER_NO).split(",");
		String textMessage = msg.getProperty(ShortMessage.TEXT);
		String sendDate = msg.getProperty(ShortMessage.TERMIN);

		for(String rn: rnSplit) {

			if(rn != null && textMessage != null) {
				BigDecimal receiverNo = new BigDecimal(rn);
				smServer.api.ShortMessage message = null;
				try {
					message = new smServer.api.ShortMessage((BigDecimal) ctx.get(AppContext.SENDER_NO),receiverNo,textMessage);
				} catch(Exception e) {
					Logger.getLogger(MessageUtil.class.getName()).log(Level.SEVERE,"Couldn't create SMS object!", e);
					return;
				}
				message.setSendDate(sendDate);
				ShortMessageSender sms = (ShortMessageSender) ctx.get(AppContext.SMS);
				sms.send(message);
			}
		}
	}
}
