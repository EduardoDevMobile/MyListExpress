package com.example.mylistexpress.common

import android.annotation.SuppressLint
import android.graphics.Paint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mylistexpress.R

class ShoppingListAdapter(
    private val items: MutableList<Product>,
    private val onItemChecked: (Int) -> Unit,
    private val onItemDeleted: (Int) -> Unit
) : RecyclerView.Adapter<ShoppingListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val productImage: ImageView = view.findViewById(R.id.productImage)
        val productName: TextView = view.findViewById(R.id.productName)
        val productCategory: TextView = view.findViewById(R.id.productCategory)
        val markAsBoughtButton: Button = view.findViewById(R.id.markAsBoughtButton)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = items[position]
        holder.productName.text = if (product.isBought) "${product.name} (Comprado)" else product.name
        holder.productCategory.text = product.category ?: ""
        holder.markAsBoughtButton.text = if (product.isBought) "Desmarcar" else "Marcar"
        holder.markAsBoughtButton.setOnClickListener { onItemChecked(position) }
        holder.deleteButton.setOnClickListener { onItemDeleted(position) }
        if (product.imageUri != null) {
            holder.productImage.setImageURI(Uri.parse(product.imageUri))
        } else {
            holder.productImage.setImageResource(android.R.drawable.ic_menu_camera)
        }
        if (product.isBought) {
            holder.productName.paintFlags = holder.productName.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.productName.paintFlags = holder.productName.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    override fun getItemCount(): Int = items.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<Product>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}