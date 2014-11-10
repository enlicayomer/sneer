package sneer.android.test;

import rx.Observable;
import rx.functions.Action1;
import sneer.android.ui.MainActivity;
import android.test.ActivityInstrumentationTestCase2;

import com.robotium.solo.Solo;

public class MainTest extends ActivityInstrumentationTestCase2<MainActivity> {

	private Solo solo;

	public MainTest() {
		super(MainActivity.class);
	}

	@Override
	public void setUp() throws Exception {
		// setUp() is run before a test case is started.
		// This is where the solo object is created.
		solo = new Solo(getInstrumentation(), getActivity());
	}

	@Override
	public void tearDown() throws Exception {
		// tearDown() is run after a test case has finished.
		// finishOpenedActivities() will finish all the activities that have
		// been opened during the test execution.
		solo.finishOpenedActivities();
	}

	public void testAll() throws Exception {
		// Unlock the lock screen
		solo.unlockScreen();

		solo.clickOnMenuItem("Advanced");
		// Assert that SystemReportActivity activity is opened
		solo.assertCurrentActivity("Expected SystemReport activity", "SystemReportActivity");

		goToAndAssert("MainActivity");

		solo.clickOnButton("Add Contact");
		solo.clickOnButton("Send Public Key");

		goToAndAssert("MainActivity");

		// Go to ConversationActivity
		solo.clickInList(1);

		sendSomeMessages();

		goToAndAssert("MainActivity");

		solo.clickOnActionBarHomeButton();
		solo.assertCurrentActivity("Expected ProfileActivity activity", "ProfileActivity");
		solo.clearEditText(0);
		solo.typeText(0, "New User");
		solo.clearEditText(1);
		solo.typeText(1, "New Nick");
		solo.clearEditText(2);
		solo.typeText(2, "New City");
		solo.clearEditText(3);
		solo.typeText(3, "New Country");
		goToAndAssert("MainActivity");

		// Go to ConversationActivity
		solo.clickInList(2);

		// send location
		solo.clickOnImageButton(0);
		solo.clickInList(2);

		// Open location
		solo.clickInList(1);
	}


	private void goToAndAssert(String activity) {
		solo.goBackToActivity(activity);
		solo.assertCurrentActivity("Expected MainActivity activity", "MainActivity");
	}


	private void sendSomeMessages() {
		Observable.range(1, 3).subscribe(new Action1<Integer>() { @Override public void call(Integer t1) {
			solo.typeText(0, "This is a test " + t1);
			solo.clickOnImageButton(0);
		}});
	}

}
