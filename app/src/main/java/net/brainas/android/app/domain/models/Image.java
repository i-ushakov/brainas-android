package net.brainas.android.app.domain.models;

import android.graphics.Bitmap;

import com.google.android.gms.drive.DriveId;
import com.google.android.gms.nearby.bootstrap.request.DisableTargetRequest;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by innok on 7/5/2016.
 */
public class Image {
    private String name;
    private DriveId googleDriveId;
    private String fileId;
    private Bitmap bitmap;
    private CopyOnWriteArrayList<ImageDownloadedObserver> observers = new CopyOnWriteArrayList<ImageDownloadedObserver>();

    public Image(String name) {
        this.name = name;
    }

    public Image(String name, Bitmap bitmap) {
        this.name = name;
        this.bitmap = bitmap;
    }

    public interface ImageDownloadedObserver {
        void onImageDownloadCompleted();
    }

    public void attachObserver(ImageDownloadedObserver observer){
        observers.add(observer);
    }

    public void detachObserver(ImageDownloadedObserver observer){
        observers.remove(observer);
    }

    public String getName() {
        return name;
    }

    public DriveId getDriveId() {
        return googleDriveId;
    }

    public void setDriveId(DriveId googleDriveId) {
        this.googleDriveId = googleDriveId;
    }

    public void setResourceId(String fileId) {
        this.fileId = fileId;
    }

    public String getResourceId() {
        return this.fileId;
    }

    public void setBitmap(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public void onDownloadCompleted() {
        Iterator<ImageDownloadedObserver> it = observers.listIterator();
        while (it.hasNext()) {
            ImageDownloadedObserver observer = it.next();
            observer.onImageDownloadCompleted();
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == null) return false;
        if (object == this) return true;
        if (!(object instanceof Image))return false;
        Image image = (Image)object;

        // check name
        String objectName = image.getName();
        if ((objectName == null && this.name != null) || (objectName != null && this.name == null)) {
            return false;
        }
        if (!image.getName().equals(this.name)) {
            return false;
        }

        // check googleDriveId
        DriveId objectGoogleDriveId = image.getDriveId();
        if ((objectGoogleDriveId == null && this.googleDriveId != null) || (objectGoogleDriveId != null && this.googleDriveId == null)) {
            return false;
        }
        if (objectGoogleDriveId != null) {
            if (!objectGoogleDriveId.equals(this.googleDriveId)) {
                return false;
            }
        }

        // check bitmap
        Bitmap objectBitmap = image.getBitmap();
        if ((objectBitmap == null && this.bitmap != null) || (objectBitmap != null && this.bitmap == null)) {
            return false;
        }
        if (objectBitmap != null) {
            if (!objectBitmap.equals(this.bitmap)) {
                return false;
            }
        }

        return true;
    }
}
