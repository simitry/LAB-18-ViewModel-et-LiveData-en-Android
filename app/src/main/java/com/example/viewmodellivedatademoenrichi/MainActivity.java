package com.example.viewmodellivedatademoenrichi;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

/**
 * MainActivity = the View layer.
 *
 * Its job is intentionally small:
 * - inflate the XML layout;
 * - connect buttons to ViewModel methods;
 * - observe LiveData and draw the latest value.
 *
 * The Activity does NOT own the counter value in the final version.
 */
public class MainActivity extends ComponentActivity {

    /*
     * This is the modern state holder.
     *
     * Android keeps it across configuration changes such as:
     * - rotation;
     * - language change;
     * - night/light theme switch;
     * - window size changes on foldables/tablets.
     */
    private CounterViewModel viewModel;

    /*
     * UI references.
     * They belong to the Activity because views are destroyed and recreated
     * together with the Activity.
     */
    private TextView tvCount;
    private Button btnIncrement;
    private Button btnDecrement;
    private Button btnReset;
    private Button btnBackgroundIncrement;

    /*
     * ============================
     * PARTIE 1: VERSION CLASSIQUE
     * ============================
     *
     * The classic broken version would look like this:
     *
     *     private int count = 0;
     *
     * Then every button click would modify count directly:
     *
     *     count++;
     *     tvCount.setText(String.valueOf(count));
     *
     * Problem:
     * - rotate the screen;
     * - Android destroys this Activity object;
     * - Android creates a brand-new Activity object;
     * - count becomes 0 again.
     *
     * onSaveInstanceState() can save simple values, but it becomes awkward for
     * real screens with asynchronous work, repositories, large data, or streams.
     * That is why the active code below uses ViewModel + LiveData instead.
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /*
         * Android creates the view hierarchy from activity_main.xml.
         * After rotation, this line creates a brand-new set of View objects.
         */
        setContentView(R.layout.activity_main);

        findViews();
        connectViewModel();
        connectButtons();
    }

    private void findViews() {
        /*
         * findViewById is intentionally used here because it is explicit for a lab.
         * In bigger projects, ViewBinding is often nicer and safer.
         */
        tvCount = findViewById(R.id.tvCount);
        btnIncrement = findViewById(R.id.btnIncrement);
        btnDecrement = findViewById(R.id.btnDecrement);
        btnReset = findViewById(R.id.btnReset);
        btnBackgroundIncrement = findViewById(R.id.btnBackgroundIncrement);
    }

    private void connectViewModel() {
        /*
         * ViewModelProvider does two jobs:
         * - if the ViewModel already exists in the ViewModelStore, return it;
         * - otherwise create it.
         *
         * "this" is a ComponentActivity, so it is:
         * - a LifecycleOwner for LiveData observation;
         * - a ViewModelStoreOwner for ViewModel storage;
         * - a SavedStateRegistryOwner for SavedStateHandle.
         */
        viewModel = new ViewModelProvider(this).get(CounterViewModel.class);

        /*
         * observe(this, observer) is lifecycle-aware.
         *
         * The observer receives updates only while this Activity is STARTED or
         * RESUMED. If the Activity is stopped/destroyed, LiveData will not try
         * to update dead views. That prevents leaks and UI crashes.
         */
        viewModel.getCount().observe(this, new Observer<Integer>() {
            @Override
            public void onChanged(Integer newCount) {
                /*
                 * This method runs on the main thread.
                 * It is the only place where the counter TextView is updated.
                 */
                tvCount.setText(String.valueOf(newCount));
            }
        });
    }

    private void connectButtons() {
        /*
         * Notice the separation:
         * - Activity receives the click.
         * - ViewModel changes the state.
         * - LiveData calls the observer.
         * - Observer updates the TextView.
         */
        btnIncrement.setOnClickListener(view -> viewModel.increment());
        btnDecrement.setOnClickListener(view -> viewModel.decrement());
        btnReset.setOnClickListener(view -> viewModel.reset());

        /*
         * Bonus button:
         * It proves that postValue() can update LiveData from a background thread.
         */
        btnBackgroundIncrement.setOnClickListener(view -> viewModel.incrementFromBackground());
    }
}
