package com.example.jordan.sunshine.app;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.jordan.sunshine.R;
import com.example.jordan.sunshine.app.data.WeatherContract;


public class ForecastFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private final String LOG_TAG = ForecastFragment.class.getSimpleName();

    private static final int FORECAST_LOADER_ID = 1;
    private static final String POSITION_KEY = "position";

    ForecastAdapter mForecastAdapter;
    private int mPosition = ListView.INVALID_POSITION;
    private ListView mWeatherListView;
    private boolean mUseTodayLayout;

    private static final String[] FORECAST_COLUMNS = {
            // In this case the id needs to be fully qualified with a table name, since
            // the content provider joins the location & weather tables in the background
            // (both have an _id column)
            // On the one hand, that's annoying.  On the other, you can search the weather table
            // using the location set by the user, which is only in the Location table.
            // So the convenience is worth it.
            WeatherContract.WeatherEntry.TABLE_NAME + "." + WeatherContract.WeatherEntry._ID,
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_SHORT_DESC,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
            WeatherContract.LocationEntry.COLUMN_COORD_LAT,
            WeatherContract.LocationEntry.COLUMN_COORD_LONG
    };


    // These indices are tied to FORECAST_COLUMNS.  If FORECAST_COLUMNS changes, these
    // must change.
    static final int COL_WEATHER_ID = 0;
    static final int COL_WEATHER_DATE = 1;
    static final int COL_WEATHER_DESC = 2;
    static final int COL_WEATHER_MAX_TEMP = 3;
    static final int COL_WEATHER_MIN_TEMP = 4;
    static final int COL_LOCATION_SETTING = 5;
    static final int COL_WEATHER_CONDITION_ID = 6;
    static final int COL_COORD_LAT = 7;
    static final int COL_COORD_LONG = 8;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onItemSelected(Uri dateUri);
    }

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getLoaderManager().initLoader(FORECAST_LOADER_ID, null, this);
    }


    void onLocationChanged() {
        updateWeather();
        getLoaderManager().restartLoader(FORECAST_LOADER_ID, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            Log.d(LOG_TAG, "manually refreshing weather");
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        if (null != savedInstanceState) {
            mPosition = savedInstanceState.getInt(POSITION_KEY);
        }

        String locationSetting = Utility.getPreferredLocation(getActivity());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        Cursor cur = getActivity().getContentResolver().query(weatherForLocationUri,
                null, null, null, sortOrder);

        mForecastAdapter = new ForecastAdapter(getActivity(), cur, 0);

        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mWeatherListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mWeatherListView.setAdapter(mForecastAdapter);

        mWeatherListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView adapterView, View view, int position, long l) {


                // CursorAdapter returns a cursor at the correct position for getItem(), or null
                // if it cannot seek to that position.
                Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
                if (cursor != null) {
                    String locationSetting = Utility.getPreferredLocation(getActivity());
                    ((Callback) getActivity())
                            .onItemSelected(WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
                                    locationSetting, cursor.getLong(COL_WEATHER_DATE)
                            ));
                }
                mPosition = position;
            }
        });

        if (savedInstanceState != null && savedInstanceState.containsKey(POSITION_KEY)) {
            mPosition = savedInstanceState.getInt(POSITION_KEY);
        }

        return rootView;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (ListView.INVALID_POSITION != mPosition) {
            outState.putInt(POSITION_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
    }

    //  example api url for 7 days of weather data for 92691 in json format
//        http://api.openweathermap.org/data/2.5/forecast/daily?q=92691&mode=json&units=metric&cnt=7
    private void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity());
        String location = Utility.getPreferredLocation(getActivity());
        weatherTask.execute(location);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        switch (i) {
            case FORECAST_LOADER_ID:
                String locationSetting = Utility.getPreferredLocation(getActivity());

                //Sort order: Ascend by date
                String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
                Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                        locationSetting, System.currentTimeMillis());

                return new CursorLoader(
                        getActivity(),
                        weatherForLocationUri,
                        FORECAST_COLUMNS,
                        null,
                        null,
                        sortOrder
                );
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mForecastAdapter.swapCursor(cursor);
        if (mPosition != ListView.INVALID_POSITION) {
            mWeatherListView.smoothScrollToPosition(mPosition);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mForecastAdapter.swapCursor(null);
    }

    public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (mForecastAdapter != null) {
            mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        }
    }
}
