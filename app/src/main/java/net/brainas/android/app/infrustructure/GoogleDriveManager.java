package net.brainas.android.app.infrustructure;

import android.content.Context;
import android.content.IntentSender;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.MetadataChangeSet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by innok on 6/22/2016.
 */
public class GoogleDriveManager implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    static public String GOOGLE_DRIVE_TAG = "GOOGLE_DRIVE";

    private static GoogleDriveManager instance = null;

    protected static final int REQUEST_CODE_RESOLUTION = 1;

    public interface CurrentTask {
        public void execute();
    }

    private GoogleDriveManager.CurrentTask currentTask;

    Context context;
    private GoogleApiClient mGoogleApiClient;

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
                    .addScope(Drive.SCOPE_APPFOLDER) // required for App Folder sample
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
        if (currentTask != null) {
            currentTask.execute();
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
            Drive.DriveApi.newDriveContents(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {

                        @Override
                        public void onResult(DriveApi.DriveContentsResult result) {
                            if (!result.getStatus().isSuccess()) {
                                Log.i(GOOGLE_DRIVE_TAG, "Failed to create image");
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
                            MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                    .setMimeType("image/png").setTitle(imageName).build();
                            // Create an intent for the file chooser, and start it.

                            Drive.DriveApi.getAppFolder(mGoogleApiClient)
                                    .createFile(mGoogleApiClient, metadataChangeSet, result.getDriveContents())
                                    .setResultCallback(fileCallback);

                            Log.i(GOOGLE_DRIVE_TAG, "Image was uploaded");
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
            currentTask = uploadImageTask;
            initGoogleApiClient();
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
