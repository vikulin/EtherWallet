package org.vikulin.etherpush;

/**
 * Created by vadym on 30.04.17.
 */

public class InviteAcceptance extends Message {

    private String publicKey;

    public InviteAcceptance(Long timestamp, String from, String to, String publicKey){
        super(Code.INVITE_ACCEPTANCE, timestamp, from, to);
        this.publicKey = publicKey;
    }

    @Override
    public byte[] getPreSignData() {
        return (String.valueOf(getCode())+Long.toString(getTimestamp())+":"+getFrom()+":"+getTo()).getBytes();
    }

    public String getPublicKey() {
        return publicKey;
    }
}
