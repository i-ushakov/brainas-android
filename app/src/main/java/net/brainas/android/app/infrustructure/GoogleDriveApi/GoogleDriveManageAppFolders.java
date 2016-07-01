package net.brainas.android.app.infrustructure.GoogleDriveApi;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by innok on 6/29/2016.
 */
public class GoogleDriveManageAppFolders implements GoogleDriveManager.CurrentTask {
    static public String GOOGLE_DRIVE_TAG = "GOOGLE_DRIVE";
    protected GoogleApiClient mGoogleApiClient;

    private HashMap<String,String> settingsParamsForSave = new HashMap<String,String>();

    public GoogleDriveManageAppFolders() {}

    @Override
    public void execute(GoogleApiClient googleApiClient) {
        this.mGoogleApiClient = googleApiClient;
        Log.i(GOOGLE_DRIVE_TAG, "Execute " + this.getClass());
        GoogleDriveGetParams googleDriveGetParams = new GoogleDriveGetParams();
        googleDriveGetParams.setOnDownloadedCallback(new OnGetParamsCallback());
        googleDriveGetParams.execute(mGoogleApiClient);
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
            settingsParamsForSave.put(GoogleDriveManager.SettingsParamNames.PROJECT_FOLDER_DRIVE_ID.name(), driveId.toString());
            createPicturesFolder(driveId);
        }

        @Override
        public void onFolderWorkDone() {

        }
    }

    private class OnPicturesFolderCallback implements GoogleDriveCreateFolder.OnFolderCreatedCallbak {

        @Override
        public void onFolderCreated(DriveId driveId) {
            Log.i(GOOGLE_DRIVE_TAG, "Pictures folder was created with driveId = " + driveId);
            settingsParamsForSave.put(GoogleDriveManager.SettingsParamNames.PICTURE_FOLDER_DRIVE_ID.name(), driveId.toString());
        }

        @Override
        public void onFolderWorkDone() {
            GoogleDriveSetParams googleDriveSetParams = new GoogleDriveSetParams();
            googleDriveSetParams.setParamsHash(settingsParamsForSave);
            googleDriveSetParams.execute(mGoogleApiClient);
        }
    }

    private class OnGetParamsCallback implements GoogleDriveGetParams.GettingParamsHandler {

        @Override
        public void onJSONSettingIsAbsent() {
            Log.i(GOOGLE_DRIVE_TAG, "We havn't ba_settings.json in appFolder");
            createProjectFolder();
        }

        @Override
        public void onGettingParamsSuccess(JSONObject currentParams, DriveId settinsJsonDriverId) {
            Log.i(GOOGLE_DRIVE_TAG, "Successfully got ba_settings.json");
            new CheckProjectFoldersAsyncTask().execute(currentParams);

        }


        final private class CheckProjectFoldersAsyncTask
                extends android.os.AsyncTask<JSONObject, Void, Void> {


            @Override
            protected Void doInBackground(JSONObject... params) {
                JSONObject currentParams = params[0];
                if (currentParams.has(GoogleDriveManager.SettingsParamNames.PROJECT_FOLDER_DRIVE_ID.name())) {
                    if (checkFolderExists(currentParams, GoogleDriveManager.SettingsParamNames.PROJECT_FOLDER_DRIVE_ID.name())) {
                        if (currentParams.has(GoogleDriveManager.SettingsParamNames.PICTURE_FOLDER_DRIVE_ID.name())) {
                            if (checkFolderExists(currentParams,GoogleDriveManager.SettingsParamNames.PICTURE_FOLDER_DRIVE_ID.name())) {
                                Log.i(GOOGLE_DRIVE_TAG, "All project folders are OK");
                            } else {
                                createPicturesFolder(currentParams);
                            }
                        } else {
                            createPicturesFolder(currentParams);
                        }
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


        private boolean checkFolderExists(JSONObject currentParams, String paramName) {
            try {
                String projectFolderDriveIdStr = currentParams.getString(paramName);
                DriveId projectFolderDriveId = DriveId.decodeFromString(projectFolderDriveIdStr);
                Metadata metadata = projectFolderDriveId.asDriveFolder().getMetadata(mGoogleApiClient).await().getMetadata();

                if (metadata != null) {
                    if (metadata.isTrashed()) {
                        projectFolderDriveId.asDriveFolder().untrash(mGoogleApiClient).await();
                    }
                    //metadata.getTitle();
                } else {
                    return false;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }
    }
}
