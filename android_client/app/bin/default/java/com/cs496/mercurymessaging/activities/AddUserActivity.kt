package com.cs496.mercurymessaging.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cs496.mercurymessaging.App
import com.cs496.mercurymessaging.database.MercuryDB.Companion.db
import com.cs496.mercurymessaging.database.tables.User
import com.cs496.mercurymessaging.databinding.ActivityAddUserBinding

class AddUserActivity: AppCompatActivity() {
    lateinit var binding: ActivityAddUserBinding
    var tag: String = this.javaClass.name

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    fun onAddClick(v: View) {
        //check if the entered values are valid; if not, inform the of user what is wrong
        if(binding.hashEdit.text.equals("")) {
            Toast.makeText(this, "Nickname field must not be empty.", Toast.LENGTH_SHORT).show()
        } else if (binding.nicknameEdit.text.length != 32){
            Toast.makeText(this, "Hash field must be 32 characters.", Toast.LENGTH_SHORT).show()
        } else {
            //add a user entry
            val hash = binding.hashEdit.text.toString()
            val user = User(hash, binding.nicknameEdit.text.toString(), false, 0)
            db?.addUser(user)

            //ask the server for the user's IP
            Log.d(tag, "$hash entered, asking central server for IP.")
            App.serverConnection.send("facilitateConnection")
            App.serverConnection.send(hash)
            //the response is handled in ServerConnection
        }
    }
}