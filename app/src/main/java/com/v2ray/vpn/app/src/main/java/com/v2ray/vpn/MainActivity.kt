package com.v2ray.vpn

import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import com.v2ray.vpn.model.ConnectionState
import com.v2ray.vpn.model.ProtocolType
import com.v2ray.vpn.model.ServerProfile
import com.v2ray.vpn.parser.V2RayConfigParser
import com.v2ray.vpn.service.V2RayManager
import com.v2ray.vpn.ui.screens.HomeScreen
import com.v2ray.vpn.ui.screens.ServersScreen
import com.v2ray.vpn.ui.screens.SettingsScreen
import com.v2ray.vpn.ui.theme.*

class MainActivity : ComponentActivity() {
    private val serverList = mutableStateListOf<ServerProfile>()
    private var pendingConnectServer: ServerProfile? = null

    private val vpnPrepareLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            pendingConnectServer?.let { server ->
                V2RayManager.startV2Ray(this, server)
            }
        } else {
            Toast.makeText(this, "VPN Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        seedSampleProfiles()

        intent?.dataString?.let { uri ->
            V2RayConfigParser.parseUri(uri)?.let { parsed ->
                serverList.add(0, parsed)
                Toast.makeText(this, "Imported profile: ${parsed.name}", Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            V2RayVPNTheme {
                val connectionState by V2RayManager.connectionState.collectAsState()
                val currentServer by V2RayManager.currentServer.collectAsState()
                val trafficStats by V2RayManager.trafficStats.collectAsState()
                var currentTab by remember { mutableStateOf(NavTab.HOME) }
                val clipboardManager = LocalClipboardManager.current

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavTab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = currentTab == tab,
                                    onClick = { currentTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                                    label = { Text(tab.title) }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentTab) {
                            NavTab.HOME -> HomeScreen(
                                connectionState = connectionState,
                                currentServer = currentServer ?: serverList.firstOrNull(),
                                trafficStats = trafficStats,
                                onToggleConnect = {
                                    val active = currentServer ?: serverList.firstOrNull()
                                    if (active == null) return@HomeScreen
                                    if (connectionState == ConnectionState.CONNECTED) {
                                        V2RayManager.stopV2Ray(this@MainActivity)
                                    } else {
                                        startVpnWithPermission(active)
                                    }
                                },
                                onSelectServerClick = { currentTab = NavTab.SERVERS },
                                onPingTest = {
                                    currentServer?.let { s -> V2RayManager.measurePing(s) { s.pingMs = it } }
                                }
                            )
                            NavTab.SERVERS -> ServersScreen(
                                servers = serverList,
                                selectedServerId = currentServer?.id ?: serverList.firstOrNull()?.id,
                                onSelectServer = { server -> startVpnWithPermission(server) },
                                onImportClick = {
                                    val text = clipboardManager.getText()?.text ?: ""
                                    val parsed = V2RayConfigParser.parseUri(text)
                                    if (parsed != null) serverList.add(0, parsed)
                                },
                                onPingAllClick = {
                                    serverList.forEach { s -> V2RayManager.measurePing(s) { s.pingMs = it } }
                                },
                                onDeleteServer = { serverList.remove(it) }
                            )
                            NavTab.SETTINGS -> SettingsScreen()
                        }
                    }
                }
            }
        }
    }

    private fun startVpnWithPermission(server: ServerProfile) {
        pendingConnectServer = server
        val intent = VpnService.prepare(this)
        if (intent != null) {
            vpnPrepareLauncher.launch(intent)
        } else {
            V2RayManager.startV2Ray(this, server)
        }
    }

    private fun seedSampleProfiles() {
        if (serverList.isEmpty()) {
            serverList.add(
                ServerProfile(
                    name = "US Fast VLESS - Singapore CDN",
                    protocol = ProtocolType.VLESS,
                    address = "sg.v2ray-node.com",
                    port = 443,
                    uuid = "e7f535d1-70ec-4013-9958-4a8ca67af542",
                    tls = "tls",
                    sni = "sg.v2ray-node.com",
                    pingMs = 48
                )
            )
        }
    }
}

enum class NavTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    HOME("Home", Icons.Default.PowerSettingsNew),
    SERVERS("Servers", Icons.Default.Dns),
    SETTINGS("Settings", Icons.Default.Tune)
}
