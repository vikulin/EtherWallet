package org.vikulin.etherchange.changer;

import java.io.Serializable;

/**
 * Created by DDD on 30.03.2017.
 */

public class Exchange implements Serializable{

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

    public String getPayee() {
        return payee;
    }

    public String getBatchRequired() {
        return batch_required;
    }

    public Long getExpiration() {
        return expiration;
    }

    public Pair getPair() {
        return pair;
    }

    public Limits getLimits() {
        return limits;
    }

    public String getHtml() {
        return html;
    }

    private String exchange_id;
    private Double send_amount;
    private Double rate;
    private Double receive_amount;
    private String receiver_id;
    private String payee;
    private String batch_required;
    private Long expiration;
    private Pair pair;
    private Limits limits;
    private String html;
}
