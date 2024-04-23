package com.cs496.mercurymessaging.activities

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cs496.mercurymessaging.App
import com.cs496.mercurymessaging.R
import com.cs496.mercurymessaging.database.MercuryDB.Companion.createDB
import com.cs496.mercurymessaging.database.MercuryDB.Companion.db
import com.cs496.mercurymessaging.database.tables.Message
import com.cs496.mercurymessaging.database.tables.User
import com.cs496.mercurymessaging.databinding.ActivityMessagesBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MessagesActivity : AppCompatActivity() {
    lateinit var binding: ActivityMessagesBinding
    lateinit var user: User
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var prefs: SharedPreferences
    var tag: String = "MessagesActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //initialize the SQLite DB
        if(db == null) {
            db = createDB(this)
        }

        App.messagesActivity = this

        prefs = getSharedPreferences("mercury", Context.MODE_PRIVATE)
        val hash = prefs.getString("user", "")
        Log.d(tag, "Target user hash is $hash")
        if(hash != "") {
            user = db!!.getUserByHash(prefs.getString("user", "")!!)
        } else {
            finish()
        }

        binding.userID.text = user.nickname

        if(App.peerSocketContainerHashMap[hash] == null) {
            Thread {
                App.serverConnection.send("facilitateConnection")
                App.serverConnection.send(hash)
            }.start()
        }

        messagesRecyclerView = binding.messagesRecyclerView
        displayMessages()
    }

    override fun onResume() {
        super.onResume()
        //initialize the SQLite DB
        if(db == null) {
            db = createDB(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.edit().putString("user", "").apply()

        App.messagesActivity = null
    }

    fun onSendClick(v: View) {
        //read from the textbox and send it as a message
        var text: String = binding.messageEdit.text.toString()
        if(text == "" || text == "N/A") {
            Toast.makeText(this, "This message is not valid.", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            App.peerSocketContainerHashMap[user.hash]?.send(text)
        }.start()

        binding.messageEdit.setText("")
    }

    //fill the recyclerView with user entries
    fun displayMessages() {
        val messages = db!!.getMessages(user)
        for(message in messages) {
            Log.d(tag, "Message: ${message.text}; User: ${message.hash}; Timestamp: ${message.timestamp}")
        }
        val llm = LinearLayoutManager(this)
        llm.stackFromEnd = true
        messagesRecyclerView.layoutManager = llm
        messagesRecyclerView.adapter = ItemAdapter(messages)
    }

    //item adapter to fill each user_recycle_item with a user's info
    class ItemAdapter(private val messageList: List<Message>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
        //inflates the layout of each recycle item
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.message_recycle_item, parent, false)
            return ItemViewHolder(view)
        }

        override fun getItemCount(): Int {
            return messageList.size
        }

        //binds UI views to variables
        inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val timestamp: TextView = itemView.findViewById(R.id.messageTimestamp)
            val message: TextView = itemView.findViewById(R.id.message)
            val background: ImageView = itemView.findViewById(R.id.background)
        }

        //using the bindings above, fill the given view holder with the respective data
        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = messageList[position]
            holder.itemView.tag = item.id
            holder.message.text = item.text
            val sdf = SimpleDateFormat("hh:mm:ss a - MMM dd, yyyy", Locale.getDefault())
            holder.timestamp.text = sdf.format(item.timestamp)

            if(item.isAuthor) {
                holder.background.setBackgroundColor(Color.parseColor("#e6f2ff"))
            } else {
                holder.background.setBackgroundColor(Color.parseColor("#ffb0ad"))
            }
        }
    }
}