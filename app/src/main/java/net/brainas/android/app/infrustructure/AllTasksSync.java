package net.brainas.android.app.infrustructure;

import android.os.AsyncTask;
import android.util.Log;

import net.brainas.android.app.domain.models.Task;

import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by Kit Ushakov on 5/9/2016.
 */
public class AllTasksSync extends AsyncTask<File, Void, String> {
    static String TAG = "AllTasksSync";
    private AllTasksSyncListener mListener = null;
    private Exception mError = null;

    @Override
    protected String doInBackground(File... files) {
        String response = null;
        List<Task> updatedTasksFromServer;
        ArrayList<Integer> deletedTasksFromServer;
        File allChangesInXMLFile;
        allChangesInXMLFile = files[0];
        // send changes to server for processing
        response = SyncHelper.sendAllChanges(allChangesInXMLFile);

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

    public AllTasksSync setListener(AllTasksSyncListener listener) {
        this.mListener = listener;
        return this;
    }

    public interface AllTasksSyncListener {
        public void onComplete(String jsonString, Exception e);
    }
}
