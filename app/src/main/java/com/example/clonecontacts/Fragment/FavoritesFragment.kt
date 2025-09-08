package com.example.clonecontacts.Fragment

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clonecontacts.Adapter.ContactsFavoritesAdapter
import com.example.clonecontacts.Adapter.DsAdapter
import com.example.clonecontacts.ChucNang
import com.example.clonecontacts.Model.Group
import com.example.clonecontacts.activity.MainActivity
import com.example.clonecontacts.R
import com.example.clonecontacts.Model.User
import java.util.Collections.swap

class FavoritesFragment : Fragment(), DsAdapter.OnSelectedUsersChangeListener {
    var dsUser_Favorites: MutableList<User> = mutableListOf()
    var dsChecked_Favorites: MutableList<Boolean> = mutableListOf()
    var dsUserYeuThich: MutableList<User> = mutableListOf()
    lateinit var editTextTimKiem: EditText
    lateinit var adapter: DsAdapter
    lateinit var keyboard: ImageView
    lateinit var addContacts: ImageView
    private var textWatcher: TextWatcher? = null // Store the TextWatcher

    private var soundPool: SoundPool? = null
    private var clickSoundId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    var tangHoacGiam = true
    var firstOrLast = true
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tangHoacGiam = arguments?.getBoolean("TangHoacGiam") ?: true
        firstOrLast = arguments?.getBoolean("FirstOrLast") ?: true
        yeuCauQuyenDayDu()
        setupSoundPool()
        timKiem()
        keyboard = view.findViewById<ImageView>(R.id.favorites_Keyboard)
        val toolBar = activity?.findViewById<Toolbar>(R.id.main_Toolbar)

        // Lấy màu hiện tại của Toolbar
        val toolbarColor = (toolBar?.background as ColorDrawable).color
        keyboard.setOnClickListener {
            playSound()
            ChucNang().open_KeyBroad(requireActivity())
        }

        addContacts = view.findViewById<ImageView>(R.id.favorites_add)
        addContacts.setOnClickListener {
            playSound()
            themContactsYeuThich(dsUser_Favorites)
        }
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
            if (tangHoacGiam != null && firstOrLast != null) {
                getContacts(tangHoacGiam, firstOrLast)
            } else {
                getContacts(tangHoacGiam, firstOrLast)
            }
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
                if (tangHoacGiam != null && firstOrLast != null) {
                    getContacts(tangHoacGiam, firstOrLast)
                } else {
                    getContacts(tangHoacGiam, firstOrLast)
                }
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

    fun timKiem() {
        editTextTimKiem = requireActivity().findViewById<EditText>(R.id.main_EdittextTimKiem)
        val nutThoat = requireActivity().findViewById<ImageView>(R.id.main_Back)
        var dsUserKetQuaTimKiem: MutableList<User> = mutableListOf()

        if (isAdded) {
            textWatcher = object : TextWatcher { // Assign to variable
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (!isAdded) return // Prevent execution if fragment is detached
                    val textTimKiem = editTextTimKiem.text.toString()
                    dsUserKetQuaTimKiem = mutableListOf()
                    for (user in dsUserYeuThich) {
                        if (user.name.toLowerCase().contains(textTimKiem.toLowerCase())) {
                            dsUserKetQuaTimKiem.add(user)
                        }
                    }
                    adapter = DsAdapter(dsUserKetQuaTimKiem, mutableListOf(), requireActivity())
                    adapter.listener = this@FavoritesFragment
                    adapter.deselectAll()
                    val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView_Favorites)
                    recyclerView?.layoutManager = LinearLayoutManager(
                        requireActivity(),
                        LinearLayoutManager.VERTICAL,
                        false
                    )
                    recyclerView?.adapter = adapter
                }
            }
            editTextTimKiem.addTextChangedListener(textWatcher) // Add the TextWatcher
            nutThoat.setOnClickListener {
                playSound()
                editTextTimKiem.setText("")
                val thanhTimKiem =
                    requireActivity().findViewById<EditText>(R.id.main_EdittextTimKiem)
                thanhTimKiem.visibility = View.GONE
                val nutThoat = requireActivity().findViewById<ImageView>(R.id.main_Back)
                nutThoat.visibility = View.GONE
                val tenMain = requireActivity().findViewById<TextView>(R.id.main_tenFragment)
                tenMain.visibility = View.VISIBLE
                adapter.deselectAll()
            }
        }
    }

    override fun onResume() {
        yeuCauQuyenDayDu()
        ChucNang().updateBotronColor(requireActivity(), keyboard, addContacts)
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.main_Toolbar)
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu)
        val activity = requireActivity() as MainActivity
        activity.setupToolbar()
        val sharedPref = activity.getSharedPreferences("CaiDat", Context.MODE_PRIVATE)
        val hienThiHinh = sharedPref.getBoolean("thu_nho_lien_he", true)
        if (!hienThiHinh) {
            ChucNang().anHinhNgay(adapter)
        }
        val hienThiSDT = sharedPref.getBoolean("hien_thi_sdt", false)
        if (!hienThiHinh) {
            ChucNang().anHinhNgay(adapter)
        }
        if (hienThiSDT) {
            ChucNang().hienSDT(adapter)
        }
        val caidat = toolbar.menu.findItem(R.id.caiDat)
        caidat.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.caiDat -> {
                    playSound()
                    ChucNang().diaLog_CaiDat(requireActivity(), adapter)
                    true
                }

                else -> false
            }
        }
        textWatcher?.let { editTextTimKiem.addTextChangedListener(it) }
        super.onResume()
    }

    override fun onStop() {
        val thanhTimKiem =
            requireActivity().findViewById<EditText>(R.id.main_EdittextTimKiem)
        thanhTimKiem.visibility = View.GONE
        val nutThoat = requireActivity().findViewById<ImageView>(R.id.main_Back)
        nutThoat.visibility = View.GONE
        val tenMain = requireActivity().findViewById<TextView>(R.id.main_tenFragment)
        tenMain.visibility = View.VISIBLE
        editTextTimKiem.setText("")
        textWatcher?.let { editTextTimKiem.removeTextChangedListener(it) } // Remove the TextWatcher
        super.onStop()
    }

    fun themContactsYeuThich(dsUser: MutableList<User>) {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_contacts_favorites, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerContacts)

        val adapter = ContactsFavoritesAdapter(
            dsUser_Favorites,
            mutableListOf(),
            dsChecked_Favorites,
            onCheckedChangeUser = { user, isChecked ->
                danhDauYeuThich(user.mobile)
            },
            onCheckedChangeGroup = { group, ischecked ->
            })

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()
        val toolBar = activity?.findViewById<Toolbar>(R.id.main_Toolbar)

        // Lấy màu hiện tại của Toolbar
        val toolbarColor = (toolBar?.background as ColorDrawable).color
        val drawable2 = ContextCompat.getDrawable(requireActivity(), R.drawable.bovuong)
            ?.mutate() as GradientDrawable
        drawable2.setColor(toolbarColor)
        dialog.window?.setBackgroundDrawable(drawable2)
        dialog.show()
    }


    fun hamXuatKiTu(User: User): Char {
        var listName1 = User.name.split(" ")
        var name1 = ""
        if (listName1.size == 0) {
            name1 = User.name
        } else {
            name1 = listName1[listName1.size - 1]
        }
        return name1[0]
    }

    fun sapXepTangHoacGiam(
        tangHoacGiam: Boolean,
        firstOrLast: Boolean,
        dsUser: MutableList<User>
    ): MutableList<User> {
        if (tangHoacGiam == false && firstOrLast == true) {
            return dsUser.asReversed()
        } else if (tangHoacGiam == false && firstOrLast == false) {
            val soLuong = dsUser.size - 1
            var user1: User = User()
            var user2: User = User()
            for (i in 0..soLuong - 1) {
                user1 = dsUser[i]
                for (j in i + 1..soLuong) {
                    user2 = dsUser[j]
                    val name1 = hamXuatKiTu(user1).toString()
                    val name2 = hamXuatKiTu(user2).toString()
                    if (name1.compareTo(name2) == 0 || name1.compareTo(name2) < 0) {
                        swap(dsUser, i, j)
                        user1 = user2
                    }
                }
            }
        } else if (tangHoacGiam == true && firstOrLast == false) {
            val soLuong = dsUser.size - 1
            var user1: User = User()
            var user2: User = User()
            for (i in 0..soLuong - 1) {
                user1 = dsUser[i]
                for (j in i + 1..soLuong) {
                    user2 = dsUser[j]
                    val name1 = hamXuatKiTu(user1).toString()
                    val name2 = hamXuatKiTu(user2).toString()
                    if (name1.compareTo(name2) == 0 || name2.compareTo(name1) < 0) {
                        swap(dsUser, i, j)
                        user1 = user2
                    }
                }
            }
        }
        return dsUser
    }

    fun getContacts(tangHoacGiam: Boolean, firstOrLast: Boolean) {
        val contentResolver = requireContext().contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
            ),
            null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        cursor?.use {
            dsUser_Favorites.clear()
            dsChecked_Favorites.clear()
            dsUserYeuThich.clear()

            val tempUserMap = mutableMapOf<String, User>() // Lưu các liên hệ theo CONTACT_ID

            while (it.moveToNext()) {
                val name =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phoneNumber =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val contactId =
                    it.getLong(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))

                // Chuẩn hóa số điện thoại
                val normalizedNumber = phoneNumber.replace(Regex("[^0-9]"), "")

                // Sử dụng contactId làm khóa để tránh trùng lặp
                if (!tempUserMap.containsKey(contactId.toString())) {
                    val user = User(name, normalizedNumber)
                    tempUserMap[contactId.toString()] = user
                    dsUser_Favorites.add(user)

                    if(kiemTraXemDaDuocDanhDauYeuThichHayChua(contactId) == 1){
                        dsUserYeuThich.add(user)
                        dsChecked_Favorites.add(true)
                    }else{
                        dsChecked_Favorites.add(false)
                    }
                }
            }

            // Cập nhật UI
            updateRecyclerView(tangHoacGiam, firstOrLast)
        } ?: run {
            Toast.makeText(requireContext(), "Không thể truy cập danh bạ!", Toast.LENGTH_SHORT)
                .show()
        }
    }


    private fun updateRecyclerView(tangHoacGiam: Boolean, firstOrLast: Boolean) {
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView_Favorites)
        dsUserYeuThich = sapXepTangHoacGiam(tangHoacGiam, firstOrLast, dsUserYeuThich)
        recyclerView?.let {
            it.layoutManager =
                LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
            adapter = DsAdapter(dsUserYeuThich, mutableListOf(), requireActivity())
            it.adapter = adapter
            adapter.listener = this
            adapter.notifyDataSetChanged()
        }
    }

    fun getContactIdByPhoneNumber(phoneNumber: String): Long? {
        val contentResolver = requireContext().contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val normalizedInputNumber = phoneNumber.replace(Regex("[^0-9]"), "")

        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val contactId = cursor.getLong(0)
                val savedNumber = cursor.getString(1).replace(Regex("[^0-9]"), "")

                if (savedNumber == normalizedInputNumber ||
                    savedNumber.endsWith(normalizedInputNumber) ||
                    normalizedInputNumber.endsWith(savedNumber)
                ) {
                    return contactId
                }
            }
        }
        return null
    }


    fun kiemTraXemDaDuocDanhDauYeuThichHayChua(contactId: Long): Int {
        val contentResolver = requireContext().contentResolver
        val uri = ContactsContract.Contacts.CONTENT_URI
        val projection = arrayOf(ContactsContract.Contacts.STARRED)
        val selection = "${ContactsContract.Contacts._ID} = ?"
        val selectionArgs = arrayOf(contactId.toString())

        contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getInt(0) // Trả về trạng thái hiện tại: 1 (yêu thích) hoặc 0 (không yêu thích)
            }
        }
        return 0 // Mặc định không yêu thích nếu không tìm thấy
    }

    fun danhDauYeuThich(phoneNumber: String) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.WRITE_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_CONTACTS), 4)
            return
        }

        val contactId = getContactIdByPhoneNumber(phoneNumber) ?: run {
            Toast.makeText(requireContext(), "Không tìm thấy liên hệ!", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val currentFavoriteState = kiemTraXemDaDuocDanhDauYeuThichHayChua(contactId)
            val newFavoriteState = if (currentFavoriteState == 1) 0 else 1

            val values = ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, newFavoriteState)
            }

            val rowsUpdated = requireContext().contentResolver.update(
                ContactsContract.Contacts.CONTENT_URI,
                values,
                "${ContactsContract.Contacts._ID} = ?",
                arrayOf(contactId.toString())
            )

            if (rowsUpdated > 0) {
                Toast.makeText(
                    requireContext(),
                    if (newFavoriteState == 1) "Đã thêm vào danh sách yêu thích" else "Đã xóa khỏi danh sách yêu thích",
                    Toast.LENGTH_SHORT
                ).show()
                getContacts(tangHoacGiam, firstOrLast) // Cập nhật lại danh sách giao diện
            } else {
                Toast.makeText(requireContext(), "Cập nhật không thành công!", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            Log.e("danhDauYeuThich", "Lỗi khi cập nhật yêu thích: ${e.message}")
            Toast.makeText(requireContext(), "Đã xảy ra lỗi!", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSelectedUsersChanged(selectedUsers: MutableList<User>) {
        Log.d("onLong", selectedUsers.size.toString())
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.main_Toolbar)
        if (selectedUsers.size == 1) {
            toolbar.menu.clear()
            toolbar.inflateMenu(R.menu.menu_favorites_longclickone)
            toolbar.setOnMenuItemClickListener {
                yeuCauQuyenDayDu()
                when (it.itemId) {
                    R.id.menu_Favorites_LongClickOne_ChinhSua -> {
                        playSound()
                        val contactUri = ChucNang().getContactUriByPhoneNumber(
                            selectedUsers[0].mobile,
                            requireActivity()
                        )
                        contactUri?.let {
                            val intentEdit = Intent(Intent.ACTION_EDIT).apply {
                                data = it
                            }
                            startActivity(intentEdit)
                        } ?: Toast.makeText(
                            requireContext(),
                            "Không tìm thấy liên hệ",
                            Toast.LENGTH_SHORT
                        ).show()
                        true
                    }

                    R.id.menu_Favorites_LongClickOne_ThemVaoNhom -> {
                        playSound()
                        if (ContextCompat.checkSelfPermission(
                                requireActivity(),
                                android.Manifest.permission.WRITE_CONTACTS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                requireActivity(),
                                arrayOf(android.Manifest.permission.WRITE_CONTACTS),
                                101
                            )
                        } else {
                            ChucNang().dialogThemVaoGroup(
                                requireActivity(), selectedUsers[0], mutableListOf()
                            )
                            adapter.deselectAll()

                        }
                        true
                    }

                    R.id.menu_Favorites_LongClickOne_ChiaSe -> {
                        playSound()
                        ChucNang().chiaSe(selectedUsers[0], requireActivity())
                        true
                    }

                    R.id.menu_Favorites_LongClickOne_GuiSMS -> {
                        playSound()
                        ChucNang().sendSMS(selectedUsers[0].mobile, "", requireActivity())
                        true
                    }

                    R.id.menu_Favorites_LongClickOne_GuiEmail -> {
                        playSound()
                        val email =
                            ChucNang().getEmailFromPhone(requireActivity(), selectedUsers[0].mobile)
                        if (email != null) {
                            ChucNang().moUngDungEmail(
                                requireActivity(),
                                listOf(email)
                            ) // hàm bạn đã viết để mở email
                        } else {
                            adapter.deselectAll()
                            Toast.makeText(
                                requireActivity(),
                                "Không tìm thấy email cho số này",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        true
                    }

                    R.id.menu_Favorites_LongClickOne_TaoPhimTat -> {
                        playSound()
                        ChucNang().createShortcut(
                            selectedUsers[0],
                            ChucNang().traAnhDaiDien(selectedUsers[0].name),
                            requireActivity()
                        )
                        true
                    }

                    R.id.menu_Favorites_LongClickOne_ChonTatCa -> {
                        playSound()
                        adapter.selectAll()
                        true
                    }

                    R.id.menu_Favorites_LongClickOne_LoaiBoKhoiMucYeuThich -> {
                        playSound()
                        ChucNang().danhDauYeuThich(selectedUsers[0], requireActivity(), 0)
                        getContacts(tangHoacGiam, firstOrLast)
                        true
                    }

                    else -> false
                }
            }
        } else if (selectedUsers.size > 1) {
            toolbar.menu.clear()
            toolbar.inflateMenu(R.menu.menu_favorites_longclickmuch)
            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_Favorites_LongClickMuch_ThemVaoNhom -> {
                        playSound()
                        if (ContextCompat.checkSelfPermission(
                                requireActivity(),
                                android.Manifest.permission.WRITE_CONTACTS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                requireActivity(),
                                arrayOf(android.Manifest.permission.WRITE_CONTACTS),
                                101
                            )
                        } else {
                            ChucNang().dialogThemVaoGroup(requireActivity(), User(), selectedUsers)
                            requireActivity().supportFragmentManager.beginTransaction().apply {
                                replace(R.id.frameLayout, FavoritesFragment())
                                commit()
                            }
                        }
                        true
                    }

                    R.id.menu_Favorites_LongClickMuch_ChiaSe -> {
                        playSound()
                        ChucNang().chiaSeNhieuUser(selectedUsers, requireActivity())
                        true
                    }

                    R.id.menu_Favorites_LongClickMuch_GuiSMS -> {
                        playSound()
                        var phoneNumbers: List<String> = listOf()
                        for (user in selectedUsers) {
                            phoneNumbers += listOf(user.mobile)
                        }
                        val uri = Uri.parse("sms:" + phoneNumbers.joinToString(","))
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        intent.putExtra("sms_body", "Hello")
                        startActivity(intent)
                        true
                    }

                    R.id.menu_Favorites_LongClickMuch_GuiEmail -> {
                        playSound()
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "message/rfc822"  // Chỉ mở ứng dụng email
                            putExtra(Intent.EXTRA_SUBJECT, "Tiêu đề email")
                            putExtra(Intent.EXTRA_TEXT, "Nội dung email")
                        }
                        val chooser = Intent.createChooser(intent, "Chia sẻ qua Email:")
                        startActivity(chooser)
                        true
                    }

                    R.id.menu_Favorites_LongclickMuch_ChonTatCa -> {
                        playSound()
                        adapter.selectAll()
                        true
                    }

                    R.id.menu_Favorites_LongclickMuch_LoaiBoKhoiMucYeuThich -> {
                        playSound()
                        for (user in selectedUsers) {
                            ChucNang().danhDauYeuThich(user, requireActivity(), 0)
                        }
                        requireActivity().supportFragmentManager.beginTransaction().apply {
                            replace(R.id.frameLayout, FavoritesFragment())
                            commit()
                        }
                        true
                    }

                    else -> false
                }
            }
        } else {
            val toolbar = requireActivity().findViewById<Toolbar>(R.id.main_Toolbar)
            toolbar.menu.clear()
            toolbar.inflateMenu(R.menu.menu)
            val mainActivity = requireActivity() as MainActivity
            mainActivity.setupToolbar()
        }
    }

    override fun onSelectedGroupsChanged(selectedGroups: MutableList<Group>) {
        TODO("Not yet implemented")
    }
    private fun setupSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(1)
            .setAudioAttributes(audioAttributes)
            .build()
        clickSoundId = soundPool?.load(getContext(), R.raw.click, 1) ?: 0
    }

    private fun playSound() {
        soundPool!!.play(clickSoundId, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    // Quan trọng: giải phóng tài nguyên khi view bị hủy
    override fun onDestroyView() {
        super.onDestroyView()
        release()
    }
    fun release() {
        if (soundPool != null) {
            soundPool!!.release()
            soundPool = null
        }
    }
}