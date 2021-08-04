package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {
    // provide testing to the RemindersListViewModel and its live data objects
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Test
    fun loadReminders_getsStoredReminders() {
        // Given a fresh ViewModel

        val reminders = MutableList(4) { index ->
            ReminderDTO(
                "title$index",
                "description$index",
                "location$index",
                index.toDouble(),
                -index.toDouble()
            )
        }

        val dataSource = FakeDataSource(reminders)
        val viewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)

        // Perform the action under testing
        viewModel.loadReminders()

        // Test results
        val value = viewModel.remindersList.getOrAwaitValue()
        assertThat(value.size, `is`(reminders.size))
        for (index in 0 until reminders.size) {
            assertThat(value[index].title, `is`("title$index"))
            assertThat(value[index].description, `is`("description$index"))
            assertThat(value[index].location, `is`("location$index"))
            assertThat(value[index].latitude, `is`(index.toDouble()))
            assertThat(value[index].longitude, `is`(-index.toDouble()))
        }
    }

    @Test
    fun loadReminders_checkLoading() {
        // Given a fresh ViewModel

        val reminders = MutableList(4) { index ->
            ReminderDTO(
                "title$index",
                "description$index",
                "location$index",
                index.toDouble(),
                -index.toDouble()
            )
        }

        val dataSource = FakeDataSource(reminders)
        val viewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)

        // Perform the action under testing
        // we want to test loading is shown before the coroutine starts, hence the pauseDispatcher()
        mainCoroutineRule.pauseDispatcher()
        viewModel.loadReminders()

        // Test results
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(true))

        // now we want to see that the loading goes away after the coroutine in viewModelloadReminders() finishes
        mainCoroutineRule.resumeDispatcher()
        assertThat(viewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_getRemindersError() {
        // Given a fresh ViewModel

        val reminders = MutableList(4) { index ->
            ReminderDTO(
                "title$index",
                "description$index",
                "location$index",
                index.toDouble(),
                -index.toDouble()
            )
        }

        val dataSource = FakeDataSource(reminders)
        // loadReminders() has a call to dataSource.getReminders(). Here we manually fake the behavior that dataSource.getReminders() throws errors
        dataSource.shouldReturnError = true
        val viewModel =
            RemindersListViewModel(ApplicationProvider.getApplicationContext(), dataSource)

        // Perform the action under testing
        viewModel.loadReminders()

        // Test results
        assertThat(viewModel.showSnackBar.getOrAwaitValue(), `is`("Test exception"))
    }
}