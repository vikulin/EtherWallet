package org.vikulin.etherwallet.adapter.pojo;

import java.util.List;

/**
 * Created by vadym on 12.03.17.
 */

public class EtherscanTransactionListResponse {
    private List<EtherscanTransaction> result;

    public List<EtherscanTransaction> getResult(){
        return result;
    }

    public void setResult(List<EtherscanTransaction> result){
        this.result = result;
    }
}
