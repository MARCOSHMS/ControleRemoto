package com.wonit.controleremotopc;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiManager;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

// Plugin do Capacitor que fica escutando a rede Wi-Fi por pacotes UDP em
// broadcast enviados pelo servidor no PC (veja startDiscoveryBroadcast em
// server.js). Quando acha um PC, avisa o JavaScript via evento
// "deviceFound" — assim o app mostra a lista sem precisar digitar IP.
@CapacitorPlugin(name = "PcDiscovery")
public class PcDiscoveryPlugin extends Plugin {

    private static final int DISCOVERY_PORT = 41234;

    private DatagramSocket socket;
    private Thread listenThread;
    private volatile boolean listening = false;
    private final Map<String, JSObject> found = new HashMap<>();

    private WifiManager.MulticastLock multicastLock;
    private WifiManager.WifiLock wifiLock;

    @PluginMethod
    public void startDiscovery(PluginCall call) {
        if (listening) {
            call.resolve();
            return;
        }
        listening = true;
        found.clear();

        // Em muitos aparelhos, o rádio Wi-Fi filtra pacotes de
        // broadcast/multicast pra economizar bateria enquanto a tela
        // está ligada em modo de baixo consumo. Esses dois "locks"
        // avisam o Android "estou de olho na rede, não filtra".
        try {
            WifiManager wifi = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifi != null) {
                multicastLock = wifi.createMulticastLock("controleRemotoPcDiscovery");
                multicastLock.setReferenceCounted(true);
                multicastLock.acquire();

                wifiLock = wifi.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "controleRemotoPcWifiLock");
                wifiLock.setReferenceCounted(true);
                wifiLock.acquire();
            }
        } catch (Exception e) {
            // Se falhar, segue tentando escutar mesmo assim
        }

        listenThread = new Thread(() -> {
            try {
                socket = new DatagramSocket(DISCOVERY_PORT);
                socket.setBroadcast(true);
                byte[] buffer = new byte[1024];

                while (listening) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    try {
                        socket.receive(packet);
                    } catch (SocketException closed) {
                        break; // socket foi fechado (stopDiscovery), sai do loop
                    }

                    String msg = new String(packet.getData(), 0, packet.getLength());
                    try {
                        JSONObject json = new JSONObject(msg);
                        if (!"controle-remoto-pc".equals(json.optString("app"))) continue;

                        String name = json.optString("name");
                        String ip = json.optString("ip");
                        int port = json.optInt("port");
                        String key = ip + ":" + port;

                        if (!found.containsKey(key)) {
                            JSObject device = new JSObject();
                            device.put("name", name);
                            device.put("ip", ip);
                            device.put("port", port);
                            found.put(key, device);
                            notifyListeners("deviceFound", device);
                        }
                    } catch (Exception parseError) {
                        // pacote de outro app/formato inesperado, ignora
                    }
                }
            } catch (Exception e) {
                if (listening) {
                    JSObject err = new JSObject();
                    err.put("message", e.getMessage());
                    notifyListeners("discoveryError", err);
                }
            }
        });
        listenThread.start();
        call.resolve();
    }

    @PluginMethod
    public void stopDiscovery(PluginCall call) {
        listening = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        try {
            if (multicastLock != null && multicastLock.isHeld()) multicastLock.release();
            if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();
        } catch (Exception e) {
            // ignora
        }
        call.resolve();
    }

    // Descobre o próprio IP do celular na rede Wi-Fi — usado como plano B
    // (escaneamento de sub-rede) quando o broadcast não chega, o que
    // acontece em redes corporativas/públicas com "isolamento de
    // cliente" ativado no roteador.
    @PluginMethod
    public void getLocalIp(PluginCall call) {
        String ip = null;
        try {
            ConnectivityManager cm = (ConnectivityManager) getContext()
                    .getApplicationContext()
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            Network network = cm.getActiveNetwork();
            LinkProperties linkProperties = cm.getLinkProperties(network);
            if (linkProperties != null) {
                for (LinkAddress la : linkProperties.getLinkAddresses()) {
                    InetAddress addr = la.getAddress();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        ip = addr.getHostAddress();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            // segue pro fallback abaixo
        }

        if (ip == null) {
            try {
                WifiManager wifi = (WifiManager) getContext()
                        .getApplicationContext()
                        .getSystemService(Context.WIFI_SERVICE);
                if (wifi != null) {
                    int ipInt = wifi.getConnectionInfo().getIpAddress();
                    if (ipInt != 0) {
                        ip = String.format(
                                "%d.%d.%d.%d",
                                (ipInt & 0xff),
                                (ipInt >> 8 & 0xff),
                                (ipInt >> 16 & 0xff),
                                (ipInt >> 24 & 0xff)
                        );
                    }
                }
            } catch (Exception e) {
                // sem sorte, ip continua null
            }
        }

        JSObject result = new JSObject();
        result.put("ip", ip);
        call.resolve(result);
    }
}
