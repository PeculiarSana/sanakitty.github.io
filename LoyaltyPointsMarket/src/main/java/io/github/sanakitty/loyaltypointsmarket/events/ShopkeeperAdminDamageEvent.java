package io.github.sanakitty.loyaltypointsmarket.events;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import io.github.sanakitty.loyaltypointsmarket.LoyaltyPointsMarket;

public class ShopkeeperAdminDamageEvent extends Event
{

	private static final HandlerList handlers = new HandlerList();
	private LivingEntity entity;
	private Player player;
	
	LoyaltyPointsMarket plugin;
	Boolean debug;
	
	public ShopkeeperAdminDamageEvent(LoyaltyPointsMarket plugin, LivingEntity entity, Player player)
	{
		this.plugin = plugin;
		debug = plugin.debug;
	
		this.entity = entity;
		this.player = player;
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
	
	/** Gets the Player who damaged the Shopkeeper while in admin mode.
	 * 
	 * @return Player
	 */
	public Player getPlayer()
	{
		return player;
	}
	
	
}
