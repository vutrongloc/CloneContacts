package com.example.clonecontacts.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.clonecontacts.Model.Group
import com.example.clonecontacts.Model.User
import com.example.clonecontacts.R


class ContactsFavoritesAdapter(
    private val users: List<User>,
    private val groups: List<Group>,
    private val checked: MutableList<Boolean>,
    private val onCheckedChangeUser: (User, Boolean) -> Unit,
    private val onCheckedChangeGroup: (Group, Boolean) -> Unit
) : RecyclerView.Adapter<ContactsFavoritesAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {}

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        var view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contact_favorites_checkbox, parent, false)
        if (groups.size > 0) {
            view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_group_checkbox, parent, false)
        }
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        var size = users.size
        if (groups.size > 0) {
            size = groups.size
        }
        return size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemView.apply {
            if (users.size > 0) {
                val checkBox: CheckBox = findViewById(R.id.cbContact)
                val tvName: TextView = findViewById(R.id.tvName)
                val tvPhone: TextView = findViewById(R.id.tvPhone)
                val user = users[position]
                tvName.text = user.name
                tvPhone.text = user.mobile
                checkBox.isChecked = checked[position]

                checkBox.setOnCheckedChangeListener(null) // tránh callback thừa
                checkBox.setOnCheckedChangeListener { _, isChecked ->
                    checked[position] = isChecked
                    onCheckedChangeUser(user, isChecked)
                }
            } else {
                val name_Group = findViewById<TextView>(R.id.tv_group)
                val cb_Group = findViewById<CheckBox>(R.id.cb_group)
                val group = groups[position]
                name_Group.setText(group.nameGroup)
                cb_Group.isChecked = checked[position]

                cb_Group.setOnCheckedChangeListener(null)
                cb_Group.setOnCheckedChangeListener { _, isChecked ->
                    checked[position] = isChecked
                    onCheckedChangeGroup(group, isChecked)
                }
            }

        }
    }
}


