package net.brainas.android.app.infrustructure;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.BrainasAppException;
import net.brainas.android.app.R;
import net.brainas.android.app.domain.models.Condition;
import net.brainas.android.app.domain.models.Event;
import net.brainas.android.app.domain.models.Task;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
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

    public static HttpsURLConnection createHttpMultipartConn(String url) throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException, KeyManagementException {
        KeyStore trustStore = KeyStore.getInstance("BKS");
        InputStream trustStoreStream = (BrainasApp.getAppContext()).getResources().openRawResource(R.raw.server);
        trustStore.load(trustStoreStream, (BrainasApp.getAppContext()).getResources().getString(R.string.key_store_pass_for_cert).toCharArray());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        URL urlObj = new URL(url);

        HttpsURLConnection connection = (HttpsURLConnection) urlObj.openConnection();
        connection.setSSLSocketFactory(sslContext.getSocketFactory());
        connection.setHostnameVerifier(new CustomHostnameVerifier());
        connection.setRequestMethod("POST");

        connection.setRequestProperty("Connection", "Keep-Alive");
        connection.setRequestProperty("Cache-Control", "no-cache");
        connection.setRequestProperty("User-Agent","Mozilla/5.0 ( compatible ) ");
        connection.setRequestProperty("Accept","*/*");
        connection.setRequestProperty(
                "Content-Type", "multipart/form-data;boundary=" + SyncHelper.boundary);

        return connection;
    }



    public static class CustomHostnameVerifier implements HostnameVerifier {
        String [] allowHosts = {
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

}
