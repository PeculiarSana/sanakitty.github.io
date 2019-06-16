package io.github.sanakitty.loyaltypointsmarket;

import java.util.ArrayList;
import java.util.List;
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
import org.bukkit.metadata.FixedMetadataValue;

import io.github.sanakitty.loyaltypointsmarket.commands.CommandAddPlayer;
import io.github.sanakitty.loyaltypointsmarket.commands.CommandEditPlayer;

public class LPMCommandExecutor implements CommandExecutor {

	LoyaltyPointsMarket plugin;

	public LPMCommandExecutor(LoyaltyPointsMarket plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		String accessToken = plugin.getConfig().getString("access-token");
		String channelName = plugin.getConfig().getString("channel-name");
		//------------------------Player Commands---------------------------
		if (sender instanceof Player)
		{
			Player p = (Player) sender;
			if (args.length > 0)
			{
				//------------------------Request---------------------------
				if (args[0].equalsIgnoreCase("request")) 
					sender.sendMessage(ChatColor.RED + "This command can only be run by the console.");
				
				//------------------------Player---------------------------
				if (args[0].equalsIgnoreCase("player"))
				{
					//------------------------Add---------------------------
					if (args[1].equalsIgnoreCase("add"))
						new CommandAddPlayer(plugin, sender, args);
					//------------------------Edit---------------------------
					if (args[1].equalsIgnoreCase("edit"))
						new CommandEditPlayer(plugin, sender, args[2], args[3]);
					//------------------------Remove---------------------------
					if (args[1].equalsIgnoreCase("remove"))
						new CommandAddPlayer(plugin, sender, args);
				}
				//------------------------Check---------------------------
				if (args[0].equalsIgnoreCase("check"))
				{
					// If no username is supplied, check the stored username based on sender's
					if (args.length == 1) 
					{ 
						// name
						UUID playerId = p.getUniqueId();
						String result = plugin.PointRequest(accessToken, playerId, channelName, true);
						if (result.contains("User not found"))
						{
							p.sendMessage(ChatColor.RED + "The Twitch user "
									+ plugin.playerData.getString(p.getUniqueId().toString() + ".twitch")
									+ " does not exist. Please contact an administrator to fix this issue.");
						}
						else
							p.sendMessage(ChatColor.GOLD + "You have " + result + " KonaCoins.");
						
					} 
					else if (args.length == 2) 
					{
						if (p.hasPermission("lpm.check.other")) 
						{
							@SuppressWarnings("deprecation")
							UUID playerId = Bukkit.getPlayer(args[1]).getUniqueId();
							String result = plugin.PointRequest(accessToken, playerId, channelName, true);
							if (result.contains("User not found"))
								p.sendMessage(ChatColor.RED + "The Twitch user " + args[1]
										+ " does not exist. Please contact an administrator to fix this issue.");
							else
								p.sendMessage(
										ChatColor.GOLD + "Twitch user " + args[1] + " has " + result + " KonaCoins.");
						} else
							p.sendMessage(ChatColor.RED + "You do not have access to that command.");
					}
				}
				
				//------------------------Add---------------------------
				if (args[0].equalsIgnoreCase("add"))
				{
					if (p.hasPermission("lpm.points.modify"))
					{
						if (args.length > 2) {
							String username = args[1];
							@SuppressWarnings("deprecation")
							UUID playerId = Bukkit.getPlayer(username).getUniqueId();
		
							Integer points = Integer.parseInt(args[2]);
		
							plugin.PointAdd(accessToken, playerId, channelName, points);
		
							if (sender instanceof Player)
								sender.sendMessage("Added " + points + " KonaCoins to Twitch User " + args[1] + ". "
										+ args[2] + " now has " + plugin.PointRequest(accessToken, playerId, channelName, true)
										+ " KonaCoins.");
							else
								Bukkit.getLogger()
										.info("Added " + points + " KonaCoins to Twitch User " + args[1] + ". " + args[1]
												+ " now has " + plugin.PointRequest(accessToken, playerId, channelName, true)
												+ " KonaCoins.");
						}
					}
					else
						p.sendMessage(ChatColor.RED + "You do not have access to that command.");
				}
				
				//------------------------Subtract---------------------------
				if (args[0].equalsIgnoreCase("subtract"))
				{
					if (p.hasPermission("lpm.points.modify"))
					{
						if (args.length > 2) {
							String username = args[1];
							@SuppressWarnings("deprecation")
							UUID playerId = Bukkit.getPlayer(username).getUniqueId();

							Integer points = Integer.parseInt(args[2]);

							plugin.PointSubtract(accessToken, playerId, channelName, points);

							if (sender instanceof Player)
								sender.sendMessage("Subtracted " + points + " KonaCoins from Twitch User " + args[1] + ". "
										+ args[1] + " has " + plugin.PointRequest(accessToken, playerId, channelName, true)
										+ " KonaCoins left.");
							else
								Bukkit.getLogger()
										.info("Subtracted " + points + " KonaCoins from Twitch User " + args[1] + ". " + args[1]
												+ " has " + plugin.PointRequest(accessToken, playerId, channelName, true)
												+ " KonaCoins left.");
						}
					}
					else
						p.sendMessage(ChatColor.RED + "You do not have access to that command.");
				}
				
				//------------------------Shop---------------------------
				if (args[0].equalsIgnoreCase("shop"))
				{
					//------------------------New---------------------------
					if (args[1].equalsIgnoreCase("new"))
					{
						NewShop(sender, args);
					}
					//------------------------List---------------------------
					if (args[1].equalsIgnoreCase("list"))
					{
						Integer number = 0;
						ArrayList<String> shopkeepers = new ArrayList<String>();
						for (String key : plugin.shopkeeperData.getConfigurationSection("shopkeepers").getKeys(false))
						{
							if (plugin.shopkeeperData.getString("shopkeepers." + key + ".owner").equalsIgnoreCase(p.getUniqueId().toString()))
							{
								number += 1;
								shopkeepers.add(key);
							}
						}
						
						p.sendMessage(ChatColor.BLUE + number.toString() + " owned shopkeepers:");
						for (String shopkeeper : shopkeepers)
						{
							p.sendMessage(ChatColor.AQUA + "  " + plugin.shopkeeperData.getString("shopkeepers." + shopkeeper + ".name") + ChatColor.WHITE +
									" - " + ChatColor.GOLD + "Unclaimed Points: " + plugin.shopkeeperData.getInt("shopkeepers." + shopkeeper + ".storedPoints"));
						}
					}
					//------------------------Pay---------------------------
					if (args[1].equalsIgnoreCase("pay"))
					{
						Integer toPay = 0;
						for (String key : plugin.shopkeeperData.getConfigurationSection("shopkeepers").getKeys(false))
						{
							if (plugin.shopkeeperData.getString("shopkeepers." + key + ".owner").equalsIgnoreCase(p.getUniqueId().toString()))
							{
								toPay += plugin.shopkeeperData.getInt("shopkeepers." + key + ".storedPoints");
								plugin.shopkeeperData.set("shopkeepers." + key + ".storedPoints", 0);
							}
						}
						plugin.PointAdd(accessToken, p.getUniqueId(), channelName, toPay);
						p.sendMessage(ChatColor.GREEN + "You've received " + toPay + " " + plugin.getConfig().getString("currency-name") + "!");
					}
				}
				
				if (args[0].equalsIgnoreCase("payday"))
				{
					plugin.Payday();
				}
			}
			else
			{
				//No arguments given
				p.sendMessage(ChatColor.RED + "Invalid arguments.");
			}
		}
			// -------------------ADMIN--------------------
			if (args[0].equalsIgnoreCase("admin")) 
			{
				if (!(sender instanceof Player))
					Bukkit.getLogger().warning("This command (-sk) can only be performed by a player.");
				else 
				{
					Player p = (Player) sender;
					if (p.hasPermission("lpm.admin"))
					{
						if (!plugin.getAdminMap().contains(p))
						{
							p.sendMessage(ChatColor.GOLD
									+ "Admin mode enabled.");
							plugin.addToAdminList(p);
						}
						else 
						{
							p.sendMessage(ChatColor.GOLD + "Deactivated admin mode.");
							plugin.removeFromAdminList(p);
						}
					}
					else
						p.sendMessage(ChatColor.RED + "You do not have access to that command.");
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
 
				//Normal shopkeeper
				if (args.length == 2) {
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
				// Creative shopkeeper
				else if (args.length >= 3 && args[2].equalsIgnoreCase("c"))
				{
					if (p.hasPermission("lpm.shopkeeper.creative"))
					{
						if (Material.getMaterial(plugin.getConfig().getString("shop-creation-item-creative")) != null)
						{
							if (plugin.getConfig().isSet("shop-creation-item-creative-name") || plugin.getConfig().isSet("shop-creation-item-creative-lore"))
							{
								ItemStack item = new ItemStack(Material.getMaterial(plugin.getConfig().getString("shop-creation-item-creative")), 1);
								ItemMeta meta = item.getItemMeta();
								if (plugin.getConfig().isSet("shop-creation-item-creative-name"))
									meta.setDisplayName(plugin.getConfig().getString("shop-creation-item-creative-name"));
								if (plugin.getConfig().isSet("shop-creation-item-creative-lore"))
								{
									meta.setLore(plugin.getConfig().getStringList("shop-creation-item-creative-lore"));
								}
								Bukkit.getLogger().info("Length: " + args.length);
								if (args.length == 4)
								{
									plugin.LoggerInfo(args[3]);
									p.setMetadata("shopkeeperProfession", new FixedMetadataValue(plugin, args[3]));
								}
								else
									p.setMetadata("shopkeeperProfession", new FixedMetadataValue(plugin, "unset"));
								
								item.setItemMeta(meta);
								p.getInventory().addItem(item);
							} 
							else
							{
								p.getInventory().addItem(new ItemStack(Material.getMaterial(plugin.getConfig().getString("shop-creation-item-creative")), 1));
							}
							p.sendMessage(ChatColor.DARK_GREEN + "You have received a Shopkeeper creation item. Right-click with it in hand on a chest to assign" + 
									" that chest as a shopkeepers' inventory, then right-click within " + plugin.getConfig().get("max-chest-distance") + " blocks" + 
									"of it to spawn your Shopkeeper!");
						}
						else
						{
							p.sendMessage(ChatColor.RED + "The item name " + plugin.getConfig().getString("shop-creation-item-creative") + " is invalid." + 
									" Please change the value given to 'shop-creation-item-creative' in this plugins' plugin.getConfig().yml file.");
						}
					}
					else
						p.sendMessage(ChatColor.RED + "You do not have the permission 'lpm.shopkeeper.creative' needed for this command.");
				}
				else
					p.sendMessage(ChatColor.RED + "Unknown command.");
			}
			else
			{
				p.sendMessage(ChatColor.RED + plugin.getConfig().getString("unregistered-player-message"));
			}
		}
	}
}
