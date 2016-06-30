package net.brainas.android.app.infrustructure;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by innok on 6/22/2016.
 */
public class GoogleDriveCreateFolder implements GoogleDriveManager.CurrentTask {

    static public String GOOGLE_DRIVE_TAG = "GOOGLE_DRIVE";

    public interface OnFolderCreatedCallbak {
        public void onFolderCreated(DriveId driveId);
    }

    private GoogleApiClient mGoogleApiClient;
    private String folderName;
    private DriveId parentDriveId;
    private OnFolderCreatedCallbak callback;

    public GoogleDriveCreateFolder(GoogleApiClient mGoogleApiClient) {
        this.mGoogleApiClient = mGoogleApiClient;
        Log.i(GOOGLE_DRIVE_TAG, this.getClass() + " class is created");

    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public void setOnFolderCreatedCallback(OnFolderCreatedCallbak callback) {
        this.callback = callback;
    }

    public void execute() {
        Log.i(GOOGLE_DRIVE_TAG, "Execute " + this.getClass());
        MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                .setTitle(folderName).build();
        if (parentDriveId != null) {
            DriveFolder folder = parentDriveId.asDriveFolder();
            folder.createFolder(
                    mGoogleApiClient, changeSet).setResultCallback(onFolderCallback);
        } else {
            Drive.DriveApi.getRootFolder(mGoogleApiClient).createFolder(
                    mGoogleApiClient, changeSet).setResultCallback(onFolderCallback);
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
                return;
            }
            if (callback != null) {
                callback.onFolderCreated(result.getDriveFolder().getDriveId());
            }
            Log.i(GOOGLE_DRIVE_TAG, "Created a folder: " + result.getDriveFolder().getDriveId());
        }
    };
}
