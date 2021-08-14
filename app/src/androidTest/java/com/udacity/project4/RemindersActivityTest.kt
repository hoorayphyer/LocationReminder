package com.udacity.project4

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorActivity
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.not
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get


@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    // An idling resource that waits for Data Binding to have no pending bindings.
    private val dataBindingIdlingResource = DataBindingIdlingResource()
    /**
     * Idling resources tell Espresso that the app is idle or busy. This is needed when operations
     * are not scheduled in the main Looper (for example when executed on a different thread).
     */
    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    /**
     * Unregister your Idling Resource so it can be garbage collected and does not leak any memory.
     */
    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    // End to End testing to the app
    @Test
    fun addReminder_ShowSnackbarWhenNoTitle() = runBlocking {
        // Set initial state.

        // Start up Reminders screen.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario) // LOOK HERE

        // Espresso code will go here.
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())

        // the following checks that a snackbar message is shown when saving without a title
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_enter_title)))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }

    // End to End testing to the app
    @Test
    fun addReminder_ShowSnackbarWhenNoLocation() = runBlocking {
        // Set initial state.

        // Start up Reminders screen.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario) // LOOK HERE

        // Espresso code will go here.
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())

        // the following checks that a snackbar message is shown when saving without a location
        onView(withId(R.id.reminderTitle)).perform(replaceText("new title"))
        onView(withId(R.id.saveReminder)).perform(click())
        onView(withId(com.google.android.material.R.id.snackbar_text)).check(matches(withText(R.string.err_select_location)))

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }

    // End to End testing to the app
    @Test
    fun addReminder_Successful() = runBlocking {
        // Set initial state.

        // Start up Reminders screen.
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario) // LOOK HERE

        // Espresso code will go here.
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
        onView(withId(R.id.addReminderFAB)).perform(click())

        onView(withId(R.id.reminderTitle)).perform(replaceText("new title"))
        onView(withId(R.id.reminderDescription)).perform(replaceText("new description"))
        onView(withId(R.id.selectLocation)).perform(click())

        onView(withId(R.id.google_map)).perform(longClick())

        onView(withId(R.id.confirmButton)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())

        // TODO this is a hack to make the following check work. I'm missing some IdlingResouces deployment somewhere around long time operations. I tried looking at the clickListener in the saveReminder button but couldn't identify any. I suspect those services were involved but I don't know how to confirm that.
        Thread.sleep(10000)

        // There is a post that I think may be relevant: https://medium.com/android-news/espresso-ui-test-for-data-binding-dbe988d97340
        onView(withId(R.id.noDataTextView)).check(matches(not(isDisplayed())))

        // the following checks that a TOAST message is shown
        // TODO this is supposed to be a toast message test, but when run it hangs. Don't know why. One difficulty is how to get the activity from activityScenario. The following way is what I found online.
//        activityScenario.onActivity {
//           onView(withText(R.string.reminder_saved)).inRoot(withDecorView(not(`is`(it.window.decorView)))).check(matches(isDisplayed()));
//        }

        // Make sure the activity is closed before resetting the db:
        activityScenario.close()
    }
}
