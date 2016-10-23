package net.brainas.android.app.infrustructure;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.domain.models.Image;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManagerFactory;

/**
 * Created by innok on 11/11/2015.
 */
public class InfrustructureHelper {

    static public final String PATH_TO_SYNC_DATE_FOLDER = "files/app_sync/sync_data/";
    public static final String PATH_TO_SEND_FOLDER = PATH_TO_SYNC_DATE_FOLDER + "for_send/";
    static public String PATH_TO_TASK_IMAGES_FOLDER = "files/app_images/task_images/";

    static private HashMap<String, Bitmap> bitmapCache = new HashMap<String, Bitmap>();

    static private  String TAG = "InfrustructureHelper";

    static public String getPathToImageFolder(int accountId) {
        String dataDir = BrainasApp.getAppContext().getApplicationInfo().dataDir + "/";
        String pathToPictureFolder = dataDir + PATH_TO_TASK_IMAGES_FOLDER + accountId + "/";
        return pathToPictureFolder;
    }

    static public String getPathToSendDir(int accountId) {
        String dataDir = BrainasApp.getAppContext().getApplicationInfo().dataDir + "/";
        String pathToSendDir = dataDir + PATH_TO_SEND_FOLDER + accountId + "/";
        return pathToSendDir;
    }

    static public Bitmap getTaskPicture(String pictureName, int accountId)  {
        String pathToPictureFolder = getPathToImageFolder(accountId);
        File imageFile = new File(pathToPictureFolder + pictureName);
        Bitmap bitmap;
        if (bitmapCache.containsKey(pictureName)) {
            bitmap = bitmapCache.get(pictureName);
        } else {
            try {
                bitmap = BitmapFactory.decodeFile(imageFile.getPath());
                bitmapCache.put(pictureName, bitmap);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "Out of memory when try to get bitmap");
                return null;
            }
        }
        return bitmap;
    }


    static  public List<File> getListOfPictures(int accountId) {
        String pathToPictureFolder = getPathToImageFolder(accountId);
        return getListOfFiles(new File(pathToPictureFolder));
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
        return  createFileInDir(dir, fileName, ext, true, true);
    }

    public static File createFileInDir(String dir, String fileName, String ext, boolean formatDate, boolean createFile) throws IOException {
        File file;

        String dateTime;
        Calendar cal = Calendar.getInstance();
        if (formatDate) {
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            dateTime = dateFormat.format(cal.getTime());
        } else {
            dateTime = Long.toString(cal.getTimeInMillis());
        }
        String fullFileName = fileName + "_" + dateTime + "." + ext;

        file = new File(dir +  fullFileName);
        file.getParentFile().mkdirs();
        if (createFile) {
            file.createNewFile();
        }
        return file;
    }

    public static File creteFileForGivenName(String dir, String fileName) {
        File file;
        file = new File(dir +  fileName);
        return file;
    }

    public static HttpsURLConnection createHttpMultipartConn(String url) throws
            IOException,
            KeyStoreException,
            CertificateException,
            NoSuchAlgorithmException,
            KeyManagementException {
        KeyStore trustStore = KeyStore.getInstance("BKS");
        InputStream trustStoreStream;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            trustStoreStream = (BrainasApp.getAppContext()).getResources().openRawResource(R.raw.server);
        } else {
            trustStoreStream = (BrainasApp.getAppContext()).getResources().openRawResource(R.raw.server_v1);
        }
        trustStore.load(trustStoreStream, (BrainasApp.getAppContext()).getResources().getString(R.string.key_store_pass_for_cert).toCharArray());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        URL urlObj = new URL(url);

        HttpsURLConnection connection;
        try {
            connection = (HttpsURLConnection) urlObj.openConnection();
        } catch (ClassCastException e) {
            throw new ClassCastException(e.getMessage());
        }
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
        connection.setHostnameVerifier(new CustomHostnameVerifier());
        connection.setRequestMethod("POST");

        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
        connection.setRequestProperty("Accept","*/*");
        connection.setRequestProperty(
                "Content-Type", "multipart/form-data;boundary=" + SyncHelper.boundary);

        connection.setConnectTimeout(7000);
        return connection;
    }



    public static class CustomHostnameVerifier implements HostnameVerifier {
        String [] allowHosts = {
                "192.168.1.100",
                "192.168.1.101",
                "192.168.1.102",
                "192.168.1.103",
                "192.168.1.104",
                "192.168.1.105",
                "brainas.net"
        };

        @Override
        public boolean verify(String hostname, SSLSession session) {
            for (String allowHost : allowHosts) {
                if (hostname.equalsIgnoreCase(allowHost)) {
                   return true;
                }
            }
          return false;
        }
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

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public static boolean moveFile(File src, File dst) throws IOException {
        if (src == null || dst == null) {
            return false;
        }
        copyFile(src, dst);
        return src.delete();
    }
    public static void copyFile(File src, File dst) throws IOException
    {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try
        {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        }
        finally
        {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

    public static void removePicture(Image picture, int accountId) {
        if (picture == null) {
            return;
        }
        File imageFolder = new File(getPathToImageFolder(accountId));
        removeFileFromDir(imageFolder, picture.getName());
        removeFromBitmapCache(picture);
    }

    public static boolean removeFileFromDir(File dir, String fileName ) {
        if (dir == null || fileName == null) {
            return false;
        }
        File file = new File(dir, fileName);
        return file.delete();

    }

    public static void removeFromBitmapCache(Image image) {
        if (bitmapCache.containsKey(image.getName())) {
            bitmapCache.remove(image.getName());
        }
    }
}
