package com.cs496.mercurymessaging.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cs496.mercurymessaging.App
import com.cs496.mercurymessaging.R
import com.cs496.mercurymessaging.database.MercuryDB.Companion.createDB
import com.cs496.mercurymessaging.database.MercuryDB.Companion.db
import com.cs496.mercurymessaging.database.tables.User
import com.cs496.mercurymessaging.databinding.ActivityMainBinding
import com.cs496.mercurymessaging.networking.MercuryService

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var recyclerView: RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mercuryServiceIntent = Intent(this, MercuryService::class.java)
        startService(mercuryServiceIntent)

        App.mainActivity = this

        //initialize the SQLite DB
        if(db == null) {
            db = createDB(this)
        }

        displayUserList()
    }

    override fun onDestroy() {
        super.onDestroy()

        App.mainActivity = null;
    }

    //fill the recyclerView with user entries
    public fun displayUserList() {
        val users = db!!.getUsers()
        val llm = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        llm.stackFromEnd = true
        recyclerView.layoutManager = llm
        recyclerView.adapter = ItemAdapter(users)
    }

    //item adapter to fill each user_recycle_item with a user's info
    class ItemAdapter(private val userList: List<User>) : RecyclerView.Adapter<ItemAdapter.ItemViewHolder>() {
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
            val username: TextView = itemView.findViewById(R.id.user_name)
        }

        //using the bindings above, fill the given view holder with the respective data
        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = userList[position]
            holder.itemView.tag = item.hash
            holder.username.text = item.nickname
        }
    }

    fun onAddUserClick(v: View) {
        //go to add user screen
        val intent = Intent(this, AddUserActivity::class.java)
        startActivity(intent)
    }
}