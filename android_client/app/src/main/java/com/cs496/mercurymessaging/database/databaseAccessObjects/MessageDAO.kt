package com.cs496.mercurymessaging.database.databaseAccessObjects

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cs496.mercurymessaging.database.tables.Message
import com.cs496.mercurymessaging.database.tables.User

@Dao
interface MessageDAO {
    //get a list of all messages with the specified user
    @Query("SELECT * FROM messages WHERE user = :user")
    fun getUserMessages(user: User): List<Message>

    //create a message
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addMessage(message: Message): Long

    //delete a message
    @Delete
    fun deleteMessage(message: Message)
}