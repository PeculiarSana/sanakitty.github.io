package io.github.sanakitty.streamlabsintegration;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class EventTesting implements Listener{
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Bukkit.broadcastMessage("Welcome to the server, " + event.getPlayer().getName() + "!");
	}
}
