package net.brainas.android.app.infrustructure.googleDriveApi;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.domain.models.Image;
import net.brainas.android.app.infrustructure.images.BasicImageDownloader;
import net.brainas.android.app.infrustructure.InfrustructureHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by innok on 6/30/2016.
 */
public class GoogleDriveDownloadImage implements GoogleDriveManager.CurrentTask {
    static public String GOOGLE_DRIVE_TAG = "GOOGLE_DRIVE";

    protected GoogleApiClient mGoogleApiClient;
    private DriveId driveId;

    private Image image;
    private  Bitmap pictureBitmap;
    private String pictureName, pictureGoogleDriveId;
    private int accountId;


    public GoogleDriveDownloadImage() {}

    public void setImage(Image image) {
        this.image = image;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    @Override
    public void execute(final GoogleApiClient mGoogleApiClient) {
        this.mGoogleApiClient = mGoogleApiClient;

        if (image == null) {
            Log.i(GOOGLE_DRIVE_TAG, "Cannot upload image, not enough data!");
            return;
        }
        driveId = image.getDriveId();
        pictureBitmap = image.getBitmap();
        if (driveId == null) {
            Log.i(GOOGLE_DRIVE_TAG, "Cannot upload image, not enough data!");
            return;
        }
        DriveFile file = driveId.asDriveFile();
        String fileExtension = file.getMetadata(mGoogleApiClient).await().getMetadata().getFileExtension();
        String fileName = file.getMetadata(mGoogleApiClient).await().getMetadata().getTitle();
        Bitmap.CompressFormat bitmapCompressFormat;
        switch (fileExtension) {
            case "png" : bitmapCompressFormat =  Bitmap.CompressFormat.PNG;
                break;
            case "jpg" : bitmapCompressFormat =  Bitmap.CompressFormat.JPEG;
                break;
            default:
                bitmapCompressFormat =  Bitmap.CompressFormat.JPEG;
        }

        DriveApi.DriveContentsResult driveContentsResult =
                file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null).await();

        if (!driveContentsResult.getStatus().isSuccess()) {
            return;
        }

        InputStream is = driveContentsResult.getDriveContents().getInputStream();
        Bitmap bitmap = BitmapFactory.decodeStream(is);
        image.setBitmap(bitmap);

        File imageFile = InfrustructureHelper.creteFileForGivenName(
                InfrustructureHelper.getPathToImageFolder(accountId),
                fileName);
        BasicImageDownloader.writeToDisk(imageFile, bitmap, new BasicImageDownloader.OnBitmapSaveListener() {
            @Override
            public void onBitmapSaved() {
                image.onDownloadCompleted();
            }

            @Override
            public void onBitmapSaveError(BasicImageDownloader.ImageError error) {
                Log.e(GOOGLE_DRIVE_TAG, "Cannot save image on disk");
            }
        }, bitmapCompressFormat, false);

        try {
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    final private ResultCallback<DriveFolder.DriveFileResult> pictureUploadCallback = new
            ResultCallback<DriveFolder.DriveFileResult>() {
                @Override
                public void onResult(DriveFolder.DriveFileResult result) {
                    if (!result.getStatus().isSuccess()) {
                        Log.e(GOOGLE_DRIVE_TAG, "Error while trying to create the picture in picture folder");
                        return;
                    }
                    DriveId driverId = result.getDriveFile().getDriveId();
                    Log.i(GOOGLE_DRIVE_TAG, "Created a picture in Pictures Folder: "
                            + driverId);
                    image.setDriveId(driverId);
                }
            };

    /*
     Drive.DriveApi.fetchDriveId(mGoogleApiClient, "0B-nWSp4lPq2nUWswOGZVSkxvZEE")
                .setResultCallback(idCallback);
     */

    final private ResultCallback<DriveApi.DriveIdResult> idCallback = new ResultCallback<DriveApi.DriveIdResult>() {
        @Override
        public void onResult(DriveApi.DriveIdResult result) {
            if (!result.getStatus().isSuccess()) {
                //showMessage("Cannot find DriveId. Are you authorized to view this file?");
                return;
            }
            DriveId driveId = result.getDriveId();
            DriveFolder folder = driveId.asDriveFolder();
            Query query = new Query.Builder()
                    .addFilter(Filters.eq(SearchableField.MIME_TYPE, "text/plain"))
                    .build();
            folder.queryChildren(mGoogleApiClient, query)
                    .setResultCallback(null);
        }
    };

}
