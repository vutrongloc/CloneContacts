package com.example.clonecontacts.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.clonecontacts.Model.User
import com.example.clonecontacts.R


class ContactsFavoritesAdapter(
    private val users: List<User>,
    private val checked: MutableList<Boolean>,
    private val onCheckedChange: (User, Boolean) -> Unit
) : RecyclerView.Adapter<ContactsFavoritesAdapter.ViewHolder>() {

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.cbContact)
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvPhone: TextView = view.findViewById(R.id.tvPhone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_favorites_checkbox, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = users[position]
        holder.tvName.text = user.name
        holder.tvPhone.text = user.mobile
        holder.checkBox.isChecked = checked[position]

        holder.checkBox.setOnCheckedChangeListener(null) // tránh callback thừa
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            checked[position] = isChecked
            onCheckedChange(user, isChecked)
        }
    }
}


