package com.example.facemate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.facemate.fragment.Mood
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import java.io.IOException


class SettingFM : AppCompatActivity() {

    private lateinit var switchDarkMode: Switch
    private lateinit var addYoga: ConstraintLayout
    private lateinit var btnClearData: ConstraintLayout
    private lateinit var btnAbout: ConstraintLayout
    private lateinit var btnLogout: ConstraintLayout
    private lateinit var imageViewPreview: ImageView
    private lateinit var cardView: CardView


    private val PICK_IMAGE_REQUEST = 101
    private var imageUri: Uri? = null

    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_setting_fm)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        switchDarkMode = findViewById(R.id.switchDarkMode)
        addYoga = findViewById(R.id.addYoga)
        btnClearData = findViewById(R.id.btnClearData)
        btnAbout = findViewById(R.id.btnAbout)
        btnLogout = findViewById(R.id.btnLogout)
        imageViewPreview = findViewById(R.id.image_view)
        cardView = findViewById(R.id.cardView)


        //bind your TextView

        val tvEmail = findViewById<TextView>(R.id.tv_email)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && !currentUser.isAnonymous) {
            val email = currentUser.email
            tvEmail.text = email ?: "No Email Found"
        } else {
            tvEmail.text = "Anonymous User"
        }


        cardView.setOnClickListener{
            intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
        //  Dark Mode Toggle
        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked)
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        //  Upload Image from Gallery
        addYoga.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }



        // 🔹 Clear Firestore Mood Data
        btnClearData.setOnClickListener {
            firestore.collection("MoodData").get()
                .addOnSuccessListener { snapshot ->
                    for (doc in snapshot.documents) {
                        doc.reference.delete()
                    }
                    Toast.makeText(this, "Mood Data Cleared", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to Clear Data", Toast.LENGTH_SHORT).show()
                }
        }

        // 🔹 Show About Toast
        btnAbout.setOnClickListener {

            showAboutDialog()
            //Toast.makeText(this, "This app uploads yoga images and tracks mood.", Toast.LENGTH_SHORT).show()
        }

        // 🔹 Firebase Logout
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Logged out successfully.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
            .setTitle("MoodMate v1.0")
            .setMessage("Create new scratch file from selection")
            .setPositiveButton("OK", null)
            .show()
    }

    // 🔹 Handle image result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            imageUri = data.data

            // Show image with Glide
            Glide.with(this).load(imageUri).into(imageViewPreview)

            // Upload to Cloudinary
            uploadImageToCloudinary(imageUri!!)
        }
    }
    // Upload to Cloudinary instead of Firebase
    private fun uploadImageToCloudinary(imageUri: Uri) {
        val contentResolver = contentResolver
        val inputStream = contentResolver.openInputStream(imageUri)
        val bytes = inputStream?.readBytes()
        if (bytes != null) {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file", "yoga_image.jpg",
                    RequestBody.create("image/*".toMediaTypeOrNull(), bytes)
                )
                // I am permission only educational image upload, like yoga, spiritual
                .addFormDataPart("upload_preset", "yoga_rohit")
                .build()

            //upload image my cloudinary db
            val request = Request.Builder()
                .url("https://api.cloudinary.com/v1_1/dagvwvx8g/image/upload")
                .post(requestBody)
                .build()

            val client = OkHttpClient()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        Toast.makeText(this@SettingFM, "Cloudinary Upload Failed", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {

                        val responseBody = response.body?.string()
                        val jsonObject = org.json.JSONObject(responseBody!!)
                        val imageUrl = jsonObject.getString("secure_url")

                        // you can save image url in firebase
                        val imageData = hashMapOf(
                            "imageUrl" to imageUrl,
                            "timestamp" to com.google.firebase.Timestamp.now()
                        )
                        FirebaseFirestore.getInstance().collection("yoga_images")
                            .add(imageData)
                            .addOnSuccessListener {
                                runOnUiThread {
                                    Toast.makeText(
                                        this@SettingFM,
                                        "Image uploaded & saved!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }   .addOnFailureListener {
                                runOnUiThread {
                                    Toast.makeText(this@SettingFM, "Upload ok, Firestore failed", Toast.LENGTH_SHORT).show()
                                }
}
//                                runOnUiThread {
//                            Toast.makeText(this@SettingFM, "Image uploaded to Cloudinary", Toast.LENGTH_SHORT).show()
//                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@SettingFM, "Cloudinary Error: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }
}
