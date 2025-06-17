import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mylistexpress.R
import com.example.mylistexpress.common.ShoppingListAdapter
import com.google.gson.Gson

class ShoppingListActivity : AppCompatActivity() {
    private val shoppingList = mutableListOf<Pair<String, Boolean>>()
    private lateinit var adapter: ShoppingListAdapter

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopping_list)

        loadShoppingList()

        val recyclerView = findViewById<RecyclerView>(R.id.shoppingListRecyclerView)
        adapter = ShoppingListAdapter(shoppingList,
            onItemChecked = { position ->
                shoppingList[position] = shoppingList[position].copy(second = !shoppingList[position].second)
                adapter.notifyItemChanged(position)
                saveShoppingList()
            },
            onItemDeleted = { position ->
                shoppingList.removeAt(position)
                adapter.notifyItemRemoved(position)
                saveShoppingList()
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.addProductButton).setOnClickListener {
            // abrir la actividad para agregar productos
        }

        findViewById<Button>(R.id.duplicateListButton).setOnClickListener {
            val newItems = shoppingList.map { it.copy(second = false) }
            shoppingList.addAll(newItems)
            adapter.notifyDataSetChanged()
            saveShoppingList()
        }

        findViewById<Button>(R.id.shareListButton).setOnClickListener {
            val listText = shoppingList.joinToString("\n") { (name, isBought) ->
                if (isBought) "$name (Comprado)" else name
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

        filterSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val filteredList = when (filters[position]) {
                    "Pendientes" -> shoppingList.filter { !it.second }
                    "Comprados" -> shoppingList.filter { it.second }
                    else -> shoppingList
                }
                adapter.updateList(filteredList)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })
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
            val type = object : com.google.gson.reflect.TypeToken<MutableList<Pair<String, Boolean>>>() {}.type
            val savedList: MutableList<Pair<String, Boolean>> = Gson().fromJson(json, type)
            shoppingList.addAll(savedList)
        }
    }
}