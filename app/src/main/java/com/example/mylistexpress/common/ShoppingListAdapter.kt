package com.example.mylistexpress.common

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mylistexpress.R

class ShoppingListAdapter(
    private val items: MutableList<Pair<String, Boolean>>,
    private val onItemChecked: (Int) -> Unit,
    private val onItemDeleted: (Int) -> Unit
) : RecyclerView.Adapter<ShoppingListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productName: TextView = view.findViewById(R.id.productName)
        val markAsBoughtButton: Button = view.findViewById(R.id.markAsBoughtButton)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val (name, isBought) = items[position]
        holder.productName.text = if (isBought) "$name (Comprado)" else name
        holder.markAsBoughtButton.text = if (isBought) "Desmarcar" else "Marcar"
        holder.markAsBoughtButton.setOnClickListener { onItemChecked(position) }
        holder.deleteButton.setOnClickListener { onItemDeleted(position) }
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<Pair<String, Boolean>>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}