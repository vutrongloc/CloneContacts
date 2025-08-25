package com.example.clonecontacts.Adapter

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.clonecontacts.Fragment.UserFragment
import com.example.clonecontacts.Model.CallHistory
import com.example.clonecontacts.Model.User
import com.example.clonecontacts.R
import com.google.android.material.bottomnavigation.BottomNavigationView


class CallHistoryAdapter(val callList: List<CallHistory>, val activity: FragmentActivity) :
    RecyclerView.Adapter<CallHistoryAdapter.CallViewHolder>() {

    class CallViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvNameOrNumber: TextView = itemView.findViewById(R.id.tvNameOrNumber)
        val tvTypeAndDate: TextView = itemView.findViewById(R.id.tvTypeAndDate)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val layout: LinearLayout = itemView.findViewById(R.id.layout_Call_Log)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_call_history, parent, false)
        return CallViewHolder(view)
    }

    override fun onBindViewHolder(holder: CallViewHolder, position: Int) {
        val call = callList[position]
        holder.tvNameOrNumber.text = call.name ?: call.number
        holder.tvTypeAndDate.text = "${call.type} • ${call.date}"
        holder.tvDuration.text = "Thời lượng: ${call.duration}s"
        holder.layout.setOnClickListener {
            val fragment = UserFragment()
            val bundle = Bundle()
            bundle.putSerializable(
                "user",
                User(callList[position].name ?: "Không rõ", callList[position].number)
            )
            fragment.arguments = bundle
            activity.supportFragmentManager.beginTransaction().apply {
                replace(R.id.frameLayout, fragment)
                addToBackStack(null)
                commit()
            }
            val toolbar = activity.findViewById<Toolbar>(R.id.main_Toolbar)
            toolbar.visibility = View.GONE
            val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom)
            bottom.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = callList.size
}
