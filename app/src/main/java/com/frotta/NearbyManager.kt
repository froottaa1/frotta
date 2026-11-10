package com.frotta

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy

class NearbyManager(
    context: Context,
    private val onStatus: (String) -> Unit,
    private val onDeviceFound: (String, String) -> Unit,
    private val onConnected: () -> Unit
) {
    private val client: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val serviceId = "com.frotta.share"
    private val strategy = Strategy.P2P_CLUSTER
    private var connectedEndpoint: String? = null

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            onStatus("تم استلام بيانات")
        }

        override fun onPayloadTransferUpdate(
            endpointId: String,
            update: PayloadTransferUpdate
        ) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                onStatus("تم النقل بنجاح")
            }
        }
    }

    private val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            client.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if (result.status.isSuccess) {
                connectedEndpoint = endpointId
                onStatus("تم الاتصال")
                onConnected()
            } else {
                onStatus("فشل الاتصال")
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpoint = null
            onStatus("انقطع الاتصال")
        }
    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            onDeviceFound(endpointId, info.endpointName)
        }

        override fun onEndpointLost(endpointId: String) {}
    }

    fun startReceiving(deviceName: String) {
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        client.startAdvertising(deviceName, serviceId, connectionCallback, options)
            .addOnSuccessListener { onStatus("جاهز للاستقبال... استنى الجهاز التاني") }
            .addOnFailureListener { onStatus("فشل: ${it.message}") }
    }

    fun startSearching() {
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()
        client.startDiscovery(serviceId, discoveryCallback, options)
            .addOnSuccessListener { onStatus("بدور على أجهزة قريبة...") }
            .addOnFailureListener { onStatus("فشل: ${it.message}") }
    }

    fun connectTo(endpointId: String, myName: String) {
        client.requestConnection(myName, endpointId, connectionCallback)
            .addOnFailureListener { onStatus("فشل الاتصال: ${it.message}") }
    }

    fun stopAll() {
        client.stopAdvertising()
        client.stopDiscovery()
        client.stopAllEndpoints()
        connectedEndpoint = null
    }
}
