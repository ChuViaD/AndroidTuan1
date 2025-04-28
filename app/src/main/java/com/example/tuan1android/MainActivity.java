package com.example.tuan1android;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.example.tuan1android.database.AppDatabase;
import com.example.tuan1android.database.ConversionHistory;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    private Button buttonHistory;
    private TextView textViewResult;

    private ExchangeRateApiService apiService;
    private Map<String, Double> conversionRates;
    private AppDatabase db;
    private LineChart lineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        lineChart = findViewById(R.id.lineChart);
        //fetchExchangeRateHistory();
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo = findViewById(R.id.spinnerTo);
        editTextAmount = findViewById(R.id.editTextAmount);
        Button buttonConvert = findViewById(R.id.buttonConvert);
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

        fetchExchangeRates(); // Load mặc định USD khi vào app

        buttonConvert.setOnClickListener(v -> {
            convertCurrency();
        });

        buttonHistory.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, HistoryActivity.class);
            startActivity(intent);
        });
    }

    /*private void fetchExchangeRates() {
        Call<ExchangeRateResponse> call = apiService.getExchangeRates(Key.API_KEY, "USD");
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
    }*/

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
    private void setupLineChart(Map<String, Double> rates) {
        if (rates == null || rates.isEmpty()) return;

        List<Entry> entries = new ArrayList<>();
        int index = 0;
        for (Map.Entry<String, Double> entry : rates.entrySet()) {
            entries.add(new Entry(index++, entry.getValue().floatValue()));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Tỷ giá USD sang các loại tiền khác");
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setColor(getResources().getColor(android.R.color.holo_purple));  // hoặc màu khác
        dataSet.setValueTextColor(getResources().getColor(android.R.color.black));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);

        lineChart.getAxisRight().setEnabled(false); // Tắt trục phải
        lineChart.getDescription().setEnabled(false); // Tắt mô tả mặc định
        lineChart.invalidate(); // Refresh lại biểu đồ
    }
    public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
        if (response.isSuccessful() && response.body() != null) {
            conversionRates = response.body().getConversionRates();
            Toast.makeText(MainActivity.this, "Tải tỷ giá thành công!", Toast.LENGTH_SHORT).show();
            setupLineChart(conversionRates); // <-- thêm dòng này nè
        } else {
            Toast.makeText(MainActivity.this, "Lỗi tải tỷ giá", Toast.LENGTH_SHORT).show();
        }
    }
    private void fetchExchangeRates() {
        // Kiểm tra trước khi gọi API
        if (apiService == null) {
            Toast.makeText(MainActivity.this, "API Service không sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Gọi API để lấy tỷ giá
        Call<ExchangeRateResponse> call = apiService.getExchangeRates(Key.API_KEY, "USD");

        call.enqueue(new Callback<ExchangeRateResponse>() {
            @Override
            public void onResponse(Call<ExchangeRateResponse> call, Response<ExchangeRateResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Nếu thành công, lấy dữ liệu tỷ giá và cập nhật
                    conversionRates = response.body().getConversionRates();

                    // Hiển thị thông báo thành công
                    Toast.makeText(MainActivity.this, "Tải tỷ giá thành công!", Toast.LENGTH_SHORT).show();

                    // Cập nhật giao diện người dùng
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Cập nhật UI (ví dụ: tự động điền tỷ giá vào Spinner nếu cần)
                        }
                    });

                } else {
                    // Nếu API trả về lỗi
                    Toast.makeText(MainActivity.this, "Lỗi tải tỷ giá", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ExchangeRateResponse> call, Throwable t) {
                // Nếu gọi API thất bại (lỗi mạng)
                Toast.makeText(MainActivity.this, "Lỗi mạng: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
