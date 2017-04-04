package net.brainas.android.app.infrustructure.synchronization.asyncTasks;

import android.os.AsyncTask;
import android.util.Log;

import net.brainas.android.app.AccountsManager;
import net.brainas.android.app.CLog;
import net.brainas.android.app.infrustructure.NetworkHelper;
import net.brainas.android.app.infrustructure.SyncHelper;
import net.brainas.android.app.infrustructure.UserAccount;
import net.brainas.android.app.services.SynchronizationService;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by Kit Ushakov on 5/9/2016.
 */
public class RefreshTokenAsyncTask extends AsyncTask<Void, Void, String> {
    static String TAG = "RefreshTokenAsyncTask";
    private ResponseListener mListener = null;
    private Exception mError = null;
    private UserAccount userAccount = null;

    public static RefreshTokenAsyncTask build(UserAccount userAccount) {
        final RefreshTokenAsyncTask refreshTokenAsyncTask = new RefreshTokenAsyncTask(userAccount);
        refreshTokenAsyncTask.setListener(new RefreshTokenAsyncTask.ResponseListener() {

            @Override
            public void onComplete(String response, Exception e) {
                refreshTokenAsyncTask.handleResponse(response);
            }
        });

        return refreshTokenAsyncTask;
    }

    protected RefreshTokenAsyncTask(UserAccount userAccount) {
       this.userAccount = userAccount;
    }

    @Override
    protected String doInBackground(Void... params) {
        String response = null;

        // send changes to server for processing
        if (NetworkHelper.isNetworkActive()) {
            response = SyncHelper.refreshAccessTokenRequest();
        } else {
            Log.i(TAG, "Network is not available");
        }

        return response;
    }

    @Override
    protected void onPostExecute(String s) {
        super.onPostExecute(s);
        if (this.mListener != null)
            this.mListener.onComplete(s, mError);
    }

    @Override
    protected void onCancelled() {
        if (this.mListener != null) {
            mError = new InterruptedException("AsyncTask cancelled");
            this.mListener.onComplete(null, mError);
        }
    }

    public RefreshTokenAsyncTask setListener(ResponseListener listener) {
        this.mListener = listener;
        return this;
    }

    public interface ResponseListener {
        public void onComplete(String jsonString, Exception e);
    }

    public void handleResponse(String response) {
        if (response == null) {
            CLog.e(TAG, "Response is NULL", null);
            return;
        } else {
            CLog.i(TAG, response);
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(response));
            Document xmlDocument = builder.parse(is);
            String accessToken = retrieveAccessToken(xmlDocument);
            if (accessToken != null) {
                SynchronizationService.accessToken = accessToken;
                this.userAccount.setAccessToken(accessToken);
                AccountsManager.saveUserAccount(userAccount);
                Log.v(TAG, "Access token was gotten :" + SynchronizationService.accessToken);
            }
        } catch (ParserConfigurationException | IOException | SAXException e) {
            CLog.e(TAG, "Cannot parse xml-document that gotten from server", e);
            e.printStackTrace();
            return;
        }
    }

    /**
     * Retrieving access token from xml response from server
     *
     * @param xmlDocument - xml-document that was got from server
     * @return accessToken
     */
    public String retrieveAccessToken(Document xmlDocument) {
        Log.i("TOKEN_TEST", "retrieveAccessToken mothod");
        String accessToken;

        Element accessTokenEl = (Element)xmlDocument.getElementsByTagName("accessToken").item(0);
        if (accessTokenEl != null) {
            accessToken = accessTokenEl.getTextContent();
            if (!accessToken.equals("")) {
                Log.i("TOKEN_TEST", "return accessToken = " + accessToken);
                return accessToken;
            } else {
                return null;
            }
        } else {
            Log.v(TAG, "We have a problem, we can't get a accessToken from server");
            return null;
        }
    }
}
