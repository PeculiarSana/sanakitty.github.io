package io.github.sanakitty.loyaltypointsmarket.events;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.sanakitty.loyaltypointsmarket.LoyaltyPointsMarket;

public class ShopkeeperDeathEvent extends Event
{
	private static final HandlerList handlers = new HandlerList();
	private LivingEntity entity;
	
	LoyaltyPointsMarket plugin;
	Boolean debug;
	
	public ShopkeeperDeathEvent(LoyaltyPointsMarket plugin, LivingEntity entity)
	{
		this.plugin = plugin;
		debug = plugin.debug;
		
		if (debug)
			Bukkit.getLogger().info("Shopkeeper Death");
		this.entity = entity;
	}

	public HandlerList getHandlers() {
	    return handlers;
	}
	
	public static HandlerList getHandlerList() {
	    return handlers;
	}
	
	/**
     * Gets the Entity involved in this event.
     *
     * @return Entity involved in this event
     */
	public LivingEntity getEntity()
	{
		return entity;
	}
	
	/**
	 * Gets the UUID of the Player who owns this Shopkeeper.
	 * 
	 * @return UUID of owning Player
	 */
	public UUID getOwnerUUID()
	{
		Bukkit.getLogger().info(plugin.getName());
		return UUID.fromString(plugin.shopkeeperData.getString(entity.getUniqueId() + ".owner"));
	}
	
	/**
	 * Gets the Location where the Entity involved in this event died.
	 * 
	 * @return Location of this event
	 */
	public Location getDeathLocation()
	{
		Bukkit.getLogger().info(plugin.getName());
		return (Location) plugin.shopkeeperData.get(entity.getUniqueId() + ".location");
	}
}
