package com.example.jordan.sunshine.app;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.example.jordan.sunshine.R;
import com.example.jordan.sunshine.app.data.WeatherContract;


public class ForecastFragment extends Fragment {

    private final String LOG_TAG = ForecastFragment.class.getSimpleName();
//    private final String DEFAULT_WEATHER_POSTAL = "92691";

    ForecastAdapter mForecastAdapter;

    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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

//        String[] fakeWeatherArray = {};
//        final List<String> weatherForecast = new ArrayList<>(Arrays.asList(fakeWeatherArray));
//        mForecastAdapter = new ArrayAdapter<>(
//                getActivity(),
//                R.layout.list_item_forecast,
//                R.id.list_item_forecast_textview,
//                weatherForecast);
//
//        weatherListView.setOnItemClickListener((parent, view, position, id) -> {
//            String forecast = mForecastAdapter.getItem(position);
////                Toast.makeText(getActivity(), forecast, Toast.LENGTH_SHORT).show();
//            Intent detailIntent = new Intent(getActivity(), DetailActivity.class)
//                    .putExtra(Intent.EXTRA_TEXT, forecast);
//            startActivity(detailIntent);
//        });

        String locationSetting = Utility.getPreferredLocation(getActivity());

        // Sort order:  Ascending, by date.
        String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";
        Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithStartDate(
                locationSetting, System.currentTimeMillis());

        Cursor cur = getActivity().getContentResolver().query(weatherForLocationUri,
                null, null, null, sortOrder);

        mForecastAdapter = new ForecastAdapter(getActivity(), cur, 0);

        final View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        ListView weatherListView = (ListView)rootView.findViewById(R.id.listview_forecast);
        weatherListView.setAdapter(mForecastAdapter);

        return rootView;
    }

    //  example api url for 7 days of weather data for 92691 in json format
//        http://api.openweathermap.org/data/2.5/forecast/daily?q=92691&mode=json&units=metric&cnt=7
    private void updateWeather() {
        FetchWeatherTask weatherTask = new FetchWeatherTask(getActivity());
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = Utility.getPreferredLocation(getActivity());
        weatherTask.execute(location);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }
}
