package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Test

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun initDb() {
        // Using an in-memory database so that the information stored here disappears when the
        // process is killed.
        database = Room.inMemoryDatabaseBuilder(
            getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun insertReminderAndGetById() = runBlockingTest {
        // GIVEN - Insert a reminder.
        val reminder = ReminderDTO("title", "description", "location", 0.0, 0.0, "myId")
        database.reminderDao().saveReminder(reminder)

        // WHEN - Get the reminder by id from the database.
        val loaded = database.reminderDao().getReminderById("myId")

        // THEN - The loaded data contains the expected values.
        assertThat(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminder.id))
        assertThat(loaded.title, `is`(reminder.title))
        assertThat(loaded.description, `is`(reminder.description))
        assertThat(loaded.location, `is`(reminder.location))
        assertThat(loaded.latitude, `is`(reminder.latitude))
        assertThat(loaded.longitude, `is`(reminder.longitude))
    }


    @Test
    fun updateReminderAndGetById() = runBlockingTest {
        // 1. Insert a reminder into the DAO.
        // 2. Update the reminder by creating a new reminder with the same ID but different attributes.
        // 3. Check that when you get the task by its ID, it has the updated values.


        // GIVEN - Insert a reminder.
        val reminder = ReminderDTO("title", "description", "location", 0.0, 0.0, "myId")
        database.reminderDao().saveReminder(reminder)

        val reminderNew = ReminderDTO("title_new", "description_new", "location_new", 1.0, 1.0, "myId")
        database.reminderDao().saveReminder(reminderNew)

        // WHEN - Get the reminder by id from the database.
        val loaded = database.reminderDao().getReminderById("myId")

        // THEN - The loaded data contains the expected values.
        assertThat(loaded as ReminderDTO, notNullValue())
        assertThat(loaded.id, `is`(reminderNew.id))
        assertThat(loaded.title, `is`(reminderNew.title))
        assertThat(loaded.description, `is`(reminderNew.description))
        assertThat(loaded.location, `is`(reminderNew.location))
        assertThat(loaded.latitude, `is`(reminderNew.latitude))
        assertThat(loaded.longitude, `is`(reminderNew.longitude))
    }
}