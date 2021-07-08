package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.viewModelScope
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.getOrAwaitValue
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.data.dto.Result

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest: AutoCloseKoinTest() {
    // provide testing to the SaveReminderView and its live data objects
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val application = ApplicationProvider.getApplicationContext() as Application

    @Test
    fun validateEnteredData() {
        // Given a fresh ViewModel
        val reminders = mutableListOf<ReminderDTO>()

        val dataSource = FakeDataSource(reminders)
        val viewModel = SaveReminderViewModel(application, dataSource)

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
        val fakeDataSource = FakeDataSource(reminders)
        val viewModel = SaveReminderViewModel(application, fakeDataSource)

        val reminder = ReminderDataItem("title", "", "location", 0.0, 0.0, "MyID")
        require(viewModel.validateEnteredData(reminder))

        viewModel.saveReminder(reminder)

        var reminderById : Result<ReminderDTO> = Result.Error("not initialized")

        viewModel.viewModelScope.launch {
            reminderById = fakeDataSource.getReminder("MyID")
        }

        assert(reminderById is Result.Success)
        (reminderById as Result.Success).data.apply{
            assertThat(title, `is`("title"))
            assertThat(description, `is`(""))
            assertThat(location, `is`("location"))
            assertThat(latitude, `is`(0.0))
            assertThat(longitude, `is`(0.0))
            assertThat(id, `is`("MyID"))
        }
    }

    @Test
    fun clearSelectedReminder() {
        val reminders = mutableListOf<ReminderDTO>()
        val viewModel = SaveReminderViewModel(application, FakeDataSource(reminders))

        viewModel.apply{
            reminderTitle.value = "title"
            reminderDescription.value = "description"
            reminderSelectedLocationStr.value = "location"
            latitude.value = 0.0
            longitude.value = 0.0
        }

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