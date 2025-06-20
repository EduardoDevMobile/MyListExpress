package com.example.mylistexpress

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddProductActivity : AppCompatActivity() {
    private var selectedImageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 1
    private val TAKE_PHOTO_REQUEST = 2
    private var photoUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        val productNameInput = findViewById<EditText>(R.id.productNameInput)
        val categorySpinner = findViewById<Spinner>(R.id.categorySpinner)
        val saveProductButton = findViewById<Button>(R.id.saveProductButton)
        val selectPhotoButton = findViewById<Button>(R.id.selectPhotoButton)
        val productImageView = findViewById<ImageView>(R.id.productImageView)

        val categories = listOf("Verduras", "Carnes", "Lácteos", "Bebidas", "Otros")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        selectPhotoButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        saveProductButton.setOnClickListener {
            val productName = productNameInput.text.toString()
            val category = categorySpinner.selectedItem.toString()

            if (productName.isNotEmpty()) {
                val resultIntent = Intent()
                resultIntent.putExtra("productName", productName)
                resultIntent.putExtra("category", category)
                selectedImageUri?.let { resultIntent.putExtra("imageUri", it.toString()) }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Por favor, ingresa el nombre del producto", Toast.LENGTH_SHORT).show()
            }
        }

        val takePhotoButton = findViewById<Button>(R.id.takePhotoButton)
        takePhotoButton.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
            } else {
                openCamera()
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val photoFile = createImageFile()
        photoFile?.also {
            photoUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", it)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(intent, TAKE_PHOTO_REQUEST)
        }
    }

    private fun createImageFile(): File? {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 10 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            findViewById<ImageView>(R.id.productImageView).setImageURI(selectedImageUri)
        } else if (requestCode == TAKE_PHOTO_REQUEST && resultCode == Activity.RESULT_OK) {
            selectedImageUri = photoUri
            findViewById<ImageView>(R.id.productImageView).setImageURI(selectedImageUri)
        }
    }
}