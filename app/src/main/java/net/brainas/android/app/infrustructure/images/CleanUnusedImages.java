package net.brainas.android.app.infrustructure.images;

import android.os.AsyncTask;
import android.util.Log;

import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.InfrustructureHelper;
import net.brainas.android.app.infrustructure.TaskDbHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by innok on 7/20/2016.
 */
public class CleanUnusedImages extends AsyncTask<Void, Void, Void> {
    public static String CLEAN_IMAGE_BOT_TAG = "CLEAN_IMAGE_TASK";
    TasksManager tasksManager;
    int accountId;

    public CleanUnusedImages setTasksManager(TasksManager tasksManager) {
        this.tasksManager = tasksManager;
        return this;
    }

    public CleanUnusedImages setAccountId(int accountId) {
        this.accountId = accountId;
        return this;
    }

    @Override
    protected Void doInBackground(Void... params) {
        if (tasksManager == null) {
            Log.i(CLEAN_IMAGE_BOT_TAG, "Cannot get image from DB cause we havn't a tasksManager object");
            return null;
        }
        ArrayList<Task> allTasks = tasksManager.getAllTasks();
        ArrayList<String> allPicturesNames = new ArrayList<>();
        for (Task task : allTasks) {
            if (task.getPicture() != null) {
                allPicturesNames.add(task.getPicture().getName());
            }
        }

        List<File> files = InfrustructureHelper.getListOfPictures(tasksManager.getAccpuntId());

        if (files == null) {
            return null;
        }

        for(File file : files) {
            if(!allPicturesNames.contains(file.getName())) {
                Log.i(CLEAN_IMAGE_BOT_TAG,"Need to delete picture file with name " + file.getName());
                if(file.delete()){
                    Log.i(CLEAN_IMAGE_BOT_TAG, file.getName() + " is deleted!");
                }else {
                    Log.i(CLEAN_IMAGE_BOT_TAG, "Delete operation is failed.");
                    System.out.println("Delete operation is failed.");
                }
            }
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {

    }
}
