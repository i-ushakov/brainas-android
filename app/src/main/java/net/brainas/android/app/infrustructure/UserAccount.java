package net.brainas.android.app.infrustructure;

import android.content.Context;
import android.content.SharedPreferences;

import net.brainas.android.app.BrainasApp;

/**
 * Created by innok on 1/18/2016.
 */
public class UserAccount {


    private int accountId;
    private String accountName;
    private String personName;


    public UserAccount(String email) {
        this.accountName = email;
    }

    public void setAccountId (int accountId) {
        this.accountId = accountId;
    }

    public int getAccountId() {
        return accountId;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setPersonName(String personName) {
        this.personName = personName;
    }

    public String getPersonName() {
        return personName;
    }
}
