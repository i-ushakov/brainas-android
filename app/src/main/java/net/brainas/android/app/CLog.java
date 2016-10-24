package net.brainas.android.app;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import net.brainas.android.app.infrustructure.InfrustructureHelper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by kit on 10/15/2016.
 */
public class CLog {
    static String TAG = "#$#CLog#";
    static boolean writeToCustomLogFile = true;
    static String pathToFiles;
    static final String FILE_NAME_FIRST = "custom_log_1.txt";
    static final String FLAG_1 = "1.flag";
    static final String FILE_NAME_SECOND = "custom_log_2.txt";
    static final String FILE_PATH_SECOND = null;
    static final String FLAG_2 = "2.flag";

    public static void init(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            pathToFiles = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getPath();
        } else{
            pathToFiles = InfrustructureHelper.getDocumentFolder().getPath();
        }
    }

    public static int i(String tag, String message) {
        if (BrainasAppSettings.writeToCustomLog()) {
            try {
                File logFile = getCurrentLogFile();
                writeToLogFile(logFile, tag, message);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Cannot write to custom log file");
            }
        }
        return Log.i(tag, message);
    }

    public static int e(String tag, String message, Exception exception) {
        if (BrainasAppSettings.writeToCustomLog()) {
            try {
                File logFile = getCurrentLogFile();
                if (exception != null) {
                    message = message + " ### " + Log.getStackTraceString(exception);
                }
                writeToLogFile(logFile, tag, message);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Cannot write to custom log file");
            }
        }
        return Log.e(tag, message);
    }

    private static void writeToLogFile(File logFile, String tag, String output) throws IOException {
        // true - append
        int lineNumber = Thread.currentThread().getStackTrace()[4].getLineNumber();
        String fileName = Thread.currentThread().getStackTrace()[4].getFileName();
        String className = Thread.currentThread().getStackTrace()[4].getClassName();
        String methodName = Thread.currentThread().getStackTrace()[4].getMethodName();
        String placeOfCapture = fileName + ":" + lineNumber + ", " + className + "." + methodName + "()";


        String content = getFormattedDate() + " " + tag + " " + output + "\r\n" + placeOfCapture + "\r\n\r\n";
        byte[] contentInBytes = content.getBytes();

        FileOutputStream fop = new FileOutputStream(logFile, true);
        synchronized (fop) {
            fop.write(contentInBytes);
        }
        fop.flush();
        fop.close();
    }

    private static File getCurrentLogFile() throws IOException {
        File logFile = new File(pathToFiles + "/" + getCurrentLogFileName());
        if(!logFile.exists()){
            logFile.createNewFile();
        }


        return logFile;
    }
    private static String getCurrentLogFileName() {
        return FILE_NAME_FIRST;
    }

    private static String getFormattedDate() {
        Calendar c = Calendar.getInstance();
        System.out.println("Current time => "+c.getTime());

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String formattedDate = df.format(c.getTime());
        return formattedDate;
    }
}
