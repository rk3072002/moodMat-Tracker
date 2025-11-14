package com.example.facemate

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.android.gms.auth.api.signin.*
import com.google.firebase.auth.*
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var email: EditText
    private lateinit var password: EditText
    private lateinit var btnLogin: Button
    private lateinit var googleBtn: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var firestore: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private val GOOGLE_SIGN_IN_CODE = 101




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Already logged in
        if (FirebaseAuth.getInstance().currentUser != null) {
            goToNext()
            return
        }

        setContentView(R.layout.activity_login)

        // Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Users")
        firestore = FirebaseFirestore.getInstance()

        // Views
        email = findViewById(R.id.userEmail)
        password = findViewById(R.id.userPassword)
        btnLogin = findViewById(R.id.Login)
        googleBtn = findViewById(R.id.google)



        // Google Sign-In config
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnLogin.setOnClickListener {
            val emailText = email.text.toString().trim()
            val passText = password.text.toString().trim()

            if (emailText.isNotEmpty() && passText.isNotEmpty()) {
                auth.signInWithEmailAndPassword(emailText, passText)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show()
                        goToNext()
                    }
                    .addOnFailureListener {
                        // Try Register
                        auth.createUserWithEmailAndPassword(emailText, passText)
                            .addOnSuccessListener {
                                val user = auth.currentUser
                                val userData = mapOf(
                                    "uid" to user!!.uid,
                                    "email" to emailText
                                )
                                // Save to Realtime Database
                                database.child(user.uid).setValue(userData)
                                // Save to Firestore
                                firestore.collection("users")
                                    .document(user.uid)
                                    .set(userData)

                                Toast.makeText(this, "User Registered & Logged In", Toast.LENGTH_SHORT).show()
                                goToNext()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                Log.e("LOGIN_ERROR", e.message.toString())
                            }
                    }
            } else {
                Toast.makeText(this, "Email aur Password daalo", Toast.LENGTH_SHORT).show()
            }
        }

        googleBtn.setOnClickListener {
            val intent = googleSignInClient.signInIntent
            startActivityForResult(intent, GOOGLE_SIGN_IN_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == GOOGLE_SIGN_IN_CODE) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            if (task.isSuccessful) {
                val account = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)

                auth.signInWithCredential(credential)
                    .addOnSuccessListener {
                        val user = auth.currentUser
                        val userData = mapOf(
                            "uid" to user!!.uid,
                            "email" to user.email
                        )
                        // Save to Realtime Database
                        database.child(user.uid).setValue(userData)
                        // Save to Firestore
                        firestore.collection("users")
                            .document(user.uid)
                            .set(userData)

                        Toast.makeText(this, "Google Login Success", Toast.LENGTH_SHORT).show()
                        goToNext()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Google Login Failed", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun goToNext() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
