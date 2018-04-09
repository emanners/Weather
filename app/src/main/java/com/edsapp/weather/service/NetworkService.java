//Myservice.java

package com.edsapp.weather.service;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.edsapp.weather.DataBroker;

import com.edsapp.weather.WeatherListener;
import com.edsapp.weather.data.ForecastList;

import java.util.Map;

import static com.edsapp.weather.MainActivity.MESSENGER_INTENT_KEY;
import static com.edsapp.weather.MainActivity.MSG_JOB_START;
import static com.edsapp.weather.MainActivity.MSG_JOB_STOP;

/**
 * Service to handle callbacks from the JobScheduler. Requests scheduled with the JobScheduler
 * ultimately land on this service's "onStartJob" method. It runs jobs.
 * It keeps the activity updated with changes via a Messenger.
 */

public class NetworkService extends JobService implements WeatherListener {

   private static final String TAG = NetworkService.class.getSimpleName();
   private Messenger mActivityMessenger;
   private DataBroker dataBroker;

   private JobParameters params;

   @Override
   public void onCreate() {
      super.onCreate();
      Log.i(TAG, "Service created");
   }

   @Override
   public void onDestroy() {
      super.onDestroy();
      Log.i(TAG, "Service destroyed");
   }

   /**
    * When the app's MainActivity is created, it starts this service. This is so that the
    * activity and this service can communicate back and forth. See "setUiCallback()"
    */
   @Override
   public int onStartCommand(Intent intent, int flags, int startId) {
      mActivityMessenger = intent.getParcelableExtra(MESSENGER_INTENT_KEY);
      dataBroker = new DataBroker(getApplicationContext(), this);
      return START_NOT_STICKY;
   }

   @Override
   public boolean onStartJob(final JobParameters params) {
      // The work that this service "does" is simply wait for a certain duration and finish
      // the job (on another thread).
      sendMessage(MSG_JOB_START, params.getJobId());
      Log.i(TAG, "on start job: " + params.getJobId());

      this.params = params;
      dataBroker.getLiveForecast();
      // Return true as there's more work to be done with this job.
      return true;
   }

   @Override
   public boolean onStopJob(JobParameters params) {
      // Stop tracking these job parameters, as we've 'finished' executing.
      sendMessage(MSG_JOB_STOP, params.getJobId());
      Log.i(TAG, "on stop job: " + params.getJobId());

      // Return false to drop the job.
      return false;
   }

   private void sendMessage(int messageID, @Nullable Object params) {
      // If this service is launched by the JobScheduler, there's no callback Messenger. It
      // only exists when the MainActivity calls startService() with the callback in the Intent.
      if (mActivityMessenger == null) {
         Log.d(TAG, "Service is bound, not started. There's no callback to send a message to.");
         return;
      }
      Message m = Message.obtain();
      m.what = messageID;
      m.obj = params;
      try {
         mActivityMessenger.send(m);
      } catch (RemoteException e) {
         Log.e(TAG, "Error passing service object back to activity.");
      }
   }

   @Override
   public void updateData(Map<String, ForecastList> forecastMap) {
      sendMessage(MSG_JOB_STOP, forecastMap);
      jobFinished(params, true);
   }

   @Override
   public void onError(String errorText, @Nullable Throwable throwable) {
      sendMessage(MSG_JOB_STOP, params.getJobId());
      jobFinished(params, true);
   }
}