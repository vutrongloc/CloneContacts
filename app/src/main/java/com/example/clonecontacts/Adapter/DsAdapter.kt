package com.example.clonecontacts.Adapter

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.cardview.widget.CardView
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.clonecontacts.Fragment.UserFragment
import com.example.clonecontacts.Fragment.UserGroupsFragment
import com.example.clonecontacts.Model.Group
import com.example.clonecontacts.R
import com.example.clonecontacts.Model.User
import com.google.android.material.bottomnavigation.BottomNavigationView

class DsAdapter(
    val dsUser: MutableList<User>,
    val dsGroup: MutableList<Group>,
    val activity: FragmentActivity,
    var hienThiHinhThuNho: Boolean = true,
    var hienThiSDT: Boolean = false
) : RecyclerView.Adapter<DsAdapter.UserViewHolder>() {
    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    var listener: OnSelectedUsersChangeListener? = null
    private val selectedUsers = mutableListOf<User>()
    private val selectedGroups = mutableListOf<Group>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        var view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        if (dsGroup.size != 0) {
            view = LayoutInflater.from(parent.context).inflate(R.layout.item_group, parent, false)
        }
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int {
        var sl = dsUser.size
        if (dsGroup.size != 0) {
            sl = dsGroup.size
        }
        return sl
    }

    interface OnSelectedUsersChangeListener {
        fun onSelectedUsersChanged(selectedUsers: MutableList<User>)
        fun onSelectedGroupsChanged(selectedGroups: MutableList<Group>)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.itemView.apply {
            if (dsUser.size != 0) {
                val ten = findViewById<TextView>(R.id.item_Ten)
                ten.setText(dsUser[position].name)
                val anh = findViewById<ImageView>(R.id.item_anh)
                val khunganh = findViewById<CardView>(R.id.cardView5)
                if (hienThiHinhThuNho) {
                    anh.visibility = View.VISIBLE
                    khunganh.visibility =View.VISIBLE
                    thayDoiAnhDaiDien(anh, ten.text[0].toString())
                } else {
                    anh.visibility = View.GONE
                    khunganh.visibility =View.GONE
                }
                val sdt = findViewById<TextView>(R.id.item_SDT)
                if (hienThiSDT) {
                    sdt.visibility = View.VISIBLE
                    sdt.setText(dsUser[position].mobile)
                } else {
                    sdt.visibility = View.GONE
                }
                Log.d("DsAdapter", "Đang hiển thị: ${dsUser[position].name}")
                val layout = findViewById<LinearLayout>(R.id.item_Layout)
                // Xử lý nhấn lì (long click)
                layout.setOnLongClickListener {
                    if (selectedUsers.contains(dsUser[position])) {
                        selectedUsers.remove(dsUser[position])
                        layout.setBackgroundColor(resources.getColor(R.color.black))
                    } else {
                        selectedUsers.add(dsUser[position])
                        layout.setBackgroundColor(resources.getColor(R.color.choose))
                    }
                    listener?.onSelectedUsersChanged(selectedUsers)
                    true
                }

                // Cập nhật màu nền nếu item đang được chọn
                if (selectedUsers.contains(dsUser[position])) {
                    layout.setBackgroundColor(resources.getColor(R.color.choose))
                } else {
                    layout.setBackgroundColor(resources.getColor(R.color.black))
                }
                layout.setOnClickListener {
                    if (selectedUsers.isEmpty()) {
                        val fragment = UserFragment()
                        val bundle = Bundle()
                        bundle.putSerializable("user", dsUser[position])
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
            } else {
                val ten = findViewById<TextView>(R.id.item_TenGroup)
                val anh = findViewById<ImageView>(R.id.item_anhGroup)
                if (hienThiHinhThuNho) {
                    anh.visibility = View.VISIBLE
                    thayDoiAnhDaiDien(anh, ten.text[0].toString())
                } else {
                    anh.visibility = View.GONE
                }
                val sl = findViewById<TextView>(R.id.item_SLGroup)
                ten.setText(dsGroup[position].nameGroup)
                thayDoiAnhDaiDien(anh, dsGroup[position].nameGroup[0].toString())
                var dsUser: MutableList<User> = mutableListOf()
                dsUser.addAll(dsGroup[position].dsUser)
                sl.setText(dsUser.size.toString())
                val linearLayout = findViewById<LinearLayout>(R.id.item_LayoutGroup)
                // Xử lý nhấn lì (long click)
                linearLayout.setOnLongClickListener {
                    if (selectedGroups.contains(dsGroup[position])) {
                        selectedGroups.remove(dsGroup[position])
                        linearLayout.setBackgroundColor(resources.getColor(R.color.black))
                    } else {
                        selectedGroups.add(dsGroup[position])
                        linearLayout.setBackgroundColor(resources.getColor(R.color.choose))
                    }
                    listener?.onSelectedGroupsChanged(selectedGroups)
                    true
                }

                // Cập nhật màu nền nếu item đang được chọn
                if (selectedGroups.contains(dsGroup[position])) {
                    linearLayout.setBackgroundColor(resources.getColor(R.color.choose))
                } else {
                    linearLayout.setBackgroundColor(resources.getColor(R.color.black))
                }
                linearLayout.setOnClickListener {
                    val toolbar = activity.findViewById<Toolbar>(R.id.main_Toolbar)
                    toolbar.menu.clear()
                    toolbar.inflateMenu(R.menu.menu_user_group)
                    val tenFragment = activity.findViewById<TextView>(R.id.main_tenFragment)
                    tenFragment.setText(dsGroup[position].nameGroup)
                    val userGroupsFragment = UserGroupsFragment()
                    val bundle = Bundle()
                    bundle.putSerializable("dsUserGroup", ArrayList(dsUser))
                    bundle.putString("tenGroup", dsGroup[position].nameGroup)
                    userGroupsFragment.arguments = bundle
                    activity.supportFragmentManager.beginTransaction().apply {
                        replace(R.id.frameLayout, userGroupsFragment)
                        addToBackStack(null)
                        commit()
                    }
                }
            }
        }
    }

    fun selectAll() {
        selectedUsers.clear()  // Xóa tất cả user được chọn trước đó
        selectedUsers.addAll(dsUser)  // Thêm tất cả user vào danh sách đã chọn
        listener?.onSelectedUsersChanged(selectedUsers) // Thông báo listener về sự thay đổi
        notifyDataSetChanged()  // Cập nhật giao diện
    }
    fun deselectAll() {
        selectedUsers.clear()  // Xóa tất cả user được chọn
        listener?.onSelectedUsersChanged(selectedUsers)
        notifyDataSetChanged()
    }
    fun thayDoiAnhDaiDien(Image: ImageView, text: String) {
        when (text[0]) {
            'A', 'a' -> Image.setImageResource(R.drawable.a)
            'B', 'b' -> Image.setImageResource(R.drawable.b)
            'C', 'c' -> Image.setImageResource(R.drawable.c)
            'D', 'd' -> Image.setImageResource(R.drawable.d)
            'E', 'e' -> Image.setImageResource(R.drawable.e)
            'F', 'f' -> Image.setImageResource(R.drawable.f)
            'G', 'g' -> Image.setImageResource(R.drawable.g)
            'H', 'h' -> Image.setImageResource(R.drawable.h)
            'I', 'i' -> Image.setImageResource(R.drawable.i)
            'J', 'j' -> Image.setImageResource(R.drawable.j)
            'K', 'k' -> Image.setImageResource(R.drawable.k)
            'L', 'l' -> Image.setImageResource(R.drawable.l)
            'M', 'm' -> Image.setImageResource(R.drawable.m)
            'N', 'n' -> Image.setImageResource(R.drawable.n)
            'O', 'o' -> Image.setImageResource(R.drawable.o)
            'P', 'p' -> Image.setImageResource(R.drawable.p)
            'Q', 'q' -> Image.setImageResource(R.drawable.q)
            'R', 'r' -> Image.setImageResource(R.drawable.r)
            'S', 's' -> Image.setImageResource(R.drawable.s)
            'T', 't' -> Image.setImageResource(R.drawable.t)
            'U', 'u' -> Image.setImageResource(R.drawable.u)
            'V', 'v' -> Image.setImageResource(R.drawable.v)
            'W', 'w' -> Image.setImageResource(R.drawable.w)
            'X', 'x' -> Image.setImageResource(R.drawable.x)
            'Y', 'y' -> Image.setImageResource(R.drawable.y)
            'Z', 'z' -> Image.setImageResource(R.drawable.z)
        }
    }
    fun selectGroupsAll() {
        selectedGroups.clear()  // Xóa tất cả user được chọn trước đó
        selectedGroups.addAll(dsGroup)  // Thêm tất cả user vào danh sách đã chọn
        listener?.onSelectedGroupsChanged(selectedGroups) // Thông báo listener về sự thay đổi
        notifyDataSetChanged()  // Cập nhật giao diện
    }
    fun deselectGroupsAll() {
        selectedGroups.clear()  // Xóa tất cả user được chọn
        listener?.onSelectedGroupsChanged(selectedGroups)
        notifyDataSetChanged()
    }
}