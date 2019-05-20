package io.github.sanakitty.loyaltypointsmarket.commands;

import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import io.github.sanakitty.loyaltypointsmarket.LoyaltyPointsMarket;

public class CommandAddPlayer {
	public CommandAddPlayer (LoyaltyPointsMarket plugin, CommandSender sender, String[] args)
	{
		String accessToken = plugin.getConfig().getString("access-token");
		String channelName = plugin.getConfig().getString("channel-name");
		
		Player p = null;
		if (sender instanceof Player)
		{
			p = (Player) sender;
			if (!p.hasPermission("lpm.addplayer"))
			{
				p.sendMessage(ChatColor.DARK_RED + "You do not have access to that command.");
				return;
			}
		}
		if (args.length != 3)
		{
			if (p != null)
				p.sendMessage(ChatColor.RED + "Usage: /lpm adduser <Minecraft Username> <Twitch Username>");
		}
			else
				Bukkit.getLogger().info("Usage: lpm adduser <Minecraft Username> <Twitch Username>");
		
		String gameName = args[1];
		String twitchName = args[2];
		@SuppressWarnings("deprecation")
		Player targetPlayer = Bukkit.getPlayer(gameName);
		UUID playerId = targetPlayer.getUniqueId();
		String result = plugin.PointRequest(accessToken, playerId, channelName, false);

		if (result != null)
			Bukkit.getLogger().log(Level.WARNING, "The Twitch user " + args[2] + " does not exist.");
		else {
			plugin.playerData.set(playerId + ".minecraft", gameName);
			plugin.playerData.set(playerId + ".twitch", twitchName);
			plugin.playerData.set(playerId + ".shopkeepers", 0);
			plugin.playerData.set(playerId + ".shopkeeperLimit", 2);
			plugin.saveCustomYml(plugin.playerData, plugin.playerFile);
		}
		
		if (sender instanceof Player)
			((Player) sender).sendMessage(ChatColor.GREEN + "Saved new player " + gameName + " with Twitch username " + twitchName);
		else
			Bukkit.getLogger().info("Saved new player " + gameName + " with Twitch username " + twitchName);
		
	}
}
