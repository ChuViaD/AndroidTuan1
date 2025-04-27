package com.example.tuan1android;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;
import com.example.tuan1android.database.AppDatabase;
import com.example.tuan1android.database.ConversionHistory;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ListView listViewHistory;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history); // tí nữa mình tạo XML này

        listViewHistory = findViewById(R.id.listViewHistory);

        db = Room.databaseBuilder(getApplicationContext(),
                        AppDatabase.class, "conversion_db")
                .allowMainThreadQueries()
                .build();

        loadHistory();
    }

    private void loadHistory() {
        List<ConversionHistory> historyList = db.conversionHistoryDao().getAllHistory();

        List<String> displayList = new ArrayList<>();
        for (ConversionHistory item : historyList) {
            String text = item.inputAmount + " " + item.fromCurrency + " → " +
                    item.resultAmount + " " + item.toCurrency +
                    " (" + item.date + ")";
            displayList.add(text);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, displayList);

        listViewHistory.setAdapter(adapter);
    }
}
