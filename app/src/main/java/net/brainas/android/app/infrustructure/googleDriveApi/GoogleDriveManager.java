package net.brainas.android.app.infrustructure.googleDriveApi;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.domain.models.Image;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by innok on 6/22/2016.
 */
public class GoogleDriveManager implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    static public String GOOGLE_DRIVE_TAG = "GOOGLE_DRIVE";
    static public String SETTINGS_JSON_FILE_NAME = "ba_settings.json";
    static public String PROJECT_FOLDER_NAME = "Brain Assistant Project";
    static public String PICTURES_FOLDER_NAME = "Pictures";

    public enum SettingsParamNames {
        PROJECT_FOLDER_DRIVE_ID,
        PROJECT_FOLDER_RESOURCE_ID,
        PICTURE_FOLDER_DRIVE_ID,
        PICTURE_FOLDER_RESOURCE_ID
    }

    private static GoogleDriveManager instance = null;

    public interface CurrentTask {
        public void execute(GoogleApiClient mGoogleApiClient);
    }

    private ArrayList<CurrentTask> currentTaskQueue = new ArrayList<>();

    Context context;
    protected GoogleApiClient mGoogleApiClient;

    public static GoogleDriveManager getInstance(Context context) {
        if(instance == null) {
            instance = new GoogleDriveManager(context);
        }
        return instance;
    }

    public void disconnect() {
         if (mGoogleApiClient != null) {
             mGoogleApiClient.disconnect();
             Log.i(GOOGLE_DRIVE_TAG, "Disconnect mGoogleApiClient ... ");
         }
        mGoogleApiClient = null;
    }

    private GoogleDriveManager(Context context) {
        this.context = context;
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
    }

    private void initGoogleApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context)
                    .addApi(Drive.API)
                    .addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER) // required for App Folder sample
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .build();
        }
        mGoogleApiClient.connect();
        Log.i(GOOGLE_DRIVE_TAG, "Try to connect mGoogleApiClient ...");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        final Iterator<CurrentTask> it = currentTaskQueue.iterator();
        while (it.hasNext()) {
            final CurrentTask currentTask = it.next();
            if (currentTask != null) {
                executeTaskInSeparateThread(currentTask, "Execute task from currentTaskQueue: " + currentTask.getClass().getName());
                it.remove();
            }
        }
        Log.i(GOOGLE_DRIVE_TAG, "GoogleApiClient connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(GOOGLE_DRIVE_TAG, "GoogleApiClient connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(GOOGLE_DRIVE_TAG,  "GoogleApiClient connection failed: " + connectionResult.toString());
        if (!connectionResult.hasResolution()) {
            // show the localized error dialog.
            //GoogleApiAvailability.getInstance().getErrorDialog(context, connectionResult.getErrorCode(), 0).show();
            return;
        }
        /*try {
            connectionResult.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (IntentSender.SendIntentException e) {
            Log.e(GOOGLE_DRIVE_TAG, "Exception while starting resolution activity", e);
        }*/
    }

    public void manageAppFolders() {
        GoogleDriveManageAppFolders googleDriveManageAppFolders = new GoogleDriveManageAppFolders();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            googleDriveManageAppFolders.execute(mGoogleApiClient);
        } else {
            currentTaskQueue.add(googleDriveManageAppFolders);
            Log.i(GOOGLE_DRIVE_TAG, "Added task to currentTaskQueue: " + googleDriveManageAppFolders.getClass().getName());
            initGoogleApiClient();
        }
    }

    public void uploadPicture(Image image) {
        GoogleDriveUploadTaskPicture googleDriveUploadTaskPicture = new GoogleDriveUploadTaskPicture();
        googleDriveUploadTaskPicture.setImage(image);
        String[] reqestedParams = {GoogleDriveManager.SettingsParamNames.PICTURE_FOLDER_DRIVE_ID.name()};
        try {
            JSONObject retrievedParams = ((BrainasApp)BrainasApp.getAppContext()).getParamsFromUserPrefs(reqestedParams);
            DriveId picturesFolderDriveId = DriveId.decodeFromString(retrievedParams.getString(GoogleDriveManager.SettingsParamNames.PICTURE_FOLDER_DRIVE_ID.name()));
            googleDriveUploadTaskPicture.setPicturesFolderDriveId(picturesFolderDriveId);
        } catch (JSONException e) {
            Log.i(GOOGLE_DRIVE_TAG, "Cannot upload task cause we havn't driveId of picture folder");
            e.printStackTrace();
            return;
        }

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            googleDriveUploadTaskPicture.execute(mGoogleApiClient);
        } else {
            currentTaskQueue.add(googleDriveUploadTaskPicture);
            Log.i(GOOGLE_DRIVE_TAG, "Added task to currentTaskQueue: " + googleDriveUploadTaskPicture.getClass().getName());
            initGoogleApiClient();
        }
    }

    public void downloadPicture(Image image, int accuntId) {
        final GoogleDriveDownloadImage googleDriveDownloadImage = new GoogleDriveDownloadImage();
        googleDriveDownloadImage.setImage(image);
        googleDriveDownloadImage.setAccountId(accuntId);

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            executeTaskInSeparateThread(googleDriveDownloadImage, null);
        } else {
            currentTaskQueue.add(googleDriveDownloadImage);
            Log.i(GOOGLE_DRIVE_TAG, "Added task to currentTaskQueue: " + googleDriveDownloadImage.getClass().getName());
            initGoogleApiClient();
        }
    }

    public DriveId checkFolderExistByResourceID (String folderResourceId) {
        if (folderResourceId == null) {
            return null;
        }

        DriveId folderDriveId = fetchDriveIdByResourceId(folderResourceId, null);
        if (folderDriveId == null) {
            return null;
        }
        return checkFolderExistsByDriverId(folderDriveId);
    }

    public DriveId checkFolderExistsByDriverId(DriveId folderDriveId) {// TODO# Refactor to checkByDriveId - precisely
        Metadata metadata = folderDriveId.asDriveFolder().getMetadata(mGoogleApiClient).await().getMetadata();

        if (metadata != null && metadata.getDriveId().getResourceId() != null) {
            if (metadata.isTrashed()) {
                folderDriveId.asDriveFolder().untrash(mGoogleApiClient).await();
            }
            //metadata.getTitle();
        } else {
            return null;
        }
        return metadata.getDriveId();
    }

    public JSONObject getFoldersIds() {
        JSONObject foldersIds = new JSONObject();

        String [] params = {
                GoogleDriveManager.SettingsParamNames.PROJECT_FOLDER_DRIVE_ID.name(),
                GoogleDriveManager.SettingsParamNames.PROJECT_FOLDER_RESOURCE_ID.name(),
                GoogleDriveManager.SettingsParamNames.PICTURE_FOLDER_DRIVE_ID.name(),
                GoogleDriveManager.SettingsParamNames.PICTURE_FOLDER_RESOURCE_ID.name()
        };

        try {
            foldersIds = ((BrainasApp)BrainasApp.getAppContext()).getParamsFromUserPrefs(params);
            ckeckFolderIdsIntegraty(foldersIds,
                    GoogleDriveManager.SettingsParamNames.PROJECT_FOLDER_DRIVE_ID.name(),
                    GoogleDriveManager.SettingsParamNames.PROJECT_FOLDER_RESOURCE_ID.name());
            ckeckFolderIdsIntegraty(foldersIds,
                    GoogleDriveManager.SettingsParamNames.PICTURE_FOLDER_DRIVE_ID.name(),
                    GoogleDriveManager.SettingsParamNames.PICTURE_FOLDER_RESOURCE_ID.name());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return foldersIds;
    }

    private void ckeckFolderIdsIntegraty(JSONObject foldersIds, String driveIdParamName, String resourceIdParamName) {
        try {
            if (foldersIds.has(driveIdParamName) && !foldersIds.has(resourceIdParamName)) {
                String resourceId = getResourceIdByDriverId(DriveId.decodeFromString(foldersIds.getString(driveIdParamName)));
                foldersIds.put(resourceIdParamName, resourceId);
                ((BrainasApp)BrainasApp.getAppContext()).saveParamsInUserPrefs(resourceIdParamName, resourceId);
                Log.i(GOOGLE_DRIVE_TAG, "Successfully got resource_id:" + resourceId  + "by drive_id for " + resourceIdParamName);

            } else if (!foldersIds.has(driveIdParamName) && foldersIds.has(resourceIdParamName)) {
                DriveId driveId = fetchDriveIdByResourceId(foldersIds.getString(resourceIdParamName), null);
                if (driveId != null) { // sometimes can be null... why? (i think it's a Google fault:)
                    foldersIds.put(driveIdParamName, driveId.toString());
                    ((BrainasApp)BrainasApp.getAppContext()).saveParamsInUserPrefs(driveIdParamName, driveId.toString());
                    Log.i(GOOGLE_DRIVE_TAG, "Successfully got drive_id:" + driveId.toString()  + "by resource_id for " + resourceIdParamName);
                } else {
                    Log.i(GOOGLE_DRIVE_TAG, "Failed to get drive_id by resource_id for " + resourceIdParamName);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getResourceIdByDriverId(DriveId driveId) {
        DriveId folderDriveId = checkFolderExistsByDriverId(driveId);
        if (folderDriveId != null) {
            String resourceId = folderDriveId.getResourceId();
            return resourceId;
        } else {
            return null;
        }
    }

    public DriveId fetchDriveIdByResourceId(String resurceId, ResultCallback<DriveApi.DriveIdResult> idCallback) {
        DriveId driveId = null;
        if (idCallback == null) {
            driveId =
                    Drive.DriveApi.fetchDriveId(mGoogleApiClient, resurceId).await().getDriveId();
        } else {
            Drive.DriveApi.fetchDriveId(mGoogleApiClient, resurceId)
                    .setResultCallback(idCallback);
        }
        return driveId;
    }

    private void executeTaskInSeparateThread(final CurrentTask currentTask, final String logMessage) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    currentTask.execute(mGoogleApiClient);
                    if (logMessage != null) {
                        Log.i(GOOGLE_DRIVE_TAG, logMessage);
                    }
                }
            };
            thread.start();
        } else {
            currentTask.execute(mGoogleApiClient);
            if (logMessage != null) {
                Log.i(GOOGLE_DRIVE_TAG, logMessage);
            }
        }
    }

}
