package net.brainas.android.app.infrustructure;

import android.os.AsyncTask;
import android.util.Log;



import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.infrustructure.googleDriveApi.GoogleDriveManager;
import net.brainas.android.app.infrustructure.synchronization.SynchronizationManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.HttpsURLConnection;

/**
 * Created by innok on 7/9/2016.
 */
public class SyncSettingsWithServerTask extends AsyncTask<String, String, String> {
    static String lineEnd = "\r\n";
    static String boundary =  "*****";

    public interface Callback {
        void onSyncSuccess();
        void onSyncFailed();
    }

    private static String SYNCHRONIZATION_TAG = "SYNCHRONIZATION";

    private Callback callback;
    private String accessToken;


    public SyncSettingsWithServerTask(String accessToken, Callback callback) {
        this.accessToken = accessToken;
        this.callback = callback;
    }

    @Override
    protected String doInBackground(String... params) {
        String response;
        JSONObject settings = buildSettingsJson();
        HttpsURLConnection connection = null;
        try {
            connection = InfrustructureHelper.createHttpMultipartConn(SynchronizationManager.serverUrl + "sync-settings");

            DataOutputStream request = new DataOutputStream(
                    connection.getOutputStream());

            request.writeBytes("--" + boundary + lineEnd);

            request.writeBytes("Content-Disposition: form-data; name=\"accessToken\"" + lineEnd);
            request.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
            request.writeBytes("Content-Length: " + accessToken + lineEnd);
            request.writeBytes(lineEnd);
            Log.i(SYNCHRONIZATION_TAG, "Sending accessToken to server " + accessToken);
            request.writeBytes(accessToken);
            request.writeBytes(lineEnd);
            request.writeBytes("--" + boundary + lineEnd);

            if (settings != null) {
                request.writeBytes("Content-Disposition: form-data; name=\"settings\"" + lineEnd);
                request.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
                request.writeBytes("Content-Length: " + settings.toString().length() + lineEnd);
                request.writeBytes(lineEnd);
                Log.i(SYNCHRONIZATION_TAG, "Sending settings to server " + settings.toString());
                request.writeBytes(settings.toString());
                request.writeBytes(lineEnd);
                request.writeBytes("--" + boundary + lineEnd);
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (KeyStoreException e1) {
            e1.printStackTrace();
        } catch (CertificateException e1) {
            e1.printStackTrace();
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (KeyManagementException e1) {
            e1.printStackTrace();
        }


        // parse server response
        try {
            if (((HttpURLConnection)connection).getResponseCode() == 200) {
                InputStream stream = ((HttpURLConnection) connection).getInputStream();
                InputStreamReader isReader = new InputStreamReader(stream);
                BufferedReader br = new BufferedReader(isReader);
                String line;
                response = "";
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                    response += line;
                }
            } else {
                Log.e(SYNCHRONIZATION_TAG, "Cannot sync settings with server! (!= 200)");
                // TODO No Token situation
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(SYNCHRONIZATION_TAG, "The Code was sent, but Token haven't gotten! (IOException)");
            // TODO No Token situation
            return null;
        }

        if (response.equals("null")) {
            return null;
        }
        return response;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);
        if (result == null) {
            Log.i(SYNCHRONIZATION_TAG, "Error while sync params with server");
            return;
        }
        Log.i(SYNCHRONIZATION_TAG, "server params: " + result);
        JSONObject settings = null;
        try {
            settings = new JSONObject(result);
            ((BrainasApp)BrainasApp.getAppContext()).saveParamsInUserPrefs(settings);
            if (callback != null) {
                callback.onSyncSuccess();
            }
        } catch (JSONException e) {
            Log.i(SYNCHRONIZATION_TAG, "Cannot parse ba_setting.json");
            e.printStackTrace();
        }
    }

    private JSONObject buildSettingsJson() {
        return  GoogleDriveManager.getInstance(BrainasApp.getAppContext()).getFoldersIds();
    }
}
