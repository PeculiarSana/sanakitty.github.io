package io.github.sanakitty.loyaltypointsmarket;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
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
import io.github.sanakitty.loyaltypointsmarket.events.ShopkeeperAdminDamageEvent;
import io.github.sanakitty.loyaltypointsmarket.events.ShopkeeperAdminInteractEvent;
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
				.contains("shopkeepers." + ((Entity) e.getInventory().getHolder()).getUniqueId().toString())) {
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
		
		if (e.getRightClicked().getType() == EntityType.VILLAGER && e.getHand().equals(EquipmentSlot.HAND) && plugin.shopkeeperData.contains("shopkeepers." + e.getRightClicked().getUniqueId().toString())) 
		{
			LivingEntity ent = (LivingEntity) e.getRightClicked();
			UUID ownerUUID = UUID.fromString(plugin.shopkeeperData.getString("shopkeepers." + ent.getUniqueId() + ".owner"));
			p.setMetadata("shopkeeperID", new FixedMetadataValue(plugin, ent.getUniqueId()));
			p.setMetadata("shopkeeperName", new FixedMetadataValue(plugin, ent.getCustomName()));

			// Code to run if the player is currently debugging
			if (p.isSneaking() && plugin.getAdminMap().contains(p)) 
			{
				ShopkeeperAdminInteractEvent adminInteract = new ShopkeeperAdminInteractEvent(plugin, ent, p);
				Bukkit.getServer().getPluginManager().callEvent(adminInteract);
			} 
			// Code to run to edit the shopkeeper
			else if (p.isSneaking() && p.getUniqueId().compareTo(UUID.fromString(plugin.shopkeeperData.get("shopkeepers." + ent.getUniqueId() + ".owner").toString())) == 0) 
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
			// Code to run to edit creative shopkeeper
			else if (p.isSneaking() && p.hasPermission("lpm.shopkeeper.creative")) 
			{
				// Adds a one tick delay to prevent issues
				Bukkit.getServer().getScheduler().runTaskLater(plugin, new Runnable()
				{
					public void run() 
					{
						ShopkeeperEditOpen(p, "Editing Creative Shopkeeper");
					}
				}, 1);
			}
			
			// Code to run if the player is just opening the shop to buy
			// Checks that the Player isn't sneaking, and isn't the owner OR is on the adminMap before allowing them to view the shop
			else if (!p.isSneaking() && (plugin.getAdminMap().contains(p) || p.getUniqueId().compareTo(ownerUUID) != 0 || plugin.shopkeeperData.getString("shopkeepers." + ent.getUniqueId() + ".type").equalsIgnoreCase("creative")))
			{
				Inventory inv = Bukkit.createInventory(new ShopkeeperShopHolder(), 27, ent.getCustomName());
				// Adds a one tick delay to prevent issues
				Bukkit.getServer().getScheduler().runTaskLater(plugin, new Runnable() 
				{
					public void run()
					{
						p.openInventory(inv);
					}
				}, 1);

				Location loc = new Location(p.getWorld(),
						Float.parseFloat(plugin.shopkeeperData.get("shopkeepers." + ent.getUniqueId() + ".chest-location.x").toString()),
						Float.parseFloat(plugin.shopkeeperData.get("shopkeepers." + ent.getUniqueId() + ".chest-location.y").toString()),
						Float.parseFloat(plugin.shopkeeperData.get("shopkeepers." + ent.getUniqueId() + ".chest-location.z").toString()));
				
				// Instantiates the shopkeeper's inventory based on his linked chest
				if(ent.getWorld().getBlockAt(loc).getType() == Material.CHEST)
				{
					Chest chest = (Chest) ent.getWorld().getBlockAt(loc).getState();
					for (int i = 0; i < chest.getBlockInventory().getSize(); i++) 
					{
						ItemStack iS = chest.getBlockInventory().getItem(i);
						String ymlLoc = "shopkeepers." + ent.getUniqueId() + ".inventory." + i;
						if (iS != null) 
						{
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

	
	  @EventHandler public void inventoryMoveEvent(InventoryMoveItemEvent e) { if
	  (e.getDestination() instanceof ShopkeeperShopHolder || e.getDestination()
	  instanceof ShopkeeperEditHolder || e.getDestination() instanceof
	  ShopkeeperPricesHolder || e.getDestination() instanceof
	  ShopkeeperItemPriceHolder || e.getDestination() instanceof
	  ShopkeeperShopHolder) { e.setCancelled(true); } }
	 

	@EventHandler
	public void onInventoryClick(InventoryClickEvent e) {
		if (e.getClickedInventory() == null) 
		{
			return;
		}

		Player p = (Player) e.getWhoClicked();
		
		if (e.getClickedInventory().getHolder() instanceof ShopkeeperShopHolder ||
			 e.getClickedInventory().getHolder() instanceof ShopkeeperEditHolder ||
			 e.getClickedInventory().getHolder() instanceof ShopkeeperPricesHolder ||
			 e.getClickedInventory().getHolder() instanceof ShopkeeperItemPriceHolder ||
			 e.getClickedInventory().getHolder() instanceof ShopkeeperShopHolder) 
		 {
			 e.setCancelled(true);
		 } else
			 return;
		
		Entity ent = null;
		for (Entity searchedEntity : p.getWorld().getEntities())
			if (searchedEntity.getUniqueId().compareTo(UUID.fromString(p.getMetadata("shopkeeperID").get(0).asString())) == 0)
				ent = searchedEntity;
		
		// ~~~~ShopkeeperShopHolder~~~~
		if (e.getClickedInventory().getHolder() instanceof ShopkeeperShopHolder) {
			if (plugin.playerData.isSet(p.getUniqueId().toString()))
			{
				if (e.getCurrentItem().getType() != Material.AIR) {
					int price = plugin.shopkeeperData
							.getInt("shopkeepers." + ent.getUniqueId().toString() + ".inventory." + e.getSlot() + ".price");
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
						String itemName = e.getCurrentItem().getItemMeta().hasDisplayName() ? e.getCurrentItem().getItemMeta().getDisplayName() : 										plugin.formatStringWithSpaces(e.getCurrentItem().getType().getKey().getKey(), "_");
						p.sendMessage(ChatColor.GREEN + "You've purchased "+ e.getCurrentItem().getAmount() + "x " + itemName + " for " + 
								price + " " + plugin.getConfig().get("currency-name") + "!");
						plugin.logToFile(LogType.Points, p.getName() + " {" + p.getUniqueId() + "} bought " + e.getCurrentItem().getAmount() +  " " + itemName + 
								" for " + price + " "
								+ plugin.getConfig().get("currency-name"));

						// Point addition for owner
						if (!plugin.shopkeeperData.getString("shopkeepers." + ent.getUniqueId() + ".type").equalsIgnoreCase("creative"))
						{
							UUID id = UUID.fromString(plugin.shopkeeperData
									.get("shopkeepers." + ent.getUniqueId().toString() + ".owner").toString());
							plugin.shopkeeperData.set("shopkeepers." + ent.getUniqueId().toString() + ".storedPoints", 
									plugin.shopkeeperData.getInt("shopkeepers." + ent.getUniqueId().toString() + ".storedPoints") + price);
							plugin.saveCustomYml(plugin.shopkeeperData, plugin.shopkeepersFile);
						}
					}
					else
					{
						p.sendMessage(ChatColor.RED + "You do not have enough " + plugin.getConfig().get("currency-name") + " to purchase this!");
					}
				}
			}
			else
				p.sendMessage(ChatColor.RED + plugin.getConfig().getString("unregistered-player-message"));
		}
		// ~~~~ShopkeeperEditHolder~~~~
		if (e.getClickedInventory().getHolder() instanceof ShopkeeperEditHolder) {
			// ROTATE the shopkeeper
			if (e.getCurrentItem().getType() == Material.COMPASS) {
				p.sendMessage(ChatColor.GOLD + "Please type in a value in degrees to rotate this shopkeeper.");
				p.sendMessage(ChatColor.GOLD + "Sets the shopkeeper's rotation to the value provided.");
				plugin.addToRotating(p);
				p.setMetadata("shopkeeper", new FixedMetadataValue(plugin, ent));
				p.closeInventory();
			} // EDIT prices
			else if (e.getCurrentItem().getType() == Material.GOLD_NUGGET) {
				ShopkeeperPricesOpen(p);
				// RENAME the shopkeeper
			} else if (e.getCurrentItem().getType() == Material.NAME_TAG) {
				p.closeInventory();
				p.sendMessage(ChatColor.GOLD + "Please type in a new name for your shopkeeper.");
				plugin.addToRenaming(p);
				p.setMetadata("shopkeeper", new FixedMetadataValue(plugin, ent));
				p.closeInventory();
				// DELETE the shopkeeper
			} else if (e.getCurrentItem().getType() == Material.FIRE_CHARGE) {
				ent.setInvulnerable(false);
				LivingEntity le = (LivingEntity) ent;
				plugin.shopkeeperData.set("shopkeepers." + ent.getUniqueId() + ".invulnerable", false);
				le.damage(9000);
				p.closeInventory();
			} // CHANGE profession
			else if (e.getCurrentItem().getType() == Material.EMERALD) {
				p.sendMessage(ChatColor.GOLD + "Please type in a profession name to change this shopkeeper's look.");
				p.sendMessage(ChatColor.GOLD + "Can be one of the following:");
				p.sendMessage(ChatColor.GOLD + "Blacksmith, Butcher, Farmer, Librarian, Nitwit, Priest");
				plugin.addToProfessionChange(p);
				p.setMetadata("shopkeeper", new FixedMetadataValue(plugin, ent));
				p.closeInventory();
			} /*else if (e.getCurrentItem().getType() == Material.MUSIC_DISC_BLOCKS) {
				if (plugin.shopkeeperData.getBoolean(ent.getUniqueId() + ".muted"))
					plugin.shopkeeperData.set(ent.getUniqueId() + ".muted", false);
				else
					plugin.shopkeeperData.set(ent.getUniqueId() + ".muted", true);
				
			}*/
		}
		// ~~~~ShopkeeperPricesHolder~~~~
		if (e.getClickedInventory().getHolder() instanceof ShopkeeperPricesHolder) {
			if (e.getCurrentItem().getType() == Material.BARRIER) {
				ShopkeeperEditOpen(p, "Editing Shopkeeper");
			} else {
				ItemStack item = e.getCurrentItem();
				ShopkeeperEditPriceOpen(p, item, plugin.shopkeeperData.getString("shopkeepers." + ent.getUniqueId().toString() + ".inventory." + p.getMetadata("slotID") + ".price"));
				p.setMetadata("slotID", new FixedMetadataValue(plugin, e.getSlot()));
				editedItem = item;
			}
		}
		// ~~~~ShopkeeperItemPriceHolder~~~~
		if (e.getClickedInventory().getHolder() instanceof ShopkeeperItemPriceHolder) {
			String slotNum = p.getMetadata("slotID").get(0).asString();
			String ymlPrice = "shopkeepers." + ent.getUniqueId().toString() + ".inventory." + slotNum + ".price";
			if (e.getCurrentItem().getType() == Material.IRON_BLOCK) {
				plugin.shopkeeperData.set(ymlPrice, plugin.shopkeeperData.getInt(ymlPrice) + 1);
				ShopkeeperEditPriceOpen(p, editedItem, plugin.shopkeeperData
						.getString(ent.getUniqueId().toString() + ".inventory." + slotNum + ".price"));
			} else if (e.getCurrentItem().getType() == Material.GOLD_BLOCK) {
				plugin.shopkeeperData.set(ymlPrice, plugin.shopkeeperData.getInt(ymlPrice) + 10);
				ShopkeeperEditPriceOpen(p, editedItem, plugin.shopkeeperData
						.getString(ent.getUniqueId().toString() + ".inventory." + slotNum + ".price"));
			} else if (e.getCurrentItem().getType() == Material.DIAMOND_BLOCK) {
				plugin.shopkeeperData.set(ymlPrice, plugin.shopkeeperData.getInt(ymlPrice) + 100);
				ShopkeeperEditPriceOpen(p, editedItem, plugin.shopkeeperData
						.getString(ent.getUniqueId().toString() + ".inventory." + slotNum + ".price"));
			} else if (e.getCurrentItem().getType() == Material.STONE) {
				plugin.shopkeeperData.set(ymlPrice, plugin.shopkeeperData.getInt(ymlPrice) - 1);
				ShopkeeperEditPriceOpen(p, editedItem, plugin.shopkeeperData
						.getString(ent.getUniqueId().toString() + ".inventory." + slotNum + ".price"));
			} else if (e.getCurrentItem().getType() == Material.OBSIDIAN) {
				plugin.shopkeeperData.set(ymlPrice, plugin.shopkeeperData.getInt(ymlPrice) - 10);
				ShopkeeperEditPriceOpen(p, editedItem, plugin.shopkeeperData
						.getString(ent.getUniqueId().toString() + ".inventory." + slotNum + ".price"));
			} else if (e.getCurrentItem().getType() == Material.BEDROCK) {
				plugin.shopkeeperData.set(ymlPrice, plugin.shopkeeperData.getInt(ymlPrice) - 100);
				ShopkeeperEditPriceOpen(p, editedItem, plugin.shopkeeperData
						.getString(ent.getUniqueId().toString() + ".inventory." + slotNum + ".price"));
			} else if (e.getCurrentItem().getType() == Material.BARRIER) {
				ShopkeeperPricesOpen(p);
			}
			plugin.saveCustomYml(plugin.shopkeeperData, plugin.shopkeepersFile);
		}
	}

	// -------------------------CUSTOM INVENTORY METHODS-------------------------
	public void ShopkeeperEditOpen(Player p, String name) {
		Inventory inv = Bukkit.createInventory(new ShopkeeperEditHolder(), 9, name);

		ItemStack rotate = newCustomItemStack(Material.COMPASS, 1, "Rotate Shopkeeper",
				Arrays.asList("Allows you to rotate this shopkeeper."));
		ItemStack delete = newCustomItemStack(Material.FIRE_CHARGE, 1, "Remove Shopkeeper",
				Arrays.asList("Removes this shopkeeper."));
		ItemStack priceAdjust = newCustomItemStack(Material.GOLD_NUGGET, 1, "Adjust Prices",
				Arrays.asList("Allows you to adjust sale prices."));
		ItemStack rename = newCustomItemStack(Material.NAME_TAG, 1, "Rename Shopkeeper", 
				Arrays.asList("Allows you to rename this shopkeeper."));
		ItemStack profession = newCustomItemStack(Material.EMERALD, 1, "Change Profession", 
				Arrays.asList("Allows you to change this shopkeeper's profession."));
		/*ItemStack mute = newCustomItemStack(Material.MUSIC_DISC_BLOCKS, 1, "Toggle Shopkeeper Sounds", 
				Arrays.asList("Allows you to toggle whether this shopkeeper makes sound or not."));*/

		inv.setItem(0, rotate);
		inv.setItem(3, priceAdjust);
		inv.setItem(4, rename);
		inv.setItem(5, delete);
		//inv.setItem(7, mute);
		inv.setItem(8, profession);

		p.openInventory(inv);
	}

	public void ShopkeeperPricesOpen(Player p) {
		String strId = p.getMetadata("shopkeeperID").get(0).asString();

		Inventory inv = Bukkit.createInventory(new ShopkeeperPricesHolder(), 4 * 9, "Editing shop prices");

		Location loc = new Location(p.getWorld(),
				Float.parseFloat(plugin.shopkeeperData.get("shopkeepers." + strId + ".chest-location.x").toString()),
				Float.parseFloat(plugin.shopkeeperData.get("shopkeepers." + strId + ".chest-location.y").toString()),
				Float.parseFloat(plugin.shopkeeperData.get("shopkeepers." + strId + ".chest-location.z").toString()));
		Chest chest = (Chest) p.getWorld().getBlockAt(loc).getState();
		for (int i = 0; i < chest.getBlockInventory().getSize(); i++) {
			ItemStack iS = chest.getBlockInventory().getItem(i);
			if (iS != null) {
				addItem(inv, iS.getType(), iS.getAmount(), i,
						plugin.shopkeeperData.getInt("shopkeepers." + strId + ".inventory." + i + ".price"));
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

		if (e.getItem() != null && item.getType() == Material.getMaterial(plugin.getConfig().getString("shop-creation-item"))) 
		{
			e.setCancelled(true);
			if (p.hasMetadata("shopkeepChestLoc")) 
			{
				if (action == Action.RIGHT_CLICK_AIR) 
				{
					p.sendMessage(ChatColor.RED + "Click on a block with this egg to spawn your Shopkeeper.");
				} else if (action == Action.RIGHT_CLICK_BLOCK) 
				{
					plugin.instantiateShopkeeper(p, ShopType.normal,
							(Block) p.getMetadata("shopkeepChestLoc").get(0).value(), "Nitwit");
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
		else if (e.getItem() != null && item.getType() == Material.getMaterial(plugin.getConfig().getString("shop-creation-item-creative"))) 
		{
			e.setCancelled(true);
			if (p.hasMetadata("shopkeepChestLoc")) 
			{
				if (action == Action.RIGHT_CLICK_AIR) 
				{
					p.sendMessage(ChatColor.RED + "Click on a block with this egg to spawn your Shopkeeper.");
				} else if (action == Action.RIGHT_CLICK_BLOCK) 
				{
					plugin.instantiateShopkeeper(p, ShopType.creative, (Block) p.getMetadata("shopkeepChestLoc").get(0).value(), 
							p.getMetadata("shopkeeperProfession").get(0).value().toString());
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
		if (plugin.shopkeeperData.contains("shopkeepers." + e.getEntity().getUniqueId().toString()))
		{
			ShopkeeperDeathEvent deathEvent = new ShopkeeperDeathEvent(plugin, e.getEntity());
			Bukkit.getServer().getPluginManager().callEvent(deathEvent);
		}
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent e)
	{
		if (e.getEntity() instanceof LivingEntity && plugin.shopkeeperData.isSet("shopkeepers." + e.getEntity().getUniqueId().toString()) && 
				plugin.shopkeeperData.getBoolean("shopkeepers." + e.getEntity().getUniqueId() + ".invulnerable"))
		{
			e.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onEntityDamageEntity(EntityDamageByEntityEvent e)
	{
		if (e.getDamager() instanceof Player && e.getEntity() instanceof LivingEntity && plugin.shopkeeperData.isSet("shopkeepers." + e.getEntity().getUniqueId().toString()))
		{
			e.setCancelled(true);
			LivingEntity entity = (LivingEntity) e.getEntity();
			Player p = (Player) e.getDamager();
			if (plugin.getAdminMap().contains(p))
			{
				plugin.LoggerInfo("AdminMap contains player");
				ShopkeeperAdminDamageEvent event = new ShopkeeperAdminDamageEvent(plugin, entity, p);
				Bukkit.getServer().getPluginManager().callEvent(event);
			}
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
		if (!plugin.shopkeeperData.getString("shopkeepers." + entity.getUniqueId() + ".type").equalsIgnoreCase("creative"))
		{
			plugin.playerData.set(e.getOwnerUUID().toString() + ".shopkeepers", plugin.playerData.getInt(e.getOwnerUUID().toString() + ".shopkeepers") - 1);
			plugin.saveCustomYml(plugin.playerData, plugin.playerFile);
		}
		
		plugin.shopkeeperData.set("shopkeepers." + entity.getUniqueId().toString(), null);
		plugin.saveCustomYml(plugin.shopkeeperData, plugin.shopkeepersFile);
	}
	
	@EventHandler
	public void shopkeeperAdminDamage(ShopkeeperAdminDamageEvent e)
	{
		Player p = e.getPlayer();
		LivingEntity ent = e.getEntity();
		String ymlLoc = "shopkeepers." + ent.getUniqueId();
		p.sendMessage(ChatColor.GOLD + "Shopkeeper data:");
		p.sendMessage(ChatColor.GOLD + "  id: " + ChatColor.RESET + ent.getUniqueId());
		p.sendMessage(ChatColor.GOLD + "  name: " + ChatColor.RESET + plugin.shopkeeperData.get(ymlLoc + ".name"));
		p.sendMessage(ChatColor.GOLD + "  owner: " + ChatColor.RESET + plugin.shopkeeperData.get(ymlLoc + ".owner"));
		p.sendMessage(ChatColor.GOLD + "  type: " + ChatColor.RESET + plugin.shopkeeperData.get(ymlLoc + ".type"));
		p.sendMessage(ChatColor.GOLD + "  location: ");
		p.sendMessage(ChatColor.GOLD + "    world: " + ChatColor.RESET + plugin.shopkeeperData.get(ymlLoc + ".location.world"));
		p.sendMessage(ChatColor.GOLD + "    x: " + ChatColor.RESET + plugin.shopkeeperData.get(ymlLoc + ".location.x"));
		p.sendMessage(ChatColor.GOLD + "    y: " + ChatColor.RESET + plugin.shopkeeperData.get(ymlLoc + ".location.y"));
		p.sendMessage(ChatColor.GOLD + "    z: " + ChatColor.RESET + plugin.shopkeeperData.get(ymlLoc + ".location.z"));
		p.sendMessage(ChatColor.GOLD + "  chest-location: ");
		p.sendMessage(ChatColor.GOLD + "    world: " + ChatColor.RESET + plugin.shopkeeperData.get(ymlLoc + ".chest-location.world"));
		p.sendMessage(ChatColor.GOLD + "    x: " + ChatColor.RESET + plugin.shopkeeperData.get(ymlLoc + ".chest-location.x"));
		p.sendMessage(ChatColor.GOLD + "    y: " + ChatColor.RESET + plugin.shopkeeperData.get(ymlLoc + ".chest-location.y"));
		p.sendMessage(ChatColor.GOLD + "    z: " + ChatColor.RESET + plugin.shopkeeperData.get(ymlLoc + ".chest-location.z"));
	}
	
	@EventHandler
	public void shopkeeperAdminClick(ShopkeeperAdminInteractEvent e)
	{
		if (e.getPlayer().isSneaking())
		{
			// Normal shopkeeper edit inventory
			if (plugin.shopkeeperData.getString("shopkeepers." + e.getEntity().getUniqueId() + ".type").equalsIgnoreCase("normal"))
			{
				// Adds a one tick delay to prevent issues
				Bukkit.getServer().getScheduler().runTaskLater(plugin, new Runnable()
				{
					public void run() 
					{
						ShopkeeperEditOpen(e.getPlayer(), "Editing Shopkeeper");
					}
				}, 1);
			}
		}
	}
	
	@EventHandler
	public void chatMessageSent(AsyncPlayerChatEvent e)
	{
		if (plugin.getRenameMap().contains(e.getPlayer()))
		{
			e.setCancelled(true);
			plugin.RenameShopkeeper(e.getPlayer(), (LivingEntity) e.getPlayer().getMetadata("shopkeeper").get(0).value(), e.getMessage());
			e.getPlayer().removeMetadata("shopkeeper", plugin);
		}
		if (plugin.getRotateMap().contains(e.getPlayer())) 
		{
			e.setCancelled(true);
			plugin.RotateShopkeeper(e.getPlayer(), (LivingEntity) e.getPlayer().getMetadata("shopkeeper").get(0).value(), e.getMessage());
		}
		if (plugin.getProfessionMap().contains(e.getPlayer())) 
		{
			e.setCancelled(true);
			plugin.ShopkeeperProfessionChange(e.getPlayer(), (LivingEntity) e.getPlayer().getMetadata("shopkeeper").get(0).value(), e.getMessage());
		}
	}
}
