package net.brainas.android.app.infrustructure.googleDriveApi;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.infrustructure.googleDriveApi.GoogleDriveManager.SettingsParamNames;
import net.brainas.android.app.infrustructure.SyncSettingsWithServerTask;
import net.brainas.android.app.services.SynchronizationService;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by innok on 6/29/2016.
 */
public class GoogleDriveManageAppFolders implements GoogleDriveManager.CurrentTask {
    static public String GOOGLE_DRIVE_TAG = "GOOGLE_DRIVE";
    protected GoogleApiClient mGoogleApiClient;
    BrainasApp app;
    private String accessToken;

    private HashMap<String,String> settingsParamsForSave = new HashMap<String,String>();

    public GoogleDriveManageAppFolders() {
        app = ((BrainasApp)BrainasApp.getAppContext());
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }
    @Override
    public void execute(GoogleApiClient googleApiClient) {
        this.mGoogleApiClient = googleApiClient;
        new CheckProjectFoldersAsyncTask().execute();
    }


    private void createProjectFolder() {
        Log.i(GOOGLE_DRIVE_TAG, "Let's create Project Folder");
        GoogleDriveCreateFolder googleDriveCreateFolder = new GoogleDriveCreateFolder(mGoogleApiClient);
        googleDriveCreateFolder.setFolderName(GoogleDriveManager.PROJECT_FOLDER_NAME);
        googleDriveCreateFolder.setOnFolderCreatedCallback(new OnProjectFolderCallback());
        googleDriveCreateFolder.execute(mGoogleApiClient);
    }

    private void createPicturesFolder(JSONObject currentParams) {
        try {
            DriveId projectFolderDriveId = DriveId.decodeFromString(currentParams.getString(GoogleDriveManager.PICTURES_FOLDER_NAME));
            createPicturesFolder(projectFolderDriveId);
        } catch (JSONException e) {
            e.printStackTrace();
            Log.i(GOOGLE_DRIVE_TAG, "Cannot create Pictures folder, cause bad driverId param");
        }
    }

    private void createPicturesFolder(DriveId parentDriveId) {
        Log.i(GOOGLE_DRIVE_TAG, "Let's create Pictures Folder");
        GoogleDriveCreateFolder googleDriveCreateFolder = new GoogleDriveCreateFolder(mGoogleApiClient);
        googleDriveCreateFolder.setFolderName(GoogleDriveManager.PICTURES_FOLDER_NAME);
        if (parentDriveId != null) {
            googleDriveCreateFolder.setParentFolderDriveId(parentDriveId);
        }
        googleDriveCreateFolder.setOnFolderCreatedCallback(new OnPicturesFolderCallback());
        googleDriveCreateFolder.execute(mGoogleApiClient);
    }

    private class OnProjectFolderCallback implements GoogleDriveCreateFolder.OnFolderCreatedCallbak {

        @Override
        public void onFolderCreated(DriveId driveId) {
            Log.i(GOOGLE_DRIVE_TAG, "Project folder was created with driveId = " + driveId);
            app.saveParamsInUserPrefs(GoogleDriveManager.SettingsParamNames.PROJECT_FOLDER_DRIVE_ID.name(), driveId.toString());
            app.removeParamFromUserPref(SettingsParamNames.PROJECT_FOLDER_RESOURCE_ID.name());
            createPicturesFolder(driveId);
        }

        @Override
        public void onFolderWorkDone() {}
    }

    private class OnPicturesFolderCallback implements GoogleDriveCreateFolder.OnFolderCreatedCallbak {

        @Override
        public void onFolderCreated(DriveId driveId) {
            Log.i(GOOGLE_DRIVE_TAG, "Pictures folder was created with driveId = " + driveId);
            app.saveParamsInUserPrefs(GoogleDriveManager.SettingsParamNames.PICTURE_FOLDER_DRIVE_ID.name(), driveId.toString());
            app.removeParamFromUserPref(SettingsParamNames.PICTURE_FOLDER_RESOURCE_ID.name());
            new SyncSettingsWithServerTask(SynchronizationService.accessToken, null).execute();
        }

        @Override
        public void onFolderWorkDone() {}
    }

    final private class CheckProjectFoldersAsyncTask
            extends android.os.AsyncTask<JSONObject, Void, Void> {


        @Override
        protected Void doInBackground(JSONObject... params) {
            JSONObject foldersIds = GoogleDriveManager.getInstance(app).getFoldersIds();
            String projDriveIdParam = GoogleDriveManager.SettingsParamNames.PROJECT_FOLDER_DRIVE_ID.name();
            String projResourceIdParam = SettingsParamNames.PROJECT_FOLDER_RESOURCE_ID.name();
            String picDriveIdParam = SettingsParamNames.PICTURE_FOLDER_DRIVE_ID.name();
            String picResourceIdParam = SettingsParamNames.PROJECT_FOLDER_RESOURCE_ID.name();

            if (foldersIds.has(projDriveIdParam)) {
                DriveId projectFolderDriveId = null;
                try {
                    projectFolderDriveId = GoogleDriveManager.getInstance(app.getApplicationContext()).checkFolderExistsByDriverId(
                            DriveId.decodeFromString(foldersIds.getString(projDriveIdParam)));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (projectFolderDriveId != null) {
                    if (foldersIds.has(picDriveIdParam)) {
                        DriveId pictureFolderDriveId = null;
                        try {
                            pictureFolderDriveId = GoogleDriveManager.getInstance(app.getApplicationContext()).checkFolderExistsByDriverId(DriveId.decodeFromString(foldersIds.getString(picDriveIdParam)));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (pictureFolderDriveId != null) {
                            Log.i(GOOGLE_DRIVE_TAG, "All project folders are OK");
                            Log.i(GOOGLE_DRIVE_TAG, "projectFolderResourceId = " + projectFolderDriveId.getResourceId());
                            Log.i(GOOGLE_DRIVE_TAG, "pictureFolderResourceId = " + pictureFolderDriveId.getResourceId());
                        } else {
                            createPicturesFolder(foldersIds);
                        }
                    } else {
                        createPicturesFolder(foldersIds);
                    }
                } else {
                    createProjectFolder();
                }
            } else {
                createProjectFolder();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            // Nothing TODO
        }
    }

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
