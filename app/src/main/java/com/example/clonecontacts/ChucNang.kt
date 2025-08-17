package com.example.clonecontacts

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.content.res.ColorStateList
import android.database.Cursor
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.clonecontacts.Adapter.DsAdapter
import com.example.clonecontacts.Model.Group
import com.example.clonecontacts.Model.User
import com.example.clonecontacts.activity.OutgoingCallActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.slider.Slider
import java.io.File
import java.io.FileOutputStream

class ChucNang {
    fun chiaSe(user: User, activity: FragmentActivity) {
        // Kiểm tra quyền READ_CONTACTS
        if (ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.READ_CONTACTS),
                7
            )
            return
        }

        // Lấy thông tin liên hệ
        val contentResolver = activity.contentResolver
        val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?"
        val selectionArgs = arrayOf(user.mobile)

        var contactName: String? = null
        var contactNumber: String? = null
        contentResolver.query(phoneUri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                contactName =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                contactNumber =
                    cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
            }
        }

        if (contactName == null || contactNumber == null) {
            Log.d("ChiaSe", "Không tìm thấy số điện thoại trong danh bạ")
            Toast.makeText(activity, "Không tìm thấy liên hệ!", Toast.LENGTH_SHORT).show()
            return
        }

        // Tạo nội dung vCard
        val vCardContent = """
        BEGIN:VCARD
        VERSION:3.0
        N:$contactName
        TEL:$contactNumber
        END:VCARD
    """.trimIndent()

        try {
            // Tạo file tendanhba.vcf trong thư mục cache
            val file = File(activity.cacheDir, user.name + ".vcf")
            FileOutputStream(file).use { outputStream ->
                outputStream.write(vCardContent.toByteArray())
            }

            // Lấy URI cho file bằng FileProvider
            val fileUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider", // Đảm bảo bạn đã cấu hình FileProvider trong manifest
                file
            )

            // Tạo Intent để chia sẻ
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/x-vcard"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(Intent.createChooser(intent, "Chia sẻ danh bạ qua:"))
            } else {
                Log.d("ChiaSe", "Không có ứng dụng hỗ trợ chia sẻ")
                Toast.makeText(
                    activity,
                    "Không có ứng dụng hỗ trợ chia sẻ!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("ChiaSe", "Lỗi khi chia sẻ danh bạ: ${e.message}")
            Toast.makeText(activity, "Lỗi khi chia sẻ: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun chiaSeNhieuUser(users: MutableList<User>, activity: FragmentActivity) {
        // Kiểm tra quyền READ_CONTACTS
        if (ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.READ_CONTACTS),
                7
            )
            return
        }

        val contentResolver = activity.contentResolver
        val phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )

        val vCardContents = StringBuilder() // Để lưu toàn bộ nội dung các liên hệ

        for (user in users) {
            val selection = "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?"
            val selectionArgs = arrayOf(user.mobile)

            var contactName: String? = null
            var contactNumber: String? = null

            contentResolver.query(phoneUri, projection, selection, selectionArgs, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        contactName =
                            cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                        contactNumber =
                            cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    }
                }

            if (contactName == null || contactNumber == null) {
                Log.d("ChiaSe", "Không tìm thấy số điện thoại của ${user.name}")
                continue // Bỏ qua user này nếu không tìm thấy thông tin
            }

            // Tạo nội dung vCard cho từng người dùng
            vCardContents.append(
                """
            BEGIN:VCARD
            VERSION:3.0
            N:$contactName
            TEL:$contactNumber
            END:VCARD
            """.trimIndent()
            ).append("\n") // Thêm dòng mới để phân cách giữa các vCard
        }

        if (vCardContents.isEmpty()) {
            Toast.makeText(activity, "Không tìm thấy liên hệ nào để chia sẻ!", Toast.LENGTH_SHORT)
                .show()
            return
        }

        try {
            // Tạo file .vcf trong thư mục cache
            val file = File(activity.cacheDir, "danhba.vcf")
            FileOutputStream(file).use { outputStream ->
                outputStream.write(vCardContents.toString().toByteArray())
            }

            // Lấy URI cho file bằng FileProvider
            val fileUri = FileProvider.getUriForFile(
                activity,
                "${activity.packageName}.fileprovider",
                file
            )

            // Tạo Intent để chia sẻ
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/x-vcard"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(Intent.createChooser(intent, "Chia sẻ danh bạ qua:"))
            } else {
                Log.d("ChiaSe", "Không có ứng dụng hỗ trợ chia sẻ")
                Toast.makeText(activity, "Không có ứng dụng hỗ trợ chia sẻ!", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("ChiaSe", "Lỗi khi chia sẻ danh bạ: ${e.message}")
            Toast.makeText(activity, "Lỗi khi chia sẻ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun moUngDungEmail(activity: FragmentActivity) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data =
                Uri.parse("mailto:")  // Chỉ cần đặt data là "mailto:" để mở trình soạn thảo email trống
        }
        activity.startActivity(intent)
    }

    fun sendSMS(phoneNumber: String, message: String, activity: FragmentActivity) {
        if (ContextCompat.checkSelfPermission(
                activity,
                android.Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(android.Manifest.permission.SEND_SMS),
                2
            )
            Log.d("nhantin","aaa")
        } else {
            val smsIntent = Intent(
                Intent.ACTION_SENDTO, Uri.parse(
                    "smsto:$phoneNumber"
                )
            )
            smsIntent.putExtra("sms_body", message)
            activity.startActivity(smsIntent)
        }
    }

    fun kiemTraQuyenGhiDanhBaDeXoaContacts(
        phoneNumber: String,
        activity: FragmentActivity,
        fragment: Fragment,
        dieuKien: Boolean
    ) {
        val permission = android.Manifest.permission.WRITE_CONTACTS
        if (ContextCompat.checkSelfPermission(
                activity,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(activity, arrayOf(permission), 3)
        } else {
            // Quyền đã được cấp, thực hiện xóa danh bạ
            deleteContact(activity, phoneNumber)
            if (dieuKien) {
                quayLaiFragment(activity, fragment)
            }
        }
    }

    fun deleteContact(context: Context, phoneNumber: String) {
        val contentResolver = context.contentResolver

        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val cursor = contentResolver.query(
            uri, arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
            "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?", arrayOf(phoneNumber), null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val contactId =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                val deleteUri =
                    Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
                contentResolver.delete(deleteUri, null, null)
                //Toast.makeText(context, "Xóa thành công!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Không tìm thấy số này!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun quayLaiFragment(activity: FragmentActivity, fragment: Fragment) {
        val isPopped = activity.supportFragmentManager.popBackStackImmediate()
        if (isPopped != true) {
            activity.supportFragmentManager.beginTransaction().apply {
                replace(R.id.frameLayout, fragment)
                commit()
            }
        }
        val toolbar = activity.findViewById<Toolbar>(R.id.main_Toolbar)
        toolbar.visibility = View.VISIBLE
        val bottom = activity.findViewById<BottomNavigationView>(R.id.bottom)
        bottom.visibility = View.VISIBLE
    }

    fun thayDoiAnhDaiDien(linearLayout: LinearLayout, text: String) {
        when (text[0]) {
            'A' -> linearLayout.setBackgroundResource(R.drawable.a)
            'B' -> linearLayout.setBackgroundResource(R.drawable.b)
            'C' -> linearLayout.setBackgroundResource(R.drawable.c)
            'D' -> linearLayout.setBackgroundResource(R.drawable.d)
            'E' -> linearLayout.setBackgroundResource(R.drawable.e)
            'F' -> linearLayout.setBackgroundResource(R.drawable.f)
            'G' -> linearLayout.setBackgroundResource(R.drawable.g)
            'H' -> linearLayout.setBackgroundResource(R.drawable.h)
            'I' -> linearLayout.setBackgroundResource(R.drawable.i)
            'J' -> linearLayout.setBackgroundResource(R.drawable.j)
            'K' -> linearLayout.setBackgroundResource(R.drawable.k)
            'L' -> linearLayout.setBackgroundResource(R.drawable.l)
            'M' -> linearLayout.setBackgroundResource(R.drawable.m)
            'N' -> linearLayout.setBackgroundResource(R.drawable.n)
            'O' -> linearLayout.setBackgroundResource(R.drawable.o)
            'P' -> linearLayout.setBackgroundResource(R.drawable.p)
            'Q' -> linearLayout.setBackgroundResource(R.drawable.q)
            'R' -> linearLayout.setBackgroundResource(R.drawable.r)
            'S' -> linearLayout.setBackgroundResource(R.drawable.s)
            'T' -> linearLayout.setBackgroundResource(R.drawable.t)
            'U' -> linearLayout.setBackgroundResource(R.drawable.u)
            'V' -> linearLayout.setBackgroundResource(R.drawable.v)
            'W' -> linearLayout.setBackgroundResource(R.drawable.w)
            'X' -> linearLayout.setBackgroundResource(R.drawable.x)
            'Y' -> linearLayout.setBackgroundResource(R.drawable.y)
            'Z' -> linearLayout.setBackgroundResource(R.drawable.z)
        }
    }

    fun traAnhDaiDien(text: String): Int {
        when (text[0]) {
            'A' -> return R.drawable.a
            'B' -> return R.drawable.b
            'C' -> return R.drawable.c
            'D' -> return (R.drawable.d)
            'E' -> return (R.drawable.e)
            'F' -> return (R.drawable.f)
            'G' -> return (R.drawable.g)
            'H' -> return (R.drawable.h)
            'I' -> return (R.drawable.i)
            'J' -> return (R.drawable.j)
            'K' -> return (R.drawable.k)
            'L' -> return (R.drawable.l)
            'M' -> return (R.drawable.m)
            'N' -> return (R.drawable.n)
            'O' -> return (R.drawable.o)
            'P' -> return (R.drawable.p)
            'Q' -> return (R.drawable.q)
            'R' -> return (R.drawable.r)
            'S' -> return (R.drawable.s)
            'T' -> return (R.drawable.t)
            'U' -> return (R.drawable.u)
            'V' -> return (R.drawable.v)
            'W' -> return (R.drawable.w)
            'X' -> return (R.drawable.x)
            'Y' -> return (R.drawable.y)
            'Z' -> return (R.drawable.z)
            else -> return 0
        }
    }

    fun danhDauGroup(
        user: User,
        groupName: String,
        activity: FragmentActivity,
        dieuKienCoNenXoaSoDaCo: String
    ) {
        Log.d("cochayvao", "c")
        // 1. Tìm ID của nhóm dựa trên tên nhóm
        val groupId = getGroupIDByNameGroup(groupName, activity)
        if (groupId == null) {
            Toast.makeText(activity, "Không tìm thấy nhóm $groupName", Toast.LENGTH_SHORT)
                .show()
            return
        }
        // 2. Tìm ID của liên lạc dựa trên số điện thoại

        val contactId = getContactIDByPhoneNumber(user.mobile, activity)
        if (contactId == null) {
            Toast.makeText(
                activity,
                "Không tìm thấy liên lạc với số ${user.mobile}",
                Toast.LENGTH_SHORT
            )
                .show()
            return
        }

        // 3. Kiểm tra xem liên lạc đã có trong nhóm chưa
        val groupMembershipCursor = kiemTraXemSDTDaCoTrongGroupHayChua(contactId, groupId, activity)
        val operations = ArrayList<ContentProviderOperation>()
        if (groupMembershipCursor?.count ?: 0 > 0) {
            if (dieuKienCoNenXoaSoDaCo.equals("Ton tai thi xoa")) {
                // Nếu đã có trong nhóm, xóa khỏi nhóm
                operations.add(
                    ContentProviderOperation.newDelete(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.CONTACT_ID} = ? AND " +
                                    "${ContactsContract.Data.MIMETYPE} = ? AND " +
                                    "${ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID} = ?",
                            arrayOf(
                                contactId.toString(),
                                ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                                groupId.toString()
                            )
                        )
                        .build()
                )
                Toast.makeText(
                    activity,
                    "Đã xóa ${user.name} khỏi nhóm $groupName",
                    Toast.LENGTH_SHORT
                )
                    .show()
            } else {
                Toast.makeText(
                    activity,
                    "Đã thêm ${user.name} vào nhóm $groupName",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        } else {
            // Nếu chưa có, thêm vào nhóm
            operations.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValue(ContactsContract.Data.RAW_CONTACT_ID, contactId)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE
                    )
                    .withValue(
                        ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID,
                        groupId
                    )
                    .build()
            )
            Toast.makeText(
                activity,
                "Đã thêm ${user.name} vào nhóm $groupName",
                Toast.LENGTH_SHORT
            )
                .show()
            Log.d("cochayvao", "thanhcong")
        }

        groupMembershipCursor?.close()

        // 4. Áp dụng các thay đổi
        try {
            val contentResolver = activity.contentResolver
            contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("Loi them vao group: ", e.message.toString())
        }
    }

    fun getGroupIDByNameGroup(groupName: String, activity: FragmentActivity): Long? {
        val contentResolver = activity.contentResolver
        val groupCursor = contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            arrayOf(ContactsContract.Groups._ID),
            "${ContactsContract.Groups.TITLE} = ?",
            arrayOf(groupName),
            null
        )

        var groupId: Long? = null
        groupCursor?.use {
            if (it.moveToFirst()) {
                groupId = it.getLong(it.getColumnIndexOrThrow(ContactsContract.Groups._ID))
            }
        }

        Log.d("DEBUG", "GroupId được tìm thấy: $groupId")  // Log kết quả
        return groupId
    }


    fun getContactIDByPhoneNumber(phoneNumber: String, activity: FragmentActivity): Long? {
        val contentResolver = activity.contentResolver
        val contactCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
            "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
            arrayOf(phoneNumber),
            null
        )

        var contactId: Long? = null
        contactCursor?.use {
            if (it.moveToFirst()) {
                contactId =
                    it.getLong(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
            }
        }
        return contactId
    }

    fun kiemTraXemSDTDaCoTrongGroupHayChua(
        contactId: Long,
        groupId: Long,
        activity: FragmentActivity
    ): Cursor {
        val contentResolver = activity.contentResolver
        val groupMembershipCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            "${ContactsContract.Data.CONTACT_ID} = ? AND " +
                    "${ContactsContract.Data.MIMETYPE} = ? AND " +
                    "${ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID} = ?",
            arrayOf(
                contactId.toString(),
                ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE,
                groupId.toString()
            ),
            null
        )
        return groupMembershipCursor!!
    }

    fun getContactGroups(activity: FragmentActivity): MutableList<Group> {
        val dsGroups = mutableListOf<Group>()
        val cursor = activity.contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            arrayOf(ContactsContract.Groups._ID, ContactsContract.Groups.TITLE),
            null, null, null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val groupId = it.getString(it.getColumnIndexOrThrow(ContactsContract.Groups._ID))
                val groupName =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.Groups.TITLE))
                
                Log.d(
                    "groups_debug",
                    "Tìm thấy nhóm: $groupName với ID: $groupId"
                ) // Kiểm tra nhóm có được lấy không

                val contacts = getContactsInGroup(groupId, activity)
                Log.d(
                    "groups_debug",
                    "Nhóm: $groupName có ${contacts.size} liên hệ"
                ) // Kiểm tra số lượng liên hệ trong nhóm

                //val contactInfo = contacts.joinToString("\n") { "${it.name}: ${it.mobile}" }
                dsGroups.add(Group(groupName, contacts))
            }
        }
        return dsGroups.distinctBy { it.nameGroup }.toMutableList()
    }

    fun getContactsInGroup(groupId: String, activity: FragmentActivity): MutableList<User> {
        val contacts = mutableListOf<User>()

        val cursor = activity.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.CONTACT_ID,  // ID của liên hệ
                ContactsContract.CommonDataKinds.GroupMembership.CONTACT_ID // Liên hệ thuộc nhóm nào
            ),
            "${ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID} = ? AND " +
                    "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(groupId, ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE),
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val contactId =
                    it.getString(it.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID))

                // Sau khi lấy được contactId, truy vấn tiếp để lấy số điện thoại
                val phoneCursor = activity.contentResolver.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    ),
                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )

                phoneCursor?.use { pc ->
                    while (pc.moveToNext()) {
                        val contactName =
                            pc.getString(pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                        val contactNumber =
                            pc.getString(pc.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))

                        Log.d(
                            "groups_debug",
                            "Nhóm ID: $groupId có liên hệ: $contactName - $contactNumber"
                        )

                        contacts.add(User(contactName, contactNumber))
                    }
                }
            }
        }

        return contacts
    }

    fun getContactUriByPhoneNumber(phoneNumber: String, activity: FragmentActivity): Uri? {
        val contentResolver = activity.contentResolver
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

    @RequiresApi(Build.VERSION_CODES.O)
    fun createShortcut(user: User, anh: Int, activity: FragmentActivity) {
        val shortcutManager = activity.getSystemService(ShortcutManager::class.java)
        // Tạo ShortcutInfo (Shortcut động)
        val shortcut = ShortcutInfo.Builder(activity, user.name)
            .setShortLabel(user.name)
            .setIcon(Icon.createWithResource(activity, anh))
            .setIntent(
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:${user.mobile}"))
            )
            .build()

        // Kiểm tra nếu có thể ghim phím tắt
        if (shortcutManager!!.isRequestPinShortcutSupported) {
            val pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(shortcut)
            val successCallback = PendingIntent.getBroadcast(
                activity, 0, pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE
            )

            shortcutManager.requestPinShortcut(shortcut, successCallback.intentSender)
            Toast.makeText(activity, "Đã tạo phím tắt ra màn hình chính!", Toast.LENGTH_SHORT)
                .show()
        } else {
            Toast.makeText(activity, "Thiết bị không hỗ trợ tạo phím tắt!", Toast.LENGTH_SHORT)
                .show()
        }
    }

    fun danhDauYeuThich(user: User, activity: FragmentActivity, themHayXoa: Int) {
        val permission = android.Manifest.permission.WRITE_CONTACTS
        if (ContextCompat.checkSelfPermission(
                activity,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(activity, arrayOf(permission), 3)
            return
        }
        try {

            val values = ContentValues().apply {
                put(ContactsContract.Contacts.STARRED, themHayXoa)
            }

            val rowsUpdated = activity.contentResolver.update(
                ContactsContract.Contacts.CONTENT_URI,
                values,
                "${ContactsContract.Contacts._ID} = ?",
                arrayOf(getContactIdByPhoneNumber(user.mobile, activity).toString())
            )

            if (rowsUpdated > 0) {
                if (themHayXoa == 1) {
                    Toast.makeText(
                        activity,
                        "Đã thêm vào danh sách yêu thích",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        activity,
                        "Xóa ${user.name} khỏi danh sách yêu thích",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(activity, "Cập nhật không thành công!", Toast.LENGTH_SHORT)
                    .show()
            }
        } catch (e: Exception) {
            Log.e("danhDauYeuThich", "Lỗi khi cập nhật yêu thích: ${e.message}")
            Toast.makeText(activity, "Đã xảy ra lỗi!", Toast.LENGTH_SHORT).show()
        }
    }

    fun getContactIdByPhoneNumber(phoneNumber: String, activity: FragmentActivity): Long? {
        val contentResolver = activity.contentResolver
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

    fun dialogThemVaoGroup(activity: FragmentActivity, user: User, dsUser: MutableList<User>) {
        val array: MutableList<String> = mutableListOf()
        val checkedItems: MutableList<Boolean> = mutableListOf()
        val dsGroup = getContactGroups(activity)
        for (group in dsGroup) {
            array.add(group.nameGroup)
            checkedItems.add(false)
        }
        val builder = AlertDialog.Builder(activity)
        builder.setMultiChoiceItems(
            array.toTypedArray(),
            checkedItems.toBooleanArray()
        ) { _, which, isChecked ->
            if (user.name.isNullOrEmpty()) {
                Log.d("cochayvao", dsUser.size.toString())
                for (user1 in dsUser) {
                    danhDauGroup(user1, array[which], activity, "")
                }
            } else {
                Log.d("cochayvao", "b")
                danhDauGroup(user, array[which], activity, "")
            }
        }
        val dialog = builder.create()
        dialog.show()
    }
    fun deleteGroupByName(groupName: String, activity: FragmentActivity): Boolean {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(Manifest.permission.WRITE_CONTACTS), 1001)
            return false
        }

        try {
            val contentResolver = activity.contentResolver
            val groupId = ChucNang().getGroupIDByNameGroup(groupName, activity)
            Log.d("DeleteGroup", "Group ID for '$groupName': $groupId")
            if (groupId == null) {
                Toast.makeText(activity, "Không tìm thấy nhóm '$groupName'", Toast.LENGTH_SHORT).show()
                return false
            }

            val (accountType, accountName) = getGroupAccount(groupId, activity)
            Log.d("DeleteGroup", "Group '$groupName' - Account Type: $accountType, Account Name: $accountName")
            if (accountType != "com.local" || accountName != "Local Contacts") {
                Toast.makeText(activity, "Không thể xóa nhóm '$groupName' vì nó không phải nhóm cục bộ", Toast.LENGTH_SHORT).show()
                return false
            }

            val groupUri = ContentUris.withAppendedId(ContactsContract.Groups.CONTENT_URI, groupId)
            var rowsDeleted = 0
            var attempts = 0
            while (attempts < 5) {  // Thử xóa tối đa 5 lần
                rowsDeleted = contentResolver.delete(groupUri, null, null)
                Log.d("DeleteGroup", "Rows deleted for group '$groupName' (attempt ${attempts + 1}): $rowsDeleted")
                if (rowsDeleted > 0) break
                attempts++
                Thread.sleep(300) // Chờ một chút rồi thử lại
            }

            activity.contentResolver.notifyChange(ContactsContract.Groups.CONTENT_URI, null) // Xóa cache ContentResolver

            if (rowsDeleted > 0) {
                val intent = Intent(Intent.ACTION_PROVIDER_CHANGED)
                intent.data = ContactsContract.Groups.CONTENT_URI
                activity.sendBroadcast(intent)
                Thread.sleep(500) // Debug only

                if (checkGroupExists(groupId, activity)) {
                    Log.w("DeleteGroup", "Group '$groupName' STILL EXISTS after deletion!")
                    Toast.makeText(activity, "Không thể xóa nhóm '$groupName'", Toast.LENGTH_SHORT).show()
                    return false
                } else {
                    Toast.makeText(activity, "Đã xóa nhóm '$groupName' thành công", Toast.LENGTH_SHORT).show()
                    Log.d("DeleteGroup", "Successfully deleted group '$groupName' with ID: $groupId")
                    return true
                }
            } else {
                Toast.makeText(activity, "Không thể xóa nhóm '$groupName'", Toast.LENGTH_SHORT).show()
                Log.w("DeleteGroup", "Failed to delete group '$groupName' after $attempts attempts")
                return false
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "Lỗi khi xóa nhóm: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("DeleteGroup", "Error deleting group '$groupName': ${e.message}", e)
            return false
        }
    }


    fun getGroupAccount(groupId: Long, activity: FragmentActivity): Pair<String?, String?> {
        val contentResolver = activity.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            arrayOf(ContactsContract.Groups.ACCOUNT_TYPE, ContactsContract.Groups.ACCOUNT_NAME),
            "${ContactsContract.Groups._ID} = ?",
            arrayOf(groupId.toString()),
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val accountType = it.getString(it.getColumnIndexOrThrow(ContactsContract.Groups.ACCOUNT_TYPE))
                val accountName = it.getString(it.getColumnIndexOrThrow(ContactsContract.Groups.ACCOUNT_NAME))
                Log.d("GroupAccount", "Group ID: $groupId - Account Type: $accountType, Account Name: $accountName")
                return Pair(accountType, accountName)
            }
        }
        return Pair(null, null)
    }
    fun checkGroupExists(groupId: Long, activity: FragmentActivity): Boolean {
        val contentResolver = activity.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.Groups.CONTENT_URI,
            arrayOf(ContactsContract.Groups._ID),
            "${ContactsContract.Groups._ID} = ?",
            arrayOf(groupId.toString()),
            null
        )
        cursor?.use {
            val exists = it.moveToFirst()
            Log.d("CheckGroupExists", "Group ID $groupId exists: $exists")
            return exists
        }
        Log.d("CheckGroupExists", "Group ID $groupId not found (cursor null)")
        return false
    }
    fun doiTenNhom(groupId: Long, newGroupName: String,activity: FragmentActivity) {
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.READ_CONTACTS), 7)
            return
        }
        if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(android.Manifest.permission.WRITE_CONTACTS), 8)
            return
        }
        val resolver: ContentResolver = activity.contentResolver

        // Tạo đối tượng ContentValues chứa tên nhóm mới
        val values = ContentValues().apply {
            put(ContactsContract.Groups.TITLE, newGroupName)
        }

        // Thực hiện cập nhật nhóm
        val uri = ContactsContract.Groups.CONTENT_URI
        val where = "${ContactsContract.Groups._ID} = ?"
        val selectionArgs = arrayOf(groupId.toString())

        val rowsUpdated = resolver.update(uri, values, where, selectionArgs)
        if(rowsUpdated > 0){
            Toast.makeText(activity,"Cập nhật tên group thành công",Toast.LENGTH_LONG).show()
        }
    }
    fun deleteGroupPermanently(context: Context, groupId: Long): Boolean {
        val resolver = context.contentResolver

        // URI tới bảng ContactsContract.Groups
        val uri = ContactsContract.Groups.CONTENT_URI

        // Điều kiện để xác định nhóm cần xóa
        val where = "${ContactsContract.Groups._ID} = ?"
        val selectionArgs = arrayOf(groupId.toString())

        // Thực hiện xóa nhóm
        return try {
            val rowsDeleted = resolver.delete(uri, where, selectionArgs)
            rowsDeleted > 0 // Trả về true nếu xóa thành công
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    fun updateBottomNavigationColor(activity: Activity) {
        val bottomNavigationView = activity.findViewById<BottomNavigationView>(R.id.bottom)
        val toolBar = activity.findViewById<Toolbar>(R.id.main_Toolbar)

        // Lấy màu hiện tại của Toolbar
        val toolbarColor = (toolBar.background as ColorDrawable).color

        // Lấy màu mặc định của BottomNavigationView (Unfocus Color)
        val defaultUnfocusColor = Color.rgb(255, 255, 255)

        // Tạo ColorStateList cho BottomNavigationView
        val colorStateList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf(-android.R.attr.state_checked)),
            intArrayOf(toolbarColor, defaultUnfocusColor)
        )

        // Cập nhật màu cho Icon và Text của BottomNavigationView
        bottomNavigationView.itemIconTintList = colorStateList
        bottomNavigationView.itemTextColor = colorStateList
    }


    fun changeToolbarColor(activity: Activity, newColor: Int) {
        val toolBar = activity.findViewById<Toolbar>(R.id.main_Toolbar)
        toolBar.setBackgroundColor(newColor)  // Trực tiếp sử dụng mã màu ARGB
    }
    fun updateBotronColor(activity: Activity,imageView: ImageView,imageView2: ImageView) {
        val toolBar = activity.findViewById<Toolbar>(R.id.main_Toolbar)

        // Lấy màu hiện tại của Toolbar
        val toolbarColor = (toolBar.background as ColorDrawable).color

        // Cập nhật màu cho botron
        val drawable = ContextCompat.getDrawable(activity, R.drawable.botron)?.mutate() as GradientDrawable
        drawable.setColor(toolbarColor)
        val drawable2 = ContextCompat.getDrawable(activity, R.drawable.bovuong)?.mutate() as GradientDrawable
        drawable2.setColor(toolbarColor)
        imageView.background = drawable
        imageView2.background = drawable2
    }
    fun showColorPickerDialog(activity: FragmentActivity, onColorSelected: (Int) -> Unit) {
        val dialogView = activity.layoutInflater.inflate(R.layout.dialog_color_picker, null)

        val redSlider = dialogView.findViewById<Slider>(R.id.redSlider)
        val greenSlider = dialogView.findViewById<Slider>(R.id.greenSlider)
        val blueSlider = dialogView.findViewById<Slider>(R.id.blueSlider)
        val selectedColorView = dialogView.findViewById<View>(R.id.selectedColorView)

        // Lấy `SharedPreferences`
        val sharedPref = activity.getSharedPreferences("ColorPicker", Context.MODE_PRIVATE)
        val savedColor = sharedPref.getInt("selected_color", Color.rgb(255, 165, 0))

        // Chuyển `Int` màu đã lưu thành các giá trị R, G, B
        val savedRed = Color.red(savedColor)
        val savedGreen = Color.green(savedColor)
        val savedBlue = Color.blue(savedColor)

        // Thiết lập giá trị ban đầu cho các `Slider`
        redSlider.value = savedRed.toFloat()
        greenSlider.value = savedGreen.toFloat()
        blueSlider.value = savedBlue.toFloat()
        selectedColorView.setBackgroundColor(savedColor)

        // Cập nhật màu mẫu khi thay đổi giá trị thanh trượt
        val updateColorView = {
            val selectedColor = Color.rgb(
                redSlider.value.toInt(),
                greenSlider.value.toInt(),
                blueSlider.value.toInt()
            )
            selectedColorView.setBackgroundColor(selectedColor)
        }

        // Lắng nghe thay đổi của các thanh trượt
        redSlider.addOnChangeListener { _, _, _ -> updateColorView() }
        greenSlider.addOnChangeListener { _, _, _ -> updateColorView() }
        blueSlider.addOnChangeListener { _, _, _ -> updateColorView() }

        // Hiển thị dialog
        val dialog = AlertDialog.Builder(activity)
            .setTitle("Chọn màu")
            .setView(dialogView)
            .setPositiveButton("Chọn") { _, _ ->
                val selectedColor = Color.rgb(
                    redSlider.value.toInt(),
                    greenSlider.value.toInt(),
                    blueSlider.value.toInt()
                )

                // Lưu màu vào `SharedPreferences`
                with(sharedPref.edit()) {
                    putInt("selected_color", selectedColor)
                    apply()
                }

                onColorSelected(selectedColor)
            }
            .setNegativeButton("Hủy", null)
            .create()

        dialog.show()
    }
    fun diaLog_CaiDat(activity: FragmentActivity, adapter: DsAdapter) {
        val array: MutableList<String> = mutableListOf()
        val checkedItems: MutableList<Boolean> = mutableListOf()
        val sharedPref = activity.getSharedPreferences("CaiDat", Context.MODE_PRIVATE)
        val sharedPrefEdit = sharedPref.edit()
        val hienThiHinh = sharedPref.getBoolean("thu_nho_lien_he", true)
        val hienThiSDT = sharedPref.getBoolean("hien_thi_sdt",false)
        checkedItems.add(hienThiHinh)
        checkedItems.add(hienThiSDT)
        checkedItems.add(true)
        array.add("Hiển thị hình thu nhỏ liên hệ")
        array.add("Hiển thị số điện thoại")
        array.add("Hợp nhất các liên hệ trùng lặp")
        val builder = AlertDialog.Builder(activity)
        builder.setMultiChoiceItems(
            array.toTypedArray(),
            checkedItems.toBooleanArray()
        ) { _, which, isChecked ->
            if(array[which] == "Hiển thị hình thu nhỏ liên hệ"){
                sharedPrefEdit.putBoolean("thu_nho_lien_he", isChecked)
                sharedPrefEdit.apply()
                adapter.hienThiHinhThuNho = isChecked
                adapter.notifyDataSetChanged()  // Cập nhật lại toàn bộ giao diện
            }
            if(array[which] == "Hiển thị số điện thoại"){
                sharedPrefEdit.putBoolean("hien_thi_sdt", isChecked)
                sharedPrefEdit.apply()
                adapter.hienThiSDT = isChecked
                adapter.notifyDataSetChanged()  // Cập nhật lại toàn bộ giao diện
            }
            if(array[which] == "Hợp nhất các liên hệ trùng lặp"){
                checkedItems[which] = true
            }
        }

        val dialog = builder.create()
        dialog.show()
    }
    fun anHinhNgay(adapter: DsAdapter){
        adapter.hienThiHinhThuNho = false
        adapter.notifyDataSetChanged()  // Cập nhật lại toàn bộ giao diện
    }
    fun hienSDT(adapter: DsAdapter){
        adapter.hienThiSDT = true
        adapter.notifyDataSetChanged()  // Cập nhật lại toàn bộ giao diện
    }
    fun open_KeyBroad(context: Context){
        val bottomSheetDialog = BottomSheetDialog(context)
        val view =
            LayoutInflater.from(context).inflate(R.layout.dialog_bottomsheet, null)
        bottomSheetDialog.setContentView(view)
        val editTextQuaySo = view.findViewById<EditText>(R.id.editText_BottomSheet)
        val button1 = view.findViewById<Button>(R.id.button1_Bottomsheet)
        val button2 = view.findViewById<Button>(R.id.button2_Bottomsheet)
        val button3 = view.findViewById<Button>(R.id.button3_Bottomsheet)
        val button4 = view.findViewById<Button>(R.id.button4_Bottomsheet)
        val button5 = view.findViewById<Button>(R.id.button5_Bottomsheet)
        val button6 = view.findViewById<Button>(R.id.button6_Bottomsheet)
        val button7 = view.findViewById<Button>(R.id.button7_Bottomsheet)
        val button8 = view.findViewById<Button>(R.id.button8_Bottomsheet)
        val button9 = view.findViewById<Button>(R.id.button9_Bottomsheet)
        val button0 = view.findViewById<Button>(R.id.button0_Bottomsheet)
        val call = view.findViewById<ImageView>(R.id.img_Bottomsheet_Call)
        val backSpace = view.findViewById<ImageView>(R.id.img_Backspace_Bottomssheet)
        button0.setOnClickListener {
            editTextQuaySo.setText(editTextQuaySo.text.toString().plus("0"))
        }
        button1.setOnClickListener {
            editTextQuaySo.setText(editTextQuaySo.text.toString().plus("1"))
        }
        button2.setOnClickListener {
            editTextQuaySo.setText(editTextQuaySo.text.toString().plus("2"))
        }
        button3.setOnClickListener {
            editTextQuaySo.setText(editTextQuaySo.text.toString().plus("3"))
        }
        button4.setOnClickListener {
            editTextQuaySo.setText(editTextQuaySo.text.toString().plus("4"))
        }
        button5.setOnClickListener {
            editTextQuaySo.setText(editTextQuaySo.text.toString().plus("5"))
        }
        button6.setOnClickListener {
            editTextQuaySo.setText(editTextQuaySo.text.toString().plus("6"))
        }
        button7.setOnClickListener {
            editTextQuaySo.setText(editTextQuaySo.text.toString().plus("7"))
        }
        button8.setOnClickListener {
            editTextQuaySo.setText(editTextQuaySo.text.toString().plus("8"))
        }
        button9.setOnClickListener {
            editTextQuaySo.setText(editTextQuaySo.text.toString().plus("9"))
        }
        backSpace.setOnClickListener {
            val doDai = editTextQuaySo.text.toString().length
            editTextQuaySo.setText(editTextQuaySo.text.toString().dropLast(1))
        }
        call.setOnClickListener {
            if (editTextQuaySo.text.toString().length != 10) {
                Toast.makeText(
                    context,
                    "Yêu cầu phải nhập dúng 10 số",
                    Toast.LENGTH_LONG
                ).show()
            } else if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.CALL_PHONE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    context as Activity, arrayOf(android.Manifest.permission.CALL_PHONE),
                    1
                )

            } else {
                makeCall(User("", editTextQuaySo.text.toString()), context)
            }
        }
        bottomSheetDialog.show()
    }
    fun makeCall(user: User, context: Context) {
        val telecomManager =
            context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val uri = Uri.parse("tel:${user.mobile}")
        val extras = Bundle()

        if (ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CALL_PHONE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(android.Manifest.permission.CALL_PHONE),
                1
            )
        } else {
            telecomManager.placeCall(uri, extras)
            // Chuyển sang OutgoingCallActivity
            val intent = Intent(context, OutgoingCallActivity::class.java)
            intent.putExtra("callee_number", user.mobile)
            intent.putExtra("callee_name", user.name)
            context.startActivity(intent)
        }
    }
}