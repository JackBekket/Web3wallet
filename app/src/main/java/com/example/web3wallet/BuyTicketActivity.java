package com.example.web3wallet;

import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.example.web3wallet.MainActivity.CUSTOM_GAS_LIMIT;
import static com.example.web3wallet.MainActivity.CUSTOM_GAS_PRICE;
import static com.example.web3wallet.MainActivity.credentials;
import static com.example.web3wallet.MainActivity.ticketfactory;
import static com.example.web3wallet.MainActivity.web3;

public class BuyTicketActivity extends AppCompatActivity {

    public Ticket721 ticket;
    public TicketSale721 ticket_sale;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buy_ticket);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        //заменяем на лямбду
        fab.setOnClickListener(view -> Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        SetupTicketContract();

    }

    public void getInfo(View v) {
        EditText eJID = (EditText) findViewById(R.id.event_jid);
        String JID = eJID.getText().toString();
        String[] saleInstances = getTicketSale(JID);    // Getting every items on sale
        String itemSaleAddress = saleInstances[0];      // Get address of particular item sale
        TicketSale721 itemSaleInstance = getSaleInstance(itemSaleAddress);  // Get instance of particular item sale
        BigDecimal price_wei = getSalePriceInfo(itemSaleInstance);
        // FIXME : convert price from wei (?)
        TextView sPrice = (TextView) findViewById(R.id.event_price);
        sPrice.setText(price_wei.toString());
    }

    public void buyTicketView(View v) {
        EditText eJID = (EditText) findViewById(R.id.event_jid); // FIXME: fix this
        String JID = eJID.getText().toString();
        String[] saleInstances = getTicketSale(JID);
        String itemSaleAddress = saleInstances[0];
        TicketSale721 itemSaleInstance = getSaleInstance(itemSaleAddress);

        EditText eAmount = (EditText) findViewById(R.id.ticket_amount);
        String sAmount = eAmount.getText().toString();
        int amount_int = Integer.parseInt(sAmount);
        BigInteger amount = BigInteger.valueOf(amount_int);
        BigDecimal amount_dec = new BigDecimal(amount);

        buyTicket(itemSaleInstance,amount_dec);
    }



    public void SetupTicketContract() {
        try {


         //   RemoteCall<String> ticket_template_address = ticketfactory.getTicketTemplateAddress().send();   //FIXME: change to async send.  //TODO: change to async send
          CompletableFuture <String> ticket_template_address = ticketfactory.getTicketTemplateAddress().sendAsync();
            String ticket_address = ticket_template_address.get();
            ticket = Ticket721.load(ticket_address, web3, credentials, CUSTOM_GAS_PRICE, CUSTOM_GAS_LIMIT);
        } catch(Exception e) {
            Log.e("eth_call_fail","error during ticket contract setup: ", e);
        }

    }

    public String[] getTicketSale(String event_jid){
        try {
            CompletableFuture<BigInteger> event_id_call = ticketfactory.getEventIdByJid(event_jid).sendAsync();

            BigInteger event_id = event_id_call.get();
            //RemoteCall<String[]> SaleInstances = ticket.eventsales(event_id);  // FIXME: Missing getter for eventsales at Ticket721.sol.
            CompletableFuture<List> SaleInstancesCall = ticket.getTicketSales(event_id).sendAsync();
            List SaleInstancesList = SaleInstancesCall.get();
            String[] SaleInstances = new String[SaleInstancesList.size()];
            SaleInstancesList.toArray(SaleInstances);
            return SaleInstances;
        } catch (Exception e) {
            Log.e("error","error in transaction remote call: " + e);
            return null;
        }
    }

    public BigInteger getEventIdByJid(String event_jid) {
        try {
            CompletableFuture<BigInteger> event_id_call = ticketfactory.getEventIdByJid(event_jid).sendAsync();
            BigInteger event_id = event_id_call.get();
            return event_id;
        } catch (Exception e) {
            Log.e("error","error in transaction remote call: " + e);
            return null;
        }
    }


    public TicketSale721 getSaleInstance(String sale_address) {
        ticket_sale = TicketSale721.load(sale_address,web3,credentials,CUSTOM_GAS_PRICE,CUSTOM_GAS_LIMIT);
        return ticket_sale;
    }

   //
    public BigDecimal getSalePriceInfo(TicketSale721 sale_instance) {
        try {
            CompletableFuture<BigInteger> price_wei_call = sale_instance.rate().sendAsync();
            BigInteger price_wei_int = price_wei_call.get();
            BigDecimal price_wei = new BigDecimal(price_wei_int);
            BigDecimal price = Convert.fromWei(price_wei, Convert.Unit.ETHER);
            return price;
        } catch (Exception e) {
            Log.e("tx-error","error in tx: " + e);
            return null;
        }
    }


    public void buyTicket(TicketSale721 sale_instance, BigDecimal amount) {
        BigDecimal price = getSalePriceInfo(sale_instance);
        BigDecimal price_wei = Convert.toWei(price, Convert.Unit.ETHER);
        BigDecimal sum = amount.multiply(price_wei);
        BigInteger sum_int = sum.toBigInteger();


        //вот эти вещи нельзя делать в активности,по хорошему нужно разделять на слои(юзаем архитектуру mvp or mvvm)
        CompletableFuture<TransactionReceipt> receipt = sale_instance.buyTicket(credentials.getAddress(),sum_int).sendAsync();
        receipt.thenAccept(transactionReceipt -> {
            // get tx receipt only if successful
            String txHash = transactionReceipt.getTransactionHash(); // can play with that
            // vtxHash = txHash;
            Log.d("receipt", "receipt"+transactionReceipt);
            Log.d("txhash", "txhash:" +txHash);
        }).exceptionally(transactionReceipt -> {
            Log.e("tx error", "tx error when BUY TICKEt:  " + transactionReceipt);
            return null;
        });
    }
}
