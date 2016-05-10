package net.brainas.android.app.infrustructure;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.BrainasAppException;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.Task;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by innok on 11/11/2015.
 */
public class InfrustructureHelper {
    static private String PATH_TO_TASK_IMAGES_FOLDER = "/app_images/task_images/";

    static public Bitmap getTaskImage(Task task) throws BrainasAppException {
        String dataDir = BrainasApp.getAppContext().getApplicationInfo().dataDir;
        File parentDir = new File(dataDir + PATH_TO_TASK_IMAGES_FOLDER + task.getId() + "/");
        if(parentDir == null) {
            throw new BrainasAppException("Wrong directory with image for task with id = " + task.getId());
        }
        List<File> fileList = InfrustructureHelper.getListOfFiles(parentDir);
        if(fileList == null) {
            throw new BrainasAppException("No image for task with id = " + task.getId());
        }
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
        if (files == null) {
            return null;
        }
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

    public static File createFileInDir(String dir, String fileName, String ext) throws IOException {
        File file;

        String dataDir = BrainasApp.getAppContext().getApplicationInfo().dataDir;

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
        Calendar cal = Calendar.getInstance();
        String xmlFileName = fileName + "_" + dateFormat.format(cal.getTime()) + "." + ext;

        file = new File(dataDir + dir +  xmlFileName);
        file.getParentFile().mkdirs();
        file.createNewFile();

        return file;
    }

    public static HttpURLConnection createHttpMultipartConn(String url) throws IOException {
        URL urlObj = new URL(url);

        HttpURLConnection connection = (HttpURLConnection) urlObj.openConnection();
        connection.setRequestMethod("POST");

        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
        connection.setRequestProperty("Accept","*/*");
        connection.setRequestProperty(
                "Content-Type", "multipart/form-data;boundary=" + SyncHelper.boundary);

        return connection;
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
