package com.wonit.controleremotopc;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONObject;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
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

    @PluginMethod
    public void startDiscovery(PluginCall call) {
        if (listening) {
            call.resolve();
            return;
        }
        listening = true;
        found.clear();

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
        call.resolve();
    }
}
