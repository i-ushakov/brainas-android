package net.brainas.android.app.infrustructure;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.domain.models.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by innok on 11/11/2015.
 */
public class InfrustructureHelper {
    static private String PATH_TO_TASK_IMAGES_FOLDER = "/app_images/task_images/";

    static public Bitmap getTaskImage(Task task) {
        String dataDir = BrainasApp.getAppContext().getApplicationInfo().dataDir;
        File parentDir = new File(dataDir + PATH_TO_TASK_IMAGES_FOLDER + task.getId() + "/");
        List<File> fileList = InfrustructureHelper.getListOfFiles(parentDir);
        if (fileList.size() > 0) {
            File imageFile = fileList.get(0);
            Bitmap bmp = BitmapFactory.decodeFile(imageFile.getPath());
            return bmp;
        }
        return null;
    }

    static public List<File> getListOfFiles(File parentDir) {
        ArrayList<File> inFiles = new ArrayList<File>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                inFiles.addAll(getListOfFiles(file));
            } else {
                //if(file.getName().endsWith(".csv")){
                inFiles.add(file);
                //}
            }
        }
        return inFiles;
    }

    /*
        String dataDir = BrainasApp.getAppContext().getApplicationInfo().dataDir;
        String string = "hello world!";
        File file = new File(dataDir + "/app_images/task_images/2/");
        if (!file.exists()) {
            file.mkdir();
        }
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(FILENAME);
            fos.write(string.getBytes());
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
     */

}
