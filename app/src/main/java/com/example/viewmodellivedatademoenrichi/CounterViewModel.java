package com.example.viewmodellivedatademoenrichi;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

/**
 * ViewModel = the "logic and state" object for the screen.
 *
 * MainActivity is allowed to disappear and be recreated during rotation.
 * This ViewModel is kept by Android inside the Activity's ViewModelStore.
 *
 * Important mental model:
 * - Activity: draws the UI and receives clicks.
 * - ViewModel: owns the data and business logic.
 * - LiveData: notifies the UI when data changes, but only when the UI is active.
 */
public class CounterViewModel extends ViewModel {

    /*
     * Key used by SavedStateHandle.
     *
     * SavedStateHandle stores small pieces of state in Android's saved-state
     * system. That means it can restore the counter after process death too,
     * not only after rotation.
     */
    private static final String KEY_COUNT = "count";

    /*
     * SavedStateHandle is injected by AndroidX because MainActivity is a
     * ComponentActivity and therefore has a default SavedStateViewModelFactory.
     */
    private final SavedStateHandle savedStateHandle;

    /*
     * MutableLiveData = writable observable data.
     *
     * It stays private because only the ViewModel should modify the value.
     * This prevents the Activity from accidentally changing app state directly.
     */
    private final MutableLiveData<Integer> countLiveData = new MutableLiveData<>();

    public CounterViewModel(SavedStateHandle handle) {
        savedStateHandle = handle;

        /*
         * Constructor is called only when Android creates the ViewModel.
         *
         * During rotation:
         * - the existing ViewModel is reused;
         * - this constructor is NOT called again;
         * - the count remains in memory.
         *
         * During process death:
         * - the old ViewModel is gone;
         * - Android creates a new one;
         * - SavedStateHandle gives us the last saved count.
         */
        Integer restoredCount = savedStateHandle.get(KEY_COUNT);
        if (restoredCount == null) {
            restoredCount = 0;
        }

        countLiveData.setValue(restoredCount);
    }

    public void increment() {
        /*
         * setValue() must be called from the main thread.
         * Button clicks already happen on the main thread, so setValue() is correct.
         */
        int current = getCurrentCountSafely();
        updateCountOnMainThread(current + 1);
    }

    public void decrement() {
        int current = getCurrentCountSafely();
        updateCountOnMainThread(current - 1);
    }

    public void reset() {
        updateCountOnMainThread(0);
    }

    public void incrementFromBackground() {
        /*
         * This method intentionally uses a background thread for the bonus part.
         *
         * postValue() is safe from any thread. Android will deliver the value
         * back to LiveData observers on the main thread.
         */
        new Thread(() -> {
            int current = getCurrentCountSafely();
            int next = current + 1;

            /*
             * SavedStateHandle is updated here too so process-death restoration
             * keeps the same value as LiveData.
             */
            savedStateHandle.set(KEY_COUNT, next);
            countLiveData.postValue(next);
        }).start();
    }

    /*
     * LiveData = read-only observable data.
     *
     * The Activity can observe it, but cannot call setValue() or postValue().
     * This is the usual MVVM safety pattern:
     * private MutableLiveData + public LiveData getter.
     */
    public LiveData<Integer> getCount() {
        return countLiveData;
    }

    private int getCurrentCountSafely() {
        Integer current = countLiveData.getValue();
        if (current == null) {
            return 0;
        }
        return current;
    }

    private void updateCountOnMainThread(int nextCount) {
        /*
         * Keep LiveData and SavedStateHandle synchronized.
         *
         * LiveData handles active UI updates.
         * SavedStateHandle handles restoration after process death.
         */
        savedStateHandle.set(KEY_COUNT, nextCount);
        countLiveData.setValue(nextCount);
    }
}
