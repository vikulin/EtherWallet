package org.vikulin.etherchange.changelly;

import java.io.Serializable;

/**
 * Created by vadym on 26.05.17.
 */

public class GenerateAddressResponse implements Serializable {

    private String jsonrpc;
    private Long id;
    private Result result;
    private Error error;

    public String getJsonRpc() {
        return jsonrpc;
    }

    public Long getId() {
        return id;
    }

    public Result getResult() {
        return result;
    }

    public Error getError() {
        return error;
    }

    /**
     * Additional fields. its do no pass from JSON response.
     * @return
     */
    public Double getSendAmount() {
        return sendAmount;
    }

    public void setSendAmount(Double sendAmount) {
        this.sendAmount = sendAmount;
    }

    public Double getReceiveAmount() {
        return receiveAmount;
    }

    public void setReceiveAmount(Double receiveAmount) {
        this.receiveAmount = receiveAmount;
    }

    public String getReceiverId() {
        return receiver_id;
    }

    public void setReceiverId(String receiver_id) {
        this.receiver_id=receiver_id;
    }

    private Double sendAmount;

    private Double receiveAmount;

    private String receiver_id;

}
