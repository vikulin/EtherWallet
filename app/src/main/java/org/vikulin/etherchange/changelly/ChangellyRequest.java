package org.vikulin.etherchange.changelly;

/**
 * Created by vadym on 26.05.17.
 */

public class ChangellyRequest {

    public ChangellyRequest(Long id,String method){
        this.id = id;
        this.method = method;
    }

    private String jsonrpc="2.0";
    private String method;
    private Long id;

    public String getJsonRpc() {
        return jsonrpc;
    }

    public String getMethod() {
        return method;
    }

    public Long getId() {
        return id;
    }
}
