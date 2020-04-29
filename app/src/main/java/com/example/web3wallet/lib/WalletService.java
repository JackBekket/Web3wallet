package com.example.web3wallet.lib;

import android.content.Context;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.web3wallet.KNS;
import com.example.web3wallet.TicketFactory721;

import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;
import org.web3j.utils.Convert;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class WalletService {

    private String deployed_net_id;
    private static String kns_address;
    private static String ticket_factory_address;
    private static Web3j web3;
    public static Credentials credentials;
    private static KNS kns;
    private static TicketFactory721 ticketfactory;

    private static final BigInteger CUSTOM_GAS_PRICE = Convert.toWei("8", Convert.Unit.GWEI).toBigInteger();
    private static final BigInteger CUSTOM_GAS_LIMIT = BigInteger.valueOf(7_000_000);

    private ECKeyPair ecKeyPair;
    private String sPrivateKeyInHex;
    private String password;
    private String fileName;
    public static File walletFile;
    private String walletPath;
    private File walletDir;

    private Context context;


    public WalletService(Context context) {
        this.context = context;
        walletPath = context.getFilesDir().getAbsolutePath();
        walletDir = new File(walletPath);


        String filePath = PreferenceManager.getDefaultSharedPreferences(context).getString("filePath", "");
        walletFile = new File(filePath);

        if (walletFile.exists() && !walletFile.isDirectory()) {
            fileName = walletFile.getName();
            loadDummyKey();
        } else {
            generatePrivateKey();
            loadDummyKey();
        }

        setupContracts();
    }

    // setting up instances of smart contracts
    public void setupContracts() {
        // FIXME: check net id for local and cloud enviroment
        String net_id = "5777";  // 5777 - for ganache (local testing)  , 42 - moonshard private network (has not been deployed yet)
        deployed_net_id = net_id;

        kns_address = KNS.getPreviouslyDeployedAddress(deployed_net_id);                    // Registry contract
        ticket_factory_address = TicketFactory721.getPreviouslyDeployedAddress(deployed_net_id); // Ticket Factory contract

        Log.d("deployed_address", "ticket_factory_address: " + ticket_factory_address);

        kns = KNS.load(kns_address, web3, credentials, CUSTOM_GAS_PRICE, CUSTOM_GAS_LIMIT);                 // entangle java contract artifact to actuall contract
        ticketfactory = TicketFactory721.load(ticket_factory_address, web3, credentials, CUSTOM_GAS_PRICE, CUSTOM_GAS_LIMIT);
    }

    public void generatePrivateKey() {
        try {
            ecKeyPair = Keys.createEcKeyPair();
            BigInteger privateKeyInDec = ecKeyPair.getPrivateKey();
            sPrivateKeyInHex = privateKeyInDec.toString(16);
            createWalletWithKey();
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchProviderException e) {
            e.printStackTrace();
        }
    }

    private void createWalletWithKey() {
        try {
            password = "1984";
            fileName = WalletUtils.generateWalletFile(password, ecKeyPair, walletDir, false);
            String filepath = walletPath + "/" + fileName;
            walletFile = new File(filepath);
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString("filePath", filepath).apply();
            Log.d("createWalletWithKey","Wallet generated" + filepath);
        } catch (CipherException | IOException e) {
            e.printStackTrace();
        }
    }

    // load dummy key with default password
    public void loadDummyKey() {
        password = "1984";
        try {
            String path = walletPath + "/" + fileName;
            walletDir = new File(path);
            credentials = WalletUtils.loadCredentials(password, walletDir);
            Log.d("my address", "my address is: " + credentials.getAddress());
        } catch (Exception e) {
            Log.d("my address", "my address is: " + credentials.getAddress());
        }
    }

    public String getMyAddress() {
        try {
            credentials = WalletUtils.loadCredentials(password, walletDir);
            Log.d("myAddress","Your address is " + credentials.getAddress());
        } catch (Exception e) {
            Log.d("myAddress","Error: "+e.getMessage());
        }
        return credentials.getAddress();
    }


    private Web3j getWeb3() {
        return web3;
    }

    TicketFactory721 getTicketfactory() {
        return ticketfactory;
    }

    public Credentials getCredentials() {
        return credentials;
    }
}
