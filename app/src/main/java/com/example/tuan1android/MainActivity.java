package com.example.tuan1android;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.tuan1android.ExchangeRateApiService;
import com.example.tuan1android.database.AppDatabase;
import com.example.tuan1android.database.ConversionHistory;
import com.example.tuan1android.ExchangeRateResponse;
import com.example.tuan1android.Key;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private Spinner spinnerFrom, spinnerTo;
    private EditText editTextAmount;
    private Button buttonConvert, buttonHistory;
    private TextView textViewResult;

    private ExchangeRateApiService apiService;
    private Map<String, Double> conversionRates;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // sẽ làm file XML ngay sau

        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        editTextAmount = findViewById(R.id.editTextAmount);
        buttonConvert = findViewById(R.id.buttonConvert);
        buttonHistory = findViewById(R.id.buttonHistory);
        textViewResult = findViewById(R.id.textViewResult);

        // Setup spinner data
        String[] currencies = {"USD", "EUR", "VND", "JPY", "CNY", "KRW"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, currencies);
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);

        // Setup Retrofit
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Key.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ExchangeRateApiService.class);

        // Setup Room DB
        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "conversion_db")
                .allowMainThreadQueries() // Tạm cho phép để đơn giản, sản phẩm thật thì dùng AsyncTask/Coroutine
                .build();

        fetchExchangeRates("USD"); // Load mặc định USD khi vào app

        buttonConvert.setOnClickListener(v -> {
            convertCurrency();
        });

        buttonHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }

    private void fetchExchangeRates(String baseCurrency) {
        Call<ExchangeRateResponse> call = apiService.getExchangeRates(Key.API_KEY, baseCurrency);
        call.enqueue(new Callback<ExchangeRateResponse>() {
            @Override
            public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    conversionRates = response.body().getConversionRates();
                    Toast.makeText(MainActivity.this, "Tải tỷ giá thành công!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Lỗi tải tỷ giá", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void convertCurrency() {
        if (conversionRates == null) {
            Toast.makeText(this, "Chưa có tỷ giá!", Toast.LENGTH_SHORT).show();
            return;
        }

        String fromCurrency = spinnerFrom.getSelectedItem().toString();
        String toCurrency = spinnerTo.getSelectedItem().toString();
        String amountStr = editTextAmount.getText().toString();

        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Nhập số tiền!", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        // Lấy tỷ giá từ base currency sang fromCurrency và toCurrency
        double fromRate = conversionRates.get(fromCurrency);
        double toRate = conversionRates.get(toCurrency);

        // Tính ra số tiền đã chuyển đổi
        double result = amount / fromRate * toRate;

        textViewResult.setText(String.format(Locale.getDefault(), "%.2f %s", result, toCurrency));

        // Lưu vào lịch sử
        saveHistory(fromCurrency, toCurrency, amount, result);
    }

    private void saveHistory(String from, String to, double inputAmount, double resultAmount) {
        ConversionHistory history = new ConversionHistory();
        history.fromCurrency = from;
        history.toCurrency = to;
        history.inputAmount = inputAmount;
        history.resultAmount = resultAmount;
        history.date = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date());

        db.conversionHistoryDao().insert(history);
    }
    private void setupDailyUpdate() {
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(UpdateRatesWorker.class,
                1, TimeUnit.DAYS)
                .build();

        WorkManager.getInstance(this).enqueue(workRequest);
    }
}
