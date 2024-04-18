package com.cs496.mercurymessaging.database

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.io.File

@Entity(tableName = "messages", foreignKeys = [ForeignKey(entity = User::class, parentColumns = arrayOf("id"), childColumns = arrayOf("user"), onDelete = ForeignKey.CASCADE)])
class Message (
    //TODO: make the user reference a foreign key
    var user: User,
    var isAuthor: Boolean,
    var text: String,
    var timestamp: Long,
    var file: File? = null
        ) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}