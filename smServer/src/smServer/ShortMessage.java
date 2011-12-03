package smServer;
import java.math.BigDecimal;

public class ShortMessage {

	private static int MAX_LENGTH = 160;
	private BigDecimal receiverNo;
	private BigDecimal SenderNo;
	private String text;
	private String sendDate;

	public ShortMessage(BigDecimal senderNo, BigDecimal receiverNo, String text) {
		setReceiverNo(receiverNo);
		setSenderNo(senderNo);
		setText(text);
	}

	BigDecimal getReceiverNo() {
		return receiverNo;
	}

	void setReceiverNo(BigDecimal receiverNo) {
		if(receiverNo.compareTo(new BigDecimal("99999999999")) > 0 )
			throw new NumberFormatException();
		this.receiverNo = receiverNo;
	}

	BigDecimal getSenderNo() {
		return SenderNo;
	}

	void setSenderNo(BigDecimal senderNo) {
		SenderNo = senderNo;
	}

	String getText() {
		return text;
	}

	void setText(String text) {
		if(text.length() > ShortMessage.MAX_LENGTH)
			throw new StringIndexOutOfBoundsException(text.length());
		this.text = text;
	}

	String getSendDate() {
		return sendDate;
	}

	void setSendDate(String sendDate) {
		this.sendDate = sendDate;
	}
	
}
