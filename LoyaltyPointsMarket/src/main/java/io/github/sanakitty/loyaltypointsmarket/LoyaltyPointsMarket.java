package io.github.sanakitty.loyaltypointsmarket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

public final class LoyaltyPointsMarket extends JavaPlugin {
	
	public static Permission perms = null;
	public String access_token = null;
	public String channel_name = null;
	public Boolean debug = false;
	
	public enum ShopType{
		normal,
		creative
	}
	
	public enum LogType{
		ShopkeeperSpawn,
		ShopkeeperRemove,
		Points
	}
	
	private static HttpsURLConnection con;
	
	public File playerFile = new File(getDataFolder()+"/players.yml");
	public FileConfiguration playerData = YamlConfiguration.loadConfiguration(playerFile);
	public File shopkeepersFile = new File(getDataFolder()+"/shopkeepers.yml");
	public FileConfiguration shopkeeperData = YamlConfiguration.loadConfiguration(shopkeepersFile);
	public File protectedChestsFile = new File(getDataFolder()+"/protected-chests.yml");
	public FileConfiguration protectedChests = YamlConfiguration.loadConfiguration(protectedChestsFile);
	
	
	@Override
	public void onEnable() {
		saveDefaultConfig();
		if (!playerFile.exists())
			saveCustomYml(playerData, playerFile);
		if (!shopkeepersFile.exists())
			saveCustomYml(shopkeeperData, shopkeepersFile);
		if (!protectedChestsFile.exists())
			saveCustomYml(protectedChests, protectedChestsFile);
		
		if (getConfig().getBoolean("debug-mode"))
			debug = true;
		
		Bukkit.getPluginManager().registerEvents(new LPMEventManager(this), this);
		this.getCommand("lpm").setExecutor(new LPMCommandExecutor(this));
		
		access_token = getConfig().get("access-token").toString();
		channel_name = getConfig().get("channel-name").toString();
		
		if (debug)
			LoggerInfo("Debug Mode is active. Loyalty Points Market will print debug info to the console.");
	}
	
    public void saveCustomYml(FileConfiguration ymlConfig, File ymlFile) {
	    try {
		    ymlConfig.save(ymlFile);
		    } catch (IOException e) {
		    	e.printStackTrace();
	    }
    }
    
	private List<Player> adminmap = new ArrayList<Player>();
	public List<Player> getAdminMap(){return adminmap;}
	public Map<UUID, Location> chestMap = new HashMap<UUID, Location>();
	
	public void addToAdminList(Player p){
		adminmap.add(p);
	}
	
	public void removeFromAdminList(Player p) {
		adminmap.remove((Object) p);
	}
	
	private List<Player> renaming = new ArrayList<Player>();
	public List<Player> getRenameMap(){return renaming;}
	
	public void addToRenaming(Player p){
		renaming.add(p);
	}
	
	public void removeFromRenaming(Player p) {
		renaming.remove((Object) p);
	}
	
	private List<Player> rotating = new ArrayList<Player>();
	public List<Player> getRotateMap(){return rotating;}
	
	public void addToRotating(Player p){
		rotating.add(p);
	}
	
	public void removeFromRotating(Player p) {
		rotating.remove((Object) p);
	}
	
	private List<Player> professionChange = new ArrayList<Player>();
	public List<Player> getProfessionMap(){return professionChange;}
	
	public void addToProfessionChange(Player p){
		professionChange.add(p);
	}
	
	public void removeFromProfessionChange(Player p) {
		professionChange.remove((Object) p);
	}
	
	public void onDisable() {
		adminmap.clear();
		renaming.clear();
		rotating.clear();
		professionChange.clear();
	}
	
	/**
	 * Spawns a new shopkeeper where the player is looking, and generates data information in the "shopkeepers.yml" file.
	 * 
	 * @param p The Player object who is attempting to spawn the Shopkeeper.
	 * @param name The name to give the Shopkeeper.
	 * @param type The ShopType of the Shopkeeper. Can be Normal, or Creative.
	 * @param chest The Chest to which the Shopkeeper is linked.
	 */
	public void instantiateShopkeeper(Player p, ShopType type, Block chest, String profession) {
		HashSet<Material> hash = new HashSet<Material>();
		hash.add(Material.AIR);
		hash.add(Material.TALL_GRASS);
		Block block = p.getTargetBlock(hash, 6);
		Location target = new Location(p.getWorld(), block.getLocation().getX() + 0.5f, block.getLocation().getY() + 1, block.getLocation().getZ() + 0.5f);
		Villager v = (Villager) p.getWorld().spawnEntity(target, EntityType.VILLAGER);
		
		UUID villagerID = v.getUniqueId();
		String villagerName = p.getName() + "'s Shopkeeper";
		
		v.setCustomName(villagerName);
		switch (profession)
		{
			case "blacksmith":
				v.setProfession(Profession.BLACKSMITH);
				break;
			case "butcher":
				v.setProfession(Profession.BUTCHER);
				break;
			case "farmer":
				v.setProfession(Profession.FARMER);
				break;
			case "librarian":
				v.setProfession(Profession.LIBRARIAN);
				break;
			case "nitwit":
				v.setProfession(Profession.NITWIT);
				break;
			case "priest":
				v.setProfession(Profession.PRIEST);
				break;
			case "unset":
				v.setProfession(Profession.NITWIT);
				break;
		}
		v.setAI(false);
		
		//Creation of shopkeeper data
		shopkeeperData.set(villagerID + ".name", v.getCustomName());
		shopkeeperData.set(villagerID + ".owner", p.getUniqueId().toString());
		shopkeeperData.set(villagerID + ".type", type.toString());
		shopkeeperData.set(villagerID + ".invulnerable", true);
		shopkeeperData.set(villagerID + ".muted", false);
		shopkeeperData.set(villagerID + ".location.world", v.getWorld().getName());
		shopkeeperData.set(villagerID + ".location.x", v.getLocation().getX());
		shopkeeperData.set(villagerID + ".location.y", v.getLocation().getY());
		shopkeeperData.set(villagerID + ".location.z", v.getLocation().getZ());
		shopkeeperData.set(villagerID + ".chest-location.world", chest.getWorld().getName());
		shopkeeperData.set(villagerID + ".chest-location.x", chest.getLocation().getX());
		shopkeeperData.set(villagerID + ".chest-location.y", chest.getLocation().getY());
		shopkeeperData.set(villagerID + ".chest-location.z", chest.getLocation().getZ());
		shopkeeperData.set(villagerID + ".inventory", null);
		saveCustomYml(shopkeeperData, shopkeepersFile);
		
		logToFile(LogType.ShopkeeperSpawn, "Shopkeeper {" + villagerID + "} spawned at " + v.getLocation());
		
		if (type != ShopType.creative)
		{
			playerData.set(p.getUniqueId().toString() + ".shopkeepers", playerData.getInt(p.getUniqueId().toString() + ".shopkeepers") + 1);
			saveCustomYml(playerData, playerFile);
		}
		
		protectedChests.set(v.getUniqueId().toString(), chest.getLocation());
	}
	
	public void RenameShopkeeper(Player p, LivingEntity shopkeeper, String name)
	{
		shopkeeperData.set(shopkeeper.getUniqueId() + ".name", name);
		saveCustomYml(shopkeeperData, shopkeepersFile);
		shopkeeper.setCustomName(name);
		p.sendMessage(ChatColor.GREEN + "Your shopkeeper has been renamed to " + name + "!");
		removeFromRenaming(p);
	}
	
	public void RotateShopkeeper(Player p, LivingEntity shopkeeper, String rotation)
	{
		try 
		{
			Location old = shopkeeper.getLocation();
			Location loc = new Location(old.getWorld(), old.getX(), old.getY(), old.getZ(), Float.parseFloat(rotation), old.getPitch());
			shopkeeper.teleport(loc);
		}
		catch (NumberFormatException exc)
		{
			p.sendMessage(ChatColor.RED + "The message " + rotation + " is not a valid rotation. Please type in a value from 0 to 360.");
			return;
		}
		p.sendMessage(ChatColor.GREEN + "Set this shopkeeper's rotation to " + rotation + " degrees.");
		removeFromRotating(p);
	}
	
	public void ShopkeeperProfessionChange(Player p, LivingEntity shopkeeper, String profession)
	{
		Villager v = (Villager) shopkeeper;
		switch (profession.toLowerCase())
		{
			case "blacksmith":
				v.setProfession(Profession.BLACKSMITH);
				removeFromProfessionChange(p);
				p.sendMessage(ChatColor.GREEN + "Change this shopkeeper's profession to " + profession);
				break;
			case "butcher":
				v.setProfession(Profession.BUTCHER);
				removeFromProfessionChange(p);
				p.sendMessage(ChatColor.GREEN + "Change this shopkeeper's profession to " + profession);
				break;
			case "farmer":
				v.setProfession(Profession.FARMER);
				removeFromProfessionChange(p);
				p.sendMessage(ChatColor.GREEN + "Change this shopkeeper's profession to " + profession);
				break;
			case "librarian":
				v.setProfession(Profession.LIBRARIAN);
				removeFromProfessionChange(p);
				p.sendMessage(ChatColor.GREEN + "Change this shopkeeper's profession to " + profession);
				break;
			case "nitwit":
				v.setProfession(Profession.NITWIT);
				removeFromProfessionChange(p);
				p.sendMessage(ChatColor.GREEN + "Change this shopkeeper's profession to " + profession);
				break;
			case "priest":
				v.setProfession(Profession.PRIEST);
				removeFromProfessionChange(p);
				p.sendMessage(ChatColor.GREEN + "Change this shopkeeper's profession to " + profession);
				break;
			default:
				p.sendMessage(ChatColor.RED + "The profession " + profession + " is unrecognized.");
				p.sendMessage(ChatColor.GOLD + "Can be one of the following:");
				p.sendMessage(ChatColor.GOLD + "Blacksmith, Butcher, Farmer, Librarian, Nitwit, Priest");
				break;
		}
	}
	
	/**
	 * Saves info to "transactionLog.txt" for future reference. Creates the file if it does not already exist, otherwise
	 * adds to it.
	 * 
	 * @param type The LogType the of info to log. Can be of type ShopkeeperSpawn, ShopkeeperRemove, or Points.
	 * @param message The message to log in the file.
	 */
	public void logToFile(LogType type, String message){
	    File dataFolder = getDataFolder();
        if(!dataFolder.exists())
        {
            dataFolder.mkdir();
        }
        
        File transactionLog = new File(getDataFolder(), "transactionLog.txt");
        if(!transactionLog.exists())
        {
            try {
            	transactionLog.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        FileWriter fw = null;
		try {
			fw = new FileWriter(transactionLog, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
        PrintWriter pw = new PrintWriter(fw);
        Date now = new Date();
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        
        pw.println("[" + format.format(now) + "] [" + type.toString() + "] " + message);
        pw.flush();
        pw.close();
	}
	
	/**
	 * Outputs a log message to the Bukkit logger.
	 * 
	 * @param message The message to print to the console.
	 */
	public void LoggerInfo(String message)
	{
		Bukkit.getLogger().info("[LoyaltyPointsMarket] " + message);
	}
	
	public String formatStringWithSpaces(String name, String splitter)
	{
		final String[] split = StringUtils.splitByWholeSeparator(name, splitter);
        final StringBuilder buff = new StringBuilder();
        for (final String str : split) {
            buff.append(StringUtils.capitalize(str.toLowerCase()) + " ");
        }
        return buff.toString().substring(0, buff.toString().length() - 1);
	}
	
	//-----------------------------------------------HTTPS Requests-----------------------------------------------//
	
	/**
	 * Sends a 'POST' Http request to the Streamlabs API using the provided authentication code, and if successful, returns
	 * an Access Token and Refresh Token, which are saved to the plugins' configuration file.
	 *  
	 * @param code The Authentication Code to provide to the request.
	 */
	public void TokenRequest(String code){
		String request = "https://streamlabs.com/api/v1.0/token";
 
        StringBuilder content = null;
            try {
    			URL    url            = new URL( request );
    			con = (HttpsURLConnection) url.openConnection();           
    			con.setDoOutput( true );
    			con.setInstanceFollowRedirects( false );
    			con.setRequestMethod( "POST" );
    			con.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded"); 
    			con.setRequestProperty( "charset", "utf-8");
    			con.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
    			con.setUseCaches( false );
    			
    			String urlParameters = "grant_type=authorization_code" + 
    			"&client_id=" + getConfig().get("client-id") + 
    			"&client_secret=" + getConfig().get("client-secret") +
    			"&redirect_uri=" + getConfig().get("redirect-uri") +
    			"&code=" + code;
    			
    			OutputStream os = con.getOutputStream();
    			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
    			writer.write(urlParameters);
    			writer.flush();
    			writer.close();
    			con.connect();

    			int responseCode = con.getResponseCode();
    			if (debug)
    			{
    				LoggerInfo("Sending 'POST' request to: " + url);
        			LoggerInfo("Parameters: " + urlParameters);
        			LoggerInfo("Response code: " + responseCode);
    			}
    			
    		} catch (MalformedURLException e) {
    			e.printStackTrace();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    		try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                 String line;
                 content = new StringBuilder();

                 while ((line = in.readLine()) != null) {
                     content.append(line);
                     content.append(System.lineSeparator());
                     LoggerInfo(line);
                 }
             } catch (IOException e) {
            	 e.printStackTrace();
             }
        String responseStr = content.toString();
        
        StringBuilder response = new StringBuilder();
        HashMap<String, String>map = readResponse(responseStr);
        
        response.append(map.get("\"access_token\""));
        response.append(map.get("\"refresh_token\""));
        LoggerInfo("Received tokens: " + response.toString());
	}
	
	/**
	 * Sends a 'GET' Http request to the Streamlabs API with the provided Access Token, Twitch username, and Twitch
	 * channel name. Returns the requests' response if 'returnResponse' is true.
	 * 
	 * @param accessToken The Access Token previously acquired from {@link TokenRequest}. Pulled from the plugins'
	 * configuration file.
	 * @param playerId The UUID of the relevant Player, used to grab the Player's Twitch username from
	 * the 'players.yml' file.
	 * @param channelName The Twitch channel name to which the plugin is intended to connect. Pulled from the plugins'
	 * configuration file.
	 * @param returnResponse Causes the method to return a numerical String with the number of points that the 
	 * requested user has.
	 * @return
	 */
	public String PointRequest(String accessToken, UUID playerId, String channel, boolean returnResponse){
		String url = "https://streamlabs.com/api/v1.0/points?access_token=" + accessToken + 
				"&username=" + playerData.get(playerId + ".twitch") + 
				"&channel=" + channel;
		
		StringBuffer response = null;
		int responseCode = 0;
		
		try {
			URL myUrl = new URL(url);
			con = (HttpsURLConnection) myUrl.openConnection();
			
			con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:66.0) Gecko/20100101 Firefox/66.0");
			con.setRequestProperty( "charset", "utf-8");
			
			con.setRequestMethod("GET");
			responseCode = con.getResponseCode();
			
			if (debug) 
			{
				LoggerInfo("Sending 'GET' request to: " + url);
    			LoggerInfo("Response code: " + con.getResponseCode());
			}
			
			if (responseCode == 404)
				return new String("User not found");

			BufferedReader in = new BufferedReader(
			        new InputStreamReader(con.getInputStream()));
			String inputLine;
			response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) 
			{
				response.append(inputLine);
				if (debug) 
					LoggerInfo(inputLine);
			}
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		
		} finally {
			con.disconnect();
		}
		
		String responseStr = response.toString();
		if (returnResponse)
			return (String)readResponse(responseStr).get("\"points\"");
		else
			return null;
	}
	
	/**
	 * Sends a 'POST' Http request to the Streamlabs API with the provided Access Token, Twitch username, Twitch
	 * channel name, and points to subtract.
	 * 
	 * @param accessToken The Access Token previously acquired from {@link TokenRequest}. Pulled from the plugins'
	 * configuration file.
	 * @param playerId The UUID of the relevant Player as a String, used to grab the Player's Twitch username from
	 * the 'players.yml' file.
	 * @param channel The Twitch channel name to which the plugin is intended to connect. Pulled from the plugins'
	 * configuration file.
	 * @param points The number of points to subtract from the relevant user.
	 * @return
	 */
	public void PointSubtract(String accessToken, UUID playerId, String channel, Integer points){
		String request = "https://streamlabs.com/api/v1.0/points/subtract";
		 
        StringBuilder content = null;
            try {
    			URL    url            = new URL( request );
    			con = (HttpsURLConnection) url.openConnection();           
    			con.setDoOutput( true );
    			con.setInstanceFollowRedirects( false );
    			con.setRequestMethod( "POST" );
    			con.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded"); 
    			con.setRequestProperty( "charset", "utf-8");
    			con.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
    			con.setUseCaches( false );
    			
    			String urlParameters = "access_token=" + getConfig().get("access-token") + 
    			"&username=" + playerData.get(playerId + ".twitch") + 
    			"&channel=" + getConfig().get("channel-name") +
    			"&points=" + points;
    			
    			OutputStream os = con.getOutputStream();
    			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
    			writer.write(urlParameters);
    			writer.flush();
    			writer.close();
    			con.connect();
    			
    			if (debug) {
    				LoggerInfo("Sending 'POST' request to: " + url);
        			LoggerInfo("Parameters: " + urlParameters);
        			LoggerInfo("Response code: " + con.getResponseCode());
    			}
    			
    		} catch (MalformedURLException e) {
    			e.printStackTrace();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    		try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                 String inputLine;
                 content = new StringBuilder();

                 while ((inputLine = in.readLine()) != null) {
                     content.append(inputLine);
                     content.append(System.lineSeparator());
                     
                     if (debug)
             			LoggerInfo(inputLine);
                 }
             } catch (IOException e) {
            	 e.printStackTrace();
             }
	}
	
	/**
	 * Sends a 'POST' Http request to the Streamlabs API with the provided Access Token, Twitch username, Twitch
	 * channel name, and points to add.
	 * 
	 * @param accessToken The Access Token previously acquired from {@link TokenRequest}. Pulled from the plugins'
	 * configuration file.
	 * @param playerId The UUID of the relevant Player as a String, used to grab the Player's Twitch username from
	 * the 'players.yml' file.
	 * @param channel The Twitch channel name to which the plugin is intended to connect. Pulled from the plugins'
	 * configuration file.
	 * @param points The number of points to add to the relevant user.
	 * @return
	 */
	public void PointAdd(String accessToken, UUID playerId, String channel, Integer points){
		String request = "https://streamlabs.com/api/v1.0/points/import";
		 
        StringBuilder content = null;
            try {
    			URL    url            = new URL( request );
    			con = (HttpsURLConnection) url.openConnection();           
    			con.setDoOutput( true );
    			con.setInstanceFollowRedirects( false );
    			con.setRequestMethod( "POST" );
    			con.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded"); 
    			con.setRequestProperty( "charset", "utf-8");
    			con.addRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
    			con.setUseCaches( false );
    			
    			String urlParameters = "access_token=" + getConfig().get("access-token") + 
    			"&channel=" + getConfig().get("channel-name") +
    			"&users[" +  playerData.get(playerId + ".twitch" ) + "]=" + points;
    			
    			OutputStream os = con.getOutputStream();
    			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
    			writer.write(urlParameters);
    			writer.flush();
    			writer.close();
    			con.connect();

    			int responseCode = con.getResponseCode();
    			LoggerInfo("Sending 'POST' request to: " + url);
    			LoggerInfo("Paremeters: " + urlParameters);
    			LoggerInfo("Response code: " + responseCode);
    			
    		} catch (MalformedURLException e) {
    			e.printStackTrace();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
    		try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                 String line;
                 content = new StringBuilder();

                 while ((line = in.readLine()) != null) {
                     content.append(line);
                     content.append(System.lineSeparator());
                     
                     if (debug)
                    	 LoggerInfo(line);
                 }
             } catch (IOException e) {
            	 e.printStackTrace();
             }
	}
	
	public HashMap<String, String> readResponse (String stringArray) {
		String[] params = stringArray.split(",");  
		HashMap<String, String> map = new HashMap<String, String>(); 
		
		for (String param : params)  
	    {  
	        String name = param.split(":")[0];
	        name = name.replace("{", "");
	        String value = param.split(":")[1];  
	        //System.out.print(name + ":" + value);
	        map.put(name, value);
	    } 
		return map;
	}
}
