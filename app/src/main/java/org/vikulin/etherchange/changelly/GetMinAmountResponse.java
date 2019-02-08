package org.vikulin.etherchange.changelly;

/**
 * Created by vadym on 26.05.17.
 */

public class GetMinAmountResponse {
    private String jsonrpc;
    private Long id;
    private String result;
    private Error error;

    public String getJsonRpc() {
        return jsonrpc;
    }

    public Long getId() {
        return id;
    }

    public String getResult() {
        return result;
    }

    public Error getError() {
        return error;
    }
}
