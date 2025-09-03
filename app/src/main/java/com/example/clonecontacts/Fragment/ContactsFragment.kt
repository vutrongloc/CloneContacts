package com.example.clonecontacts.Fragment

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.speech.RecognizerIntent
import android.telecom.TelecomManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clonecontacts.Adapter.DsAdapter
import com.example.clonecontacts.ChucNang
import com.example.clonecontacts.Model.Group
import com.example.clonecontacts.activity.MainActivity
import com.example.clonecontacts.R
import com.example.clonecontacts.Model.User
import java.util.Collections.swap


class ContactsFragment : Fragment(), DsAdapter.OnSelectedUsersChangeListener {
    lateinit var adapter: DsAdapter
    var dsUser: MutableList<User> = mutableListOf()
    lateinit var editTextTimKiem: EditText
    lateinit var keyboard: ImageView
    lateinit var mic: ImageView
    private val REQUEST_CODE_SPEECH = 100
    private var textWatcher: TextWatcher? = null // Store the TextWatcher
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_contacts, container, false)
    }

    var tangHoacGiam = true
    var firstOrLast = true
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tangHoacGiam = arguments?.getBoolean("TangHoacGiam") ?: true
        firstOrLast = arguments?.getBoolean("FirstOrLast") ?: true
        yeuCauQuyenDayDu()
        timKiem(dsUser)
        keyboard = view.findViewById<ImageView>(R.id.contacts_Keyboard)
        keyboard.setOnClickListener {
            ChucNang().open_KeyBroad(requireActivity())
        }
        mic = view.findViewById(R.id.contacts_mic)
        mic.setColorFilter(Color.WHITE)
        mic.setOnClickListener {
            startVoiceRecognition()
        }

    }

    fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN") // Tiếng Việt
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Nói tên hoặc số điện thoại...")
        }
        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH)
        } catch (e: Exception) {
            Toast.makeText(
                requireActivity(),
                "Không hỗ trợ nhận diện giọng nói!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_SPEECH && resultCode == RESULT_OK) {
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spokenText = matches?.get(0) ?: return

            // Nếu nói là số điện thoại
            if (spokenText.replace("\\s".toRegex(), "").matches(Regex("^0\\d{9}$"))) {
                ChucNang().makeCall(
                    User(
                        ChucNang().getContactNameFromNumber(
                            requireActivity(),
                            spokenText.replace("\\s".toRegex(), "")
                        ) ?: "Không rõ", spokenText.replace("\\s".toRegex(), "")
                    ), requireActivity()
                )
            } else {
                // Nếu nói tên thì tìm trong danh bạ
                val phone = ChucNang().findPhoneNumberByName(spokenText, requireActivity())
                if (phone != null) {
                    ChucNang().makeCall(
                        User(
                            ChucNang().getContactNameFromNumber(
                                requireActivity(),
                                phone
                            ) ?: "Không rõ", phone
                        ), requireActivity()
                    )
                } else {
                    Toast.makeText(
                        requireActivity(),
                        "Không tìm thấy số cho $spokenText",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
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

    override fun onResume() {
        yeuCauQuyenDayDu()
        ChucNang().updateBotronColor(requireActivity(), keyboard, mic)
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.main_Toolbar)
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu)
        val activity = requireActivity() as MainActivity
        val caidat = toolbar.menu.findItem(R.id.caiDat)
        caidat.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.caiDat -> {
                    ChucNang().diaLog_CaiDat(requireActivity(), adapter)
                    true
                }

                else -> false
            }
        }
        activity.setupToolbar()
        val sharedPref = activity.getSharedPreferences("CaiDat", Context.MODE_PRIVATE)
        val hienThiHinh = sharedPref.getBoolean("thu_nho_lien_he", true)
        val hienThiSDT = sharedPref.getBoolean("hien_thi_sdt", false)
        if (!hienThiHinh) {
            ChucNang().anHinhNgay(adapter)
        }
        if (hienThiSDT) {
            ChucNang().hienSDT(adapter)
        }
        textWatcher?.let { editTextTimKiem.addTextChangedListener(it) }
        super.onResume()
    }

    fun timKiem(dsUser: MutableList<User>) {
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
                    for (user in dsUser) {
                        if (user.name.toLowerCase().contains(textTimKiem.toLowerCase())) {
                            dsUserKetQuaTimKiem.add(user)
                        }
                    }
                    adapter = DsAdapter(dsUserKetQuaTimKiem, mutableListOf(), requireActivity())
                    adapter.listener = this@ContactsFragment
                    adapter.deselectAll()
                    val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView)
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
                Log.d("timkiem", "bamvaonutthoat2")
                editTextTimKiem.setText("")
                val thanhTimKiem =
                    requireActivity().findViewById<EditText>(R.id.main_EdittextTimKiem)
                thanhTimKiem.visibility = View.GONE
                nutThoat.visibility = View.GONE
                val tenMain = requireActivity().findViewById<TextView>(R.id.main_tenFragment)
                tenMain.visibility = View.VISIBLE
                adapter.deselectAll()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val thanhTimKiem =
            requireActivity().findViewById<EditText>(R.id.main_EdittextTimKiem)
        thanhTimKiem.visibility = View.GONE
        val nutThoat = requireActivity().findViewById<ImageView>(R.id.main_Back)
        nutThoat.visibility = View.GONE
        val tenMain = requireActivity().findViewById<TextView>(R.id.main_tenFragment)
        tenMain.visibility = View.VISIBLE
        editTextTimKiem.setText("")
        textWatcher?.let { editTextTimKiem.removeTextChangedListener(it) } // Remove the TextWatcher
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
        Log.d("sapxep", "da tra ve")
        return dsUser
    }

    fun getContacts(tangHoacGiam: Boolean, firstOrLast: Boolean) {
        val contentResolver = requireContext().contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC" // Sắp xếp theo tên tăng dần (A -> Z)
        )
        dsUser.clear() // Dọn dẹp dsUser trước khi thêm mới

        cursor?.use {
            while (it.moveToNext()) {
                val name =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phoneNumber =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))

                // Chỉ thêm user mới nếu chưa tồn tại trong danh sách (tránh trùng lặp)
                if (dsUser.none { user -> user.name == name }) {
                    dsUser.add(User(name, phoneNumber))
                    Log.d("tenNguoiDung", name)
                }
            }

            // Gọi hàm sắp xếp
            dsUser = sapXepTangHoacGiam(tangHoacGiam, firstOrLast, dsUser)

            adapter = DsAdapter(dsUser, mutableListOf(), requireActivity())
            adapter.listener = this@ContactsFragment
            val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView)
            recyclerView?.layoutManager =
                LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
            recyclerView?.adapter = adapter
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onSelectedUsersChanged(selectedUsers: MutableList<User>) {
        Log.d("timkiem", "onLongClick")
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.main_Toolbar)
        if (selectedUsers.size == 1) {
            toolbar.menu.clear()
            toolbar.inflateMenu(R.menu.menu_longclickone)
            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_LongClickOne_ChinhSua -> {
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

                    R.id.menu_LongClickOne_DanhDauYeuThich -> {
                        ChucNang().danhDauYeuThich(selectedUsers[0], requireActivity(), 1)
                        adapter.deselectAll()
                        true
                    }

                    R.id.menu_LongClickOne_ChiaSe -> {
                        ChucNang().chiaSe(selectedUsers[0], requireActivity())
                        true
                    }

                    R.id.menu_LongClickOne_guiSmS -> {
                        ChucNang().sendSMS(selectedUsers[0].mobile, "", requireActivity())
                        true
                    }

                    R.id.menu_LongClickOne_GuiEmail -> {
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

                    R.id.menu_LongClickOne_XoaBo -> {
                        ChucNang().kiemTraQuyenGhiDanhBaDeXoaContacts(
                            selectedUsers[0].mobile,
                            requireActivity(),
                            User(),
                            false
                        )
                        requireActivity().supportFragmentManager.beginTransaction().apply {
                            replace(R.id.frameLayout, ContactsFragment())
                            commit()
                        }
                        true
                    }

                    R.id.menu_LongClickOne_TaoPhimTat -> {
                        ChucNang().createShortcut(
                            selectedUsers[0],
                            ChucNang().traAnhDaiDien(selectedUsers[0].name),
                            requireActivity()
                        )
                        true
                    }

                    R.id.menu_LongClickOne_ThemvaoNhom -> {
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
                                requireActivity(),
                                selectedUsers[0],
                                mutableListOf()
                            )
                            adapter.deselectAll()
                        }
                        true
                    }

                    R.id.menu_LongClickOne_ChonTatCa -> {
                        adapter.selectAll()
                        true
                    }

                    else -> false
                }
            }
        } else if (selectedUsers.size > 1) {
            toolbar.menu.clear()
            toolbar.inflateMenu(R.menu.menu_longclickmuch)
            toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_LongClickMuch_DanhDauYeuThich -> {
                        for (user in selectedUsers) {
                            ChucNang().danhDauYeuThich(user, requireActivity(), 1)
                        }
                        adapter.deselectAll()
                        true
                    }

                    R.id.menu_LongClickMuch_ThemVaoGroup -> {
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
                                replace(R.id.frameLayout, ContactsFragment())
                                commit()
                            }
                        }
                        true
                    }

                    R.id.menu_LongClickMuch_ChiaSe -> {
                        ChucNang().chiaSeNhieuUser(selectedUsers, requireActivity())
                        true
                    }

                    R.id.menu_LongClickMuch_XoaBo -> {
                        val permission = Manifest.permission.WRITE_CONTACTS
                        if (ContextCompat.checkSelfPermission(
                                requireActivity(),
                                permission
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            ActivityCompat.requestPermissions(
                                requireActivity(),
                                arrayOf(permission),
                                3
                            )
                        } else {
                            // Quyền đã được cấp, thực hiện xóa danh bạ
                            for (user in selectedUsers) {
                                ChucNang().deleteContact(requireActivity(), user.mobile)
                            }

                            requireActivity().supportFragmentManager.beginTransaction().apply {
                                replace(R.id.frameLayout, ContactsFragment())
                                commit()
                            }
                        }
                        true
                    }

                    R.id.menu_LongClickMuch_boChonTatCa -> {
                        adapter.deselectAll()
                        true
                    }

                    R.id.menu_LongClickMuch_GuiSMS -> {
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

                    R.id.menu_LongClickMuch_GuiEmail -> {
                        var ds_Email = listOf<String>()
                        for (user in selectedUsers) {
                            val email = ChucNang().getEmailFromPhone(requireActivity(), user.mobile)
                            if (email != null) {
                                ds_Email += email
                            }
                        }
                        if (ds_Email.isNotEmpty()) {
                            ChucNang().moUngDungEmail(requireActivity(), ds_Email)
                        } else {
                            adapter.deselectAll()
                            Toast.makeText(
                                requireActivity(),
                                "Các số vừa chọn không thấy email",
                                Toast.LENGTH_SHORT
                            ).show()
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

    fun checkAndRequestPermissionsREAD_CONTACTS() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_CONTACTS), 100
            )
            return
        }
    }

}
