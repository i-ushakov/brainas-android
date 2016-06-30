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

import net.brainas.android.app.infrustructure.GoogleDriveApi.GoogleDriveGetParams;
import net.brainas.android.app.infrustructure.GoogleDriveApi.GoogleDriveManager;

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
    JSONObject newParams = new JSONObject();
    DriveId settingsJsonDriveId;

    public GoogleDriveSetParams(GoogleApiClient mGoogleApiClient) {
        this.mGoogleApiClient = mGoogleApiClient;
        Log.i(GOOGLE_DRIVE_TAG, this.getClass() + " class is created");
    }

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
    public void execute() {
        Log.i(GOOGLE_DRIVE_TAG, "Let's execute " + this.getClass());
        GoogleDriveGetParams googleDriveGetParams = new GoogleDriveGetParams(mGoogleApiClient);
        googleDriveGetParams.setOnDownloadedCallback(this);
        googleDriveGetParams.execute();
    }

    private void saveSettingsParams(JSONObject retrievedParams, DriveId settingsJsonDriveId) {
        JSONObject newParams;
        if (retrievedParams != null) {
            newParams = retrievedParams;
        } else {
            newParams = new JSONObject();
        }
        this.settingsJsonDriveId = settingsJsonDriveId;
        Iterator it = paramsHash.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println(pair.getKey() + " = " + pair.getValue());
            try {
                newParams.put(pair.getKey().toString(), pair.getValue().toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            newParams = retrievedParams;

            // create new contents resource
            Drive.DriveApi.newDriveContents(mGoogleApiClient)
                    .setResultCallback(driveContentsCallback);
        }
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
                        settingsJsonDriveId.asDriveFile().open(mGoogleApiClient, DriveFile.MODE_READ_WRITE, null)
                                .setResultCallback(settingsFileOpenedCallback);
                    } else {
                        OutputStream outputStream = result.getDriveContents().getOutputStream();
                        PrintWriter printWriter = new PrintWriter(outputStream);
                        printWriter.print(newParams.toString());
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
                    DriveContents contents = result.getDriveContents();
                    OutputStream outputStream = contents.getOutputStream();
                    PrintWriter printWriter = new PrintWriter(outputStream);
                    printWriter.print(newParams.toString());
                    printWriter.close();
                }
            };
}
