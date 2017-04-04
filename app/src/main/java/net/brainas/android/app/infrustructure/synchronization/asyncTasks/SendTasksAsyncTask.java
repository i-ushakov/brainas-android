package net.brainas.android.app.infrustructure.synchronization.asyncTasks;

import android.os.AsyncTask;
import android.util.Log;

import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.NetworkHelper;
import net.brainas.android.app.infrustructure.SyncHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kit Ushakov on 5/9/2016.
 */
public class SendTasksAsyncTask extends AsyncTask<File, Void, String> {
    static String TAG = "SendTasksAsyncTask";
    private AllTasksSyncListener mListener = null;
    private Exception mError = null;

    @Override
    protected String doInBackground(File... files) {
        String response = null;
        File allChangesInXMLFile;
        allChangesInXMLFile = files[0];
        // send changes to server for processing
        if (NetworkHelper.isNetworkActive()) {
            response = SyncHelper.sendTasksRequest(allChangesInXMLFile);
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

    public SendTasksAsyncTask setListener(AllTasksSyncListener listener) {
        this.mListener = listener;
        return this;
    }

    public interface AllTasksSyncListener {
        public void onComplete(String jsonString, Exception e);
    }
}
