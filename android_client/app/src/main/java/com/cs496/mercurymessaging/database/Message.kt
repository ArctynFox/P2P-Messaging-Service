package com.cs496.mercurymessaging.database

import androidx.annotation.NonNull
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.io.File

@Entity(tableName = "messages")
class Message (
    //TODO: make the user reference a foreign key
    var user: User,
    var isAuthor: Boolean,
    var text: String,
    var timeStamp: Long,
    var file: File? = null
        ) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
}