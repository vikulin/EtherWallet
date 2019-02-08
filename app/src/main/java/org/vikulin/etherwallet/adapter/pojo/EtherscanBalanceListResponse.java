package org.vikulin.etherwallet.adapter.pojo;

import java.util.List;

/**
 * Created by vadym on 10.09.17.
 */

public class EtherscanBalanceListResponse {
    private List<EtherscanBalance> result;

    public List<EtherscanBalance> getResult(){
        return result;
    }

    public void setResult(List<EtherscanBalance> result){
        this.result = result;
    }
}
