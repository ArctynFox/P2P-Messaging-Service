package com.cs496.mercurymessaging.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.cs496.mercurymessaging.database.MercuryDB
import com.cs496.mercurymessaging.database.MercuryDB.Companion.createDB
import com.cs496.mercurymessaging.database.Message
import com.cs496.mercurymessaging.databinding.ActivityMessagesBinding

class MessagesActivity : AppCompatActivity() {
    lateinit var binding: ActivityMessagesBinding
    lateinit var db: MercuryDB
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = createDB(this)
    }

    fun onSendClick(v: View) {
        //read from the textbox and send it as a message
        var text: String = "Placeholder"
        var ip = "0.0.0.0"
        var hash = ""
        var timestamp = System.currentTimeMillis()

        //val message = Message(user, true, text, timestamp)

        //db.addMessage(message)
    }
}