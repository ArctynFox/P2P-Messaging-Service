package com.cs496.mercurymessaging.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cs496.mercurymessaging.database.databaseAccessObjects.MessageDAO
import com.cs496.mercurymessaging.database.databaseAccessObjects.UserDAO
import com.cs496.mercurymessaging.database.tables.Message
import com.cs496.mercurymessaging.database.tables.User

@Database(entities = [User::class, Message::class], version = 1)
abstract class MercuryDB : RoomDatabase() {
    companion object {
        @JvmField
        var db: MercuryDB? = null

        //gets reference to database and initializes it if it isn't already
        fun createDB(context: Context): MercuryDB {
            db = Room.databaseBuilder(context, MercuryDB::class.java, "mercury").allowMainThreadQueries().build()
            db!!.initialize()

            return db as MercuryDB
        }
    }

    //abstract methods to initialize the tables; functionality is auto-generated at compile-time
    abstract fun messageDao(): MessageDAO
    abstract fun userDao(): UserDAO

    //initializes the database access objects
    fun initialize() {
        messageDao()
        userDao()
    }

    //insert message
    fun addMessage(message: Message): Long {
        //updateUser(message.hash, message.nickname, message.user.isConnected, System.currentTimeMillis())
        return messageDao().addMessage(message)
    }

    //get all messages with a specific user
    fun getMessages(user: User): List<Message> {
        return messageDao().getUserMessages(user.hash)
    }

    fun getAllMessages(): List<Message> {
        return messageDao().getMessages()
    }

    //insert user
    fun addUser(user: User): Long {
        return userDao().addUser(user)
    }

    //update user
    fun updateUser(hash: String, nickname: String, isConnected: Boolean, timestamp: Long): Int {
        return userDao().updateUser(hash, nickname, isConnected, timestamp)
    }

    //get all users
    fun getUsers(): List<User> {
        return userDao().getAllUsers()
    }

    fun deleteUser(user: User) {
        return userDao().deleteUser(user);
    }

    fun getUserByHash(hash: String): User {
        return userDao().getUserByHash(hash)
    }

    fun doesUserExist(hash: String): Boolean {
        return userDao().userExists(hash) != 0L;
    }
}