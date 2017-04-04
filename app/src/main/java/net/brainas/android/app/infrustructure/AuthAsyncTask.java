package net.brainas.android.app.infrustructure;

import android.os.AsyncTask;
import android.util.Log;

import net.brainas.android.app.domain.models.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kit Ushakov on 5/9/2016.
 */
public class AuthAsyncTask extends AsyncTask<String, Void, String> {
    static String TAG = "AuthAsyncTask";
    private AuthAsyncTaskListener mListener = null;
    private Exception mError = null;

    @Override
    protected String doInBackground(String... accessCode) {
        String response = null;
        // send changes to server for processing
        if (NetworkHelper.isNetworkActive()) {
            response = SyncHelper.sendAuthRequest(accessCode[0]);
        } else {
            Log.i(TAG, "Network is not available");
        }

        return response;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (this.mListener != null)
            this.mListener.onComplete(s, mError);
    }

    @Override
    protected void onCancelled() {
        if (this.mListener != null) {
            mError = new InterruptedException("AsyncTask cancelled");
            this.mListener.onComplete(null, mError);
        }
    }

    public AuthAsyncTask setListener(AuthAsyncTaskListener listener) {
        this.mListener = listener;
        return this;
    }

    public interface AuthAsyncTaskListener {
        public void onComplete(String jsonString, Exception e);
    }
}
