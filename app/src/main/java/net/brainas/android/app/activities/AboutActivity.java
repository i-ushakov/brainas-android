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
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.TextView;

import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;

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
        String pdf = "https://brainas.net/docs/about.pdf";
        webView.loadUrl("http://drive.google.com/viewerng/viewer?embedded=true&url=" + pdf);
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
