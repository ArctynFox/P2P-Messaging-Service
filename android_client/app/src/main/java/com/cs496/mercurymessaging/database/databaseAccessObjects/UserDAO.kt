package com.cs496.mercurymessaging.database.databaseAccessObjects

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cs496.mercurymessaging.database.tables.User

@Dao
interface UserDAO {
    //get a list of all the known users
    @Query("SELECT * FROM users ORDER BY nickname")
    fun getAllUsers(): List<User>

    //get a user by hash
    @Query("SELECT * FROM users WHERE hash = :hash")
    fun getUserByHash(hash: String): User

    //check if a user exists (0 if no, anything else if yes)
    @Query("SELECT COUNT(*) FROM users WHERE hash = :hash")
    fun userExists(hash: String): Long

    //add a user
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addUser(user: User): Long

    //update a user entry
    @Query("UPDATE users SET nickname = :nickname, isConnected = :isConnected, timestamp = :timestamp WHERE hash = :hash")
    fun updateUser(hash: String, nickname: String, isConnected: Boolean, timestamp: Long): Int

    //delete a user
    @Delete
    fun deleteUser(user: User)
}