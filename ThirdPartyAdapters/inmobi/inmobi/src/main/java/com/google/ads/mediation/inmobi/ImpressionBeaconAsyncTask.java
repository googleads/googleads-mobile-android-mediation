package com.google.ads.mediation.inmobi;

        import android.os.AsyncTask;
        import android.util.Log;

        import java.io.IOException;
        import java.net.HttpURLConnection;
        import java.net.MalformedURLException;
        import java.net.URL;
        import java.text.SimpleDateFormat;
        import java.util.Date;
        import java.util.Locale;
/**
 * Created by vineet.srivastava on 5/25/17.
 */

public class ImpressionBeaconAsyncTask extends AsyncTask {
    private static final String TAG = "ImpressionBeaconTask";
    private static final long retryDelay = 20;
    /**
     * Override this method to perform a computation on a background thread. The
     * specified parameters are the parameters passed to {@link #execute}
     * by the caller of this task.
     * <p>
     * This method can call {@link #publishProgress} to publish updates
     * on the UI thread.
     *
     * @param params The parameters of the task which impressionTracker URLs.
     * @return A result, defined by the subclass of this task.
     * @see #onPreExecute()
     * @see #onPostExecute
     * @see #publishProgress
     */
    @Override
    protected Object doInBackground(Object[] params) {
        String[] impressionTrackers = (String[]) params;
        for (String impressionTracker : impressionTrackers
                ) {
            try {
                String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SS", Locale.US).format(new Date());
                impressionTracker = impressionTracker.replace("\\/", "/").replace("$TS", timeStamp);
                URL impressionTrackerURL = new URL(impressionTracker);
                HttpURLConnection conn = (HttpURLConnection) impressionTrackerURL.openConnection();
                int responseCode = conn.getResponseCode();
                for (int i = 1; i <= 3 && responseCode != 200; ++i) {
                    Log.d(ImpressionBeaconAsyncTask.TAG, "Impression Beacon failed");
                    //Retry after some delay
                    Thread.sleep( retryDelay );
                    conn = (HttpURLConnection) impressionTrackerURL.openConnection();
                    responseCode = conn.getResponseCode();
                }
            }
            catch (MalformedURLException e) {
                e.printStackTrace();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
}

