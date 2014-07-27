    package br.com.pellegrini.sunshine;

    /**
     * Created by Fabricio on 17/07/2014.
     */

    import android.content.Context;
    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.net.Uri;
    import android.os.AsyncTask;
    import android.os.Bundle;
    import android.preference.PreferenceManager;
    import android.support.v4.app.Fragment;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.Menu;
    import android.view.MenuInflater;
    import android.view.MenuItem;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.AdapterView;
    import android.widget.ArrayAdapter;
    import android.widget.ListView;
    import android.widget.Toast;

    import org.json.JSONArray;
    import org.json.JSONException;
    import org.json.JSONObject;

    import java.io.BufferedReader;
    import java.io.IOException;
    import java.io.InputStream;
    import java.io.InputStreamReader;
    import java.net.HttpURLConnection;
    import java.net.URL;
    import java.text.SimpleDateFormat;
    import java.util.ArrayList;
    import java.util.Date;

    /**
     * A placeholder fragment containing a simple view.
     */
    public class ForecastFragment extends Fragment {

        private static final String LOG_TAG = ForecastFragment.class.getSimpleName();
        private ArrayAdapter<String> mArrayAdapter;

        public ForecastFragment() {
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setHasOptionsMenu(true);
        }

        @Override
        public void onStart() {
            super.onStart();
            updateWeather();
        }

        @Override
        public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
            inflater.inflate(R.menu.forecastfragment, menu);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {

            int id = item.getItemId();
            if(id == R.id.action_refresh) {

                updateWeather();

                return true;
            }
            return super.onOptionsItemSelected(item);
        }

        private void updateWeather() {
            FetchWeatherTask task = new FetchWeatherTask();
            SharedPreferences lPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String lLocation = lPreferences.getString(getString(R.string.pref_location_key),
                    getString(R.string.pref_location_default_value));

            task.execute(lLocation, getString(R.string.pref_units_metric));
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {



            View rootView = inflater.inflate(R.layout.fragment_main, container, false);

            mArrayAdapter = new ArrayAdapter<String>(
                    getActivity(),
                    R.layout.list_item_forecast,
                    R.id.list_item_forecast_textview,
                    new ArrayList<String>());

            ListView lListView = (ListView) rootView.findViewById(R.id.listview_forecast);

            lListView.setAdapter(mArrayAdapter);
            lListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String text = mArrayAdapter.getItem(position);
                    //Toast.makeText(getActivity(), text, Toast.LENGTH_SHORT).show();

                    Intent lIntent = new Intent(getActivity(), DetailActivity.class).putExtra(Intent.EXTRA_TEXT, text);
                    //lIntent.putExtra()
                    startActivity(lIntent);
                }
            });


            return rootView;
        }

        public class FetchWeatherTask extends AsyncTask<String, Void, String[]> {

            private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();

            @Override
            protected void onPostExecute(String[] result) {
                if (result != null) {
                    mArrayAdapter.clear();
                    for(String dayForecastStr : result) {
                        mArrayAdapter.add(dayForecastStr);
                    }
                }
            }

            @Override
            protected String[] doInBackground(String... params) {

                // These two need to be declared outside the try/catch
                // so that they can be closed in the finally block.
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;

                // Will contain the raw JSON response as a string.
                String forecastJsonStr[] = null;

                String lPostalCode = params[0];
                String lMode = "json";
                String lUnits = params[1];
                String lCount = "7";

                try {

                    //Uri.Builder builder = new Uri.Builder();
                    //builder.scheme("https").authority("www.myawesomesite.com")
                    // .appendPath("turtles").appendPath("types")
                    // .appendQueryParameter("type", "1").appendQueryParameter("sort", "relevance");

                    final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";

                    Uri uri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                            .appendQueryParameter("q", lPostalCode)
                            .appendQueryParameter("mode", lMode)
                            .appendQueryParameter("units", lUnits)
                            .appendQueryParameter("cnt", lCount).build();
                    // Construct the URL for the OpenWeatherMap query
                    // Possible parameters are avaiable at OWM's forecast API page, at
                    // http://openweathermap.org/API#forecast
                    //"http://api.openweathermap.org/data/2.5/forecast/daily?q=94043&mode=json&units=metric&cnt=7"

                    Log.i(LOG_TAG, uri.toString());

                    URL url = new URL(uri.toString());

                    // Create the request to OpenWeatherMap, and open the connection
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // Read the input stream into a String
                    InputStream inputStream = urlConnection.getInputStream();
                    StringBuffer buffer = new StringBuffer();
                    if (inputStream == null) {
                        // Nothing to do.
                        return null;
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                        // But it does make debugging a *lot* easier if you print out the completed
                        // buffer for debugging.
                        buffer.append(line + "\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No point in parsing.
                        return null;
                    }
                    forecastJsonStr = getWeatherDataFromJson(buffer.toString(), Integer.parseInt(lCount));

                    for(String forecast : forecastJsonStr) {
                        Log.d(LOG_TAG, forecast);
                    }

                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error ", e);
                    // If the code didn't successfully get the weather data, there's no point in attemping
                    // to parse it.
                    return null;
                } catch (JSONException e) {
                    Log.e(LOG_TAG, "Error ", e);
                    e.printStackTrace();
                } finally{
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (final IOException e) {
                            Log.e(LOG_TAG, "Error closing stream", e);
                        }
                    }
                }
                return forecastJsonStr;
            }

            /* The date/time conversion code is going to be moved outside the asynctask later,
 * so for convenience we're breaking it out into its own method now.
 */
            private String getReadableDateString(long time){
                // Because the API returns a unix timestamp (measured in seconds),
                // it must be converted to milliseconds in order to be converted to valid date.
                Date date = new Date(time * 1000);
                SimpleDateFormat format = new SimpleDateFormat("E, MMM d");
                return format.format(date).toString();
            }

            /**
             * Prepare the weather high/lows for presentation.
             */
            private String formatHighLows(double high, double low) {
                // For presentation, assume the user doesn't care about tenths of a degree.
                SharedPreferences lPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                String lUnits = lPreferences.getString(getString(R.string.pref_units_key),getString(R.string.pref_units_metric));

                if(lUnits.equals(getString(R.string.pref_units_imperial))) {
                    high = (high * 1.8) + 32;
                    low = (low * 1.8) +32;
                } else if (!lUnits.equals(getString(R.string.pref_units_metric))) {
                    Log.d(LOG_TAG, "Unit type not found: " + lUnits);
                }

                long roundedHigh = Math.round(high);
                long roundedLow = Math.round(low);

                String highLowStr = roundedHigh + "/" + roundedLow;
                return highLowStr;
            }

            /**
             * Take the String representing the complete forecast in JSON Format and
             * pull out the data we need to construct the Strings needed for the wireframes.
             *
             * Fortunately parsing is easy:  constructor takes the JSON string and converts it
             * into an Object hierarchy for us.
             */
            private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                    throws JSONException {

                // These are the names of the JSON objects that need to be extracted.
                final String OWM_LIST = "list";
                final String OWM_WEATHER = "weather";
                final String OWM_TEMPERATURE = "temp";
                final String OWM_MAX = "max";
                final String OWM_MIN = "min";
                final String OWM_DATETIME = "dt";
                final String OWM_DESCRIPTION = "main";

                JSONObject forecastJson = new JSONObject(forecastJsonStr);
                JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

                String[] resultStrs = new String[numDays];
                for(int i = 0; i < weatherArray.length(); i++) {
                    // For now, using the format "Day, description, hi/low"
                    String day;
                    String description;
                    String highAndLow;

                    // Get the JSON object representing the day
                    JSONObject dayForecast = weatherArray.getJSONObject(i);

                    // The date/time is returned as a long.  We need to convert that
                    // into something human-readable, since most people won't read "1400356800" as
                    // "this saturday".
                    long dateTime = dayForecast.getLong(OWM_DATETIME);
                    day = getReadableDateString(dateTime);

                    // description is in a child array called "weather", which is 1 element long.
                    JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                    description = weatherObject.getString(OWM_DESCRIPTION);

                    // Temperatures are in a child object called "temp".  Try not to name variables
                    // "temp" when working with temperature.  It confuses everybody.
                    JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                    double high = temperatureObject.getDouble(OWM_MAX);
                    double low = temperatureObject.getDouble(OWM_MIN);

                    highAndLow = formatHighLows(high, low);
                    resultStrs[i] = day + " - " + description + " - " + highAndLow;
                }

                return resultStrs;
            }
        }

    }