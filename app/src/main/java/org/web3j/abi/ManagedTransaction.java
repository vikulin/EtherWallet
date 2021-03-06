package org.web3j.abi;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

/**
 * Generic transaction manager
 */
public abstract class ManagedTransaction {

    // Sensible default as of November 2016...
    public static final BigInteger GAS_PRICE = BigInteger.valueOf(22000000000L);

    private static final int SLEEP_DURATION = 15000;
    private static final int ATTEMPTS = 40;

    protected Web3j web3j;
    protected Credentials credentials;

    protected BigInteger gasPrice;
    protected BigInteger gasLimit;

    private int sleepDuration = SLEEP_DURATION;
    private int attempts = ATTEMPTS;

    protected ManagedTransaction(Web3j web3j, Credentials credentials,
                                 BigInteger gasPrice, BigInteger gasLimit) {
        this.web3j = web3j;
        this.credentials = credentials;

        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
    }

    // In case anyone wishes to override the defaults
    public int getSleepDuration() {
        return sleepDuration;
    }

    public void setSleepDuration(int sleepDuration) {
        this.sleepDuration = sleepDuration;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    protected TransactionReceipt signAndSend(RawTransaction rawTransaction)
            throws InterruptedException, ExecutionException {

        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);

        // This might be a good candidate for using functional composition with CompletableFutures
        EthSendTransaction transactionResponse = web3j.ethSendRawTransaction(hexValue)
                .sendAsync().get();

        if (transactionResponse.hasError()) {
            throw new RuntimeException("Error processing transaction request: " +
                    transactionResponse.getError().getMessage());
        }

        String transactionHash = transactionResponse.getTransactionHash();

        return waitForTransactionReceipt(transactionHash);
    }

    protected BigInteger getNonce(String address) throws InterruptedException, ExecutionException {
        EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(
                address, DefaultBlockParameterName.LATEST).sendAsync().get();

        return ethGetTransactionCount.getTransactionCount();
    }

    protected BigInteger getGasPrice() throws InterruptedException, ExecutionException {
        EthGasPrice ethGasPrice = web3j.ethGasPrice().sendAsync().get();
        return ethGasPrice.getGasPrice();
    }

    private TransactionReceipt waitForTransactionReceipt(
            String transactionHash) throws InterruptedException, ExecutionException {
        return getTransactionReceipt(transactionHash, sleepDuration, attempts);
    }

    private TransactionReceipt getTransactionReceipt(
            String transactionHash, int sleepDuration, int attempts)
            throws InterruptedException, ExecutionException {

        TransactionReceipt receiptOptional =
                sendTransactionReceiptRequest(transactionHash);
        for (int i = 0; i < attempts; i++) {
            if (receiptOptional == null) {
                Thread.sleep(sleepDuration);
                receiptOptional = sendTransactionReceiptRequest(transactionHash);
            } else {
                return receiptOptional;
            }
        }

        throw new ExecutionException(new Exception("Transaction receipt was not generated after " +
                        ((sleepDuration * attempts) / 1000 +
                        " seconds for transaction: " + transactionHash)));
    }

    private TransactionReceipt sendTransactionReceiptRequest(
            String transactionHash) throws InterruptedException, ExecutionException {
        EthGetTransactionReceipt transactionReceipt =
                web3j.ethGetTransactionReceipt(transactionHash).sendAsync().get();

        if (transactionReceipt.hasError()) {
            throw new RuntimeException("Error processing request: " +
                    transactionReceipt.getError().getMessage());
        }

        return transactionReceipt.getTransactionReceipt();
    }
}
