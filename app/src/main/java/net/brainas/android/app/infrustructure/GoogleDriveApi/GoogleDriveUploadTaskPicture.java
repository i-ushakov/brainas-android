package net.brainas.android.app.infrustructure.GoogleDriveApi;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

import net.brainas.android.app.infrustructure.GoogleDriveApi.GoogleDriveManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by innok on 6/30/2016.
 */
public class GoogleDriveUploadTaskPicture implements GoogleDriveManager.CurrentTask {
    static public String GOOGLE_DRIVE_TAG = "GOOGLE_DRIVE";

    protected GoogleApiClient mGoogleApiClient;
    private DriveId picturesFolderDriveId;


    private  Bitmap bitmap;
    private String imageName;

    public GoogleDriveUploadTaskPicture(GoogleApiClient mGoogleApiClient) {
        this.mGoogleApiClient = mGoogleApiClient;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }
    public void setImageName(String imageName) {
        this.imageName = imageName;
    }
    public void setPicturesFolderDriveId(DriveId picturesFolderDriveId) {
        this.picturesFolderDriveId = picturesFolderDriveId;
    }

    @Override
    public void execute() {
        if (bitmap == null || imageName == null || picturesFolderDriveId == null) {
            Log.i(GOOGLE_DRIVE_TAG, "Cannot upload image, not enough data!");
            return;
        }
        Log.i(GOOGLE_DRIVE_TAG, "Try to upload image with name " + imageName);
        final Bitmap image = bitmap;

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
                        DriveFolder folder = picturesFolderDriveId.asDriveFolder();
                        MetadataChangeSet metadataChangeSet = new MetadataChangeSet.Builder()
                                .setMimeType("image/png").setTitle(imageName).build();
                        // Create an intent for the file chooser, and start it.

                        folder.createFile(mGoogleApiClient, metadataChangeSet, result.getDriveContents())
                                .setResultCallback(pictureUploadCallback);

                        Log.i(GOOGLE_DRIVE_TAG, "Image was uploaded");
                    }
                });

    }


    final private ResultCallback<DriveFolder.DriveFileResult> pictureUploadCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.e(GOOGLE_DRIVE_TAG, "Error while trying to create the picture in picture folder");
                        return;
                    }
                    Log.i(GOOGLE_DRIVE_TAG, "Created a picture in Pictures Folder: "
                            + result.getDriveFile().getDriveId());
                }
            };
}
