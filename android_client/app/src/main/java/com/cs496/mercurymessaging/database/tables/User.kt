package com.cs496.mercurymessaging.database.tables

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
class User (
    @PrimaryKey
    var hash: String = "",
    var nickname: String = "",
    var isConnected: Boolean = false,
    var timestamp: Long = Long.MAX_VALUE
)