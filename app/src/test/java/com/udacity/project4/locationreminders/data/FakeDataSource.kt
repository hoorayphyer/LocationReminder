package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource( private val reminders : MutableList<ReminderDTO>) : ReminderDataSource {
    var shouldReturnError = false

    // Create a fake data source to act as a double to the real data source

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        if (shouldReturnError) {
            return Result.Error("Test exception")
        }
        return Result.Success(reminders)
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        if (shouldReturnError) {
            return Result.Error("Test exception")
        }
        val res = reminders.filter{
            it.id == id
        }
        return if (res.isEmpty()) {
            Result.Error("No such element with id $id found")
        } else if (res.size != 1 ) {
            Result.Error("More than one element with id $id")
        } else {
            Result.Success(res[0])
        }
    }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }


}