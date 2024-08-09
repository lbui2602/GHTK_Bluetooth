package com.example.ghtk_bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ghtk_bluetooth.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private val REQUEST_BLUETOOTH_PERMISSION = 1
    private val REQUEST_BLUETOOTH_SCAN_PERMISSION = 2
    private var bluetoothAdapter: BluetoothAdapter? = null
    private lateinit var binding: ActivityMainBinding
    private val bluetoothDevices = mutableListOf<BluetoothDevice>()
    private lateinit var bluetoothDeviceAdapter: BluetoothDeviceAdapter
    private val requestPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value }) {
                enableBluetooth()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null && intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                updateBluetoothStatus(state)
            }
        }
    }

    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (!bluetoothDevices.contains(it)) {
                            bluetoothDevices.add(it)
                            bluetoothDeviceAdapter.notifyItemInserted(bluetoothDevices.size - 1)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Toast.makeText(this@MainActivity, "Bắt đầu quét thiết bị", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bluetoothDeviceAdapter = BluetoothDeviceAdapter(listOf())
        binding.recyclerViewDevices.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewDevices.adapter = bluetoothDeviceAdapter

        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth không được hỗ trợ trên thiết bị này", Toast.LENGTH_SHORT).show()
            return
        }

        updateBluetoothStatus(bluetoothAdapter!!.state)
        bluetoothDeviceAdapter.setList(bluetoothDevices)
        binding.btnConnect.setOnClickListener {
            toggleBluetooth()
            bluetoothDeviceAdapter.setList(bluetoothDevices)
            Log.e("TAg",bluetoothDevices.size.toString())
        }

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        registerReceiver(bluetoothStateReceiver, filter)


    }

    private fun updateBluetoothStatus(state: Int) {
        when (state) {
            BluetoothAdapter.STATE_ON -> {
                binding.btnConnect.text = "Disconnect"
                toggleBluetooth()
            }
            BluetoothAdapter.STATE_OFF -> {
                binding.btnConnect.text = "Connect"
                bluetoothDevices.clear()
                bluetoothDeviceAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun toggleBluetooth() {
        requestPermissionsLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )
            if (permissions.any {
                    ContextCompat.checkSelfPermission(
                        this,
                        it
                    ) != PackageManager.PERMISSION_GRANTED
                }) {
                requestPermissionsLauncher.launch(permissions)
            } else {
                enableBluetooth()
            }
        } else {
            enableBluetooth()
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableBluetooth() {
        val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(enableBluetoothIntent, REQUEST_BLUETOOTH_PERMISSION)
        if(bluetoothAdapter!!.isEnabled){
            val discoveryFilter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            registerReceiver(discoveryReceiver, discoveryFilter)
            startDiscovery()
        }
    }


    @SuppressLint("MissingPermission")
    private fun startDiscovery() {
        if (bluetoothAdapter!!.isDiscovering) {
            bluetoothAdapter!!.cancelDiscovery()
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            bluetoothAdapter!!.startDiscovery()
            Log.e("TAG","bluetooth scan is ok")
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), REQUEST_BLUETOOTH_SCAN_PERMISSION)
            Log.e("TAG","bluetooth scan is not ok")
        }

        if(bluetoothAdapter!!.startDiscovery()){
            Log.e("TAG",bluetoothDevices.size.toString())
        }
        else{
            Log.e("TAG","false")
        }
    }

    private fun checkBluetoothScanPermissionAndStartDiscovery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), REQUEST_BLUETOOTH_SCAN_PERMISSION)
        } else {
            startDiscovery()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothStateReceiver)
        unregisterReceiver(discoveryReceiver)
    }
}
