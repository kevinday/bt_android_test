package kday.braintreetest;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;


import com.braintreepayments.api.Braintree;
import com.braintreepayments.api.Braintree.BraintreeSetupFinishedListener;
import com.braintreepayments.api.dropin.BraintreePaymentActivity;
import com.braintreepayments.api.Braintree.ErrorListener;
import com.braintreepayments.api.Braintree.PaymentMethodNonceListener;
import com.braintreepayments.api.dropin.Customization;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.dropin.Customization;
import com.braintreepayments.api.dropin.Customization.CustomizationBuilder;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements PaymentMethodNonceListener, ErrorListener{


    private OkHttpClient mHttpClient;

    private String mClientToken;
    private Braintree mBraintree;
    private String mPaymentNonce=null;

    private TextView mTextView;

    private Float mAmount = new Float(50.0);

    private final String mServerRoot = "https://still-tor-7502.herokuapp.com/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHttpClient = new OkHttpClient();

        if (mClientToken == null) {
            getClientToken(null);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void onBraintreeSubmit(View v) {
        Intent intent = new Intent(this, BraintreePaymentActivity.class);
        Customization customization = new CustomizationBuilder()
                .primaryDescription("Checkout")
                .secondaryDescription("test transaction")
                .amount(mAmount.toString())
                .submitButtonText("Do it")
                .build();

        intent.putExtra(BraintreePaymentActivity.EXTRA_CLIENT_TOKEN, mClientToken)
        .putExtra(BraintreePaymentActivity.EXTRA_CUSTOMIZATION, customization);
        startActivityForResult(intent, 100);
    }

    public void getClientToken(View v) {
        Request getClientTokenRequest = new Request.Builder()
                //.url("http://10.0.2.2:3000/client_token")
                .url(mServerRoot + "client_token")
                .build();

        mHttpClient.newCall(getClientTokenRequest).enqueue(new Callback() {
            @Override
            public void onResponse(final Response response) throws IOException {
                final String responseBody = response.body().string();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccessful()) {
                            try {
                                setClientToken(
                                        new JSONObject(responseBody).getString("client_token"));
                            } catch (JSONException e) {
                                updateTextView("Unable to decode client token");
                            }
                        } else {
                            updateTextView("Unable to get a client token. Response Code: " +
                                             response.code() + " Response body: " + responseBody);
                        }
                    }
                });
            }

            @Override
            public void onFailure(Request request, final IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateTextView("Request Failed");
                    }
                });
            }
        });
    }

    public void submitNonce(View v){

        if (mPaymentNonce == null){
            updateTextView("Get a Payment Nonce first.");
            return;
        }
        RequestBody formBody = new FormEncodingBuilder()
                .add("payment_method_nonce", mPaymentNonce)
                .add("payment_amount", mAmount.toString())
                .build();
        Request getClientTokenRequest = new Request.Builder()
                .url(mServerRoot + "purchases")
                .post(formBody)
                .build();

        mHttpClient.newCall(getClientTokenRequest).enqueue(new Callback() {
            @Override
            public void onResponse(final Response response) throws IOException {
                final String responseBody = response.body().string();
                updateTextView("Nonce submitted");
            }

            @Override
            public void onFailure(Request request, final IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateTextView("Nonce post Failed");
                    }
                });
            }

        });
    }

    @Override
    public void onUnrecoverableError(Throwable throwable) {
        //showDialog("An unrecoverable error was encountered (" +
        //        throwable.getClass().getSimpleName() + "): " + throwable.getMessage());
    }

    @Override
    public void onRecoverableError(ErrorWithResponse error) {
        //showDialog("A recoverable error occurred: " + error.getMessage());
    }

    @Override
    public void onPaymentMethodNonce(String paymentMethodNonce) {
        updateTextView("payment nonce: " + paymentMethodNonce);
        mPaymentNonce=paymentMethodNonce;
    }

    private void setClientToken(String clientToken) {
        updateTextView("Client token is: " + clientToken);
        mClientToken = clientToken;

        Braintree.setup(MainActivity.this, mClientToken, new BraintreeSetupFinishedListener() {
            @Override
            public void onBraintreeSetupFinished(boolean setupSuccessful,
                                                 Braintree braintree,
                                                 String errorMessage, Exception exception) {
                if (setupSuccessful) {
                    mBraintree = braintree;
                    mBraintree.addListener(MainActivity.this);
                    updateTextView("Braintree initialized");
                } else {
                    updateTextView(errorMessage);
                }
            }
        });

        //mBraintree.addListener(this);

    }

    private void updateTextView(final String text){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mTextView == null){
                    mTextView = (TextView)findViewById(R.id.textView);
                }
                mTextView.setText(text);
            }
        });

    }
}
