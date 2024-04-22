package com.cs496.mercurymessaging.activities

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
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
    private lateinit var prefs: SharedPreferences
    var tag: String = "MainActivity"
    var perms = arrayOf(
        "android.permission.POST_NOTIFICATIONS"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if(!MercuryService.isRunning) {
            Log.d(tag,"Starting Mercury Service.")
            val mercuryServiceIntent = Intent(this, MercuryService::class.java)
            startService(mercuryServiceIntent)
        }

        App.mainActivity = this

        prefs = getSharedPreferences("mercury", Context.MODE_PRIVATE)

        //initialize the SQLite DB
        if(db == null) {
            db = createDB(this)
        }

        recyclerView = binding.recyclerView
        displayUserList()
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

        App.mainActivity = null;
    }

    //fill the recyclerView with user entries
    public fun displayUserList() {
        val users = db!!.getUsers()
        val llm = LinearLayoutManager(this)
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
            val layout: ConstraintLayout = itemView.findViewById(R.id.userRecycleLayout)
        }

        //using the bindings above, fill the given view holder with the respective data
        override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
            val item = userList[position]
            holder.itemView.tag = item.hash
            holder.username.text = item.nickname

            holder.layout.setOnClickListener {
                App.mainActivity.prefs.edit().putString("user", item.hash).commit()
                if(db == null) {
                    db = createDB(holder.username.context)
                }
                val intent = Intent(holder.username.context, MessagesActivity::class.java)
                holder.username.context.startActivity(intent)
            }
        }
    }

    fun onAddUserClick(v: View) {
        //go to add user screen
        val intent = Intent(this, AddUserActivity::class.java)
        startActivity(intent)
    }
}