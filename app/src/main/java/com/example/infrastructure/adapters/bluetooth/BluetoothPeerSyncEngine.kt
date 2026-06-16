package com.example.infrastructure.adapters.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import java.nio.ByteBuffer
import java.util.UUID

class BluetoothPeerSyncEngine(
    private val context: Context,
    private val onPeerStreakSynced: (Int) -> Unit
) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private var isScanning = false
    private var isAdvertising = false

    private val serviceUuid = UUID.fromString("0000FEF3-0000-1000-8000-00805F9B34FB")
    private val serviceParcelUuid = ParcelUuid(serviceUuid)

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            val record = result?.scanRecord ?: return
            val serviceData = record.getServiceData(serviceParcelUuid) ?: return
            if (serviceData.size >= 4) {
                val peerStreak = ByteBuffer.wrap(serviceData).int
                onPeerStreakSynced(peerStreak)
            }
        }

        override fun onScanFailed(errorCode: Int) {
            isScanning = false
        }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            isAdvertising = true
        }

        override fun onStartFailure(errorCode: Int) {
            isAdvertising = false
        }
    }

    @SuppressLint("MissingPermission")
    fun startSync(localStreak: Int) {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return

        if (!isScanning) {
            val scanner = adapter.bluetoothLeScanner
            if (scanner != null) {
                val filter = ScanFilter.Builder()
                    .setServiceUuid(serviceParcelUuid)
                    .build()
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                try {
                    scanner.startScan(listOf(filter), settings, scanCallback)
                    isScanning = true
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }

        if (!isAdvertising) {
            val advertiser = adapter.bluetoothLeAdvertiser
            if (advertiser != null) {
                val settings = AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                    .setConnectable(false)
                    .build()

                val streakBytes = ByteBuffer.allocate(4).putInt(localStreak).array()
                val data = AdvertiseData.Builder()
                    .addServiceUuid(serviceParcelUuid)
                    .addServiceData(serviceParcelUuid, streakBytes)
                    .build()

                try {
                    advertiser.startAdvertising(settings, data, advertiseCallback)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopSync() {
        val adapter = bluetoothAdapter ?: return
        if (isScanning) {
            val scanner = adapter.bluetoothLeScanner
            if (scanner != null) {
                try {
                    scanner.stopScan(scanCallback)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
            isScanning = false
        }

        if (isAdvertising) {
            val advertiser = adapter.bluetoothLeAdvertiser
            if (advertiser != null) {
                try {
                    advertiser.stopAdvertising(advertiseCallback)
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
            isAdvertising = false
        }
    }
}
