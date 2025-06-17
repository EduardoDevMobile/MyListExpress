package com.example.mylistexpress

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddProductActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_product)

        val productNameInput = findViewById<EditText>(R.id.productNameInput)
        val categorySpinner = findViewById<Spinner>(R.id.categorySpinner)
        val saveProductButton = findViewById<Button>(R.id.saveProductButton)

        val categories = listOf("Verduras", "Carnes", "Lácteos", "Bebidas", "Otros")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        saveProductButton.setOnClickListener {
            val productName = productNameInput.text.toString()
            val category = categorySpinner.selectedItem.toString()

            if (productName.isNotEmpty()) {
                Toast.makeText(this, "Producto guardado: $productName ($category)", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Por favor, ingresa el nombre del producto", Toast.LENGTH_SHORT).show()
            }
        }
    }
}