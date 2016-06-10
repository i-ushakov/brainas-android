package net.brainas.android.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.OptionalPendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;

import net.brainas.android.app.infrustructure.NetworkHelper;
import net.brainas.android.app.infrustructure.UserAccount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by innok on 1/20/2016.
 */
public class AccountsManager implements
        GoogleApiClient.OnConnectionFailedListener {
    private static String serverClientId = "";
    private ProgressDialog mProgressDialog;

    public static final int RC_SIGN_IN = 9001;


    private BrainasApp app;
    private GoogleSignInOptions gso;
    private HashMap<Integer, GoogleApiClient> GoogleApiClients= new HashMap<Integer, GoogleApiClient>();
    private  GoogleApiClient mGoogleApiClient = null;
    private UserAccount userAccount = null;
    private String accessCode = null;
    private List<SingInObserver> observers = new ArrayList<>();
    private boolean serverIsOffline = false;

    public interface SingInObserver {
        void updateAfterSingIn(UserAccount userAccount);
        void updateAfterSingOut();
    }
    public void attach(SingInObserver observer){
        observers.add(observer);
    }

    public void detach(SingInObserver observer){
        observers.remove(observer);
    }

    public AccountsManager() {
        app = ((BrainasApp)BrainasApp.getAppContext());
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(Scopes.PLUS_LOGIN)) // "https://www.googleapis.com/auth/plus.login"
                .requestServerAuthCode(app.getResources().getString(R.string.client_ID_for_web_application))
                .build();
    }

    static public UserAccount getUserAccountByName(String accountName) {
        BrainasApp app = ((BrainasApp)(BrainasApp.getAppContext()));
        UserAccount userAccount = app.getUserAccountDbHelper().retrieveUserAccountFromDB(accountName);
        return userAccount;
    }

    static public boolean saveUserAccount(UserAccount userAccount) {
        BrainasApp app = ((BrainasApp)(BrainasApp.getAppContext()));
        if (app.getUserAccountDbHelper().saveUserAccount(userAccount) != 0) {
            return true;
        }
        return false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d("TEST", "onConnectionFailed:" + connectionResult);
    }

    public boolean initialSingIn(AppCompatActivity activity) {
        showProgressDialog(activity);
        buildApiClient(activity);
        if (NetworkHelper.isNetworkActive()) {
            OptionalPendingResult<GoogleSignInResult> opr = Auth.GoogleSignInApi.silentSignIn(GoogleApiClients.get(activity.hashCode()));
            if (opr.isDone()) {//opr.isCanceled()
                // If the user's cached credentials are valid, the OptionalPendingResult will be "done"
                // and the GoogleSignInResult will be available instantly.
                Log.d("ACCOUNT_MANAGER", "Got cached sign-in");
                GoogleSignInResult result = opr.get();
                handleSignInResult(result, activity);
            } else {
                if (!doesLastUserHaveTheToken() || !setLastUserAccount(activity)) {
                    Log.d("ACCOUNT_MANAGER", "Try to sign-in...");
                    signIn(activity);
                }
            }
        } else {
            if (!setLastUserAccount(activity)) {
                Toast.makeText(activity, "You must to have an internet connection for start of using Brain Assistant's app.", Toast.LENGTH_LONG).show();
                return false;
            }
        }
        return true;
    }

    private boolean setLastUserAccount(AppCompatActivity activity) {
        userAccount = app.getLastUsedAccount();
        if (userAccount != null) {
            hideProgressDialog();
            app.setUserAccount(userAccount);
            notifyAllObserversAboutSingIn();
            //Toast.makeText(activity, "You are signed in OFFLINE as " + userAccount.getPersonName(), Toast.LENGTH_LONG).show();
            return true;
        }
        return false;
    }

    private boolean doesLastUserHaveTheToken() {
        if (app.getLastUsedAccount().getAccessToken() != null) {
            return true;
        }
        return false;
    }

    public void switchAccount(AppCompatActivity activity) {
        if (NetworkHelper.isNetworkActive()) {
            showProgressDialog(activity);
            buildApiClient(activity);
            signOut(activity);
            signIn(activity);
        } else {
            Toast.makeText(activity, "You must have network connection to switch account", Toast.LENGTH_LONG).show();
        }
    }

    public boolean isOnline() {
        if (NetworkHelper.isNetworkActive()) {
            return true;
        }
        return false;
    }

    public boolean handleSignInResult(GoogleSignInResult result, AppCompatActivity activity) {
        hideProgressDialog();
        Log.d("TEST", "handleSignInResult:" + result.isSuccess());
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            String personName = acct.getDisplayName();
            String accountEmail = acct.getEmail();
            userAccount = new UserAccount(accountEmail);
            userAccount.setPersonName(personName);
            String accessCode = acct.getServerAuthCode();//acct.getIdToken();
            userAccount.setAccessCode(accessCode);
            saveUserAccount(userAccount);
            app.setUserAccount(userAccount);
            saveUserAccount();
            notifyAllObserversAboutSingIn();
            Toast.makeText(activity, "You are signed in as " + personName, Toast.LENGTH_LONG).show();
            return true;
        } else {
            if(userAccount != null) {
                userAccount.setAccessCode(null);
                app.setUserAccount(null);
                notifyAllObserversAboutSingOut();
            }
            Toast.makeText(activity, "You must sign in to Brain Assistant's app to continue.", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public boolean isUserSingIn() {
        // May be add Online/Offline params to account (set when user make signin)
        if (userAccount != null) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isUserHvaeToken() {
        updateUserFromDb();
        if (userAccount != null && userAccount.getAccessToken() != null) {
            return true;
        } else {
            return false;
        }
    }

    public UserAccount updateUserFromDb() {
        if (this.userAccount != null) {
            this.userAccount = getUserAccountByName(this.userAccount.getAccountName());
        }
        return this.userAccount;
    }

    public Integer getCurrentAccountId() {
        if (userAccount != null) {
            return userAccount.getId();
        } else {
            return null;
        }
    }

    public boolean isUserAuthorized() {
        if (NetworkHelper.isNetworkActive() && userAccount != null && userAccount.getAccessCode() != null) {
            return true;
        } else {
            return false;
        }
    }

    public UserAccount getUserAccount() {
        return this.userAccount;
    }

    public void buildApiClient(AppCompatActivity activity) {
        if (GoogleApiClients.get(activity.hashCode()) == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(activity)
                        .enableAutoManage(activity /* FragmentActivity */, this /* OnConnectionFailedListener */)
                        .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                        .build();
            GoogleApiClients.put(activity.hashCode(), mGoogleApiClient);
        }
    }

    public void signIn(AppCompatActivity activity) {
        buildApiClient(activity);
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(GoogleApiClients.get(activity.hashCode()));
        activity.startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public boolean saveUserAccount() {
        if (app.getUserAccountDbHelper().saveUserAccount(this.userAccount) != 0) {
            return true;
        }
        return false;
    }

    private void signOut(AppCompatActivity activity) {
        buildApiClient(activity);
        Auth.GoogleSignInApi.signOut(GoogleApiClients.get(activity.hashCode())).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        userAccount.setAccessCode(null);
                        userAccount = null;
                        notifyAllObserversAboutSingOut();
                    }
                });
    }

    private void notifyAllObserversAboutSingIn() {
        for (SingInObserver observer : observers) {
            observer.updateAfterSingIn(userAccount);
        }
    }

    private void notifyAllObserversAboutSingOut() {
        for (SingInObserver observer : observers) {
            observer.updateAfterSingOut();
        }
    }

    private void showProgressDialog(AppCompatActivity activity) {
        if (mProgressDialog == null) {
            mProgressDialog = new ProgressDialog(activity);
            mProgressDialog.setMessage(activity.getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    private void hideProgressDialog() {
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.hide();
        }
    }
}
