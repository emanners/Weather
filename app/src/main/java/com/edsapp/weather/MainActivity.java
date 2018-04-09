package com.edsapp.weather;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.PersistableBundle;
import android.support.v4.view.LayoutInflaterCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.edsapp.weather.data.ForecastList;
import com.edsapp.weather.service.NetworkService;
import com.edsapp.weather.utility.TimeHelper;

import com.mikepenz.iconics.context.IconicsLayoutInflater;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements WeatherListener {

    public static final int MSG_JOB_START = 1;
    public static final int MSG_JOB_STOP = 2;

    public static final String MESSENGER_INTENT_KEY
            = BuildConfig.APPLICATION_ID + ".MESSENGER_INTENT_KEY";

    private static final char CH_DEGREE = 0x00B0;
    public static final int NUM_OF_DAYS = 6;

    private LayoutInflater vi = null;
    private DataBroker dataBroker;

    private ComponentName serviceComponent;
    private int mJobId = 0;

    // Handler for incoming messages from the service.
    private IncomingMessageHandler incomingMessageHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory(getLayoutInflater(), new IconicsLayoutInflater(getDelegate()));
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.weather_title));
        setContentView(R.layout.activity_main);

        vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (LayoutInflaterCompat.getFactory(vi) == null)
            LayoutInflaterCompat.setFactory(vi, new IconicsLayoutInflater(getDelegate()));

        dataBroker = new DataBroker(this,this);
        dataBroker.setTimePeriod(DataBroker.IMMEDIATE);

        serviceComponent = new ComponentName(MainActivity.this, NetworkService.class);
        incomingMessageHandler = new IncomingMessageHandler(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        dataBroker.getForecast();
        Toast.makeText(getApplicationContext(), R.string.usage_hint, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        // A service can be "started" and/or "bound". In this case, it's "started" by this Activity
        // and "bound" to the JobScheduler (also called "Scheduled" by the JobScheduler). This call
        // to stopService() won't prevent scheduled jobs to be processed. However, failing
        // to call stopService() would keep it alive indefinitely.
        stopService(new Intent(this, NetworkService.class));
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Start service and provide it a way to communicate with this class.
        Intent startServiceIntent = new Intent(this, NetworkService.class);
        Messenger messengerIncoming = new Messenger(incomingMessageHandler);
        startServiceIntent.putExtra(MESSENGER_INTENT_KEY, messengerIncoming);
        startService(startServiceIntent);
        scheduleJob();
    }

    /**
     * Start the scheduled Job
     */
    public void scheduleJob() {
        JobInfo.Builder builder = new JobInfo.Builder(mJobId++, serviceComponent);
        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // Best Guess at WIFI, but this could be metered as well!!

        builder.setRequiresDeviceIdle(false);
        builder.setRequiresCharging(false);

        // Extras, work duration.
        PersistableBundle extras = new PersistableBundle();
        builder.setExtras(extras);
        builder.setPeriodic(dataBroker.getTimePeriod());

        JobScheduler tm = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (tm != null) {
            tm.schedule(builder.build());
        }
    }

    public void refresh() {
        dataBroker.getForecast();
    }
    /**
     *
     * This method is used to update the UI and generate the dynamic days weather forecast
     *
     * @param forecastMap - the map that is returned from the weather get forecast service
     *
     *
     */
    public void updateData(Map<String, ForecastList> forecastMap) {
        ViewGroup parentGroup = findViewById(R.id.fiveDayLayout);
        parentGroup.removeAllViews();

        if (forecastMap == null) {
            return; // TODO - unable to retrieve data at all.
        }

        TextView tempText = findViewById(R.id.tempText);
        TextView descriptionText = findViewById(R.id.descriptionText);

        descriptionText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataBroker.getForecast();
            }
        });
        TextView iconText = findViewById(R.id.iconText);
        iconText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dataBroker.getForecast();
            }
        });
        populateForecast(forecastMap, parentGroup, tempText, descriptionText, iconText);
    }

    public void populateForecast(Map<String, ForecastList> forecastMap,
                                 ViewGroup parentGroup,
                                 TextView tempText,
                                 TextView descriptionText,
                                 TextView iconText) {
        int i = 0;
        String desc;
        String temp;
        String icon;

        for (String day: TimeHelper.getNextDays(NUM_OF_DAYS)) {
            ForecastList forecastList = forecastMap.get(day);

            if ((forecastList != null) && !forecastList.getWeather().isEmpty()) {
                icon = "{wic-owm-" + forecastList.getWeather().get(0).getId() +"}";
                desc = forecastList.getWeather().get(0).getDescription();
                temp = String.format(Locale.ENGLISH,"%d%c",forecastList.getMain().getTemp().intValue(), CH_DEGREE);

            } else {
                // TODO Add some Error processing
                continue;
            }

            if (i++ == 0) { // Today's weather
                tempText.setText(temp);
                iconText.setText(icon);
                descriptionText.setText(desc);
                iconText.animate().setDuration(2000);
                iconText.animate().rotationYBy(720);

            } else { // The next set of weather days
                View v = vi.inflate(R.layout.miniday, parentGroup, false);
                v.setTag(day);
                TextView textView = v.findViewById(R.id.dayText);
                TextView textIcon = v.findViewById(R.id.dayIcon);
                TextView textTemp = v.findViewById(R.id.dayTemp);
                textView.setText(day);
                textIcon.setText(icon);
                textTemp.setText(temp);
                parentGroup.addView(v);
            }
        }
    }

    @Override
    public void onError(String errorText, Throwable throwable) {
        Toast.makeText(getApplicationContext(), errorText, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A {@link Handler} allows you to send messages associated with a thread. A {@link Messenger}
     * uses this handler to communicate from {@link NetworkService}.
     */
    private static class IncomingMessageHandler extends Handler {

        // Prevent possible leaks with a weak reference.
        private final WeakReference<MainActivity> mActivity;

        IncomingMessageHandler(MainActivity activity) {
            super(/* default looper */);
            this.mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            MainActivity mainActivity = mActivity.get();
            if (mainActivity == null) {
                // Activity is no longer available, exit.
                return;
            }
            Message m;
            switch (msg.what) {
                /*
                 * Receives callback from the service when a job has landed
                 * on the app. Turns on indicator and sends a message to turn it off after
                 * a second.
                 */
                case MSG_JOB_START:
                    // Start received
                    break;
                /*
                 * Receives callback from the service when a job that previously landed on the
                 * app must stop executing. Sends a message to turn it
                 * off after two seconds.
                 */
                case MSG_JOB_STOP:
                    // Stop received,
                    //msg.obj
                    mainActivity.refresh(); // Retrieve data from the data Broker.
                    break;
                default:
                    break;
            }
        }
    }
}
