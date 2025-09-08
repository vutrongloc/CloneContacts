package com.example.clonecontacts.Fragment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
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

class UserGroupsFragment : Fragment(), DsAdapter.OnSelectedUsersChangeListener {
    var dsUser: MutableList<User> = mutableListOf()
    var nameGroup = ""
    lateinit var adapter: DsAdapter

    private var soundPool: SoundPool? = null
    private var clickSoundId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user_groups, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupSoundPool()
        val dsUser = arguments?.getSerializable("dsUserGroup") as MutableList<User>
        nameGroup = arguments?.getString("tenGroup").toString()
        getContacts()
        if (!dsUser.isNullOrEmpty()) {
            val nhomNayTrong = view.findViewById<TextView>(R.id.user_Groups_NhomNayTrong)
             nhomNayTrong.layoutParams.height = 0
            nhomNayTrong.layoutParams.width = 0
        }
        val toolBar = requireActivity().findViewById<Toolbar>(R.id.main_Toolbar)

        // Lấy màu hiện tại của Toolbar
        val toolbarColor = (toolBar.background as ColorDrawable).color
        val drawable2 = ContextCompat.getDrawable(requireActivity(), R.drawable.bovuong)?.mutate() as GradientDrawable
        drawable2.setColor(toolbarColor)
        val add = view.findViewById<ImageView>(R.id.user_Groups_add)
        add.background = drawable2
        add.setOnClickListener {
            playSound()
            themContactVaoGroup()
        }
        super.onViewCreated(view, savedInstanceState)
    }

    fun chiase(selectedUsers: MutableList<User>) {

        var ds_Email = listOf<String>()
        for (user in selectedUsers){
            val email = ChucNang().getEmailFromPhone(requireActivity(), user.mobile)
            if ( email != null){
                ds_Email += email
            }
        }
        if ( ds_Email.isNotEmpty() ){
            ChucNang().moUngDungEmail(requireActivity(), ds_Email)
        } else {
            adapter.deselectAll()
            Toast.makeText(requireActivity(), "Các số vừa chọn không thấy email", Toast.LENGTH_SHORT).show()
        }
        true

    }


    fun getGroupContacts(context: Context, groupName: String): List<String> {
        val phoneNumbers = mutableListOf<String>()

        val groupCursor = context.contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            arrayOf(ContactsContract.Groups._ID),
            "${ContactsContract.Groups.TITLE} = ?",
            arrayOf(groupName),
            null
        )

        groupCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                val groupId = cursor.getString(0)

                val selection =
                    "${ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID} = ? AND " +
                            "${ContactsContract.Data.MIMETYPE} = '${ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE}'"

                val membersCursor = context.contentResolver.query(
                    ContactsContract.Data.CONTENT_URI,
                    arrayOf(ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID),
                    selection,
                    arrayOf(groupId),
                    null
                )

                membersCursor?.use { mCursor ->
                    while (mCursor.moveToNext()) {
                        val contactId = mCursor.getString(0)

                        val phoneCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )

                        phoneCursor?.use { pCursor ->
                            while (pCursor.moveToNext()) {
                                phoneNumbers.add(pCursor.getString(0))
                            }
                        }
                    }
                }
            }
        }
        return phoneNumbers
    }

    private fun getContacts() {
        val contentResolver = requireContext().contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null, null, null
        )

        cursor?.use {
            dsUser = mutableListOf()
            while (it.moveToNext()) {
                val name =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val phoneNumber =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                dsUser.add(User(name, phoneNumber))
            }
        }
        var dsUserInGroup: MutableList<User> = mutableListOf()
        for (user in dsUser) {
            val contactId = ChucNang().getContactIDByPhoneNumber(user.mobile,requireActivity())
            val groupId =ChucNang().getGroupIDByNameGroup(nameGroup,requireActivity())
            contactId?.let {
                if (ChucNang().kiemTraXemSDTDaCoTrongGroupHayChua(it, groupId!!,requireActivity())?.count ?: 0 > 0) {
                    dsUserInGroup.add(user)

                }
            }
        }
        adapter = DsAdapter(dsUserInGroup, mutableListOf(), requireActivity())
        adapter.listener = this
        val recyclerView = view?.findViewById<RecyclerView>(R.id.recyclerView_User_Groups)
        recyclerView?.layoutManager =
            LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
        recyclerView?.adapter = adapter

    }

    fun themContactVaoGroup() {
        var array: MutableList<User> = mutableListOf()
        var checkedItems: MutableList<Boolean> = mutableListOf()
        for (user in dsUser) {
            val contactId = ChucNang().getContactIDByPhoneNumber(user.mobile,requireActivity())
            val groupId =ChucNang().getGroupIDByNameGroup(nameGroup,requireActivity())
            contactId?.let {
                if (ChucNang().kiemTraXemSDTDaCoTrongGroupHayChua(it, groupId!!,requireActivity())?.count ?: 0 > 0) {
                    checkedItems.add(true)
                    array.add(user)
                } else {
                    checkedItems.add(false)
                    array.add(user)
                }
            }
        }

        val dialogView =
            LayoutInflater.from(activity).inflate(R.layout.dialog_contacts_favorites, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerContacts)

        val adapter = ContactsFavoritesAdapter(
            array,
            mutableListOf(),
            checkedItems,
            onCheckedChangeUser = { user, isChecked ->
                ChucNang().danhDauGroup(user, nameGroup,requireActivity(),"Ton tai thi xoa")
                val nhomNayTrong = view?.findViewById<TextView>(R.id.user_Groups_NhomNayTrong)
                nhomNayTrong?.setText("")
                nhomNayTrong?.layoutParams?.height = 0
                nhomNayTrong?.layoutParams?.width = 0
                getContacts()
            },
            onCheckedChangeGroup = { group, ischecked ->
            })

        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter

        val dialog = AlertDialog.Builder(activity)
            .setView(dialogView)
            .create()
        val toolBar = requireActivity().findViewById<Toolbar>(R.id.main_Toolbar)

        // Lấy màu hiện tại của Toolbar
        val toolbarColor = (toolBar?.background as ColorDrawable).color
        val drawable2 = ContextCompat.getDrawable(requireActivity(), R.drawable.bovuong)
            ?.mutate() as GradientDrawable
        drawable2.setColor(toolbarColor)
        dialog.window?.setBackgroundDrawable(drawable2)
        dialog.show()
    }

    override fun onStop() {
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.main_Toolbar)
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu_group)
        val tenFragment = requireActivity().findViewById<TextView>(R.id.main_tenFragment)
        tenFragment.setText("Contacts")
        super.onStop()
    }

    override fun onResume() {
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.main_Toolbar)
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu_user_group)
        val guiSMS = toolbar.menu.findItem(R.id.menu_Group_Chat)
        guiSMS.setOnMenuItemClickListener{
            playSound()
            val phoneNumbers = getGroupContacts(requireActivity(), nameGroup)
            if (phoneNumbers.isNotEmpty()) {
                val uri = Uri.parse("sms:" + phoneNumbers.joinToString(","))
                val intent = Intent(Intent.ACTION_VIEW, uri)
                intent.putExtra("sms_body", "Hello")
                startActivity(intent)
            } else {
                Toast.makeText(
                    requireActivity(),
                    "Không có số điện thoại nào trong nhóm!",
                    Toast.LENGTH_SHORT
                ).show()
            }
            true
        }
        val guiemail = toolbar.menu.findItem(R.id.menu_Group_Email)
        guiemail.setOnMenuItemClickListener{
            playSound()
            chiase(dsUser)
            true
        }
        val sharedPref = requireActivity().getSharedPreferences("CaiDat", Context.MODE_PRIVATE)
        val hienThiHinh = sharedPref.getBoolean("thu_nho_lien_he", true)
        if(!hienThiHinh){
            ChucNang().anHinhNgay(adapter)
        }
        val hienThiSDT = sharedPref.getBoolean("hien_thi_sdt",false)
        if(!hienThiHinh){
            ChucNang().anHinhNgay(adapter)
        }
        if (hienThiSDT){
            ChucNang().hienSDT(adapter)
        }
        val tenFragment = requireActivity().findViewById<TextView>(R.id.main_tenFragment)
        tenFragment.setText(nameGroup)
        super.onResume()
    }

    override fun onSelectedUsersChanged(selectedUsers: MutableList<User>) {
        val toolbar = requireActivity().findViewById<Toolbar>(R.id.main_Toolbar)
        toolbar.menu.clear()
        if(selectedUsers.size >= 1){
            toolbar.inflateMenu(R.menu.menu_usergroup_longclickoneormuch)
        }
        if (selectedUsers.isEmpty()) {
            toolbar.inflateMenu(R.menu.menu_user_group)
            (requireActivity() as MainActivity).setupToolbar()
            return
        }
        val chinhSua = toolbar.menu.findItem(R.id.menu_UserGroup_LongClickOneOrMuch_ChinhSua)
        chinhSua?.isVisible = selectedUsers.size == 1
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_Group_Chat -> {
                    playSound()
                    val phoneNumbers = getGroupContacts(requireActivity(), nameGroup)
                    if (phoneNumbers.isNotEmpty()) {
                        val uri = Uri.parse("sms:" + phoneNumbers.joinToString(","))
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        intent.putExtra("sms_body", "Hello")
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            requireActivity(),
                            "Không có số điện thoại nào trong nhóm!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    true
                }
                R.id.menu_UserGroup_LongClickOneOrMuch_ChinhSua -> {
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
                R.id.menu_UserGroup_LongClickOneOrMuch_ChiaSe -> {
                    playSound()
                    if(selectedUsers.size > 1){
                        ChucNang().chiaSeNhieuUser(selectedUsers, requireActivity())
                    }
                    else if (selectedUsers.size == 1){
                        ChucNang().chiaSe(selectedUsers[0], requireActivity())
                    }
                    true
                }
                R.id.menu_UserGroup_LongClickOneOrMuch_GuiSMS -> {
                    playSound()
                    var phoneNumbers:MutableList<String> = mutableListOf()
                    for(user in selectedUsers){
                        phoneNumbers.add(user.mobile)
                    }
                    if (phoneNumbers.isNotEmpty()) {
                        val uri = Uri.parse("sms:" + phoneNumbers.joinToString(","))
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        intent.putExtra("sms_body", "Hello")
                        startActivity(intent)
                    } else {
                        Toast.makeText(
                            requireActivity(),
                            "Không có số điện thoại nào trong nhóm!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    true
                }
                R.id.menu_UserGroup_LongClickOneOrMuch_GuiEmail -> {
                    playSound()
                    chiase(selectedUsers)
                    true
                }
                R.id.menu_UserGroup_LongClickOneOrMuch_ChonTatCa -> {
                    playSound()
                    adapter.selectAll()
                    true
                }
                R.id.menu_UserGroup_LongClickOneOrMuch_XoaBo -> {
                    playSound()
                    for(user in selectedUsers){
                        ChucNang().deleteContact(requireContext(),user.mobile)
                    }
                    true
                }
                R.id.menu_UserGroup_LongClickOneOrMuch_LoaiBoKhoiNhom -> {
                    playSound()
                    for(user in selectedUsers){
                        ChucNang().danhDauGroup(user, nameGroup,requireActivity(),"Ton tai thi xoa")
                    }
                    getContacts()
                    adapter.deselectAll()
                    true
                }
                else -> {
                    true
                }
            }

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