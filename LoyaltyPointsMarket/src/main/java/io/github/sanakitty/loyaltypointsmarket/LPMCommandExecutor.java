package io.github.sanakitty.loyaltypointsmarket;

import java.util.ArrayList;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.sanakitty.loyaltypointsmarket.LoyaltyPointsMarket.ShopType;
import io.github.sanakitty.loyaltypointsmarket.commands.CommandAddPlayer;

public class LPMCommandExecutor implements CommandExecutor {

	LoyaltyPointsMarket plugin;

	public LPMCommandExecutor(LoyaltyPointsMarket plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		String accessToken = plugin.getConfig().getString("access-token");
		String channelName = plugin.getConfig().getString("channel-name");
		if (args.length > 0) {
			//---------------------POINTS---------------------
			if (args[0].equalsIgnoreCase("points")) {
				if (args[1].equalsIgnoreCase("check")) { // COMMAND: /lpm check <username>
					if (sender instanceof Player) {
						Player p = (Player) sender;
						if (args.length == 2) { // If no username is supplied, check the stored username based on sender's
												// name
							UUID playerId = p.getUniqueId();
							String result = plugin.PointRequest(accessToken, playerId, channelName, true);
							if (result.contains("User not found"))
								p.sendMessage(ChatColor.RED + "The Twitch user "
										+ plugin.playerData.getString(p.getUniqueId().toString() + ".twitch")
										+ " does not exist. Please contact an administrator to fix this issue.");
							else
								p.sendMessage(ChatColor.GOLD + "You have " + result + " KonaCoins.");
						} else if (args.length == 3) {
							if (p.hasPermission("lpm.check.nonuser")) {
								UUID playerId = Bukkit.getPlayer(args[2]).getUniqueId();
								String result = plugin.PointRequest(accessToken, playerId, channelName, true);
								if (result.contains("User not found"))
									p.sendMessage(ChatColor.RED + "The Twitch user " + args[2]
											+ " does not exist. Please contact an administrator to fix this issue.");
								else
									p.sendMessage(
											ChatColor.GOLD + "Twitch user " + args[2] + " has " + result + " KonaCoins.");
							} else
								p.sendMessage(ChatColor.RED
										+ "You do not have permission to check another user's currency count.");

						}
					} else if (args.length == 1) {
						Bukkit.getLogger().log(Level.WARNING, "Please supply a username.");
						Bukkit.getLogger().log(Level.WARNING, "Usage: lpm check <username>.");
					} else if (args.length == 2) {
						UUID playerId = Bukkit.getPlayer(args[1]).getUniqueId();
						String result = plugin.PointRequest(accessToken, playerId, channelName, true);
						Bukkit.getLogger().info("Twitch user " + args[1] + " has " + result + " KonaCoins.");
					} else
						Bukkit.getLogger().log(Level.WARNING, "Usage: lpm check <username>");
				} 
				//SUBTRACT
				else  if (args[1].equalsIgnoreCase("subtract")) {
					if (args.length > 2) {
						String username = args[2];
						@SuppressWarnings("deprecation")
						UUID playerId = Bukkit.getPlayer(username).getUniqueId();

						Integer points = Integer.parseInt(args[3]);

						plugin.PointSubtract(accessToken, playerId, channelName, points);

						if (sender instanceof Player)
							sender.sendMessage("Subtracted " + points + " KonaCoins from Twitch User " + args[2] + ". "
									+ args[2] + " has " + plugin.PointRequest(accessToken, playerId, channelName, true)
									+ " KonaCoins left.");
						else
							Bukkit.getLogger()
									.info("Subtracted " + points + " KonaCoins from Twitch User " + args[2] + ". " + args[2]
											+ " has " + plugin.PointRequest(accessToken, playerId, channelName, true)
											+ " KonaCoins left.");
					}
				}
				//ADD
				else  if (args[1].equalsIgnoreCase("add")) {
					if (args.length > 2) {
						String username = args[2];
						@SuppressWarnings("deprecation")
						UUID playerId = Bukkit.getPlayer(username).getUniqueId();

						Integer points = Integer.parseInt(args[3]);

						plugin.PointAdd(accessToken, playerId, channelName, points);

						if (sender instanceof Player)
							sender.sendMessage("Added " + points + " KonaCoins to Twitch User " + args[2] + ". "
									+ args[2] + " now has " + plugin.PointRequest(accessToken, playerId, channelName, true)
									+ " KonaCoins.");
						else
							Bukkit.getLogger()
									.info("Added " + points + " KonaCoins to Twitch User " + args[2] + ". " + args[2]
											+ " now has " + plugin.PointRequest(accessToken, playerId, channelName, true)
											+ " KonaCoins.");
					}
				}
			} else if (args[0].equalsIgnoreCase("request")) { // COMMAND: /lpm request
				if ((sender instanceof Player)) {
					sender.sendMessage("This command can only be run by the console.");
					return false;
				} else if (args.length != 2) {
					plugin.getLogger().log(Level.WARNING, "Please supply an authorization token.");
					return false;
				} else
					plugin.TokenRequest(args[1]);

			} else if (args[0].equalsIgnoreCase("addplayer")) { // COMMAND: /lpm adduser <IGN> <Twitch Name>
				new CommandAddPlayer(plugin, sender, args);
			} 
			//---------------------SHOP---------------------
			//TODO: Add check to see if the player is registered on the list of players
			else if (args[0].equalsIgnoreCase("shop")) 
			{ 
				if (args[1].equalsIgnoreCase("new")) 
				{
					NewShop(sender, args);
				}
				else if (args[1].equalsIgnoreCase("admin")) 
				{
					if (args.length > 2) 
					{
						if (!(sender instanceof Player))
							Bukkit.getLogger().warning("This command (-sk) can only be performed by a player.");
						else 
						{
							Player p = (Player) sender;
							if (args[2].equalsIgnoreCase("-sk")) 
							{
								p.sendMessage(ChatColor.GOLD
										+ "Right-click a Villager to check if they are a shopkeeper, and if so, check who owns them.");
								plugin.addToList(p);
							} 
							else 
							{
								p.sendMessage(ChatColor.GOLD + "Deactivated admin mode.");
								plugin.removeFromList(p);
							}
						}
					} 
					else if (sender instanceof Player && plugin.getAdminMap().contains(sender)) 
					{
						sender.sendMessage(ChatColor.GOLD + "Exiting admin check mode.");
						plugin.removeFromList((Player) sender);
					}
				}
			}
		}
		return true;
	}
	
	public void NewShop(CommandSender sender, String[] args)
	{
		if (sender instanceof Player) 
		{
			Player p = (Player) sender;
			if (plugin.playerData.isSet(p.getUniqueId().toString()))
			{
				int currentShopkeepers = plugin.playerData.getInt(p.getUniqueId().toString() + ".shopkeepers");
				int shopkeeperLimit = plugin.playerData.getInt(p.getUniqueId().toString() + ".shopkeeperLimit");
				
				//Creative shopkeeper
				if (args.length == 3 && args[2].equalsIgnoreCase("-c")) 
				{
					if (p.hasPermission("lpm.shopkeeperAdmin")) 
					{
						if (args.length == 4)
							plugin.instantiateShopkeeper(p, args[1], ShopType.creative, null);
						else
							plugin.instantiateShopkeeper(p, null, ShopType.creative, null);
					} 
					else
						p.sendMessage(ChatColor.RED + "You do not have permission to create a creative shopkeeper.");
				} 
				//Normal shopkeeper
				else if (args.length == 2) {
					if (currentShopkeepers < shopkeeperLimit)
					{
						if (Material.getMaterial(plugin.getConfig().getString("shop-creation-item")) != null)
						{
							if (plugin.getConfig().isSet("shop-creation-item-name") || plugin.getConfig().isSet("shop-creation-item-name"))
							{
								ItemStack item = new ItemStack(Material.getMaterial(plugin.getConfig().getString("shop-creation-item")), 1);
								ItemMeta meta = item.getItemMeta();
								if (plugin.getConfig().isSet("shop-creation-item-name"))
									meta.setDisplayName(plugin.getConfig().getString("shop-creation-item-name"));
								if (plugin.getConfig().isSet("shop-creation-item-lore"))
								{
									ArrayList<String> lore = new ArrayList<String>();
									meta.setLore(plugin.getConfig().getStringList("shop-creation-item-lore"));
								}
									
								item.setItemMeta(meta);
								p.getInventory().addItem(item);
							} 
							else
							{
								p.getInventory().addItem(new ItemStack(Material.getMaterial(plugin.getConfig().getString("shop-creation-item")), 1));
							}
							p.sendMessage(ChatColor.DARK_GREEN + "You have received a Shopkeeper creation item. Right-click with it in hand on a chest to assign" + 
									" that chest as a shopkeepers' inventory, then right-click within " + plugin.getConfig().get("max-chest-distance") + " blocks" + 
									"of it to spawn your Shopkeeper!");
						}
						else
						{
							p.sendMessage(ChatColor.RED + "The item name " + plugin.getConfig().getString("shop-creation-item") + " is invalid." + 
									" Please change the value given to 'shop-creation-item' in this plugins' plugin.getConfig().yml file.");
						}
					}
					else 
					{
						p.sendMessage(ChatColor.RED + "You already have " + shopkeeperLimit + " Shopkeepers active.");
					}
				}
			}
			else
			{
				p.sendMessage(ChatColor.RED + plugin.getConfig().getString("unregistered-player-message"));
			}
		}
	}
}
