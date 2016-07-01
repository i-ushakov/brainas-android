package net.brainas.android.app.infrustructure.GoogleDriveApi;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import net.brainas.android.app.BrainasApp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by innok on 6/29/2016.
 */
public class GoogleDriveGetParams implements GoogleDriveManager.CurrentTask {
    static public String GOOGLE_DRIVE_TAG = "GOOGLE_DRIVE";
    protected GoogleApiClient mGoogleApiClient;

    private GettingParamsHandler callback;
    private DriveId settingsJsonDriveId;

    public interface GettingParamsHandler {
        public void onJSONSettingIsAbsent();
        public void onGettingParamsSuccess(JSONObject currentParams, DriveId settingsJsonDriveId);
    }

    public GoogleDriveGetParams() {}

    public void setOnDownloadedCallback(GettingParamsHandler callback) {
        this.callback = callback;
    }

    @Override
    public void execute(GoogleApiClient googleApiClient) {
        this.mGoogleApiClient = googleApiClient;
        Log.i(GOOGLE_DRIVE_TAG, "Let's execute " + this.getClass());
        Query query = new Query.Builder()
                .addFilter(Filters.eq(SearchableField.MIME_TYPE, "text/json"))
                .addFilter(Filters.eq(SearchableField.TITLE, GoogleDriveManager.SETTINGS_JSON_FILE_NAME))
                .build();
        Drive.DriveApi.getAppFolder(mGoogleApiClient).queryChildren(mGoogleApiClient, query)
                .setResultCallback(onSettingsJsonFileLoaded);
    }

    final private ResultCallback<DriveApi.MetadataBufferResult> onSettingsJsonFileLoaded = new
            ResultCallback<DriveApi.MetadataBufferResult>() {
                @Override
                public void onResult(DriveApi.MetadataBufferResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.i(GOOGLE_DRIVE_TAG, "Problem with getting ba_settings.json");
                        return;
                    }
                    if (result.getMetadataBuffer().getCount() > 0) {
                        settingsJsonDriveId = result.getMetadataBuffer().get(0).getDriveId();
                        new RetrieveSettingJsonContentsAsyncTask().execute(settingsJsonDriveId);
                        Log.i(GOOGLE_DRIVE_TAG, "Successfully got ba_settings.json");
                    } else {
                        callback.onJSONSettingIsAbsent();
                        Log.i(GOOGLE_DRIVE_TAG, "We havn't ba_settings.json in appFolder");
                    }
                }
            };


    final private class RetrieveSettingJsonContentsAsyncTask
            extends GoogleApiClientAsyncTask<DriveId, Boolean, String> {

        public RetrieveSettingJsonContentsAsyncTask() {
            super(mGoogleApiClient);
        }

        @Override
        protected String doInBackgroundConnected(DriveId... params) {
            String contents = null;
            DriveFile file = params[0].asDriveFile();
            DriveApi.DriveContentsResult driveContentsResult =
                    file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await();
            if (!driveContentsResult.getStatus().isSuccess()) {
                return null;
            }
            DriveContents driveContents = driveContentsResult.getDriveContents();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(driveContents.getInputStream()));
            StringBuilder builder = new StringBuilder();
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                contents = builder.toString();
            } catch (IOException e) {
                Log.e(GOOGLE_DRIVE_TAG, "IOException while reading from the stream", e);
            }

            driveContents.discard(mGoogleApiClient);
            return contents;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result == null) {
                Log.i(GOOGLE_DRIVE_TAG, "Error while reading from the file");
                return;
            }
            Log.i(GOOGLE_DRIVE_TAG, "ba_setting.json contents: " + result);
            JSONObject currentParams = null;
            try {
                currentParams = new JSONObject(result);
                ((BrainasApp)BrainasApp.getAppContext()).saveParamsInPrefs(currentParams);
                callback.onGettingParamsSuccess(currentParams, settingsJsonDriveId);
            } catch (JSONException e) {
                Log.i(GOOGLE_DRIVE_TAG, "Cannot parse ba_setting.json");
                e.printStackTrace();
            }
        }
    }
}
