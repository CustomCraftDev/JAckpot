package net.Animes4Me.Jackpot;
	import java.sql.ResultSet;
import java.util.ArrayList;
	import java.util.Random;
	import java.util.UUID;

	import org.bukkit.Sound;
	import org.bukkit.command.CommandSender;
	import org.bukkit.entity.Player;
	import org.bukkit.plugin.RegisteredServiceProvider;
	import net.milkbowl.vault.economy.Economy;
	
	public class Jackpot implements Runnable {
		private ArrayList<Deposit> bids;
		private Economy economy;
		private Plugin plugin;
		private Random random;
		private SQLite db;
		
		private boolean isRunning;
		private int countdown;
		private int task;
			
		public Jackpot(Plugin plugin, SQLite db) {
			this.db = db;
			this.plugin = plugin;
			random = new Random();
			setupEconomy();
			
			if(economy != null){
				isRunning = false;
				bids = new ArrayList<Deposit>();
			}else{
				plugin.sendMessage(plugin.getServer().getConsoleSender(), plugin.getMessage("messages.vault_economy_missing"));
				plugin.disable(plugin.getServer().getConsoleSender());
			}
		}

		@Override
		public void run() {
			if(isRunning){
				
				--this.countdown;
		        if (this.countdown == 0) {
		        	endRound();
		        	plugin.getServer().getScheduler().cancelTask(task);
		            return;
		        }
		        if (this.countdown < 4 || this.countdown == 10 || this.countdown == 30 || this.countdown == 60) {
		        	plugin.broadcast(plugin.getMessage("messages.round.countdown").replace("<Time>", "" + countdown));
		        	if(plugin.config.getBoolean("settings.play_soundeffects.value")){
		        		for(Player p : plugin.getServer().getOnlinePlayers()){
		        			p.playSound(p.getLocation(), Sound.valueOf(plugin.config.getString("settings.play_soundeffects.countdown")), 1, 1);
		        		}
		        	}
		        }				
			}else{
				plugin.getServer().getScheduler().cancelTask(task);
			}
		}
		
		protected void deposit(CommandSender sender, String[] args, boolean min, boolean max, boolean tax, boolean mult) {
			if(sender instanceof Player){				
				
				if(args.length > 1){
					if(args[1] == null){
						plugin.sendMessage(sender, plugin.getMessage("messages.bid.error_all"));
						return;
					}
					
					Long l = 0L;
					try{
						 l = Long.parseLong(args[1]) >= 0 ? Long.parseLong(args[1]) : 0;
					}catch(Exception ex){
						plugin.sendMessage(sender, plugin.getMessage("messages.bid.error_long"));
						return;
					}
					
					if(min || plugin.config.getLong("settings.min_deposit.value") <= l) {
						Player p = (Player) sender;
						UUID uuid = p.getUniqueId();
						
						if(!max && plugin.config.getLong("settings.max_deposit.value") < l){
							l = plugin.config.getLong("settings.max_deposit.value");
							plugin.sendMessage(sender, plugin.getMessage("messages.bid.error_max").replace("<Money>", "" + l));
						}

						if(!mult && !plugin.config.getBoolean("settings.multiple_bids.value")){
							for(Deposit d : bids){
								if(d.UUID.equals(uuid.toString())){
									plugin.sendMessage(sender, plugin.getMessage("messages.bid.multiple"));
									return;
								}
							}
						}

						if (!economy.has(plugin.getServer().getOfflinePlayer(uuid), l)){
							plugin.sendMessage(sender, plugin.getMessage("messages.bid.not_enough_money").replace("<Money>", "" + economy.getBalance(plugin.getServer().getOfflinePlayer(uuid))));
							return;
						}
						economy.withdrawPlayer(plugin.getServer().getOfflinePlayer(uuid), l);

						for(Deposit d : bids){
							if(d.UUID.equals(uuid.toString())){
								if(d.MONEY + l > Long.MAX_VALUE){
									plugin.sendMessage(sender, plugin.getMessage("messages.bid.error_max_multiple"));
								}else{
									d.MONEY += l;
									d.BYPASS = tax;
									deposit_sucess(sender, d);
								}
								return;
							}
						}

						Deposit d = new Deposit(uuid.toString(), l, tax);
						bids.add(d);
						deposit_sucess(sender, d);				

						startRound();			
					}else{
						plugin.sendMessage(sender, plugin.getMessage("messages.bid.error_min").replace("<Money>", "" + plugin.config.getLong("settings.min_deposit.value")));
					}
				}else{
					plugin.help(sender);
				}
			}else{
				plugin.sendMessage(plugin.getServer().getConsoleSender(), plugin.getMessage("messages.console_not_allowed"));
			}	
		}

		protected void forceRound(CommandSender sender) {
			if(isRunning){
				plugin.sendMessage(sender, plugin.getMessage("messages.round.is_active"));
			}else{
				isRunning = true;
				countdown = plugin.config.getInt("settings.round_time.value");
				plugin.broadcast(plugin.getMessage("messages.round.start").replace("<Time>", "" + countdown));
				
				if(plugin.config.getBoolean("settings.play_soundeffects.value")){
	        		for(Player p : plugin.getServer().getOnlinePlayers()){
	        			p.playSound(p.getLocation(), Sound.valueOf(plugin.config.getString("settings.play_soundeffects.round_start")), 1, 1);
	        		}
	        	}
				
				task = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0L, 20L);
			}
		}
		
		protected void cancelRound(CommandSender sender) {
			if(!isRunning){
				plugin.sendMessage(sender, plugin.getMessage("messages.round.not_active"));
			}else{
				isRunning = false;
				if(plugin.config.getBoolean("settings.play_soundeffects.value")){
	        		for(Player p : plugin.getServer().getOnlinePlayers()){
	        			p.playSound(p.getLocation(), Sound.valueOf(plugin.config.getString("settings.play_soundeffects.round_cancel")), 1, 1);
	        		}
	        	}
				
				for(Deposit d : bids){
					economy.depositPlayer(plugin.getServer().getOfflinePlayer(UUID.fromString(d.UUID)), d.MONEY);
				}
				
				bids = new ArrayList<Deposit>();	
				plugin.broadcast(plugin.getMessage("messages.round.canceled"));
			}
		}
		
		protected String getInfoP(CommandSender sender) {
			return plugin.getMessage("messages.info.players").replace("<Count>", "" + bids.size());
		}
		
		protected String getInfoM(CommandSender sender) {
			long all = getJackpot();
			return plugin.getMessage("messages.info.money").replace("<Jackpot>", "" + all);
		}
	
		protected String getInfoC(CommandSender sender) {
			if(sender instanceof Player){
				for(Deposit d : bids){
					if(d.UUID.equals(((Player) sender).getUniqueId().toString())){
						return plugin.getMessage("messages.info.chance").replace("<Chance>", "" + getChance(d.MONEY));
					}
				}
				return plugin.getMessage("messages.info.chance").replace("<Chance>", "0");
				
			}else{
				return plugin.getMessage("messages.console_not_allowed");
			}
		}

		private void endRound(){
			isRunning = false;
			if(plugin.config.getBoolean("settings.play_soundeffects.value")){
        		for(Player p : plugin.getServer().getOnlinePlayers()){
        			p.playSound(p.getLocation(), Sound.valueOf(plugin.config.getString("settings.play_soundeffects.round_end")), 1, 1);
        		}
        	}
			
	        Float f = Float.valueOf(random.nextFloat() * 100.0f);
	        Float state = Float.valueOf(0.0f);
	        Long all = getJackpot();
	        
	        String winner = "";
	        boolean bypass = false;
	        
	        for (Deposit d : bids) {
	            Float chance = Float.valueOf((float)d.MONEY / (float)all * 100.0f);
	            state = Float.valueOf(state.floatValue() + chance.floatValue());
	            if (f.floatValue() > state.floatValue()){
	            	continue;
	            }
	            
	            storeStats(d);
	            winner = d.UUID;
	            bypass = d.BYPASS; 
	            break;
	        }
	        
	        if(plugin.getServer().getPlayer(UUID.fromString(winner)) != null) {
		        if(bypass){
			        plugin.sendMessage(plugin.getServer().getPlayer(UUID.fromString(winner)), plugin.getMessage("messages.round.won_private").replace("<Jackpot>", "" + all));
			        broadcast_notAll(winner, plugin.getMessage("messages.round.won_global").replace("<Winner>", plugin.getServer().getPlayer(UUID.fromString(winner)).getDisplayName()).replace("<Jackpot>", "" + all));
			        economy.depositPlayer(plugin.getServer().getOfflinePlayer(UUID.fromString(winner)), all);
		        }else{
		        	Long price = all - all * plugin.config.getLong("settings.tax_percentage.value") / 100;
			        plugin.sendMessage(plugin.getServer().getPlayer(UUID.fromString(winner)), plugin.getMessage("messages.round.won_private").replace("<Jackpot>", "" + price));
			        broadcast_notAll(winner, plugin.getMessage("messages.round.won_global").replace("<Winner>", plugin.getServer().getPlayer(UUID.fromString(winner)).getDisplayName()).replace("<Jackpot>", "" + price));
			        economy.depositPlayer(plugin.getServer().getOfflinePlayer(UUID.fromString(winner)), (double) price);
		        }
	        }else{
	        	if(bypass){
	        		broadcast_notAll(winner, plugin.getMessage("messages.round.won_global").replace("<Winner>", plugin.getServer().getOfflinePlayer(UUID.fromString(winner)).getName()).replace("<Jackpot>", "" + all));
			        economy.depositPlayer(plugin.getServer().getOfflinePlayer(UUID.fromString(winner)), all);
		        }else{
		        	Long price = all - all * plugin.config.getLong("settings.tax_percentage.value") / 100;
		        	broadcast_notAll(winner, plugin.getMessage("messages.round.won_global").replace("<Winner>", plugin.getServer().getOfflinePlayer(UUID.fromString(winner)).getName()).replace("<Jackpot>", "" + price));
			        economy.depositPlayer(plugin.getServer().getOfflinePlayer(UUID.fromString(winner)), (double) price);
		        }
	        }

			bids = new ArrayList<Deposit>();
		}

		private void broadcast_notAll(String uuid_not_him, String message){
			for(Player p : plugin.getServer().getOnlinePlayers()){
				if(!p.getUniqueId().toString().equals(uuid_not_him)){
					plugin.sendMessage(p, message);
				}
			}
		}
		
		private void deposit_sucess(CommandSender sender, Deposit d) {
			long all = getJackpot();
            
            float chance = (float)d.MONEY / (float)all * 100.0f;
            chance = (double)chance % 0.1 >= 0.05 ? chance - chance % 0.1f + 0.1f : (chance-=chance % 0.1f);
            
			plugin.sendMessage(sender, plugin.getMessage("messages.bid.new").replace("<Chance>", "" + chance));
			plugin.broadcast(plugin.getMessage("messages.bid.global").replace("<Player>", plugin.getServer().getPlayer(UUID.fromString(d.UUID)).getDisplayName()).replace("<Deposit>", "" + d.MONEY).replace("<Jackpot>", "" + all));
			
			for(Deposit temp : bids){
				if(!temp.UUID.equals(d.UUID)){
					
					float otherchance = (float)temp.MONEY / (float)all * 100.0f;
	                otherchance = (double)otherchance % 0.1 >= 0.05 ? otherchance - otherchance % 0.1f + 0.1f : (otherchance-=otherchance % 0.1f);
					
					plugin.sendMessage(plugin.getServer().getPlayer(UUID.fromString(temp.UUID)), plugin.getMessage("messages.bid.private").replace("<Chance>", "" + otherchance));
				}
			}
		}
		
		private void startRound(){
			if(!isRunning){
				if(bids.size() > 1){
					isRunning = true;
					countdown = plugin.config.getInt("settings.round_time.value");
					plugin.broadcast(plugin.getMessage("messages.round.start").replace("<Time>", "" + countdown));
					
					if(plugin.config.getBoolean("settings.play_soundeffects.value")){
		        		for(Player p : plugin.getServer().getOnlinePlayers()){
		        			p.playSound(p.getLocation(), Sound.valueOf(plugin.config.getString("settings.play_soundeffects.round_start")), 1, 1);
		        		}
		        	}
					
					task = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, this, 0L, 20L);
				}
			}
		}

		private float getChance(long money) {
			float chance = (float)money / (float)getJackpot() * 100.0f;
            return new Double((double)chance % 0.1 >= 0.05 ? chance - chance % 0.1f + 0.1f : (chance-=chance % 0.1f)).floatValue();
		}
		
		private long getJackpot(){
			long all = 0L;
			for(Deposit d : bids){
				all += d.MONEY;
			}
			return all;
		}
		
		private boolean setupEconomy() {
			RegisteredServiceProvider<Economy> economyProvider = plugin.getServer().getServicesManager().getRegistration(Economy.class);		
			economy = economyProvider != null ? (Economy)economyProvider.getProvider() : null;
			return economy != null;
		}
		
		private class Deposit {
			private String UUID;
			private long MONEY;
			private boolean BYPASS;
			
			private Deposit(String uuid, long money, boolean tax_bypass){
				UUID = uuid;
				MONEY = money;
				BYPASS = tax_bypass;
			}
		}

		public String getAllMoney(CommandSender sender) {
			if(sender instanceof Player){
				UUID uuid = ((Player) sender).getUniqueId();
				return "" + new Double(economy.getBalance(plugin.getServer().getOfflinePlayer(uuid))).longValue();
			}else{
				plugin.sendMessage(plugin.getServer().getConsoleSender(), plugin.getMessage("messages.console_not_allowed"));
			}
			return null;
		}

		public String getInfoA(CommandSender sender) {
			String msg = "";
			for(Deposit d : bids){
				msg += "\n" + plugin.getServer().getOfflinePlayer(UUID.fromString(d.UUID)).getName() + " - %" + getChance(d.MONEY);
			}
			return msg;
		}
		
		private void storeStats(Deposit d){
			for(Deposit temp : bids){
				if(temp.equals(d)){
					db.saveStatistic(d.UUID, "+" + d.MONEY);
				}else{
					db.saveStatistic(d.UUID, "-" + d.MONEY);
				}
			}
		}

		public void getStatsOthers(CommandSender sender, String[] args) {
			Player requested = plugin.getServer().getPlayer(args[1]);
			
			if(requested != null){
				ResultSet rs = db.getStatistic(requested.getUniqueId().toString());
				try{
					plugin.sendMessage(sender, plugin.getMessage("messages.stats.header").replace("<Player>", requested.getName()));
					while(rs.next()){
						String MONEY = rs.getString("MONEY");
						
						if(MONEY.startsWith("+")){
							plugin.sendMessage(sender, plugin.getMessage("messages.stats.line_plus").replace("<Money>", MONEY));
						}else{
							plugin.sendMessage(sender, plugin.getMessage("messages.stats.line_minus").replace("<Money>", MONEY));
						}
					}
				}catch(Exception ex){}
			}else{
				plugin.sendMessage(sender, plugin.getMessage("messages.player_not_online").replace("<Player>", args[1]));
			}
		}
		
		public void getStats(CommandSender sender) {
			Player p = (Player) sender;
			ResultSet rs = db.getStatistic(p.getUniqueId().toString());

			try{
				plugin.sendMessage(sender, plugin.getMessage("messages.stats.header").replace("<Player>", p.getName()));
				while(rs.next()){
					String MONEY = rs.getString("MONEY");
					
					if(MONEY.startsWith("+")){
						plugin.sendMessage(sender, plugin.getMessage("messages.stats.line_plus").replace("<Money>", MONEY));
					}else{
						plugin.sendMessage(sender, plugin.getMessage("messages.stats.line_minus").replace("<Money>", MONEY));
					}
				}
			}catch(Exception ex){}
		}
		
	}
