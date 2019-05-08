package io.github.sanakitty.streamlabsintegration;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.HttpsURLConnection;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class StreamlabsIntegration extends JavaPlugin {
	
	private static HttpsURLConnection post;
	private static HttpsURLConnection get;
	
	@Override
	public void onEnable() {
		this.saveDefaultConfig();
		getServer().getPluginManager().registerEvents(new EventTesting(), this);
	}
	
	public void onDisable() {
		getLogger().info("OnDisable has been invoked!");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		//COMMAND: /slirequest
		if (cmd.getName().equalsIgnoreCase("slirequest")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("This command can only be run by a player.");
			} else {
				Player player = (Player) sender;
				getLogger().info("Received tokens: " + TokenRequest());
			}
			return true;
		}
		//COMMAND: /slicheck <user>
		if (cmd.getName().equalsIgnoreCase("slicheck")) {
			if (sender instanceof Player){
				Player player = (Player) sender;
				String accessToken = this.getConfig().getString("access_token");
				String username = args[0];
				String channelName = this.getConfig().getString("channel_name");
				
				player.sendMessage("Twitch user " + args[0] + " has " + PointRequest(accessToken, username, channelName) + " KonaCoins.");
			}
			getLogger().info("Received tokens: " + TokenRequest());
			return true;
		}
		return false; 
	}
	
	
	//-----------------------------------------------HTTPS Requests-----------------------------------------------//
	
	public static String TokenRequest(){
		String url = "https://streamlabs.com/api/v1.0/token";
        String urlParameters = "grant_type=authorization_code&client_id=gpQvF31AaIZiS62Bu8OlmDv1DKOaPJgZeFURGpCG&client_secret=8yXDOGrczOx7FN1QcY2YiY65fa2fKTrzN73CIdF0&redirect_uri=https://sanakitty.github.io/&code=bmFx2BBF8RvMLwvT6hMFP929ZVhnt3QimPs4WU9D";
        byte[] postData = urlParameters.getBytes(StandardCharsets.UTF_8);
 
        StringBuilder content = null;
        
        try {

            URL myurl = new URL(url);
            post = (HttpsURLConnection) myurl.openConnection();

            post.setDoOutput(true);
            post.setRequestMethod("POST");
            post.setRequestProperty("User-Agent", "Java client");
            post.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            try (DataOutputStream wr = new DataOutputStream(post.getOutputStream())) {
                wr.write(postData);
            }

            

            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(post.getInputStream()))) {

                String line;
                content = new StringBuilder();

                while ((line = in.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }

        } catch (IOException e){
        	e.getStackTrace();
        }
        
        finally {
            
        	post.disconnect();
        }
        return content.toString();
	}
	
	public static String PointRequest(String accessToken, String username, String channelName){
		String url = "https://streamlabs.com/api/v1.0/points?access_token=" + accessToken + "&username=" + username + "&channel=" + channelName;
		
		StringBuilder content = null;
		try {
			URL myUrl = new URL(url);
			get = (HttpsURLConnection) myUrl.openConnection();
			
			get.setRequestMethod("GET");
			content = new StringBuilder();
			try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(get.getInputStream()))) {

                String line;
                content = new StringBuilder();

                while ((line = in.readLine()) != null) {
                    content.append(line);
                    content.append(System.lineSeparator());
                }
            }
			
		} catch (ProtocolException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		
		} finally {
			get.disconnect();
		}
		
		return content.toString();
	}
}
