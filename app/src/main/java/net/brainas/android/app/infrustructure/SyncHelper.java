package net.brainas.android.app.infrustructure;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

/**
 * Created by innok on 2/5/2016.
 */
public class SyncHelper {
    private static final String syncDateDir = "/app_sync/sync_data";
    private static final String syncDateDirForSend = syncDateDir + "/for_send/";

    TaskChangesDbHelper taskChangesDbHelper;

    SyncHelper(TaskChangesDbHelper taskChangesDbHelper) {
        this.taskChangesDbHelper = taskChangesDbHelper;
    }

    public File getLastChangedTasksInXml() {
        return new File("test");
    }

    public File getAllChangesInJSON() throws IOException, JSONException {
        File changesFile = InfrustructureHelper.createFileInDir(syncDateDirForSend, "all_tasks_changes", "json");
        JSONArray tasksChanges = taskChangesDbHelper.getAllTasksChanges();
        JSONObject allChanges = new JSONObject();
        allChanges.put("tasks", tasksChanges);
        Files.write(allChanges.toString(), changesFile, Charsets.UTF_8);

        return changesFile;
    }
}
