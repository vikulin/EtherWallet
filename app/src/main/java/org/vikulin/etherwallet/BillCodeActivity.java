package org.vikulin.etherwallet;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Display;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;
import com.google.zxing.WriterException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.DefaultBlockParameterNumber;
import org.web3j.protocol.core.Request;
import org.web3j.protocol.core.methods.response.EthBlock;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

import static org.vikulin.etherwallet.ShowCodeActivity.encodeAsBitmap;
import static org.vikulin.etherwallet.ShowCodeActivity.getResizedBitmap;
import static org.web3j.utils.Numeric.hexStringToByteArray;
import static org.web3j.utils.Numeric.toHexString;

public class BillCodeActivity extends FullScreenActivity implements ZXingScannerView.ResultHandler {

    public static final String SELL_DATA = "sell_items";
    public static final int PAYMENT_SUCCESS = 2070;

    private ZXingScannerView mScannerView;
    private String sellItems;
    private PaymentCheckTask paymentCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill);
        final Bundle extras = getIntent().getExtras();
        final ImageView imageView = (ImageView) findViewById(R.id.barCodeImage);

        ViewTreeObserver vto = imageView.getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            public boolean onPreDraw() {
                imageView.getViewTreeObserver().removeOnPreDrawListener(this);
                Display display = getWindowManager().getDefaultDisplay();
                Point size = new Point();
                display.getSize(size);
                int w = size.x;
                int h = size.y;
                if(extras!=null) {
                    sellItems = extras.getString(SELL_DATA);
                    try {
                        Bitmap bitmap = encodeAsBitmap(sellItems, BillCodeActivity.this);
                        imageView.setImageBitmap(getResizedBitmap(bitmap, (w>h)?h:w, (w>h)?h:w));
                    } catch (WriterException e) {
                        e.printStackTrace();
                    }
                } else{

                }
                return true;
            }
        });

        mScannerView = (ZXingScannerView) findViewById(R.id.transactionScanner);
        mScannerView.setFormats(Arrays.asList(BarcodeFormat.QR_CODE));
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void handleResult(Result result) {
        String transactionHexValue = result.getText();
        mScannerView.stopCameraPreview();
        if (paymentCheck==null || paymentCheck.scheduledExecutionTime()==0){
            new SendTransactionTask(transactionHexValue).execute((Void) null);
        }
    }

    private final class PaymentCheckTask extends TimerTask {

        private final String transactionHash;
        private final Web3j web3j;
        private BigInteger startBlock;
        private int index = 0;

        public PaymentCheckTask(Web3j web3j, BigInteger startBlock, String transactionHash){
            this.transactionHash = transactionHash;
            this.web3j = web3j;
            this.startBlock = startBlock;
        }

        public void run() {
            try {
                BigInteger currentBlock = web3j.ethBlockNumber().sendAsync().get().getBlockNumber();
                if(currentBlock.compareTo(startBlock)>0) {
                    EthTransaction o = web3j.ethGetTransactionByHash(transactionHash).sendAsync().get();
                    //EthBlock.TransactionObject o = findTransactionByHash(web3j, startBlock, currentBlock, transactionHash);
                    if (o != null && o.getTransaction()!=null) {
                        BigInteger value = o.getTransaction().getValue();
                        JSONObject json = new JSONObject(sellItems);
                        JSONArray jsonArray = json.getJSONArray("i");
                        int size = jsonArray.length();
                        double total = 0;
                        for (int i = 0; i < size; i++) {
                            JSONObject si = (JSONObject) jsonArray.get(i);
                            total += si.getDouble("p");
                        }
                        BigInteger expectedValue = Convert.toWei(Double.toString(total), Convert.Unit.ETHER).toBigInteger();
                        if (value.compareTo(expectedValue) >= 0) {
                            showInfoDialogOnUiThread(getString(R.string.success), "(" + total + ") "+getString(R.string.payment_successful)+"!", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    setResult(RESULT_OK);
                                    finish();
                                }
                            });
                        } else {
                            showInfoDialogOnUiThread("", getString(R.string.insufficient_funds)+"!");
                        }
                        this.cancel();
                    }
                    index++;
                    startBlock = currentBlock;
                    if (index > 11) {
                        this.cancel();
                        showInfoDialogOnUiThread("", getString(R.string.not_received_payment));
                    }
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
                showAlertDialogOnUiThread("Error!","ETH transaction error:"+e.getMessage());
            } catch (InterruptedException e) {
                e.printStackTrace();
                showAlertDialogOnUiThread("Error!", "ETH transaction error:"+e.getMessage());
            } catch (JSONException e) {
                e.printStackTrace();
                showAlertDialogOnUiThread("Error!", "ETH transaction error:"+e.getMessage());
            }
        }

        public EthBlock.TransactionObject findTransactionByHash(Web3j web3j, BigInteger startBlock, BigInteger currentBlock, String transactionHash) throws ExecutionException, InterruptedException {

            System.out.println(currentBlock);
            for(BigInteger searchBlock=startBlock; currentBlock.compareTo(searchBlock)>0; searchBlock=searchBlock.add(BigInteger.ONE)){
                DefaultBlockParameterNumber searchBlockNumber = new DefaultBlockParameterNumber(searchBlock);
                EthBlock index = web3j.ethGetBlockByNumber(searchBlockNumber, true).sendAsync().get();
                web3j.ethBlockNumber().sendAsync().get().getBlockNumber();
                System.out.println("block#"+searchBlock.toString());
                for(EthBlock.TransactionResult tr: index.getBlock().getTransactions()){
                    EthBlock.TransactionObject o =(EthBlock.TransactionObject)tr.get();
                    if(transactionHash.equalsIgnoreCase(o.get().getHash())){
                        //success
                        System.out.println("found hash#:"+transactionHash+" input:"+o.get().getInput());
                        return o;
                    }
                }
            }
            return null;
        }
    }

    private final class SendTransactionTask extends AsyncTask<Void, Void, Exception> {

        private String transactionHexValue;
        private EthSendTransaction ethSendTransaction;
        private Web3j web3j;
        private BigInteger startBlock;

        SendTransactionTask(String transactionHexValue) {
            this.transactionHexValue = transactionHexValue;
        }

        @Override
        protected Exception doInBackground(Void... params) {
            try {
                //Web3j init
                web3j = Web3jFactory.build(new HttpService("https://mainnet.infura.io/erbkhNQe0QE11SJcEi1B"));
            } catch (RuntimeException e){
                e.printStackTrace();
                return e;
            }
            try {
                startBlock = web3j.ethBlockNumber().sendAsync().get().getBlockNumber();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return e;
            } catch (ExecutionException e) {
                e.printStackTrace();
                return e;
            }
            Request<?, EthSendTransaction> ether = web3j.ethSendRawTransaction(SendTransactionTask.this.transactionHexValue);
            try {
                ethSendTransaction = ether.sendAsync().get();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return e;
            } catch (ExecutionException e) {
                e.printStackTrace();
                return e;
            }
            if (ethSendTransaction==null){
                return new Exception("ETH transaction is null");
            }
            if(ethSendTransaction!=null && ethSendTransaction.hasError()){
                return new Exception("ETH transaction error: "+ethSendTransaction.getError().getMessage());
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Exception exception) {
            if (exception==null) {
                String transactionHash = ethSendTransaction.getTransactionHash();
                // showMessage("ETH transaction result:"+ethSendTransaction.getResult());
                Timer timer = new Timer();
                paymentCheck = new PaymentCheckTask(web3j, startBlock, transactionHash);
                timer.scheduleAtFixedRate(paymentCheck, 0, 1000*BLOCK_TIME);
            } else {
                mScannerView.resumeCameraPreview(BillCodeActivity.this);
                showAlertDialog("",exception.getMessage());
            }
        }

    }


    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Web3j web3j = Web3jFactory.build(new HttpService("https://mainnet.infura.io/erbkhNQe0QE11SJcEi1B"));
        //Web3j web3j = KeyFilePasswordActivity.getWeb3j();
        String uuid = UUID.randomUUID().toString().replace("-","");
        System.out.println(uuid);
        System.out.println(toHexString(hexStringToByteArray(uuid)));
        EthGetBalance request = web3j.ethGetBalance("0x6dcfe076f96a3dc4ea32aaebd8ddefab39c8e1ae", DefaultBlockParameterName.LATEST).sendAsync().get();
        BigInteger balance = request.getBalance();
        System.out.println(balance);
        EthGetTransactionReceipt transactionReceipt =
                web3j.ethGetTransactionReceipt("0xc9652cdcee778326c7d992c77afd0e6964ecdfdf0f43f2df94f540dc2951f94c").sendAsync().get();
        System.out.println(transactionReceipt+" "+transactionReceipt.getTransactionReceipt()+" "+transactionReceipt.getTransactionReceipt().getContractAddress());
    }

    private static final int BLOCK_TIME=10;
}
