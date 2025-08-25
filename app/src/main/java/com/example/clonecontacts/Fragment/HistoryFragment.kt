package com.example.clonecontacts.Fragment

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clonecontacts.Adapter.CallHistoryAdapter
import com.example.clonecontacts.ChucNang
import com.example.clonecontacts.R

class HistoryFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private val REQUEST_CALL_LOG = 101

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView = view.findViewById(R.id.recyclerViewHistory)
        val toolbar = requireActivity().findViewById<androidx.appcompat.widget.Toolbar>(R.id.main_Toolbar)
        toolbar.menu.clear()

        if (ContextCompat.checkSelfPermission(requireActivity(), android.Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(android.Manifest.permission.READ_CALL_LOG),
                REQUEST_CALL_LOG
            )
        } else {
            loadCallHistory()
        }
    }

    private fun loadCallHistory() {
        val callList = ChucNang().getCallHistory(requireContext(), false)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = CallHistoryAdapter(callList, requireActivity())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CALL_LOG && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            loadCallHistory()
        }
    }
}