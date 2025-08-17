import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.ContactsContract
import android.provider.MediaStore
import android.util.Log
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.Date
import java.util.Locale

class ContactImporter(private val context: Context, private val ten: String) {

    fun importContactsFromVcf(uri: Uri): Boolean {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return false
            val reader = BufferedReader(InputStreamReader(inputStream))
            val ops = ArrayList<ContentProviderOperation>()
            var currentLine: String?
            var name: String? = null
            var phone: String? = null
            var hasContact = false

            while (reader.readLine().also { currentLine = it } != null) {
                when {
                    currentLine!!.startsWith("BEGIN:VCARD") -> {
                        name = null
                        phone = null
                    }

                    currentLine!!.startsWith("FN:") -> {
                        name = currentLine!!.substringAfter("FN:")
                        Log.d("ContactImporter", "Đọc tên: $name")
                    }

                    currentLine!!.startsWith("TEL") -> {
                        phone = currentLine!!.substringAfter(":")
                        Log.d("ContactImporter", "Đọc số điện thoại: $phone")
                    }

                    currentLine!!.startsWith("END:VCARD") -> {
                        if (!phone.isNullOrEmpty()) {
                            if (name.isNullOrEmpty()) name = "Liên hệ không tên"
                            hasContact = true

                            // Kiểm tra xem số điện thoại có tồn tại trong danh bạ không
                            val contactId = getContactIdByPhoneNumber(phone)

                            if (contactId != null) { // Nếu tồn tại
                                Log.d(
                                    "ContactImporter",
                                    "Số điện thoại $phone đã tồn tại trong danh bạ."
                                )

                                if (ten == "Favorites") {
                                    // Cập nhật thành yêu thích nếu chưa được đánh dấu
                                    val updateValues = ContentValues().apply {
                                        put(ContactsContract.Contacts.STARRED, 1)
                                    }
                                    contentResolver.update(
                                        ContactsContract.Contacts.CONTENT_URI,
                                        updateValues,
                                        "${ContactsContract.Contacts._ID}=?",
                                        arrayOf(contactId.toString())
                                    )
                                    Log.d(
                                        "ContactImporter",
                                        "Số điện thoại $phone được đánh dấu yêu thích."
                                    )
                                }
                            } else { // Nếu không tồn tại
                                val rawContactInsertIndex = ops.size
                                ops.add(
                                    ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                                        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                                        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                                        .build()
                                )
                                ops.add(
                                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                        .withValueBackReference(
                                            ContactsContract.Data.RAW_CONTACT_ID,
                                            rawContactInsertIndex
                                        )
                                        .withValue(
                                            ContactsContract.Data.MIMETYPE,
                                            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                                        )
                                        .withValue(
                                            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
                                            name
                                        )
                                        .build()
                                )
                                ops.add(
                                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                                        .withValueBackReference(
                                            ContactsContract.Data.RAW_CONTACT_ID,
                                            rawContactInsertIndex
                                        )
                                        .withValue(
                                            ContactsContract.Data.MIMETYPE,
                                            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                                        )
                                        .withValue(
                                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                                            phone
                                        )
                                        .withValue(
                                            ContactsContract.CommonDataKinds.Phone.TYPE,
                                            ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                                        )
                                        .build()
                                )

                                if (ten == "Favorites") {
                                    ops.add(
                                        ContentProviderOperation.newUpdate(ContactsContract.RawContacts.CONTENT_URI)
                                            .withSelection(
                                                "${ContactsContract.RawContacts._ID}=?",
                                                arrayOf(rawContactInsertIndex.toString())
                                            )
                                            .withValue(ContactsContract.Contacts.STARRED, 1)
                                            .build()
                                    )
                                }
                            }
                        }
                    }
                }
            }

            inputStream.close()

            if (!hasContact) {
                Log.e("ContactImporter", "Không tìm thấy liên hệ nào trong tệp VCF.")
                return false
            }

            return try {
                if (ops.isNotEmpty()) {
                    contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                    Log.d("ContactImporter", "Nhập danh bạ thành công!")
                    true
                } else {
                    Log.e("ContactImporter", "Không có thao tác nào để thực hiện.")
                    true
                }
            } catch (e: Exception) {
                Log.e("ContactImporter", "Lỗi khi thêm danh bạ: ${e.message}")
                false
            }
        } catch (e: Exception) {
            Log.e("ContactImporter", "Lỗi khi đọc tệp VCF: ${e.message}")
            return false
        }
    }

    // Hàm kiểm tra xem số điện thoại đã tồn tại hay chưa
    private fun getContactIdByPhoneNumber(phone: String): Long? {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.CONTACT_ID),
            "${ContactsContract.CommonDataKinds.Phone.NUMBER} = ?",
            arrayOf(phone),
            null
        )
        var contactId: Long? = null
        if (cursor != null && cursor.moveToFirst()) {
            contactId = cursor.getLong(0)
        }
        cursor?.close()
        return contactId
    }


    fun exportContactsToVcf(): Boolean {
        val contentResolver = context.contentResolver
        val exportFavoritesOnly = ten == "Favorites"
        val selection =
            if (exportFavoritesOnly) "${ContactsContract.Contacts.STARRED} = ?" else null
        val selectionArgs = if (exportFavoritesOnly) arrayOf("1") else null

        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            selection,
            selectionArgs,
            null
        )

        if (cursor == null || cursor.count == 0) {
            Log.e(
                "ContactImporter",
                if (exportFavoritesOnly) "Không tìm thấy liên hệ yêu thích nào." else "Không tìm thấy liên hệ nào."
            )
            return false
        }

        try {
            // Tạo tên file có chứa thời gian hiện tại
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val currentTime = dateFormat.format(Date())
            val fileName =
                if (exportFavoritesOnly) "${ten}_$currentTime.vcf" else "${ten}_$currentTime.vcf"
            var filePath: String? = null

            // Tạo file mới
            val outputStream = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/x-vcard")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri: Uri? =
                    contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                filePath =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/$fileName"
                contentResolver.openOutputStream(uri!!)
            } else {
                val downloadsDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val file = File(downloadsDir, fileName)
                if (file.exists()) file.delete()
                filePath = file.absolutePath
                FileOutputStream(file)
            }

            if (outputStream == null) {
                Log.e("ContactImporter", "Không thể mở luồng ghi dữ liệu.")
                return false
            }

            val writer = BufferedWriter(OutputStreamWriter(outputStream))

            while (cursor.moveToNext()) {
                val name = cursor.getString(0) ?: "Liên hệ không tên"
                val phone = cursor.getString(1) ?: continue

                writer.write("BEGIN:VCARD\n")
                writer.write("VERSION:3.0\n")
                writer.write("FN:$name\n")
                writer.write("TEL:$phone\n")
                writer.write("END:VCARD\n")
            }

            writer.close()
            cursor.close()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && filePath != null) {
                MediaStore.Images.Media.insertImage(contentResolver, filePath, fileName, null)
            }

            Log.d(
                "ContactImporter",
                "Xuất danh bạ thành công! File được lưu trong thư mục Tải xuống."
            )
            return true
        } catch (e: IOException) {
            Log.e("ContactImporter", "Lỗi khi xuất danh bạ: ${e.message}")
            return false
        }
    }
}
