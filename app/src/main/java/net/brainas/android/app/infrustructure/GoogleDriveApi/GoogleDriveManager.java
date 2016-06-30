package net.brainas.android.app.infrustructure.GoogleDriveApi;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveId;

import net.brainas.android.app.BrainasApp;

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

    public enum SettingsParamNames{
        PROJECT_FOLDER_DRIVE_ID,
        PICTURE_FOLDER_DRIVE_ID
    }

    private static GoogleDriveManager instance = null;

    public interface CurrentTask {
        public void execute();
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
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Iterator<CurrentTask> it = currentTaskQueue.iterator();
        while (it.hasNext()) {
            CurrentTask currentTask = it.next();
            if (currentTask != null) {
                currentTask.execute();
                Log.i(GOOGLE_DRIVE_TAG, "Execute task from currentTaskQueue: " + currentTask.getClass().getName());
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
        GoogleDriveManageAppFolders googleDriveManageAppFolders = new GoogleDriveManageAppFolders(mGoogleApiClient);
        if (mGoogleApiClient.isConnected()) {
            googleDriveManageAppFolders.execute();
        } else {
            currentTaskQueue.add(googleDriveManageAppFolders);
            Log.i(GOOGLE_DRIVE_TAG, "Added task to currentTaskQueue: " + googleDriveManageAppFolders.getClass().getName());
            initGoogleApiClient();
        }
    }

    public void uploadPicture(Bitmap bitmap, String imageName) {
        GoogleDriveUploadTaskPicture googleDriveUploadTaskPicture = new GoogleDriveUploadTaskPicture(mGoogleApiClient);
        googleDriveUploadTaskPicture.setBitmap(bitmap);
        googleDriveUploadTaskPicture.setImageName(imageName);
        String[] reqestedParams = {GoogleDriveManager.SettingsParamNames.PICTURE_FOLDER_DRIVE_ID.name()};
        try {
            JSONObject retrievedParams = ((BrainasApp)BrainasApp.getAppContext()).getParamsFromPref(reqestedParams);
            DriveId picturesFolderDriveId = DriveId.decodeFromString(retrievedParams.getString(GoogleDriveManager.SettingsParamNames.PICTURE_FOLDER_DRIVE_ID.name()));
            googleDriveUploadTaskPicture.setPicturesFolderDriveId(picturesFolderDriveId);
        } catch (JSONException e) {
            Log.i(GOOGLE_DRIVE_TAG, "Cannot upload task cause we havn't driveId of picture folder");
            e.printStackTrace();
            return;
        }

        if (mGoogleApiClient.isConnected()) {
            googleDriveUploadTaskPicture.execute();
        } else {
            currentTaskQueue.add(googleDriveUploadTaskPicture);
            Log.i(GOOGLE_DRIVE_TAG, "Added task to currentTaskQueue: " + googleDriveUploadTaskPicture.getClass().getName());
            initGoogleApiClient();
        }
    }
}
