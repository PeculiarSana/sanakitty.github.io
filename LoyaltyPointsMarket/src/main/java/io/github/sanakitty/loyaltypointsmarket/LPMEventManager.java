package io.github.sanakitty.loyaltypointsmarket;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import io.github.sanakitty.loyaltypointsmarket.LoyaltyPointsMarket.LogType;
import io.github.sanakitty.loyaltypointsmarket.LoyaltyPointsMarket.ShopType;
import io.github.sanakitty.loyaltypointsmarket.Inventories.ShopkeeperEditHolder;
import io.github.sanakitty.loyaltypointsmarket.Inventories.ShopkeeperItemPriceHolder;
import io.github.sanakitty.loyaltypointsmarket.Inventories.ShopkeeperPricesHolder;
import io.github.sanakitty.loyaltypointsmarket.Inventories.ShopkeeperShopHolder;
import io.github.sanakitty.loyaltypointsmarket.events.ShopkeeperDeathEvent;
import net.md_5.bungee.api.ChatColor;

public class LPMEventManager implements Listener {

	LoyaltyPointsMarket plugin;

	public LPMEventManager(LoyaltyPointsMarket plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onVillagerInventoryOpen(InventoryOpenEvent e) {
		if (e.getInventory().getType() == InventoryType.MERCHANT && plugin.shopkeeperData
				.contains(((Villager) e.getInventory().getHolder()).getUniqueId().toString())) {
			e.setCancelled(true);
		}
	}

	@EventHandler
	public void onVillagerInteract(PlayerInteractEntityEvent e) {
		Player p = e.getPlayer();
		// Safety feature to prevent baby villagers if clicking with an egg
		if (p.getInventory().getItemInMainHand() != null && p.getInventory().getItemInMainHand().getType() == Material.VILLAGER_SPAWN_EGG) 
		{
			if (p.hasMetadata("shopkeepChestLoc"))
				p.sendMessage(ChatColor.RED + "Click on a block with this egg to spawn your Shopkeeper.");
			else
				p.sendMessage(ChatColor.RED + "Click on a chest to assign it as your Shopkeepers' inventory.");
			e.setCancelled(true);
		}
		
		if (e.getRightClicked().getType() == EntityType.VILLAGER && e.getHand().equals(EquipmentSlot.HAND) && plugin.shopkeeperData.contains(e.getRightClicked().getUniqueId().toString())) 
		{
			Villager v = (Villager) e.getRightClicked();
			p.setMetadata("shopkeeperID", new FixedMetadataValue(plugin, v.getUniqueId()));
			p.setMetadata("shopkeeperName", new FixedMetadataValue(plugin, v.getCustomName()));

			// Code to run if the player is currently debugging
			if (plugin.getAdminMap().contains((Object) p)) 
			{
				p.sendMessage(ChatColor.GOLD + "Shopkeeper data:");
				p.sendMessage(ChatColor.GOLD + "  id: " + ChatColor.RESET + v.getUniqueId());
				p.sendMessage(ChatColor.GOLD + "  name: " + ChatColor.RESET + plugin.shopkeeperData.get(v.getUniqueId() + ".name"));
				p.sendMessage(ChatColor.GOLD + "  owner: " + ChatColor.RESET + plugin.shopkeeperData.get(v.getUniqueId() + ".owner"));
				p.sendMessage(ChatColor.GOLD + "  type: " + ChatColor.RESET + plugin.shopkeeperData.get(v.getUniqueId() + ".type"));
				p.sendMessage(ChatColor.GOLD + "  location: ");
				p.sendMessage(ChatColor.GOLD + "    world: " + ChatColor.RESET + plugin.shopkeeperData.get(v.getUniqueId() + ".location.world"));
				p.sendMessage(ChatColor.GOLD + "    x: " + ChatColor.RESET + plugin.shopkeeperData.get(v.getUniqueId() + ".location.x"));
				p.sendMessage(ChatColor.GOLD + "    y: " + ChatColor.RESET + plugin.shopkeeperData.get(v.getUniqueId() + ".location.y"));
				p.sendMessage(ChatColor.GOLD + "    z: " + ChatColor.RESET + plugin.shopkeeperData.get(v.getUniqueId() + ".location.z"));
				p.sendMessage(ChatColor.GOLD + "  chest-location: ");
				p.sendMessage(ChatColor.GOLD + "    world: " + ChatColor.RESET + plugin.shopkeeperData.get(v.getUniqueId() + ".chest-location.world"));
				p.sendMessage(ChatColor.GOLD + "    x: " + ChatColor.RESET + plugin.shopkeeperData.get(v.getUniqueId() + ".chest-location.x"));
				p.sendMessage(ChatColor.GOLD + "    y: " + ChatColor.RESET + plugin.shopkeeperData.get(v.getUniqueId() + ".chest-location.y"));
				p.sendMessage(ChatColor.GOLD + "    z: " + ChatColor.RESET + plugin.shopkeeperData.get(v.getUniqueId() + ".chest-location.z"));
				

			
			} // Code to run to edit the shopkeeper
			else if (p.isSneaking() && p.getUniqueId().compareTo(UUID.fromString(plugin.shopkeeperData.get(v.getUniqueId() + ".owner").toString())) == 0) 
			{
				// Normal shopkeeper edit inventory
				if (plugin.shopkeeperData.getString(v.getUniqueId() + ".type").equalsIgnoreCase("normal"))
				{
					// Adds a one tick delay to prevent issues
					Bukkit.getServer().getScheduler().runTaskLater(plugin, new Runnable()
					{
						public void run() 
						{
							ShopkeeperEditOpen(p, "Editing Shopkeeper");
						}
					}, 1);

				}
			
			} //Code to run if the player is just opening the shop to buy
			else if (!p.isSneaking()) 
			{
				Inventory inv = Bukkit.createInventory(new ShopkeeperShopHolder(), 27, v.getCustomName());
				// Adds a one tick delay to prevent issues
				Bukkit.getServer().getScheduler().runTaskLater(plugin, new Runnable() 
				{
					public void run()
					{
						p.openInventory(inv);
					}
				}, 1);

				Location loc = new Location(p.getWorld(),
						Float.parseFloat(plugin.shopkeeperData.get(v.getUniqueId() + ".chest-location.x").toString()),
						Float.parseFloat(plugin.shopkeeperData.get(v.getUniqueId() + ".chest-location.y").toString()),
						Float.parseFloat(plugin.shopkeeperData.get(v.getUniqueId() + ".chest-location.z").toString()));
				
				// Instantiates the shopkeeper's inventory based on his linked chest
				if(v.getWorld().getBlockAt(loc).getType()==Material.CHEST)
				{
					Chest chest = (Chest) v.getWorld().getBlockAt(loc).getState();
					for (int i = 0; i < chest.getBlockInventory().getSize(); i++) {
						ItemStack iS = chest.getBlockInventory().getItem(i);
						String ymlLoc = v.getUniqueId() + ".inventory." + i;
						if (iS != null) {
							if (!plugin.shopkeeperData.isSet(ymlLoc + ".price")) 
							{
								plugin.shopkeeperData.set(ymlLoc + ".price", 0);
								plugin.saveCustomYml(plugin.shopkeeperData, plugin.shopkeepersFile);
							}
							addItem(inv, iS.getType(), iS.getAmount(), i, plugin.shopkeeperData.getInt(ymlLoc + ".price"));
						} 
						else 
						{
							plugin.shopkeeperData.set(ymlLoc, null);
							plugin.saveCustomYml(plugin.shopkeeperData, plugin.shopkeepersFile);
						}
					}
				}
			}
		}
	}

	public ItemStack newCustomItemStack(Material material, int quantity, String displayName, List<String> lore) {
		ItemStack itemStack = new ItemStack(material, quantity);
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setDisplayName(displayName);
		itemMeta.setLore(lore);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public void addItem(Inventory inv, Material material, int quantity, int slot, int price) {
		ItemStack itemStack = new ItemStack(material, quantity);
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setLore(Arrays.asList(
				ChatColor.GREEN + "Cost: " + ChatColor.GOLD + price + " " + plugin.getConfig().get("currency-name")));
		itemStack.setItemMeta(itemMeta);
		inv.setItem(slot, itemStack);
	}

	ItemStack editedItem = null;

	/*
	 * @EventHandler public void inventoryMoveEvent(InventoryMoveItemEvent e) { if
	 * (e.getDestination() instanceof ShopkeeperShopHolder || e.getDestination()
	 * instanceof ShopkeeperEditHolder || e.getDestination() instanceof
	 * ShopkeeperPricesHolder || e.getDestination() instanceof
	 * ShopkeeperItemPriceHolder || e.getDestination() instanceof
	 * ShopkeeperShopHolder) { e.setCancelled(true); } }
	 */

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if (e.getClickedInventory() == null) 
		{
			return;
		}

		Player p = (Player) e.getWhoClicked();
		String twitchName = plugin.playerData.get(p.getUniqueId().toString() + ".twitch").toString();
		Villager v = null;
		if (true) {
			for (Entity ent : p.getWorld().getEntities())
				if (ent.getUniqueId().compareTo(UUID.fromString(p.getMetadata("shopkeeperID").get(0).asString())) == 0)
					v = (Villager) ent;
		}
		
		if (e.getClickedInventory().getHolder() instanceof ShopkeeperShopHolder ||
			 e.getClickedInventory().getHolder() instanceof ShopkeeperEditHolder ||
			 e.getClickedInventory().getHolder() instanceof ShopkeeperPricesHolder ||
			 e.getClickedInventory().getHolder() instanceof ShopkeeperItemPriceHolder ||
			 e.getClickedInventory().getHolder() instanceof ShopkeeperShopHolder) 
		 {
			 e.setCancelled(true);
		 }
		
		// ~~~~ShopkeeperShopHolder~~~~
		if (e.getClickedInventory().getHolder() instanceof ShopkeeperShopHolder) {
			if (e.getCurrentItem() != null) {
				int price = plugin.shopkeeperData
						.getInt(v.getUniqueId().toString() + ".inventory." + e.getSlot() + ".price");
				if (Integer.parseInt(plugin.PointRequest(plugin.access_token,
						p.getUniqueId(), plugin.getConfig().getString("channel-name"), true)) >= price) 
				{
					// Point subtraction from customer
					plugin.PointSubtract(plugin.access_token, p.getUniqueId(), plugin.channel_name, price);
					p.sendMessage("You have "
							+ plugin.PointRequest(plugin.access_token, p.getUniqueId(), plugin.channel_name, true)
							+ " KonaCoins left.");
					p.getInventory()
							.addItem(new ItemStack(e.getCurrentItem().getType(), e.getCurrentItem().getAmount()));
					plugin.logToFile(LogType.Points, p.getName() + " {" + p.getUniqueId() + "} spent " + price + " "
							+ plugin.getConfig().get("currency-name"));

					// Point addition for owner
					UUID id = UUID.fromString(plugin.shopkeeperData
							.get(v.getUniqueId().toString() + ".owner").toString());
					if (plugin.shopkeeperData
							.get(v.getUniqueId().toString() + ".type") != ShopType.creative) 
					{
						plugin.PointAdd(plugin.access_token, id, plugin.channel_name, price);
					}
				}
				else
				{
					p.sendMessage(ChatColor.RED + "You do not have enough " + plugin.getConfig().get("currency-name") + " to purchase this!");
				}
			}
		}
		// ~~~~ShopkeeperEditHolder~~~~
		if (e.getClickedInventory().getHolder() instanceof ShopkeeperEditHolder) {
			// EDIT prices
			if (e.getCurrentItem().getType() == Material.GOLD_NUGGET) {
				ShopkeeperPricesOpen(p);
				// RENAME the shopkeeper
			} else if (e.getCurrentItem().getType() == Material.INK_SAC) {
				p.closeInventory();
				p.sendMessage(ChatColor.GOLD + "This feature is not yet implemented. Sorry!");
				// DELETE the shopkeeper
			} else if (e.getCurrentItem().getType() == Material.FIRE_CHARGE) {
				v.setInvulnerable(false);
				v.damage(9000);
				p.closeInventory();
			}
		}
		// ~~~~ShopkeeperPricesHolder~~~~
		if (e.getClickedInventory().getHolder() instanceof ShopkeeperPricesHolder) {
			if (e.getCurrentItem().getType() == Material.BARRIER) {
				ShopkeeperEditOpen(p, "Editing Shopkeeper");
			} else {
				ItemStack item = e.getCurrentItem();
				ShopkeeperEditPriceOpen(p, item, plugin.shopkeeperData.getString(v.getUniqueId().toString() + ".inventory." + p.getMetadata("slotID") + ".price"));
				p.setMetadata("slotID", new FixedMetadataValue(plugin, e.getSlot()));
				editedItem = item;
			}
		}
		// ~~~~ShopkeeperItemPriceHolder~~~~
		if (e.getClickedInventory().getHolder() instanceof ShopkeeperItemPriceHolder) {
			String slotNum = p.getMetadata("slotID").get(0).asString();
			String ymlPrice = v.getUniqueId().toString() + ".inventory." + slotNum + ".price";
			if (e.getCurrentItem().getType() == Material.IRON_BLOCK) {
				plugin.shopkeeperData.set(ymlPrice, plugin.shopkeeperData.getInt(ymlPrice) + 1);
				ShopkeeperEditPriceOpen(p, editedItem, plugin.shopkeeperData
						.getString(v.getUniqueId().toString() + ".inventory." + slotNum + ".price"));
			} else if (e.getCurrentItem().getType() == Material.GOLD_BLOCK) {
				plugin.shopkeeperData.set(ymlPrice, plugin.shopkeeperData.getInt(ymlPrice) + 10);
				ShopkeeperEditPriceOpen(p, editedItem, plugin.shopkeeperData
						.getString(v.getUniqueId().toString() + ".inventory." + slotNum + ".price"));
			} else if (e.getCurrentItem().getType() == Material.DIAMOND_BLOCK) {
				plugin.shopkeeperData.set(ymlPrice, plugin.shopkeeperData.getInt(ymlPrice) + 100);
				ShopkeeperEditPriceOpen(p, editedItem, plugin.shopkeeperData
						.getString(v.getUniqueId().toString() + ".inventory." + slotNum + ".price"));
			} else if (e.getCurrentItem().getType() == Material.STONE) {
				plugin.shopkeeperData.set(ymlPrice, plugin.shopkeeperData.getInt(ymlPrice) - 1);
				ShopkeeperEditPriceOpen(p, editedItem, plugin.shopkeeperData
						.getString(v.getUniqueId().toString() + ".inventory." + slotNum + ".price"));
			} else if (e.getCurrentItem().getType() == Material.OBSIDIAN) {
				plugin.shopkeeperData.set(ymlPrice, plugin.shopkeeperData.getInt(ymlPrice) - 10);
				ShopkeeperEditPriceOpen(p, editedItem, plugin.shopkeeperData
						.getString(v.getUniqueId().toString() + ".inventory." + slotNum + ".price"));
			} else if (e.getCurrentItem().getType() == Material.BEDROCK) {
				plugin.shopkeeperData.set(ymlPrice, plugin.shopkeeperData.getInt(ymlPrice) - 100);
				ShopkeeperEditPriceOpen(p, editedItem, plugin.shopkeeperData
						.getString(v.getUniqueId().toString() + ".inventory." + slotNum + ".price"));
			} else if (e.getCurrentItem().getType() == Material.BARRIER) {
				ShopkeeperPricesOpen(p);
			}
			plugin.saveCustomYml(plugin.shopkeeperData, plugin.shopkeepersFile);
		}
	}

	// -------------------------CUSTOM INVENTORY METHODS-------------------------
	public void ShopkeeperEditOpen(Player p, String name) {
		Inventory inv = Bukkit.createInventory(new ShopkeeperEditHolder(), 9, name);

		ItemStack delete = newCustomItemStack(Material.FIRE_CHARGE, 1, "Remove Shopkeeper",
				Arrays.asList("Removes this shopkeeper."));
		ItemStack priceAdjust = newCustomItemStack(Material.GOLD_NUGGET, 1, "Adjust Prices",
				Arrays.asList("Allows you to adjust sale prices."));
		ItemStack rename = newCustomItemStack(Material.INK_SAC, 1, "Rename Shopkeeper", Arrays.asList(
				"Allows you to rename this shopkeeper.", ChatColor.RED + "This feature is not yet implemented!"));

		inv.setItem(3, priceAdjust);
		inv.setItem(4, rename);
		inv.setItem(5, delete);

		p.openInventory(inv);
	}

	public void ShopkeeperPricesOpen(Player p) {
		String strId = p.getMetadata("shopkeeperID").get(0).asString();

		Inventory inv = Bukkit.createInventory(new ShopkeeperPricesHolder(), 4 * 9, "Editing shop prices");

		Location loc = new Location(p.getWorld(),
				Float.parseFloat(plugin.shopkeeperData.get(strId + ".chest-location.x").toString()),
				Float.parseFloat(plugin.shopkeeperData.get(strId + ".chest-location.y").toString()),
				Float.parseFloat(plugin.shopkeeperData.get(strId + ".chest-location.z").toString()));
		Chest chest = (Chest) p.getWorld().getBlockAt(loc).getState();
		for (int i = 0; i < chest.getBlockInventory().getSize(); i++) {
			ItemStack iS = chest.getBlockInventory().getItem(i);
			if (iS != null) {
				addItem(inv, iS.getType(), iS.getAmount(), i,
						plugin.shopkeeperData.getInt(strId + ".inventory." + i + ".price"));
			}
		}
		ItemStack back = newCustomItemStack(Material.BARRIER, 1, "Go Back", null);
		inv.setItem(31, back);

		p.openInventory(inv);
	}

	public void ShopkeeperEditPriceOpen(Player p, ItemStack item, String currentPrice) {
		Inventory inv = Bukkit.createInventory(new ShopkeeperItemPriceHolder(), 9 * 2, "Price: " + currentPrice);
		ItemStack newItem = newCustomItemStack(item.getType(), item.getAmount(), item.getItemMeta().getDisplayName(),
				item.getItemMeta().getLore());
		ItemStack add = newCustomItemStack(Material.IRON_BLOCK, 1, "Add 1", null);
		ItemStack addTen = newCustomItemStack(Material.GOLD_BLOCK, 1, "Add 10", null);
		ItemStack addHundred = newCustomItemStack(Material.DIAMOND_BLOCK, 1, "Add 100", null);
		ItemStack subtract = newCustomItemStack(Material.STONE, 1, "Subtract 1", null);
		ItemStack subtractTen = newCustomItemStack(Material.OBSIDIAN, 1, "Subtract 10", null);
		ItemStack subtractHundred = newCustomItemStack(Material.BEDROCK, 1, "Subtract 100", null);
		ItemStack back = newCustomItemStack(Material.BARRIER, 1, "Go Back", null);
		inv.setItem(4, newItem);
		inv.setItem(10, addHundred);
		inv.setItem(11, addTen);
		inv.setItem(12, add);
		inv.setItem(13, back);
		inv.setItem(14, subtract);
		inv.setItem(15, subtractTen);
		inv.setItem(16, subtractHundred);
		p.openInventory(inv);
	}

	// ---------------------------------------------------------------------------

	@EventHandler
	public void onPlayerClick(PlayerInteractEvent e) {
		Player p = e.getPlayer();
		Action action = e.getAction();
		ItemStack item = e.getItem();

		if (e.getItem() != null && item.getType() == Material.VILLAGER_SPAWN_EGG) {
			e.setCancelled(true);
			if (p.hasMetadata("shopkeepChestLoc")) {
				if (action == Action.RIGHT_CLICK_AIR) {
					p.sendMessage(ChatColor.RED + "Click on a block with this egg to spawn your Shopkeeper.");
				} else if (action == Action.RIGHT_CLICK_BLOCK) {
					plugin.instantiateShopkeeper(p, null, ShopType.normal,
							(Block) p.getMetadata("shopkeepChestLoc").get(0).value());
					p.removeMetadata("shopkeepChestLoc", plugin);
					item.setAmount(0);
				}
			} else {
				if (action == Action.RIGHT_CLICK_AIR) {
					p.sendMessage(ChatColor.RED + "Click on a chest to assign it as your Shopkeepers' inventory.");
				} else if (action == Action.RIGHT_CLICK_BLOCK) {
					if (e.getClickedBlock().getType() == Material.CHEST) {
						p.setMetadata("shopkeepChestLoc", new FixedMetadataValue(plugin, e.getClickedBlock()));
						p.sendMessage(ChatColor.GREEN + "Chest assigned as Shopkeeper's inventory.");
					} else {
						p.sendMessage(ChatColor.RED + "Click on a chest to assign it as your Shopkeepers' inventory.");
					}
				}
			}
		}
	}

	@EventHandler
	public void onEntityDeath(EntityDeathEvent e) {
		if (plugin.shopkeeperData.contains(e.getEntity().getUniqueId().toString())) 
		{
			ShopkeeperDeathEvent deathEvent = new ShopkeeperDeathEvent(plugin, e.getEntity());
			Bukkit.getServer().getPluginManager().callEvent(deathEvent);
		}
	}
	
	@EventHandler
	public void onShopkeeperDeath(ShopkeeperDeathEvent e)
	{
		Entity entity = e.getEntity();
		if (plugin.debug)
		{
			plugin.LoggerInfo("Shopkeeper {" + entity.getUniqueId() + "} was removed");
		}
		plugin.logToFile(LogType.ShopkeeperRemove, "Shopkeeper {" + entity.getUniqueId() + "} was removed");
		plugin.playerData.set(e.getOwnerUUID().toString() + ".shopkeepers", plugin.playerData.getInt(e.getOwnerUUID().toString() + ".shopkeepers") - 1);
		plugin.saveCustomYml(plugin.playerData, plugin.playerFile);
		
		plugin.shopkeeperData.set(entity.getUniqueId().toString(), null);
		plugin.saveCustomYml(plugin.shopkeeperData, plugin.shopkeepersFile);
	}
}
