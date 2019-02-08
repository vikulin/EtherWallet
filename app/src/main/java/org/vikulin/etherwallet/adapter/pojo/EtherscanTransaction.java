package org.vikulin.etherwallet.adapter.pojo;

/**
 * Created by vadym on 12.03.17.
 */

public class EtherscanTransaction {

    private String timeStamp;

    private String from;

    private String to;

    private String value;

    private String confirmations;

    private String contractAddress;

    private String hash;

    private String isError;

    private String input;

    private TransactionType transactionType;

    public String getTimeStamp() {
        return timeStamp;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getValue() {
        return value;
    }

    public String getConfirmations() {
        return confirmations;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public String getHash() {
        return hash;
    }

    public String getIsError() {
        return isError;
    }

    public String getInput() {
        return input;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

}
