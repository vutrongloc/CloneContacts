package com.example.clonecontacts.activity

import ContactImporter
import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.telecom.TelecomManager
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.clonecontacts.ChucNang
import com.example.clonecontacts.Fragment.ContactsFragment
import com.example.clonecontacts.Fragment.FavoritesFragment
import com.example.clonecontacts.Fragment.GroupsFragment
import com.example.clonecontacts.Fragment.HistoryFragment
import com.example.clonecontacts.Fragment.UserFragment
import com.example.clonecontacts.Model.User
import com.example.clonecontacts.R
import com.example.clonecontacts.Service.OutgoingCallListener
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    // Constants
    companion object {
        private const val REQUEST_CODE_SET_DEFAULT_DIALER = 100
        private const val REQUEST_CODE_PERMISSIONS = 101
        private const val REQUEST_CODE_CONTACTS_FULL = 200
        const val PICK_VCF_REQUEST = 1
        private const val TAG = "MainActivity1111"
    }

    // UI Elements

    private lateinit var dialog: Dialog
    private lateinit var callListener: OutgoingCallListener

    // State Variables

    private var dialogInitialized = false

    // RoleManager for dialer role
    private val roleManager: RoleManager? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getSystemService(RoleManager::class.java)
        } else {
            null
        }
    }

    // Activity Result Launcher for Default Dialer
    private val setDefaultDialerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // Check if your app is now the default dialer
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && roleManager?.isRoleHeld(
                        RoleManager.ROLE_DIALER
                    ) == true
                ) {
                    Toast.makeText(
                        this,
                        "Ứng dụng này hiện được đặt làm trình quay số mặc định",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Vui lòng đặt ứng dụng này làm ứng dụng gọi điện mặc định để tiếp tục",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    this,
                    "Vui lòng đặt ứng dụng này làm ứng dụng gọi điện mặc định để tiếp tục",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        setupFullScreenMode()
        applySavedTheme()
        initializeUI()
        initializeCallListener()
        requestPermissions()

        val userName = intent.getStringExtra("user_Shortcut_Name")
        val userMobile = intent.getStringExtra("user_Shortcut_Mobile")
        if (userMobile != null && userName != null) {
            val fragment = UserFragment()
            val bundle = Bundle()
            bundle.putSerializable("user", User(userName, userMobile))
            fragment.arguments = bundle
            val toolbar = findViewById<Toolbar>(R.id.main_Toolbar)
            toolbar.visibility = View.GONE
            val bottom = findViewById<BottomNavigationView>(R.id.bottom)
            bottom.visibility = View.GONE
            supportFragmentManager.beginTransaction()
                .replace(R.id.frameLayout, fragment)
                .commit()

        }
    }

    // Full screen setup
    private fun setupFullScreenMode() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    // Initialize UI components
    private fun initializeUI() {

        setupToolbar()
        setupExitButton()
        setupBottomNavigation()
    }

    // Initialize call listener
    private fun initializeCallListener() {
        callListener = OutgoingCallListener(this)
        callListener.startListening()
    }

    // Setup toolbar with menu listeners
    fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.main_Toolbar)

        val toolbarColor = (toolbar.background as ColorDrawable).color
        val drawable =
            ContextCompat.getDrawable(this, R.drawable.bovuong)?.mutate() as GradientDrawable
        drawable.setColor(toolbarColor)

        if (!dialogInitialized) {
            dialog = Dialog(this).apply {
                setContentView(R.layout.dialog_sap_xep)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            }
            val root = dialog.findViewById<View>(R.id.dialog_sap_xep)
            root.background = drawable
            dialogInitialized = true
        }
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.vuongMien -> {
                    showColorPicker()
                    true
                }

                R.id.timKiem -> {
                    showSearchBar()
                    true
                }

                R.id.sapXepTheo -> {
                    showSortDialog()
                    true
                }

                R.id.nhapDanhBaTuTep -> {
                    importContactsFromFile()
                    true
                }

                R.id.themContact -> {
                    themContacts()
                    true
                }

                R.id.xuatDanhBaTuTep -> {
                    exportContactsToFile()
                    true
                }

                else -> false
            }
        }
    }

    fun themContacts() {
        val intent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
            // Sets the MIME type to match the Contacts Provider
            type = ContactsContract.RawContacts.CONTENT_TYPE
        }
        startActivity(intent)
    }

    // Setup exit button
    private fun setupExitButton() {
        findViewById<ImageView>(R.id.main_Back).setOnClickListener { hideSearchBar() }
    }

    // Setup bottom navigation
    private fun setupBottomNavigation() {
        findViewById<BottomNavigationView>(R.id.bottom).setOnItemSelectedListener {
            when (it.itemId) {
                R.id.Contacts -> {
                    requestFullContactPermissions()
                    true
                }

                R.id.Favorites -> replaceFragment(FavoritesFragment())
                R.id.Groups -> replaceFragment(GroupsFragment())
                else -> replaceFragment(HistoryFragment())
            }
        }
    }


    // Setup call button


    // Request all necessary permissions
    private fun requestPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS
        )
        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest, REQUEST_CODE_PERMISSIONS)
        } else {
            Log.d(TAG, "All required permissions granted")
            requestDefaultDialer()
        }
    }

    // Request default dialer role
    private fun requestDefaultDialer() {
        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (roleManager?.isRoleHeld(RoleManager.ROLE_DIALER) != true) {
                val intent = roleManager!!.createRequestRoleIntent(RoleManager.ROLE_DIALER)
                setDefaultDialerLauncher.launch(intent)
                Log.d(TAG, "Requesting default dialer role for Android Q+")
            } else {
                Log.d(TAG, "App is already default dialer")
            }
        } else {
            if (packageName != telecomManager.defaultDialerPackage) {
                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
                startActivityForResult(intent, REQUEST_CODE_SET_DEFAULT_DIALER)
                Log.d(TAG, "Requesting default dialer for pre-Android Q")
            } else {
                Log.d(TAG, "App is already default dialer")
            }
        }
    }

    // Place a call
    private fun placeCall(number: String) {
        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            telecomManager.placeCall(Uri.parse("tel:$number"), Bundle())
            Log.d(TAG, "Placing call to: $number")
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1)
        }
    }

    // Apply saved theme color
    private fun applySavedTheme() {
        val sharedPref = getSharedPreferences("ColorPicker", Context.MODE_PRIVATE)
        val color = sharedPref.getInt("selected_color", Color.rgb(255, 165, 0))
        doiMauUngDung(color)
    }

    // Change app color
    private fun doiMauUngDung(color: Int) {
        ChucNang().changeToolbarColor(this, color)
        ChucNang().updateBottomNavigationColor(this)
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frameLayout)
        val newFragment = when (currentFragment) {
            is GroupsFragment -> GroupsFragment()
            is FavoritesFragment -> FavoritesFragment()
            else -> ContactsFragment()
        }
        replaceFragment(newFragment)
    }

    // Toolbar menu actions
    private fun showColorPicker(): Boolean {
        ChucNang().showColorPickerDialog(this) { color -> doiMauUngDung(color) }
        return true
    }

    private fun showSearchBar(): Boolean {
        findViewById<EditText>(R.id.main_EdittextTimKiem).visibility = View.VISIBLE
        findViewById<ImageView>(R.id.main_Back).visibility = View.VISIBLE
        findViewById<TextView>(R.id.main_tenFragment).visibility = View.GONE
        Log.d("timkiem", "bamvaotimkiem")
        return true
    }

    private fun hideSearchBar() {
        findViewById<EditText>(R.id.main_EdittextTimKiem).visibility = View.GONE
        findViewById<ImageView>(R.id.main_Back).visibility = View.GONE
        findViewById<TextView>(R.id.main_tenFragment).visibility = View.VISIBLE
    }

    private fun showSortDialog(): Boolean {
        dialog.findViewById<TextView>(R.id.dialogSX_OK).setOnClickListener {
            val upOrDownGroup = dialog.findViewById<RadioGroup>(R.id.dialog_UpOrDown)
            val firstOrLastGroup = dialog.findViewById<RadioGroup>(R.id.dialog_FirstOrLast)
            val upOrDownId = upOrDownGroup.checkedRadioButtonId
            val firstOrLastId = firstOrLastGroup.checkedRadioButtonId
            if (upOrDownId != -1 && firstOrLastId != -1) {
                val upOrDown = dialog.findViewById<RadioButton>(upOrDownId)
                val firstOrLast = dialog.findViewById<RadioButton>(firstOrLastId)
                val currentFragment = supportFragmentManager.findFragmentById(R.id.frameLayout)
                currentFragment?.let { truyenThongTinSapXepToiFragment(upOrDown, firstOrLast, it) }
            }
            dialog.dismiss()
        }
        dialog.findViewById<TextView>(R.id.dialogSX_Huy_Bo).setOnClickListener { dialog.dismiss() }
        dialog.show()
        return true
    }

    private fun importContactsFromFile(): Boolean {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_CONTACTS),
                101
            )
        } else {
            requestContactPermission()
        }
        return true
    }

    private fun exportContactsToFile(): Boolean {
        val readContactsGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        val writeContactsGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        when {
            !writeContactsGranted -> ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_CONTACTS),
                102
            )

            !readContactsGranted -> ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                102
            )

            else -> exportContacts()
        }
        return true
    }

    private fun exportContacts() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frameLayout)
        val type = when (currentFragment) {
            is ContactsFragment -> "Contacts"
            is FavoritesFragment -> "Favorites"
            else -> "Groups"
        }
        val success = ContactImporter(this, type).exportContactsToVcf()
        Toast.makeText(
            this,
            if (success) "Đã xuất tệp vào mục downloads" else "Không xuất tệp thành công",
            Toast.LENGTH_LONG
        ).show()
    }

    // Fragment utilities
    private fun truyenThongTinSapXepToiFragment(
        upOrDown: RadioButton,
        firstOrLast: RadioButton,
        fragment: Fragment
    ) {
        val isAscending = upOrDown.text.toString() == "Tăng Dần"
        val isFirstName = firstOrLast.text.toString() == "First name"
        val newFragment = when (fragment) {
            is ContactsFragment -> ContactsFragment()
            is FavoritesFragment -> FavoritesFragment()
            else -> fragment
        }
        newFragment.arguments = Bundle().apply {
            putBoolean("TangHoacGiam", isAscending)
            putBoolean("FirstOrLast", isFirstName)
        }
        replaceFragment(newFragment)
    }

    private fun replaceFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frameLayout, fragment)
            .commit()
        return true
    }

    // Contact permissions
    private fun requestContactPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                1
            )
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_CONTACTS),
                102
            )
        } else if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), 102)
        } else {
            startActivityForResult(
                Intent(Intent.ACTION_GET_CONTENT).setType("*/*"),
                PICK_VCF_REQUEST
            )
        }
    }

    private fun requestFullContactPermissions() {
        val permissions = arrayOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.CALL_PHONE
        )
        if (permissions.all {
                ContextCompat.checkSelfPermission(
                    this,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }) {
            replaceFragment(ContactsFragment())
        } else {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_CONTACTS_FULL)
        }
    }

    // Import VCF file
    private fun importVcfFile(uri: Uri) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.frameLayout)
        val type = when (currentFragment) {
            is ContactsFragment -> "Contacts"
            is FavoritesFragment -> "Favorites"
            else -> "Groups"
        }
        val success = ContactImporter(this, type).importContactsFromVcf(uri)
        Toast.makeText(
            this,
            if (success) "Đã nhập danh bạ thành công" else "Lỗi khi nhập danh bạ",
            Toast.LENGTH_SHORT
        ).show()
        if (success) {
            val newFragment = when (currentFragment) {
                is ContactsFragment -> ContactsFragment()
                is FavoritesFragment -> FavoritesFragment()
                else -> GroupsFragment()
            }
            replaceFragment(newFragment)
        }
    }

    // Lifecycle methods
    override fun onDestroy() {
        super.onDestroy()
        callListener.stopListening()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: requestCode = $requestCode, resultCode = $resultCode")
        when (requestCode) {
            PICK_VCF_REQUEST -> if (resultCode == Activity.RESULT_OK) data?.data?.let {
                importVcfFile(
                    it
                )
            }

            REQUEST_CODE_SET_DEFAULT_DIALER -> {
                Toast.makeText(
                    this,
                    if (resultCode == RESULT_OK) "Default dialer set successfully" else "Failed to set default dialer",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Log.d(TAG, "All permissions granted")
                    requestDefaultDialer()
                } else {
                    Toast.makeText(
                        this,
                        "Please grant all permissions to set as default dialer",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.d(TAG, "Some permissions denied: ${permissions.joinToString()}")
                }
            }

            REQUEST_CODE_CONTACTS_FULL -> {
                if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) replaceFragment(
                    ContactsFragment()
                )
                else Toast.makeText(
                    this,
                    "Cần cấp quyền đầy đủ để truy cập danh bạ và thực hiện cuộc gọi",
                    Toast.LENGTH_SHORT
                ).show()
            }

            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Retry call if needed
                } else {
                    Toast.makeText(
                        this,
                        "Cần quyền CALL_PHONE để thực hiện cuộc gọi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}