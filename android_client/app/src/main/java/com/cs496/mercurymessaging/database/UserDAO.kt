package com.cs496.mercurymessaging.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserDAO {
    //get a list of all the known users
    @Query("SELECT * FROM users")
    fun getAllUsers(): List<User>

    @Query("SELECT * FROM users WHERE id = :id")
    fun getUser(id: Long): User

    //add a user
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addUser(user: User): Long

    //update a user entry
    @Query("UPDATE users SET ip = :ip, nickname = :nickname, isConnected = :isConnected WHERE id = :id")
    fun updateUser(id: Long, ip: String, nickname: String, isConnected: Boolean): Long

    //delete a user
    @Delete
    fun deleteUser(user: User)
}