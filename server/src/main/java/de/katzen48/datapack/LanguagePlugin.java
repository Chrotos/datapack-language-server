package de.katzen48.datapack;

import java.net.Socket;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.services.LanguageClient;

@Plugin(name = "Language-Server", version = "1.0-SNAPSHOT")
public class LanguagePlugin extends JavaPlugin implements Listener {
    private CommandCompiler commandCompiler;
    private DefaultLanguageServer languageServer;
    private Socket socket;

    @Override
    public void onEnable() {
        super.onEnable();

        commandCompiler = new CommandCompiler(getLogger());
        languageServer = new DefaultLanguageServer(commandCompiler);

        try {
            socket = new Socket("127.0.0.1", 8123);
            try {
                Launcher<LanguageClient> launcher = Launcher.createLauncher(languageServer, LanguageClient.class, socket.getInputStream(), socket.getOutputStream());
                LanguageClient client = launcher.getRemoteProxy();
                languageServer.connect(client);
                launcher.startListening();
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.shutdown();
            }
        } catch (Exception e) {
            Bukkit.shutdown();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();

        try {
            if (languageServer != null) {
                languageServer.shutdown();
            }
        } catch (Exception ignored) {}

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {}
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () ->{
            if (event.getEntity() != null && event.getEntity().isValid()) {
                event.getEntity().remove();
            }
        });
    }
}