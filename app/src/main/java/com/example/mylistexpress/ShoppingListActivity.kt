package com.example.mylistexpress

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mylistexpress.common.Product
import com.example.mylistexpress.common.ShoppingListAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ShoppingListActivity : AppCompatActivity() {
    private val shoppingList = mutableListOf<Product>()
    private val filteredList = mutableListOf<Product>()
    private lateinit var adapter: ShoppingListAdapter

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_list)
        val prefs = getSharedPreferences("MyListExpressPrefs", Context.MODE_PRIVATE)
        val userName = prefs.getString("userName", "Usuario")
        findViewById<TextView>(R.id.welcomeText).text = "Bienvenido, $userName"

        val rootLayout = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.rootLayout)

        val colors = listOf(0xFFE3F2FD.toInt(), 0xFFFFF9C4.toInt(), 0xFFFFEBEE.toInt())
        val bgIdx = prefs.getInt("bgColorIdx", 0)
        rootLayout.setBackgroundColor(colors[bgIdx])

        val changeBgButton = Button(this).apply { text = "Cambiar fondo" }
        rootLayout.addView(changeBgButton)
        changeBgButton.setOnClickListener {
            val idx = prefs.getInt("bgColorIdx", 0)
            val nextIdx = (idx + 1) % colors.size
            rootLayout.setBackgroundColor(colors[nextIdx])
            prefs.edit().putInt("bgColorIdx", nextIdx).apply()
        }

        loadShoppingList()
        filteredList.addAll(shoppingList)
        updateCounters()
        updateMessages()

        val recyclerView = findViewById<RecyclerView>(R.id.shoppingListRecyclerView)
        adapter = ShoppingListAdapter(filteredList,
            onItemChecked = { position ->
                val product = filteredList[position]
                val idx = shoppingList.indexOf(product)
                if (idx != -1) {
                    shoppingList[idx] = product.copy(isBought = !product.isBought)
                    updateFilter()
                    saveShoppingList()
                }
            },
            onItemDeleted = { position ->
                val product = filteredList[position]
                shoppingList.remove(product)
                updateFilter()
                saveShoppingList()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.addProductButton).setOnClickListener {
            val intent = Intent(this, AddProductActivity::class.java)
            startActivityForResult(intent, 100)
        }

        findViewById<Button>(R.id.duplicateListButton).setOnClickListener {
            val newItems = shoppingList.map { it.copy(isBought = false) }
            shoppingList.addAll(newItems)
            updateFilter()
            saveShoppingList()
            Toast.makeText(this, "Lista duplicada", Toast.LENGTH_SHORT).show()
            updateCounters()
            updateMessages()
        }

        findViewById<Button>(R.id.shareListButton).setOnClickListener {
            val listText = shoppingList.joinToString("\n") { product ->
                if (product.isBought) "${product.name} (Comprado)" else product.name
            }

            if (listText.isNotEmpty()) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, "Lista de compras:\n$listText")
                    type = "text/plain"
                }
                startActivity(Intent.createChooser(shareIntent, "Compartir lista de compras"))
            }
        }

        val filterSpinner = findViewById<Spinner>(R.id.filterSpinner)
        val filters = listOf("Todos", "Pendientes", "Comprados")
        val filterAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filters)
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = filterAdapter

        filterSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                updateFilter()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        findViewById<Button>(R.id.clearListButton).setOnClickListener {
            shoppingList.clear()
            updateFilter()
            saveShoppingList()
            Toast.makeText(this, "Lista eliminada", Toast.LENGTH_SHORT).show()
            updateCounters()
            updateMessages()
        }
    }

    private fun saveShoppingList() {
        val sharedPreferences = getSharedPreferences("MyListExpressPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(shoppingList)
        editor.putString("shoppingList", json)
        editor.apply()
    }

    private fun loadShoppingList() {
        val sharedPreferences = getSharedPreferences("MyListExpressPrefs", Context.MODE_PRIVATE)
        val json = sharedPreferences.getString("shoppingList", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Product>>() {}.type
            val savedList: MutableList<Product> = Gson().fromJson(json, type)
            shoppingList.addAll(savedList)
        }
    }

    private fun updateCounters() {
        findViewById<TextView>(R.id.totalCount).text = "Total: ${shoppingList.size}"
        findViewById<TextView>(R.id.boughtCount).text = "Comprados: ${shoppingList.count { it.isBought }}"
    }

    private fun updateMessages() {
        val msgView = findViewById<TextView>(R.id.messageText)
        when {
            shoppingList.isEmpty() -> {
                msgView.text = "La lista está vacía"
                msgView.visibility = View.VISIBLE
            }
            shoppingList.count { !it.isBought } > 10 -> {
                msgView.text = "¡Tu lista es larga, no olvides nada!"
                msgView.visibility = View.VISIBLE
            }
            else -> msgView.visibility = View.GONE
        }
    }

    private fun updateFilter() {
        val filterSpinner = findViewById<Spinner>(R.id.filterSpinner)
        val selected = filterSpinner.selectedItem as String
        filteredList.clear()
        filteredList.addAll(when (selected) {
            "Pendientes" -> shoppingList.filter { !it.isBought }
            "Comprados" -> shoppingList.filter { it.isBought }
            else -> shoppingList
        })
        adapter.notifyDataSetChanged()
        updateCounters()
        updateMessages()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            val productName = data.getStringExtra("productName") ?: return
            val imageUri = data.getStringExtra("imageUri")
            val category = data.getStringExtra("category")
            shoppingList.add(Product(productName, false, imageUri, category))
            updateFilter()
            saveShoppingList()
        }
    }
}