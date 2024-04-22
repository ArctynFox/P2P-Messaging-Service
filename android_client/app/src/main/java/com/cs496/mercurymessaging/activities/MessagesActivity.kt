package com.cs496.mercurymessaging.activities

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cs496.mercurymessaging.App
import com.cs496.mercurymessaging.R
import com.cs496.mercurymessaging.database.MercuryDB
import com.cs496.mercurymessaging.database.MercuryDB.Companion.createDB
import com.cs496.mercurymessaging.database.tables.Message
import com.cs496.mercurymessaging.database.tables.User
import com.cs496.mercurymessaging.databinding.ActivityMessagesBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MessagesActivity : AppCompatActivity() {
    lateinit var binding: ActivityMessagesBinding
    lateinit var db: MercuryDB
    lateinit var user: User
    private lateinit var messagesRecyclerView: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMessagesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //initialize the SQLite DB
        if(MercuryDB.db == null) {
            MercuryDB.db = createDB(this)
        }

        App.messagesActivity = this

        displayMessages()
    }

    override fun onDestroy() {
        super.onDestroy()

        App.messagesActivity = null
    }

    fun onSendClick(v: View) {
        //read from the textbox and send it as a message
        var text: String = binding.messageEdit.text.toString()
        if(text == "" || text == "N/A") {
            Toast.makeText(this, "This message is not valid.", Toast.LENGTH_SHORT).show()
            return
        }
        val timestamp = System.currentTimeMillis()

        val message = Message(user, true, text, timestamp)

        db.addMessage(message)

        App.peerSocketContainerHashMap
    }

    //fill the recyclerView with user entries
    public fun displayMessages() {
        val messages = MercuryDB.db!!.getMessages(user)
        val llm = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        llm.stackFromEnd = true
        messagesRecyclerView.layoutManager = llm
        messagesRecyclerView.adapter = ItemAdapter(messages)
    }

    //item adapter to fill each user_recycle_item with a user's info
    class ItemAdapter(private val userList: List<Message>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
        //inflates the layout of each recycle item
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.user_recycle_item, parent, false)
            return ItemViewHolder(view)
        }

        override fun getItemCount(): Int {
            return userList.size
        }

        //binds UI views to variables
        inner class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val timestamp: TextView = itemView.findViewById(R.id.messageTimestamp)
            val message: TextView = itemView.findViewById(R.id.message)
            val background: ImageView = itemView.findViewById(R.id.background)
        }

        //using the bindings above, fill the given view holder with the respective data
        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = userList[position]
            holder.itemView.tag = item.id
            holder.message.text = item.text
            val sdf = SimpleDateFormat("hh:mm:ss a - MMM dd, yyyy", Locale.getDefault())
            val tzOffset = TimeZone.getDefault().getOffset(Date().time)
            holder.timestamp.text = sdf.format(item.timestamp + tzOffset)

            if(item.isAuthor) {
                holder.background.setBackgroundColor(Color.parseColor("#93fa9a"))
            } else {
                holder.background.setBackgroundColor(Color.parseColor("#ffb0ad"))
            }
        }
    }
}