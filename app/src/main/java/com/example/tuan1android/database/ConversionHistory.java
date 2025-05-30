package com.example.tuan1android.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "conversion_history")
public class ConversionHistory {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String fromCurrency;
    public String toCurrency;
    public double inputAmount;
    public double resultAmount;
    public String date;
}
