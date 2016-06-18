package net.brainas.android.app.activities.taskedit;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.domain.helpers.TasksManager;
import net.brainas.android.app.domain.models.Task;
import net.brainas.android.app.infrustructure.BasicImageDownloader;
import net.brainas.android.app.infrustructure.InfrustructureHelper;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by innok on 6/15/2016.
 */
public class SearchPictureActivity extends AppCompatActivity {
    static private String IMG_DOWNLOAD_TAG ="DOWNLOADING_IMAGES";

    private Toolbar toolbar;

    private String searchTerm = "";
    private ProgressDialog progressDialog;
    private CopyOnWriteArrayList<PictureDowmloadObserver> observers = new CopyOnWriteArrayList<PictureDowmloadObserver>();

    public interface PictureDowmloadObserver {
        void updateAfterPictureWasDownloaded();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_picture);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });


        searchTerm = null;
        try {
            if (getIntent().getStringExtra("searchTerm") != null) {
                searchTerm = URLEncoder.encode(getIntent().getStringExtra("searchTerm"), "utf-8");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        initWebView();
    }

    public void attachObserver(PictureDowmloadObserver observer){
        observers.add(observer);
    }

    public void detachObserver(PictureDowmloadObserver observer){
        observers.remove(observer);
    }

    public void notifyAllObservers() {
        Iterator<PictureDowmloadObserver> it = observers.listIterator();
        while (it.hasNext()) {
            PictureDowmloadObserver observer = it.next();
            observer.updateAfterPictureWasDownloaded();
        }
    }

    private void initWebView() {
        WebView googleSearchWebView = (WebView)findViewById(R.id.googleSearchWebView);
        googleSearchWebView.setWebViewClient(new GoogleSearchWebViewClient());
        googleSearchWebView.loadUrl("https://www.google.ru/search?q=" + searchTerm + "&tbm=isch");
        this.registerForContextMenu(googleSearchWebView);

        googleSearchWebView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                WebView.HitTestResult hr = ((WebView)v).getHitTestResult();
                if (hr.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE){
                    showProgressDialog();
                    BasicImageDownloader basicImageDownloader = new BasicImageDownloader(new OnImageLoaderListener());
                    basicImageDownloader.download(hr.getExtra(), true);
                }
                return true;
            }
        });
    }


    private class GoogleSearchWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.contains("google") && url.contains("tbm=isch")) {
                view.loadUrl(url);
                return true;
            } else {
                Toast.makeText(SearchPictureActivity.this, "This action is no avalibale", Toast.LENGTH_SHORT).show();
                return true;
            }
        }
    }


    private class OnImageLoaderListener implements BasicImageDownloader.OnImageLoaderListener {
        @Override
        public void onError(BasicImageDownloader.ImageError error) {
            progressDialog.hide();
            Toast.makeText(SearchPictureActivity.this, "Cannot load image from internet", Toast.LENGTH_SHORT).show();
            Log.e(IMG_DOWNLOAD_TAG, "Cannot load image from internet");
        }

        @Override
        public void onProgressChange(int percent) {
            if (progressDialog != null) {
                progressDialog.setProgress(percent);
            };
        }

        @Override
        public void onComplete(Bitmap result) {
            final File imageFile;
            try {
                imageFile = InfrustructureHelper.createFileInDir(
                        InfrustructureHelper.PATH_TO_TASK_IMAGES_FOLDER,
                        "task_img", "png",
                        false, false
                );
                BasicImageDownloader.writeToDisk(imageFile, result, new BasicImageDownloader.OnBitmapSaveListener() {
                    @Override
                    public void onBitmapSaved() {
                        notifyAllObservers();
                        progressDialog.hide();
                        Intent data = new Intent();
                        data.putExtra(EditTaskActivity.IMAGE_REQUEST_EXTRA_FIELD, imageFile.getName());
                        setResult(RESULT_OK,data);
                        SearchPictureActivity.this.finish();
                    }

                    @Override
                    public void onBitmapSaveError(BasicImageDownloader.ImageError error) {
                        Toast.makeText(SearchPictureActivity.this, "Cannot save image for task ", Toast.LENGTH_SHORT).show();
                        Log.e(IMG_DOWNLOAD_TAG, "Cannot save image on disk");
                    }
                }, Bitmap.CompressFormat.PNG, false);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(SearchPictureActivity.this, "Cannot save image for task ", Toast.LENGTH_SHORT).show();
                Log.e(IMG_DOWNLOAD_TAG, "Cannot save image on disk");
            }
            progressDialog.hide();
        }
    }

    private void showProgressDialog() {
        progressDialog = new ProgressDialog(SearchPictureActivity.this);
        progressDialog.setMessage("Downloading Image");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(true);
        progressDialog.setProgress(0);
        progressDialog.setMax(100);
        progressDialog.show();
    }
}
