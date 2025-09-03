package com.example.clonecontacts.Fragment

import ContactImporter
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clonecontacts.Adapter.DsAdapter
import com.example.clonecontacts.ChucNang
import com.example.clonecontacts.Model.Group
import com.example.clonecontacts.activity.MainActivity.Companion.PICK_VCF_REQUEST
import com.example.clonecontacts.R
import com.example.clonecontacts.Model.User

class GroupsFragment : Fragment(), DsAdapter.OnSelectedUsersChangeListener {
    lateinit var adapter: DsAdapter
    lateinit var keyboard: ImageView
    lateinit var add: ImageView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        keyboard = view.findViewById<ImageView>(R.id.groups_Keyboard)
        add = view.findViewById<ImageView>(R.id.groups_add)
        yeuCauQuyenDayDu()
        keyboard.setOnClickListener {
            ChucNang().open_KeyBroad(requireActivity())
        }
        add.setOnClickListener {
            diaLogAddGroup("Tạo một nhóm mới", Group())
        }
    }

    fun diaLogAddGroup(tieuDeDiaLog: String, group: Group) {
        val toolBar = requireActivity().findViewById<Toolbar>(R.id.main_Toolbar)
        val toolbarColor = (toolBar.background as ColorDrawable).color
        val drawable = ContextCompat.getDrawable(requireActivity(), R.drawable.bovuong)
            ?.mutate() as GradientDrawable
        drawable.setColor(toolbarColor)

        val dialog = Dialog(requireActivity())
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        val layoutInflater = LayoutInflater.from(requireActivity())
        val view = layoutInflater.inflate(R.layout.custom_dialog, null)
        view.background = drawable
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val textEditText = view.findViewById<EditText>(R.id.dialog_TieuDe)
        var tieuDe = view.findViewById<TextView>(R.id.textView)
        val huy = view.findViewById<TextView>(R.id.dialog_HuyBo)
        val ok = view.findViewById<TextView>(R.id.dialog_OK)

        tieuDe.setText(tieuDeDiaLog.toString())
        dialog.show()
        huy.setOnClickListener {
            dialog.dismiss()
        }
        if (tieuDeDiaLog.equals("Tạo một nhóm mới")) {
            ok.setOnClickListener {
                if (textEditText.text.toString().trim().isNullOrEmpty()) {
                    Toast.makeText(requireActivity(), "Tên nhóm chưa được nhập", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    createContactGroup(textEditText.text.toString().trim(), requireActivity())
                    getGroup()
                    dialog.dismiss()
                }
            }
        } else {
            ok.setOnClickListener {
                val groupID = ChucNang().getGroupIDByNameGroup(group.nameGroup, requireActivity())
                if (groupID != null) {
                    if (ChucNang().getGroupIDByNameGroup(
                            textEditText.text.toString().trim(),
                            requireActivity()
                        ) == null
                    ) {
                        ChucNang().doiTenNhom(
                            groupID,
                            textEditText.text.toString().trim(),
                            requireActivity()
                        )
                        getGroup()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(
                            requireActivity(),
                            "Tên đã tồn tại hãy chọn tên khác!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    fun getGroup() {
        val dsDroups: MutableList<Group> = ChucNang().getContactGroups(requireActivity())
        if (dsDroups.isEmpty()) {
            Log.e("GroupsFragment", "Danh sách nhóm rỗng!")
        } else {
            val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView_Groups)
            adapter = DsAdapter(mutableListOf(), dsDroups, requireActivity())
            adapter.listener = this@GroupsFragment
            recyclerView?.layoutManager =
                LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
            recyclerView?.adapter = adapter
        }
    }

    fun doiMauUngDung(color: Int) {
        ChucNang().changeToolbarColor(requireActivity(), color)
        ChucNang().updateBottomNavigationColor(requireActivity())
        var currentFragment =
            requireActivity().supportFragmentManager.findFragmentById(R.id.frameLayout)
        var ten = Fragment()
        ten = GroupsFragment()
        requireActivity().supportFragmentManager.beginTransaction().apply {
            replace(R.id.frameLayout, ten)
            commit()
        }
    }

    override fun onResume() {
        yeuCauQuyenDayDu()
        ChucNang().updateBotronColor(requireActivity(), keyboard, add)
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.main_Toolbar)
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu_group)
        val sharedPref = requireActivity().getSharedPreferences("CaiDat", Context.MODE_PRIVATE)
        val hienThiHinh = sharedPref.getBoolean("thu_nho_lien_he", true)
        if (!hienThiHinh) {
            ChucNang().anHinhNgay(adapter)
        }
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_Group_XuatDanhBaTuTepVCF -> {
                    if (ContextCompat.checkSelfPermission(
                            requireActivity(),
                            android.Manifest.permission.WRITE_CONTACTS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(android.Manifest.permission.WRITE_CONTACTS),
                            102
                        )
                    } else if (ContextCompat.checkSelfPermission(
                            requireActivity(),
                            android.Manifest.permission.READ_CONTACTS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(android.Manifest.permission.READ_CONTACTS),
                            102
                        )
                    } else {
                        var currentFragment =
                            requireActivity().supportFragmentManager.findFragmentById(R.id.frameLayout)
                        var ten = ""
                        if (currentFragment is ContactsFragment) {
                            ten = "Contacts"
                        } else if (currentFragment is FavoritesFragment) {
                            ten = "Favorites"
                        } else {
                            ten = "Groups"
                        }
                        val contactImporter = ContactImporter(requireContext(), ten)
                        val a = contactImporter.exportContactsToVcf()
                        if (a) {
                            Toast.makeText(
                                requireActivity(),
                                "Đã xuất tệp vào mục dowloads",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        } else {
                            Toast.makeText(
                                requireActivity(),
                                "Không xuất tệp thành công",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    }
                    true
                }

                R.id.menu_Group_NhapDanhBaTuTepVCF -> {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            android.Manifest.permission.WRITE_CONTACTS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(android.Manifest.permission.WRITE_CONTACTS),
                            101
                        )
                    } else {
                        requestContactPermission()
                    }
                    true
                }

                R.id.menu_Group_VuongMien -> {
                    ChucNang().showColorPickerDialog(requireActivity()) { color ->
                        doiMauUngDung(color)
                    }
                    true
                }

                R.id.menu_Group_CaiDat -> {
                    ChucNang().diaLog_CaiDat(requireActivity(), adapter)
                    true
                }

                else -> false
            }
        }
        super.onResume()
    }

    fun yeuCauQuyenDayDu() {
        val canReadContacts = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.READ_CONTACTS
        )
        val canWriteContacts = ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.WRITE_CONTACTS
        )

        if (canReadContacts == PackageManager.PERMISSION_GRANTED && canWriteContacts == PackageManager.PERMISSION_GRANTED) {
            getGroup()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(
                    android.Manifest.permission.READ_CONTACTS,
                    android.Manifest.permission.WRITE_CONTACTS
                ),
                200
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 200) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                getGroup()
            } else {
                Toast.makeText(
                    requireActivity(),
                    "Cần cấp quyền đầy đủ để truy cập danh bạ",
                    Toast.LENGTH_SHORT
                ).show()
                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(
                        android.Manifest.permission.READ_CONTACTS,
                        android.Manifest.permission.WRITE_CONTACTS
                    ),
                    200
                )
            }
        }
    }

    fun createContactGroup(groupName: String, activity: FragmentActivity) {
        val contentResolver = activity.contentResolver

        val contentValues = ContentValues().apply {
            put(ContactsContract.Groups.TITLE, groupName)
            put(
                ContactsContract.Groups.ACCOUNT_TYPE,
                "com.local"
            )  // Nhóm cục bộ, không thuộc tài khoản nào
            put(ContactsContract.Groups.ACCOUNT_NAME, "Local Contacts")  // Tên tài khoản cục bộ
        }

        try {
            val uri = contentResolver.insert(ContactsContract.Groups.CONTENT_URI, contentValues)

            if (uri != null) {
                Toast.makeText(activity, "Nhóm '$groupName' đã được tạo!", Toast.LENGTH_SHORT)
                    .show()
                Log.d("CreateGroup", "Nhóm '$groupName' đã được tạo với URI: $uri")
            } else {
                Toast.makeText(activity, "Tạo nhóm thất bại!", Toast.LENGTH_SHORT).show()
                Log.e("CreateGroup", "Tạo nhóm '$groupName' thất bại.")
            }
        } catch (e: SecurityException) {
            Toast.makeText(activity, "Không có quyền tạo nhóm danh bạ!", Toast.LENGTH_SHORT)
                .show()
            Log.e("CreateGroup", "Lỗi bảo mật: ${e.message}")
        } catch (e: Exception) {
            Toast.makeText(activity, "Lỗi khi tạo nhóm: ${e.message}", Toast.LENGTH_SHORT)
                .show()
            Log.e("CreateGroup", "Lỗi tạo nhóm '$groupName': ${e.message}", e)
        }
    }


    override fun onSelectedUsersChanged(selectedUsers: MutableList<User>) {

    }

    lateinit var doiTen: MenuItem
    override fun onSelectedGroupsChanged(selectedGroups: MutableList<Group>) {
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.main_Toolbar)
        toolbar.menu.clear()
        Log.d("soluong", selectedGroups.size.toString())
        toolbar.inflateMenu(R.menu.menu_group)
        if (selectedGroups.size >= 1) {
            toolbar.menu.clear()
            toolbar.inflateMenu(R.menu.menu_longclickoneormuch_group)
            doiTen = toolbar.menu.findItem(R.id.menu_LongClickOneOrMuch_Group_DoiTen)
            doiTen.isVisible = selectedGroups.size == 1
        }
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_LongClickOneOrMuch_Group_DoiTen -> {
                    if (selectedGroups.size == 1) {
                        diaLogAddGroup("Thay đổi tên nhóm", selectedGroups[0])
                    }
                    adapter.deselectGroupsAll()
                    true
                }

                R.id.menu_LongClickOneOrMuch_Group_SellectAll -> {
                    adapter.selectGroupsAll()
                    true
                }

                R.id.menu_LongClickOneOrMuch_Group_XoaBo -> {
                    for (group in selectedGroups) {
                        val groupID =
                            ChucNang().getGroupIDByNameGroup(group.nameGroup, requireActivity())
                        if (ChucNang().deleteGroupPermanently(requireContext(), groupID!!)) {
                            Log.d("DeleteGroup", "Đã xóa nhóm: ${group.nameGroup}")
                        } else {
                            Log.e("DeleteGroup", "Không thể xóa nhóm: ${group.nameGroup}")
                        }
                    }
                    adapter.deselectGroupsAll()
                    getGroup() // Cập nhật lại danh sách
                    true
                }

                R.id.menu_Group_XuatDanhBaTuTepVCF -> {
                    if (ContextCompat.checkSelfPermission(
                            requireActivity(),
                            android.Manifest.permission.WRITE_CONTACTS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(android.Manifest.permission.WRITE_CONTACTS),
                            102
                        )
                    } else if (ContextCompat.checkSelfPermission(
                            requireActivity(),
                            android.Manifest.permission.READ_CONTACTS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(android.Manifest.permission.READ_CONTACTS),
                            102
                        )
                    } else {
                        var currentFragment =
                            requireActivity().supportFragmentManager.findFragmentById(R.id.frameLayout)
                        var ten = ""
                        if (currentFragment is ContactsFragment) {
                            ten = "Contacts"
                        } else if (currentFragment is FavoritesFragment) {
                            ten = "Favorites"
                        } else {
                            ten = "Groups"
                        }
                        val contactImporter = ContactImporter(requireContext(), ten)
                        val a = contactImporter.exportContactsToVcf()
                        if (a) {
                            Toast.makeText(
                                requireActivity(),
                                "Đã xuất tệp vào mục dowloads",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        } else {
                            Toast.makeText(
                                requireActivity(),
                                "Không xuất tệp thành công",
                                Toast.LENGTH_LONG
                            )
                                .show()
                        }
                    }
                    true
                }

                R.id.menu_Group_NhapDanhBaTuTepVCF -> {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            android.Manifest.permission.WRITE_CONTACTS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(
                            requireActivity(),
                            arrayOf(android.Manifest.permission.WRITE_CONTACTS),
                            101
                        )
                    } else {
                        requestContactPermission()
                    }
                    true
                }

                R.id.menu_Group_VuongMien -> {
                    ChucNang().showColorPickerDialog(requireActivity()) { color ->
                        doiMauUngDung(color)
                    }
                    true
                }

                R.id.menu_Group_CaiDat -> {
                    ChucNang().diaLog_CaiDat(requireActivity(), adapter)
                    true
                }

                else -> false
            }
        }
    }

    fun requestContactPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
                != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    requireActivity(),
                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    1
                )
            }
        }

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.WRITE_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.WRITE_CONTACTS),
                102
            )
        } else if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.READ_CONTACTS),
                102
            )
        } else {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, PICK_VCF_REQUEST)
        }
    }
}