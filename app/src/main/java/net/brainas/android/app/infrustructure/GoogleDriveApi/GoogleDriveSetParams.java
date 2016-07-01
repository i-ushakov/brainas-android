package net.brainas.android.app.infrustructure.GoogleDriveApi;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

import net.brainas.android.app.BrainasApp;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by innok on 6/29/2016.
 */
public class GoogleDriveSetParams implements GoogleDriveGetParams.GettingParamsHandler, GoogleDriveManager.CurrentTask {
    static public String GOOGLE_DRIVE_TAG = "GOOGLE_DRIVE";
    protected GoogleApiClient mGoogleApiClient;

    HashMap<String, String> paramsHash = new HashMap<>();
    JSONObject paramsForSave;
    DriveId settingsJsonDriveId;

    public GoogleDriveSetParams () {}

    public void setParamsHash(HashMap<String, String>  paramsHash) {
        this.paramsHash = paramsHash;
    }

    @Override
    public void onJSONSettingIsAbsent() {
        saveSettingsParams(null, null);
    }

    @Override
    public void onGettingParamsSuccess(JSONObject params, DriveId settingsJsonDriveId) {
        saveSettingsParams(params, settingsJsonDriveId);
    }

    @Override
    public void execute(GoogleApiClient googleApiClient) {
        this.mGoogleApiClient = googleApiClient;
        Log.i(GOOGLE_DRIVE_TAG, "Let's execute " + this.getClass());
        GoogleDriveGetParams googleDriveGetParams = new GoogleDriveGetParams();
        googleDriveGetParams.setOnDownloadedCallback(this);
        googleDriveGetParams.execute(mGoogleApiClient);
    }

    private void saveSettingsParams(JSONObject retrievedParams, DriveId settingsJsonDriveId) {
        if (retrievedParams != null) {
            paramsForSave = retrievedParams;
        } else {
            paramsForSave = new JSONObject();
        }
        this.settingsJsonDriveId = settingsJsonDriveId;
        Iterator it = paramsHash.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            try {
                paramsForSave.put(pair.getKey().toString(), pair.getValue().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            ((BrainasApp)BrainasApp.getAppContext()).saveParamsInPrefs(paramsForSave);
        } catch (JSONException e) {
            Log.i(GOOGLE_DRIVE_TAG, "Cannot save settings params: " + paramsForSave + " in pref");
            e.printStackTrace();
        }


        // create new contents resource
        Drive.DriveApi.newDriveContents(mGoogleApiClient)
                .setResultCallback(driveContentsCallback);
    }

    final private ResultCallback<DriveApi.DriveContentsResult> driveContentsCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.i(GOOGLE_DRIVE_TAG, "Error while trying to create new ba_settings.json contents");
                        return;
                    }

                    if (settingsJsonDriveId != null) {
                        settingsJsonDriveId.asDriveFile().open(mGoogleApiClient, DriveFile.MODE_WRITE_ONLY, null)
                                .setResultCallback(settingsFileOpenedCallback);
                    } else {
                        OutputStream outputStream = result.getDriveContents().getOutputStream();
                        PrintWriter printWriter = new PrintWriter(outputStream);
                        printWriter.print(paramsForSave.toString());
                        printWriter.close();

                        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                .setTitle(GoogleDriveManager.SETTINGS_JSON_FILE_NAME)
                                .setMimeType("text/json")
                                .build();

                        Drive.DriveApi.getAppFolder(mGoogleApiClient)
                                .createFile(mGoogleApiClient, changeSet, result.getDriveContents())
                                .setResultCallback(fileCallback);
                    }
                }
            };


    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.i(GOOGLE_DRIVE_TAG, "Error while trying to create the ba_settings.json");
                        return;
                    }
                    Log.i(GOOGLE_DRIVE_TAG, "Created a ba_settings.json in App Folder: "
                            + result.getDriveFile().getDriveId());
                }
            };

    ResultCallback<DriveApi.DriveContentsResult> settingsFileOpenedCallback =
            new ResultCallback<DriveApi.DriveContentsResult>() {
                @Override
                public void onResult(DriveApi.DriveContentsResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.i(GOOGLE_DRIVE_TAG, "Cannot open ba_settings.json");
                        return;
                    }
                    Log.i(GOOGLE_DRIVE_TAG, "The ba_settings.json successful opened");
                    DriveContents driveContents = result.getDriveContents();
                    new EditContentsAsyncTask().execute(driveContents);
                }
            };

    private class EditContentsAsyncTask extends GoogleApiClientAsyncTask<DriveContents, Void, Boolean> {

        public EditContentsAsyncTask() {
            super(mGoogleApiClient);
        }

        @Override
        protected Boolean doInBackgroundConnected(DriveContents... args) {
            DriveContents driveContents = args[0];
            OutputStream outputStream = driveContents.getOutputStream();
            PrintWriter printWriter = new PrintWriter(outputStream);
            printWriter.print(paramsForSave.toString());
            printWriter.close();
            com.google.android.gms.common.api.Status status =
                    driveContents.commit(mGoogleApiClient, null).await();
            return status.getStatus().isSuccess();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                Log.i(GOOGLE_DRIVE_TAG, "Error while editing ba_settings.json");
                return;
            }

            Log.i(GOOGLE_DRIVE_TAG, "Successfully edited ba_settings.json. It's content now: " + paramsForSave);
        }
    }

}
