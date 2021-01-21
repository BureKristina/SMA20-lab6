package com.example.lab6;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lab6.model.MonthlyExpenses;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import static com.google.android.gms.common.util.ArrayUtils.contains;

public class MainActivity extends AppCompatActivity {

    private TextView tStatus;
    private EditText eSearch, eIncome, eExpenses;

    private DatabaseReference databaseReference;
    ValueEventListener databaseListener;
    private final String[] months = {"january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tStatus = findViewById(R.id.tStatus);
        eSearch = findViewById(R.id.eSearch);
        eIncome = findViewById(R.id.eIncome);
        eExpenses = findViewById(R.id.eExpenses);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getMonths(new FirebaseCallback() {
            @Override
            public void onCallBack(ArrayList<String> m) {
                Spinner spinner = MainActivity.this.findViewById(R.id.spinnerMonths);
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this.getBaseContext(), android.R.layout.simple_spinner_dropdown_item, m);
                spinner.setAdapter(adapter);
                spinner.setSelection(0, false);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (!parent.getItemAtPosition(position).toString().toLowerCase().equals(eSearch.getText().toString()))
                            eSearch.setText(parent.getItemAtPosition(position).toString().toLowerCase());
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });
            }
        });
    }

    public void clicked(View view) {
        String currentMonth;
        switch (view.getId()) {
            case R.id.bSearch:
                if (!eSearch.getText().toString().isEmpty()) {
                    // save text to lower case (all our months are stored online in lower case)
                    currentMonth = eSearch.getText().toString().toLowerCase();
                    SharedPreferences.Editor sharedPreferencesEditor = getSharedPreferences("lastMonth", MODE_PRIVATE).edit();
                    sharedPreferencesEditor.putString("month", currentMonth);
                    sharedPreferencesEditor.apply();
                    createNewDBListener(currentMonth);

                    tStatus.setText("Searching ...");
                } else {
                    Toast.makeText(this, "Enter a month", Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.bUpdate:
                boolean hasIncome = eIncome.getText().toString().isEmpty();
                boolean hasExpenses = eExpenses.getText().toString().isEmpty();
                boolean hasMonth = eSearch.getText().toString().isEmpty();

                if (!hasIncome && !hasExpenses && !hasMonth && contains(months, eSearch.getText().toString().toLowerCase())) {
                    currentMonth = eSearch.getText().toString().toLowerCase();
                    float income = Float.parseFloat(eIncome.getText().toString());
                    float expenses = Float.parseFloat(eExpenses.getText().toString());

                    MonthlyExpenses monthlyExpenses = new MonthlyExpenses(currentMonth, income, expenses);
                    databaseReference.child("calendar").child(currentMonth).child("income").setValue(income);
                    databaseReference.child("calendar").child(currentMonth).child("expenses").setValue(expenses);
                } else {
                    Toast.makeText(this, "Enter the month, income and expenses",
                            Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void createNewDBListener(final String currentMonth) {
        // remove previous databaseListener
        if (databaseReference != null && currentMonth != null && databaseListener != null)
            databaseReference.child("calendar").child(currentMonth).removeEventListener(databaseListener);

        databaseListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    // This method is called once with the initial value and again
                    // whenever data at this location is updated.
                    MonthlyExpenses monthlyExpense = dataSnapshot.getValue(MonthlyExpenses.class);
                    // explicit mapping of month name from entry key
                    monthlyExpense.month = dataSnapshot.getKey();

                    eIncome.setText(String.valueOf(monthlyExpense.getIncome()));
                    eExpenses.setText(String.valueOf(monthlyExpense.getExpenses()));
                    tStatus.setText("Found entry for " + currentMonth);
                } catch (NullPointerException e) {
                    eIncome.setText(String.valueOf(0));
                    eExpenses.setText(String.valueOf(0));
                    tStatus.setText("There is no entry for " + currentMonth);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
            }
        };

        // set new databaseListener
        databaseReference.child("calendar").child(currentMonth).addValueEventListener(databaseListener);
    }

    private void getMonths(final FirebaseCallback callback) {
        final ArrayList<String> firebaseMonths = new ArrayList<String>();
        Query query = databaseReference.child("calendar").orderByValue();
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot month : snapshot.getChildren()) {
                        firebaseMonths.add(month.getKey());
                    }
                    callback.onCallBack(firebaseMonths);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        Spinner spinner = findViewById(R.id.spinnerMonths);
        String[] items = firebaseMonths.toArray(new String[0]);
        System.out.println(items.toString());

    }

    private interface FirebaseCallback {
        void onCallBack(ArrayList<String> m);
    }
}