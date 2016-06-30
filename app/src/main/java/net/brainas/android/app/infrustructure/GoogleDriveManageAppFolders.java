package net.brainas.android.app.infrustructure;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.widget.DataBufferAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by innok on 6/29/2016.
 */
public class GoogleDriveManageAppFolders implements GoogleDriveManager.CurrentTask {
    static public String GOOGLE_DRIVE_TAG = "GOOGLE_DRIVE";
    protected GoogleApiClient mGoogleApiClient;

    public GoogleDriveManageAppFolders(GoogleApiClient mGoogleApiClient) {
        this.mGoogleApiClient = mGoogleApiClient;
        Log.i(GOOGLE_DRIVE_TAG, this.getClass() + " class is created");
    }

    @Override
    public void execute() {
        Log.i(GOOGLE_DRIVE_TAG, "Execute " + this.getClass());
        GoogleDriveGetParams googleDriveGetParams = new GoogleDriveGetParams(mGoogleApiClient);
        googleDriveGetParams.setOnDownloadedCallback(new OnGetParamsCallback());
        googleDriveGetParams.execute();
    }

    private void createProjectFolder() {
        Log.i(GOOGLE_DRIVE_TAG, "Let's create Project Folder");
        GoogleDriveCreateFolder googleDriveCreateFolder = new GoogleDriveCreateFolder(mGoogleApiClient);
        googleDriveCreateFolder.setFolderName(GoogleDriveManager.PROJECT_FOLDER_NAME);
        googleDriveCreateFolder.setOnFolderCreatedCallback(new OnProjectFolderCallback());
        googleDriveCreateFolder.execute();
    }

    private void createPicturesFolder(DriveId parentDriveId) {
        Log.i(GOOGLE_DRIVE_TAG, "Let's create Pictures Folder");
        GoogleDriveCreateFolder googleDriveCreateFolder = new GoogleDriveCreateFolder(mGoogleApiClient);
        googleDriveCreateFolder.setFolderName(GoogleDriveManager.PICTURES_FOLDER_NAME);
        googleDriveCreateFolder.setParentFolderDriveId(parentDriveId);
        googleDriveCreateFolder.setOnFolderCreatedCallback(new OnPicturesFolderCallback());
        googleDriveCreateFolder.execute();
    }

    private class OnProjectFolderCallback implements GoogleDriveCreateFolder.OnFolderCreatedCallbak {

        @Override
        public void onFolderCreated(DriveId driveId) {
            Log.i(GOOGLE_DRIVE_TAG, "Project folder was created with driveId = " + driveId);
            GoogleDriveSetParams googleDriveSetParams = new GoogleDriveSetParams(mGoogleApiClient);
            HashMap<String, String> paramsHash = new HashMap<String ,String >();
            paramsHash.put(GoogleDriveParamsNames.PARAM_NAME.PROJECT_FOLDER_DRIVE_ID.toString(), driveId.toString());
            googleDriveSetParams.setParamsHash(paramsHash);
            googleDriveSetParams.execute();
            createPicturesFolder(driveId);
        }
    }

    private class OnPicturesFolderCallback implements GoogleDriveCreateFolder.OnFolderCreatedCallbak {

        @Override
        public void onFolderCreated(DriveId driveId) {
            Log.i(GOOGLE_DRIVE_TAG, "Pictures folder was created with driveId = " + driveId);
            GoogleDriveSetParams googleDriveSetParams = new GoogleDriveSetParams(mGoogleApiClient);
            HashMap<String, String> paramsHash = new HashMap<String ,String >();
            paramsHash.put(GoogleDriveParamsNames.PARAM_NAME.PICTURE_FOLDER_DRIVE_ID.toString(), driveId.toString());
            googleDriveSetParams.setParamsHash(paramsHash);
            googleDriveSetParams.execute();
        }
    }

    private class OnGetParamsCallback implements GoogleDriveGetParams.GettingParamsHandler {

        @Override
        public void onJSONSettingIsAbsent() {
            Log.i(GOOGLE_DRIVE_TAG, "We havn't settings.json in appFolder");
            createProjectFolder();
        }

        @Override
        public void onGettingParamsSuccess(JSONObject currentParams, DriveId settinsJsonDriverId) {
            Log.i(GOOGLE_DRIVE_TAG, "Successfully got settings.json");
            if (currentParams.has(GoogleDriveParamsNames.PARAM_NAME.PROJECT_FOLDER_DRIVE_ID.toString())) {
                // TODO check if exist
                if (currentParams.has(GoogleDriveParamsNames.PARAM_NAME.PICTURE_FOLDER_DRIVE_ID.toString())) {
                    // TODO check if exist
                    Log.i(GOOGLE_DRIVE_TAG, "All projects folders are OK");
                } else {
                    try {
                        DriveId projectFolderDriveId = DriveId.decodeFromString(currentParams.getString(GoogleDriveParamsNames.PARAM_NAME.PROJECT_FOLDER_DRIVE_ID.toString()));
                        createPicturesFolder(projectFolderDriveId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                createProjectFolder();
            }
        }
    }
}
