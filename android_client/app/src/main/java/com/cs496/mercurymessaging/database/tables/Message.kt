package com.cs496.mercurymessaging.database.tables

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(tableName = "messages", foreignKeys = [ForeignKey(entity = User::class, parentColumns = arrayOf("hash"), childColumns = arrayOf("hash"), onDelete = ForeignKey.CASCADE)])
class Message (
    var hash: String,
    var isAuthor: Boolean,
    var text: String,
    var timestamp: Long
        ) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}