package com.team06.maca

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.team06.maca.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.loginButton.setOnClickListener {
            val username = binding.usernameEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                val intent = Intent(this, FetchActivity::class.java)
                intent.putExtra("USER_NAME", username) // Pass the actual username
                
                // Determine user type based on credentials (example logic)
                val userType = if (username.equals("free", ignoreCase = true) && password == "free") {
                    "Free User"
                } else {
                    "Premium User"
                }
                intent.putExtra("USER_TYPE", userType)
                
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
