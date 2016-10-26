package net.brainas.android.app.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;

import java.io.IOException;
import java.io.InputStream;

public class AboutActivity extends AppCompatActivity {

    private BrainasApp app;
    private Toolbar toolbar;
    private GridView tasksGrid;
    private TextView userNotSignedInMessage;
    private String searchText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        app = ((BrainasApp)BrainasApp.getAppContext());
        setTitle("About");

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

        WebView webView = (WebView) findViewById(R.id.about_webview);
        webView.getSettings().setJavaScriptEnabled(true);
        WebSettings settings = webView.getSettings();
        settings.setDefaultTextEncodingName("utf-8");

        String fileName = "about_en.html";

        try {
            InputStream is = getAssets().open(fileName);
            int size = is.available();

            byte[] buffer = new byte[size];
            int bytesRead = is.read(buffer);
            is.close();

            String html = new String(buffer, 0, bytesRead, "UTF-8");

            webView.loadDataWithBaseURL("file:///android_asset/", html,
                    "text/html", "UTF-8", null);

            webView.setInitialScale(100);
            //webView.getSettings().setLoadWithOverviewMode(true);
            //webView.getSettings().setUseWideViewPort(true);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        BrainasApp.activityResumed();
    }

    @Override
    public void onPause() {
        super.onPause();
        BrainasApp.activityPaused();
    }
}
