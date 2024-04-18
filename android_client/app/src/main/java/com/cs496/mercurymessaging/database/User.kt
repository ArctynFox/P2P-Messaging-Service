package com.cs496.mercurymessaging.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
class User (
    var hash: String = "",
    var ip: String = "0.0.0.0",
    var nickname: String = "",
    var isConnected: Boolean = false
        ) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}