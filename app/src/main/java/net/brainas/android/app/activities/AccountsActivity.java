package net.brainas.android.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.BrainasApp;
import net.brainas.android.app.R;
import net.brainas.android.app.infrustructure.NetworkHelper;
import net.brainas.android.app.infrustructure.UserAccount;

/**
 * Created by innok on 12/7/2015.
 */
public class AccountsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private BrainasApp app;
    private AccountsManager accountsManager;
    private UserAccount userAccount;


    private TextView accountNameValue;
    private TextView userNameValue;
    private LinearLayout accountInfoBlock;
    private Button singInButton;
    private TextView accountActivityTitle;
    private LinearLayout offlineModeWarning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (app.getAccountsManager().isUserSingIn()) {
                    onBackPressed();
                } else {
                    Toast.makeText(AccountsActivity.this, "You must sign in to Brain Assistant's app to continue.", Toast.LENGTH_LONG).show();
                }
            }
        });

        app = (BrainasApp) (BrainasApp.getAppContext());
        accountActivityTitle = (TextView) findViewById(R.id.account_activity_title);
        accountInfoBlock = (LinearLayout) findViewById(R.id.account_info);
        singInButton = (Button) findViewById(R.id.sing_in_button);
        accountNameValue = (TextView) findViewById(R.id.account_name_value);
        userNameValue = (TextView) findViewById(R.id.user_name_value);
        offlineModeWarning = (LinearLayout) findViewById(R.id.offline_mode_warning);

        renderContent();

        singInButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (app.getAccountsManager().isUserSingIn()) {
                    app.getAccountsManager().switchAccount(AccountsActivity.this);
                } else {
                    app.getAccountsManager().initialSingIn(AccountsActivity.this);
                    renderContent();
                }
            }
        });

        if(!app.getAccountsManager().isUserSingIn()) {
            app.getAccountsManager().initialSingIn(this);
        } else {
            app.getAccountsManager().buildApiClient(this);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        BrainasApp.activityResumed();
    }

    @Override
    public void onPause() {
        super.onPause();
        BrainasApp.activityPaused();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return true;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == AccountsManager.RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (app.getAccountsManager().handleSignInResult(result, this)) {
                renderContent();
                finish();
            } else {
                Toast.makeText(AccountsActivity.this, "You must sign in to Brain Assistant's app to continue.", Toast.LENGTH_LONG).show();
                renderContent();
            }
        }
    }

    private void renderContent() {
        accountsManager = app.getAccountsManager();
        if (accountsManager.isUserSingIn()) {
            accountInfoBlock.setVisibility(View.VISIBLE);
            userAccount = accountsManager.getUserAccount();
            accountActivityTitle.setText(R.string.accounts_header_signedin);
            accountNameValue.setText(app.getAccountsManager().getUserAccount().getAccountName());
            userNameValue.setText(app.getAccountsManager().getUserAccount().getPersonName());
            singInButton.setText("CHANGE ACCOUNT");
            if(!NetworkHelper.isNetworkActive()) {
                offlineModeWarning.setVisibility(View.VISIBLE);
            }
        } else {
            accountActivityTitle.setText(R.string.accounts_header_not_signedin);
            accountInfoBlock.setVisibility(View.GONE);
            accountNameValue.setText("");
            userNameValue.setText("");
            singInButton.setText("SIGN IN");
        }
    }
}

