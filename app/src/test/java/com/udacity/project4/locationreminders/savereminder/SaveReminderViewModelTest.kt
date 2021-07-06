package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {
    // provide testing to the SaveReminderView and its live data objects

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Test
    fun validateEnteredData() {
        // Given a fresh ViewModel
        val reminders = mutableListOf<ReminderDTO>()

        val dataSource = FakeDataSource(reminders)
        val viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), dataSource)

        // invalid reminder
        val invalidReminder = ReminderDataItem("", "", "", 0.0, 0.0)
        assertThat( viewModel.validateEnteredData(invalidReminder), `is`(false))

        // valid reminder
        val validReminder = ReminderDataItem("title", "", "location", 0.0, 0.0)
        assertThat( viewModel.validateEnteredData(validReminder), `is`(true))
    }

    @Test
    fun saveReminder() {
        // Given a fresh ViewModel
        val reminders = mutableListOf<ReminderDTO>()
        val viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), FakeDataSource(reminders))

        val reminder = ReminderDataItem("title", "", "location", 0.0, 0.0)
        require(viewModel.validateEnteredData(reminder))

        // TODO this is a coroutine call
        // viewModel.saveReminder(reminder)

//        val dataSource = viewModel.dataSource.getOrAwaitValue()
//        assertThat( dataSource.size, `is`(1) )

    }

    @Test
    fun clearSelectedReminder() {
        val reminders = mutableListOf<ReminderDTO>()
        val viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext(), FakeDataSource(reminders))

        viewModel.reminderTitle.value = "title"
        viewModel.reminderDescription.value = "description"
        viewModel.reminderSelectedLocationStr.value = "location"
        viewModel.latitude.value = 0.0
        viewModel.longitude.value = 0.0

        viewModel.onClear()

        viewModel.apply {
            assertThat(reminderTitle.getOrAwaitValue(), nullValue())
            assertThat(reminderDescription.getOrAwaitValue(), nullValue())
            assertThat(reminderSelectedLocationStr.getOrAwaitValue(), nullValue())
            assertThat(latitude.getOrAwaitValue(), nullValue())
            assertThat(longitude.getOrAwaitValue(), nullValue())
        }
    }
}