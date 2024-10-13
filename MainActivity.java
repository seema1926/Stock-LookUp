package com.example.stock_lookup;

import androidx.appcompat.app.AppCompatActivity;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    TextView stockPriceTextView, exchangeTextView, companyText;
    EditText symbol;
    Button updateButton;
    String symbolString;
    private RequestQueue requestQueue;
    double previousPrice = 0.0;
    private static final String API_KEY = "70d963ad25123c5aeb38d4f781c39095";   // API key here
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Timer timer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        stockPriceTextView = findViewById(R.id.stockPriceTextView);
        exchangeTextView = findViewById(R.id.exchangeTextView);
        companyText = findViewById(R.id.companyTextView);

        updateButton = findViewById(R.id.updateButton);
        symbol = findViewById(R.id.symbolName);
        requestQueue = Volley.newRequestQueue(this);

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                symbolString = symbol.getText().toString();
                if(!symbolString.isEmpty())
                {
                    fetchStockPrice();
                }
                else {
                    Toast.makeText(MainActivity.this, "Please enter a valid symbol", Toast.LENGTH_SHORT).show();
                }
            }
        });

        timer = new Timer();
        symbolString = symbol.getText().toString();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(!symbolString.isEmpty())
                {
                    fetchStockPrice();
                }
            }
        }, 0, 2*1000);

    }

    private void fetchStockPrice() {
        String symbolString = symbol.getText().toString();
        String apiUrl = "https://marketstack.com/" + symbolString + "/quote?token=" +API_KEY;
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, apiUrl, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String stockPrice = response.getString("latestPrice");
                            String exchange = response.getString("primaryExchange");
                            String company = response.getString("companyName");

                            double stockPriceValue = Double.parseDouble(stockPrice);
                            updateStockPrice(stockPriceValue, "Exchange: " + exchange, "Company: " + company);

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Log.e("ERROR", "Error parsing JSON: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if(error.networkResponse!=null&& error.networkResponse.data!=null)
                        {
                            String responseBody = new String(error.networkResponse.data);
                            Log.e("API_ERROR", "Error Response: " + responseBody);
                        }
                        Log.e("API_ERROR", "Error fetching stock price: " + error.getMessage());
                    }
                });
        requestQueue.add(request);

    }

    private void updateStockPrice(double currentPrice, String exchange, String company) {
        double instantaneousChange = currentPrice - previousPrice;
        previousPrice = currentPrice;
        handler.post(new Runnable() {
            @Override
            public void run() {
                startBlinkAnimation(stockPriceTextView);
                String formattedPrice = String.format("%2f", currentPrice);
                stockPriceTextView.setText(formattedPrice);
                int textColor = (instantaneousChange>=0.00) ? Color.GREEN : Color.RED;
                stockPriceTextView.setTextColor(textColor);
                exchangeTextView.setText(exchange);
                companyText.setText(company);
            }
        });

    }


    private void startBlinkAnimation(final TextView textView)
    {
        ObjectAnimator blink = ObjectAnimator.ofFloat(textView, "alpha", 1f, 0f, 1f);
        blink.setDuration(200);
        blink.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(timer!=null)
        {
            timer.cancel();
            timer.purge();
        }
    }
}