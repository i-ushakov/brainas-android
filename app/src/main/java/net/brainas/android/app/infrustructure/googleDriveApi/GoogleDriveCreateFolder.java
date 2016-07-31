package net.brainas.android.app.infrustructure.googleDriveApi;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

/**
 * Created by innok on 6/22/2016.
 */
public class GoogleDriveCreateFolder implements GoogleDriveManager.CurrentTask {

    static public String GOOGLE_DRIVE_TAG = "GOOGLE_DRIVE";

    public interface OnFolderCreatedCallbak {
        void onFolderCreated(DriveId driveId);
        void onFolderWorkDone();
    }

    private GoogleApiClient mGoogleApiClient;
    private String folderName;
    private DriveId parentDriveId;
    private OnFolderCreatedCallbak callback;

    public GoogleDriveCreateFolder(GoogleApiClient mGoogleApiClient) {
        this.mGoogleApiClient = mGoogleApiClient;

    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public void setOnFolderCreatedCallback(OnFolderCreatedCallbak callback) {
        this.callback = callback;
    }

    public void execute(GoogleApiClient googleApiClient) {
        this.mGoogleApiClient = googleApiClient;

        Log.i(GOOGLE_DRIVE_TAG, "Execute " + this.getClass());
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(folderName).build();
        if (parentDriveId != null) {
            DriveFolder folder = parentDriveId.asDriveFolder();
            folder.createFolder(
                    googleApiClient, changeSet).setResultCallback(onFolderCallback);
        } else {
            Drive.DriveApi.getRootFolder(googleApiClient).createFolder(
                    googleApiClient, changeSet).setResultCallback(onFolderCallback);
        }
    }

    public void setParentFolderDriveId(DriveId parentDriveId) {
        this.parentDriveId = parentDriveId;
    }
    
    final ResultCallback<DriveFolder.DriveFolderResult> onFolderCallback = new ResultCallback<DriveFolder.DriveFolderResult>() {
        @Override
        public void onResult(DriveFolder.DriveFolderResult result) {
            if (!result.getStatus().isSuccess()) {
                Log.i(GOOGLE_DRIVE_TAG, "Error while trying to create the folder");
                callback.onFolderWorkDone();
                return;
            }
            if (callback != null) {
                callback.onFolderCreated(result.getDriveFolder().getDriveId());
                callback.onFolderWorkDone();
            }
            Log.i(GOOGLE_DRIVE_TAG, "Created a folder: " + result.getDriveFolder().getDriveId());
        }
    };
}
