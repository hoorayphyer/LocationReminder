package com.udacity.project4.locationreminders.reminderslist

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {
    //    TODO: add testing for the error messages.
    @Test
    fun activeReminderList_NoDataBoxDisplayedInUi() {
        launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

    @Test
    fun navigateToAddReminderScreen() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(null, R.style.AppTheme)
        val navController = mock(NavController::class.java) // a mock navController

        // make scenario use the mock navController
        scenario.onFragment {
            Navigation.setViewNavController(it.view!!, navController)
        }

        onView(withId(R.id.addReminderFAB)).perform(click())

        // verify
        verify(navController).navigate(
            // this statement is from safeargs
            ReminderListFragmentDirections.toSaveReminder()
        )
    }

}