package com.example.mylistexpress

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mylistexpress.common.Product
import com.example.mylistexpress.common.ShoppingListAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class ShoppingListActivity : AppCompatActivity() {
    private val shoppingList = mutableListOf<Product>()
    private val filteredList = mutableListOf<Product>()
    private lateinit var adapter: ShoppingListAdapter

    companion object {
        private const val IMAGE_PICK_CODE = 1010
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    }

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

        checkNotificationPermission()

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
                showNotification("Producto eliminado", "Se eliminó ${product.name} de la lista de compras.")
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.addProductButton).setOnClickListener {
            val intent = Intent(this, AddProductActivity::class.java)
            startActivityForResult(intent, 100)
        }

        findViewById<Button>(R.id.duplicateListButton).setOnClickListener {
            if (shoppingList.isNotEmpty()) {
                try {
                    val newItems = shoppingList.map { product ->
                        val newImageUri = product.imageUri?.let {
                            val sourceUri = Uri.parse(it)
                            if (sourceUri.scheme == "content") {
                                copyImageToInternalStorageSafe(sourceUri)?.toString() ?: it
                            } else {
                                it
                            }
                        }
                        product.copy(isBought = false, imageUri = newImageUri)
                    }
                    shoppingList.addAll(newItems)
                    updateFilter()
                    saveShoppingList()
                    Toast.makeText(this, "Lista duplicada", Toast.LENGTH_SHORT).show()
                    updateCounters()
                    updateMessages()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error al duplicar la lista: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "La lista está vacía, no se puede duplicar", Toast.LENGTH_SHORT).show()
            }
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

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val channelId = "shopping_list_channel"
        val channelName = "Lista de Compras"
        val notificationId = 1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Notificaciones de la lista de compras"
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(this)) {
            notify(notificationId, builder.build())
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
            val productName = data.getStringExtra("productName")
            val category = data.getStringExtra("category")
            val imageUriString = data.getStringExtra("imageUri")

            if (!productName.isNullOrEmpty() && !category.isNullOrEmpty()) {
                val product = Product(
                    name = productName,
                    isBought = false,
                    imageUri = imageUriString,
                    category = category
                )
                shoppingList.add(product)
                updateFilter()
                saveShoppingList()
                showNotification("Producto agregado", "Se agregó $productName a la lista de compras.")
            }
        }

        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK && data != null) {
            val selectedImageUri = data.data ?: return
            val internalUri = copyImageToInternalStorageSafe(selectedImageUri)

            if (internalUri != null) {
                val product = Product(
                    name = "Producto ${shoppingList.size + 1}",
                    isBought = false,
                    imageUri = internalUri.toString(),
                    category = "General"
                )
                shoppingList.add(product)
                updateFilter()
                saveShoppingList()
            } else {
                Toast.makeText(this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyImageToInternalStorageSafe(uri: Uri): Uri? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
                ?: throw Exception("No se pudo abrir InputStream")

            val fileName = "IMG_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            val outputStream = file.outputStream()

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}