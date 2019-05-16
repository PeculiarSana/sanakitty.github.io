package io.github.sanakitty.loyaltypointsmarket.Inventories;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ShopkeeperPricesHolder implements InventoryHolder{

	private Inventory inv;
	String name;
	
	@Override
	public Inventory getInventory() {
		return inv;
	}

}
