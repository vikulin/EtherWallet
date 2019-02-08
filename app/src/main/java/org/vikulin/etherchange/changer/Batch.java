package org.vikulin.etherchange.changer;

import java.io.Serializable;

/**
 * Created by vadym on 02.04.17.
 */

public class Batch implements Serializable {

    private String success;
    private String exchange_id;
    private Double send_amount;
    private Double rate;
    private Double receive_amount;
    private String receiver_id;

    public String getSuccess() {
        return success;
    }

    public String getExchangeId() {
        return exchange_id;
    }

    public Double getSendAmount() {
        return send_amount;
    }

    public Double getRate() {
        return rate;
    }

    public Double getReceiveAmount() {
        return receive_amount;
    }

    public String getReceiverId() {
        return receiver_id;
    }

}
