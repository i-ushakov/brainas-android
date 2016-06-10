package net.brainas.android.app.infrustructure;

import android.content.Context;
import android.support.test.runner.AndroidJUnit4;
import static android.support.test.InstrumentationRegistry.getTargetContext;
import android.test.InstrumentationTestCase;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.Before;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;


/**
 * Created by Kit Ushakov on 4/28/2016.
 */
@RunWith(AndroidJUnit4.class)
public class UserAccountDbHelperFTest extends InstrumentationTestCase {
    private static String TAG = "UserAccountDbHelperFTest";
    UserAccountDbHelper userAccountDbHelper;

    UserAccountDbHelper userAccountDbHelperSpy;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Context context = getTargetContext();
        context.deleteDatabase(UserAccountDbHelper.TABLE_USER_ACCOUNTS);
        AppDbHelper appDbHelper = new AppDbHelper(context);
        userAccountDbHelper = new UserAccountDbHelper(appDbHelper);
    }

    @After
    public void tearDown() throws Exception {
        //userAccountDbHelper.close();
    }

    @Test
    public void testUpdateOrCreate () {
        String accountName = "kit.ushakov@gmail.com";
        UserAccount userAccount = new UserAccount(accountName);
        userAccount.setPersonName("Test Testov");
        userAccount.setAccessCode("A-c-cesS_Code");
        userAccountDbHelper.saveUserAccount(userAccount);
        UserAccount actualUserAccount = userAccountDbHelper.retrieveUserAccountFromDB(accountName);
        assertEquals(userAccount.getAccountName(), actualUserAccount.getAccountName());
        assertEquals("A-c-cesS_Code", actualUserAccount.getAccessCode());

        userAccount.setAccessCode("A-c-cesS_Code2");
        userAccountDbHelper.saveUserAccount(userAccount);
        actualUserAccount = userAccountDbHelper.retrieveUserAccountFromDB(accountName);
        assertEquals("A-c-cesS_Code2", actualUserAccount.getAccessCode());
    }
}
