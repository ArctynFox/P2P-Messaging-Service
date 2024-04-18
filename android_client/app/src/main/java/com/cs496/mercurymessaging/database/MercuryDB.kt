package com.cs496.mercurymessaging.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [User::class, Message::class], version = 1)
abstract class MercuryDB : RoomDatabase() {
    companion object {
        lateinit var db: MercuryDB

        //gets reference to database and initializes it if it isn't already
        fun createDB(context: Context): MercuryDB {
            db = Room.databaseBuilder(context, MercuryDB::class.java, "mercury").allowMainThreadQueries().build()
            db.initialize()

            return db
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
        return messageDao().addMessage(message)
    }

    //get all messages with a specific user
    fun getMessages(user: User): List<Message> {
        return messageDao().getUserMessages(user)
    }

    //insert user
    fun addUser(user: User): Long {
        return userDao().addUser(user)
    }

    //update user
    fun updateUser(id: Long, ip: String, nickname: String, isConnected: Boolean): Long {
        return userDao().updateUser(id, ip, nickname, isConnected)
    }

    //get all users
    fun getUsers(): List<User> {
        return userDao().getAllUsers()
    }

    fun getUserByID(id: Long): User {
        return userDao().getUser(id)
    }
}