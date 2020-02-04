package com.example.web3wallet;

import androidx.appcompat.app.AppCompatActivity;

//import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.web3wallet.ui.main.MainFragment;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;

import java.io.Console;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.Provider;
import java.security.Security;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {


    private String password;
    private EditText ePassword;
    private String walletPath;
    private File walletDir;
    private String fileName;

    private Web3j web3;
    private Credentials credentials;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        // workaround for bug with ECDA keys
        setupBouncyCastle();

        walletPath = getFilesDir().getAbsolutePath();
        walletDir = new File(walletPath);


        connectToLocalNetwork();


       /*
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, MainFragment.newInstance())
                    .commitNow();
        }

        */





    }



    public void createSimpleWallet(View v) {

        ePassword = (EditText) findViewById(R.id.newPass);
        password = ePassword.getText().toString();

        try{
          fileName =  WalletUtils.generateLightNewWalletFile(password,walletDir);
          String filepath = walletPath + "/" + fileName;
            toastAsync("Wallet generated" + filepath);
        }
        catch (Exception e){
            toastAsync(e.getMessage());
        }


    }


    public void loadSimpleWallet(View v) {

        ePassword = (EditText) findViewById(R.id.editPass);
        password = ePassword.getText().toString();


        try {
            String path = walletPath + "/" +fileName;
            walletDir = new File(path);
            credentials = WalletUtils.loadCredentials(password, walletDir);
            toastAsync("Your address is " + credentials.getAddress());


            showAddress(v);
            showBalance(v);
        }
        catch (Exception e){
            toastAsync(e.getMessage());
        }
    }


    public String getMyAddress () {

        try {
            credentials = WalletUtils.loadCredentials(password, walletDir);
            toastAsync("Your address is " + credentials.getAddress());

        }
        catch (Exception e){
            toastAsync(e.getMessage());
        }

        return credentials.getAddress();

    }


    // using Infura as a provider for public eth network
    public void connectToInfuraNetwork(View v) {
        toastAsync("Connecting to Ethereum network...");
        // FIXME: Add your own API key here
        web3 = Web3j.build(new HttpService("https://rinkeby.infura.io/v3/YOURKEY"));
        try {
            Web3ClientVersion clientVersion = web3.web3ClientVersion().sendAsync().get();
            if(!clientVersion.hasError()){
                toastAsync("Connected!");
            }
            else {
                toastAsync(clientVersion.getError().getMessage());
            }
        } catch (Exception e) {
            toastAsync(e.getMessage());
        }
    }


    // using LOCAL client node as web3 provider (geth/ganache)
    public void connectToLocalNetwork() {
        toastAsync("Connecting to LOCAL ETH network...");

        // FIXME: for bug with ganache connection. Should be replaced by address of our node
        web3 = Web3j.build(new HttpService("HTTP://100.124.25.117:8545")); // defaults to http://localhost:8545/
        try {
            Web3ClientVersion clientVersion = web3.web3ClientVersion().sendAsync().get();
            if(!clientVersion.hasError()){
                toastAsync("Connected!");
            }
            else {
                toastAsync(clientVersion.getError().getMessage());
            }
        } catch (Exception e) {
            toastAsync(e.getMessage());
            Log.e("connection", e.getMessage());
        }
    }

    public String getBalance() {


        EthGetBalance ethbalance = null;
        try {
            ethbalance = web3
                    .ethGetBalance(credentials.getAddress(), DefaultBlockParameterName.LATEST)
                    .sendAsync()
                    .get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        BigInteger wei = ethbalance.getBalance();
        BigDecimal balance = Convert.fromWei(String.valueOf(wei), Convert.Unit.ETHER);
        String strBalance = String.valueOf(balance);

        return strBalance;

    }

    public void showBalance(View v) {
        String balance = getBalance();

        TextView mBalance = (TextView) findViewById(R.id.userBalance);
        mBalance.setText(balance);

    }

    public void showAddress(View v) {

        String address = getMyAddress();
        TextView mAddress = (TextView) findViewById(R.id.walletAdress);
        mAddress.setText(address);
        Log.d("address",address);

    }


    public void toastAsync(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        });
    }


    // WARN! workaround for bug with ECDA signatures.
    private void setupBouncyCastle() {
        final Provider provider = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (provider == null) {
            // Web3j will set up the provider lazily when it's first used.
            return;
        }
        if (provider.getClass().equals(BouncyCastleProvider.class)) {
            // BC with same package name, shouldn't happen in real life.
            return;
        }
        // Android registers its own BC provider. As it might be outdated and might not include
        // all needed ciphers, we substitute it with a known BC bundled in the app.
        // Android's BC has its package rewritten to "com.android.org.bouncycastle" and because
        // of that it's possible to have another BC implementation loaded in VM.
        Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
    }


}
