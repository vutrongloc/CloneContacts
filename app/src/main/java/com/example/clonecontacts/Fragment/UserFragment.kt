package com.example.clonecontacts.Fragment

import android.content.ContentValues
import android.content.Context
import android.content.Context.TELECOM_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.example.clonecontacts.ChucNang
import com.example.clonecontacts.R
import com.example.clonecontacts.User
import com.example.clonecontacts.activity.OutgoingCallActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.jar.Manifest


class UserFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val user = arguments?.getSerializable("user") as User
        val ten = view.findViewById<TextView>(R.id.textView1)
        ten.setText(user.name)
        val nen = view.findViewById<LinearLayout>(R.id.anhNen)
        ChucNang().thayDoiAnhDaiDien(nen, user.name)
        goiDien(user)
        val nhanTin = view.findViewById<ImageView>(R.id.user_mess)
        nhanTin.setOnClickListener {
            ChucNang().sendSMS(user.mobile, "",requireActivity())
        }
        val chinhSua = view.findViewById<ImageView>(R.id.user_pen)
        chinhSua.setOnClickListener {
            chinhSua(user.mobile)
        }
        val delete = view.findViewById<ImageView>(R.id.user_delete)
        delete.setOnClickListener {
            ChucNang().kiemTraQuyenGhiDanhBaDeXoaContacts(user.mobile,requireActivity(),UserFragment(),true)
        }
        val chiaSe = view.findViewById<ImageView>(R.id.user_share)
        chiaSe.setOnClickListener {
            ChucNang().chiaSe(user,requireActivity())
        }
        val danhDauYeuThich = view.findViewById<ImageView>(R.id.user_star)
        val contactId = getContactIdByPhoneNumber(user.mobile)
        contactId?.let {
            if (kiemTraXemDaDuocDanhDauYeuThichHayChua(it) != 1) {
                danhDauYeuThich.setImageResource(R.drawable.baseline_star_24)
            } else {
                danhDauYeuThich.setImageResource(R.drawable.star)
            }
        }
        danhDauYeuThich.setOnClickListener {
            danhDauYeuThich(user.mobile, danhDauYeuThich)
        }
        val back = view.findViewById<ImageView>(R.id.user_back)
        back.setOnClickListener {
            ChucNang().quayLaiFragment(requireActivity(),UserFragment())
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    requireActivity().supportFragmentManager.popBackStack()
                    val toolbar = requireActivity().findViewById<Toolbar>(R.id.main_Toolbar)
                    toolbar.visibility = View.VISIBLE
                    val bottom = requireActivity().findViewById<BottomNavigationView>(R.id.bottom)
                    bottom.visibility = View.VISIBLE
                }
            }
        )
        super.onViewCreated(view, savedInstanceState)
    }



    fun danhDauYeuThich(phoneNumber: String, Image: ImageView) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.WRITE_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_CONTACTS), 4)
        } else {
            val contactId = getContactIdByPhoneNumber(phoneNumber)
            contactId?.let {
                var newFavoriteState = kiemTraXemDaDuocDanhDauYeuThichHayChua(it)
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
                    if (newFavoriteState == 1) {
                        Image.setImageResource(R.drawable.baseline_star_24)
                        Toast.makeText(
                            requireContext(),
                            "Cập nhật vào danh sách yêu thích",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Image.setImageResource(R.drawable.star)
                        Toast.makeText(
                            requireContext(),
                            "Xóa khỏi danh sách yêu thích",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Cập nhật không thành công!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } ?: Toast.makeText(requireContext(), "Không tìm thấy liên hệ!", Toast.LENGTH_SHORT).show()
    }

    fun kiemTraXemDaDuocDanhDauYeuThichHayChua(contactId: Long): Int {
        val contentResolver = requireContext().contentResolver
        val uri = ContactsContract.Contacts.CONTENT_URI

        // Kiểm tra trạng thái hiện tại của dấu sao
        val projection = arrayOf(ContactsContract.Contacts.STARRED)
        val selection = "${ContactsContract.Contacts._ID} = ?"
        val selectionArgs = arrayOf(contactId.toString())

        var isFavorite = false
        contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                isFavorite = cursor.getInt(0) == 1 // Nếu là 1 thì đang có dấu sao
            }
        }

        // Đảo trạng thái dấu sao
        val newFavoriteState = if (isFavorite) 0 else 1
        return newFavoriteState
    }


    fun getContactIdByPhoneNumber(phoneNumber: String): Long? {
        val contentResolver = requireContext().contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?"
        val selectionArgs = arrayOf(phoneNumber)

        contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val contactId = cursor.getLong(0)
                return contactId
            }
        }
        return null
    }
    fun chinhSua(phoneNumber: String) {
        val contactUri = getContactUriByPhoneNumber(phoneNumber)
        contactUri?.let {
            val intentEdit = Intent(Intent.ACTION_EDIT).apply {
                data = it
            }
            startActivity(intentEdit)
        } ?: Toast.makeText(requireContext(), "Không tìm thấy liên hệ", Toast.LENGTH_SHORT).show()
    }

    fun getContactUriByPhoneNumber(phoneNumber: String): Uri? {
        val contentResolver = requireContext().contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY
            ),
            "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
            arrayOf(phoneNumber),
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val contactId =
                    it.getLong(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                val lookupKey =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY))
                return ContactsContract.Contacts.getLookupUri(contactId, lookupKey)
            }
        }
        return null
    }

    fun goiDien(user: User) {
        val sdt = view?.findViewById<TextView>(R.id.textView2)
        val icGoi = view?.findViewById<ImageView>(R.id.user_call)
        sdt?.setText(user.mobile)
        sdt?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireActivity(),
                    android.Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(android.Manifest.permission.CALL_PHONE),
                    1
                )

            } else {
                makeCall(user)
            }
        }
        icGoi?.setOnClickListener {
            if (ContextCompat.checkSelfPermission(
                    requireActivity(),
                    android.Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    requireActivity(), arrayOf(android.Manifest.permission.CALL_PHONE),
                    1
                )

            } else {
                makeCall(user)
            }
        }
    }

    fun makeCall(user: User) {
        val telecomManager = requireActivity().getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val uri = Uri.parse("tel:${user.mobile}")
        val extras = Bundle()

        if (ContextCompat.checkSelfPermission(requireActivity(),android.Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(android.Manifest.permission.CALL_PHONE), 1)
        } else {
            telecomManager.placeCall(uri, extras)
            // Chuyển sang OutgoingCallActivity
            val intent = Intent(requireActivity(), OutgoingCallActivity::class.java)
            intent.putExtra("callee_number", user.mobile)
            intent.putExtra("callee_name", user.name)
            startActivity(intent)
        }
    }
}