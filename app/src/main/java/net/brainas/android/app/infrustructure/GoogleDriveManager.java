package net.brainas.android.app.infrustructure;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by innok on 6/22/2016.
 */
public class GoogleDriveManager implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    static public String GOOGLE_DRIVE_TAG = "GOOGLE_DRIVE";
    static public String PROJECT_FOLDER_NAME = "Brain Assistant Project";
    static public String PICTURES_FOLDER_NAME = "Pictures";

    static public String EXISTING_FOLDER_ID = "0B-nWSp4lPq2nc0I0ODRXcDN0M0k";

    private static GoogleDriveManager instance = null;
    protected DriveId mFolderDriveId;

    protected static final int REQUEST_CODE_RESOLUTION = 1;

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

    private class UploadImageTask implements CurrentTask {
        private  Bitmap bitmap;
        private String imageName;

        public void setBitmap(Bitmap bitmap) {
            this.bitmap = bitmap;
        }
        public void setImageName(String imageName) {
            this.imageName = imageName;
        }

        public void execute() {
            Log.i(GOOGLE_DRIVE_TAG, "Try to upload image");
            final Bitmap image = bitmap;
            if (true) {
                ///GoogleDriveCreateFolder.getInstance(context).rebuildFolderStructure();
            }

            Drive.DriveApi.fetchDriveId(mGoogleApiClient, EXISTING_FOLDER_ID)
                    .setResultCallback(new ResultCallback<DriveApi.DriveIdResult>() {
                        @Override
                        public void onResult(DriveApi.DriveIdResult result) {
                            if (!result.getStatus().isSuccess()) {
                                Log.i(GOOGLE_DRIVE_TAG, "Cannot find DriveId. Are you authorized to view this file?");
                                return;
                            }
                            mFolderDriveId = result.getDriveId();
                            Drive.DriveApi.newDriveContents(mGoogleApiClient)
                                    .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                                        @Override
                                        public void onResult(DriveApi.DriveContentsResult result) {
                                            if (!result.getStatus().isSuccess()) {
                                                Log.e(GOOGLE_DRIVE_TAG, "Error while trying to create new file contents");
                                                return;
                                            }
                                            OutputStream outputStream = result.getDriveContents().getOutputStream();
                                            ByteArrayOutputStream bitmapStream = new ByteArrayOutputStream();
                                            image.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream);
                                            try {
                                                outputStream.write(bitmapStream.toByteArray());
                                            } catch (IOException e1) {
                                                Log.i(GOOGLE_DRIVE_TAG, "Unable to upload image file");
                                            }
                                            DriveFolder folder = mFolderDriveId.asDriveFolder();
                                            MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                                    .setMimeType("image/png").setTitle(imageName).build();
                                            // Create an intent for the file chooser, and start it.

                                            folder.createFile(mGoogleApiClient, metadataChangeSet, result.getDriveContents())
                                                    .setResultCallback(fileCallback);

                                            Log.i(GOOGLE_DRIVE_TAG, "Image was uploaded");
                                        }
                                    });
                        }
                    });
        }
    }

    public void uploadImage(Bitmap bitmap, final String imageName) {
        UploadImageTask uploadImageTask = new UploadImageTask();
        uploadImageTask.setBitmap(bitmap);
        uploadImageTask.setImageName(imageName);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            uploadImageTask.execute();
        } else {
            currentTaskQueue.add(uploadImageTask);
            Log.i(GOOGLE_DRIVE_TAG, "Added task to currentTaskQueue: " + uploadImageTask.getClass().getName());
            initGoogleApiClient();
        }
    }

    public void saveSettingsParams(HashMap<String, String> settingsParams) {
        SaveSettingsParamsTask saveSettingsParamsTask = new SaveSettingsParamsTask();
        saveSettingsParamsTask.setSettingsParams(settingsParams);
        if (mGoogleApiClient.isConnected()) {
            saveSettingsParamsTask.execute();
        } else {
            // = saveSettingsParamsTask;
            initGoogleApiClient();
        }
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

    private class SaveSettingsParamsTask implements CurrentTask {
        HashMap<String, String> settingsParams;
        void setSettingsParams(HashMap<String, String> settingsParams) {
            this.settingsParams = settingsParams;
        }
        public void execute() {

        }
    }

    final private ResultCallback<DriveFolder.DriveFileResult> fileCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.e(GOOGLE_DRIVE_TAG, "Error while trying to create the file");
                        return;
                    }
                    Log.i(GOOGLE_DRIVE_TAG, "Created a file in App Folder: "
                            + result.getDriveFile().getDriveId());
                }
            };
}
