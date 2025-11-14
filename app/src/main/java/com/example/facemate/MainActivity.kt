package com.example.facemate

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.facemate.fragment.Feed
import com.example.facemate.fragment.History
import com.example.facemate.fragment.Mood
import com.example.facemate.fragment.QuoteFragment
import com.example.facemate.fragment.Stats
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    //initialize bottomNavigation
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        //Initialize Firebase Authentication and sign in user without email and password
        FirebaseAuth.getInstance().signInAnonymously()

        bottomNavigationView = findViewById(R.id.bottom_nav)
        loadFragment(Mood())

        //your existing bottom nav setup code

        bottomNavigationView.setOnItemSelectedListener {
            when(it.itemId){
                R.id.mood -> loadFragment(Mood())
                R.id.feed -> loadFragment(Feed())
                R.id.history -> loadFragment(History())
                R.id.stats -> loadFragment(Stats())
                R.id.quote -> loadFragment(QuoteFragment())
            }
            true
        }

       //setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
        setSupportActionBar(toolbar)
        Log.d("MainActivity", "onOptionsItemSelected:")


    }
// load or initialize fragment main activity
    private fun loadFragment(fragment: Fragment) {
supportFragmentManager.beginTransaction()
    .replace(R.id.fragment_container, fragment).commit()
    }

    //show menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        val menuItem = menu?.findItem(R.id.notification)
        val actionView = menuItem?.actionView
        val badgeTextView = actionView?.findViewById<TextView>(R.id.tv_badge_count)

        //  Set the count (example: 3)
        fetchUnreadQuoteCount { count ->
            if (count > 0) {
                badgeTextView?.text = count.toString()
                badgeTextView?.visibility = View.VISIBLE
            } else {
                badgeTextView?.visibility = View.GONE
            }
        }

        //  Click listener to handle as menu item
        actionView?.setOnClickListener {
            onOptionsItemSelected(menuItem)
        }
        return true
    }
    // Handle icon click
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.menu_settings ->{
                startActivity(Intent(this, SettingFM::class.java))
                true
            }
            R.id.notification -> {
                //  Load QuoteFragment on notification click
                loadFragment(QuoteFragment())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }


    }
    private fun fetchUnreadQuoteCount(callback: (Int) -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("quotes")

        ref.get().addOnSuccessListener { snapshot ->
            var count = 0
            for (child in snapshot.children) {
                val author = child.child("author").getValue(String::class.java)
                if (author != null && author != FirebaseAuth.getInstance().currentUser?.email?.replace(".", "_")) {
                    val isRead = child.child("isReadBy").child(uid).getValue(Boolean::class.java) ?: false
                    if (!isRead) count++
                }
            }
            callback(count)
        }.addOnFailureListener {
            callback(0)
        }
    }

    fun updateNotificationBadge(count: Int) {

        val toolbar = findViewById<Toolbar>(R.id.main_toolbar)
        val menuItem = toolbar.menu.findItem(R.id.notification)
        val actionView = menuItem?.actionView
        val badgeTextView = actionView?.findViewById<TextView>(R.id.tv_badge_count)

        if (count > 0) {
            badgeTextView?.text = count.toString()
            badgeTextView?.visibility = View.VISIBLE
        } else {
            badgeTextView?.visibility = View.GONE
        }
    }


}