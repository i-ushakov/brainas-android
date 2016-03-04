package net.brainas.android.app.infrustructure;

/**
 * Created by innok on 1/18/2016.
 */
public class UserAccount {


    private int localAccountId;
    private String accountName;
    private String personName;
    private String accessCode = null;


    public UserAccount(String email) {
        this.accountName = email;
    }

    public void setLocalAccountId(int localAccountId) {
        this.localAccountId = localAccountId;
    }

    public int getLocalAccountId() {
        return localAccountId;
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

    public void setAccessCode(String accessToken) {
        this.accessCode = accessToken;
    }

    public String getAccessCode() {
        return this.accessCode;
    }
}
