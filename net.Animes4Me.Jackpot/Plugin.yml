package net.Animes4Me.Jackpot;
	import java.io.File;
	import java.util.ArrayList;
	import java.util.Arrays;
	import java.util.List;
	import java.util.UUID;

	import org.bukkit.ChatColor;
	import org.bukkit.command.Command;
	import org.bukkit.command.CommandSender;
	import org.bukkit.command.ConsoleCommandSender;
	import org.bukkit.configuration.file.FileConfiguration;
	import org.bukkit.entity.Player;
	import org.bukkit.plugin.java.JavaPlugin;

	public class Plugin extends JavaPlugin {
		public FileConfiguration config;
		private ArrayList<UUID> ignorelist;
		private Jackpot jackpot;
		
		@Override
		public void onEnable(){
			config = getConfig();
			config.options().copyDefaults(true);
			saveConfig();		
			
			ignorelist = new ArrayList<UUID>();
			jackpot = new Jackpot(this, new SQLite(this));
		}

		@Override
        public List<String> onTabComplete(CommandSender sender, Command cmd, String commandLabel, String[] args) {
			if (cmd.getName().equalsIgnoreCase("jackpot")) {
                if (args.length == 1 && sender.hasPermission("jackpot.cmd.help")) {
                	return Arrays.asList("info","deposit","stats","ignore","reload","start","cancel","reset","disable");
                }
			}
			return null;
		}
		
		@Override
		public boolean onCommand(CommandSender sender, Command cmd, String alias, String[] args) {
			if(args.length != 0){
				if(args[0].toLowerCase().equals("info")){
					if(sender.hasPermission("jackpot.cmd.info") || sender instanceof ConsoleCommandSender){
						info(sender, args);
					}else{
						no_perm(sender);
					}
					return true;
				}else if(args[0].toLowerCase().equals("deposit")){
					if(sender.hasPermission("jackpot.cmd.deposit") || sender instanceof ConsoleCommandSender){
						deposit(sender, args);
					}else{
						no_perm(sender);
					}
					return true;
				}else if(args[0].toLowerCase().equals("ignore")){
					if(sender.hasPermission("jackpot.cmd.ignore") || sender instanceof ConsoleCommandSender){
						ignore(sender);
					}else{
						no_perm(sender);
					}
					return true;
				}else if(args[0].toLowerCase().equals("stats")){
					if(sender.hasPermission("jackpot.cmd.stats") || sender instanceof ConsoleCommandSender){
						stats(sender, args);
					}else{
						no_perm(sender);
					}
					return true;
				}else if(args[0].toLowerCase().equals("reload")){
					if(sender.hasPermission("jackpot.cmd.reload") || sender instanceof ConsoleCommandSender){
						reload(sender);
					}else{
						no_perm(sender);
					}
					return true;
				}else if(args[0].toLowerCase().equals("start")){
					if(sender.hasPermission("jackpot.cmd.start") || sender instanceof ConsoleCommandSender){
						start(sender);
					}else{
						no_perm(sender);
					}
					return true;
				}else if(args[0].toLowerCase().equals("cancel")){
					if(sender.hasPermission("jackpot.cmd.cancel") || sender instanceof ConsoleCommandSender){
						cancel(sender);
					}else{
						no_perm(sender);
					}
					return true;
				}else if(args[0].toLowerCase().equals("reset")){
					if(sender.hasPermission("jackpot.cmd.reset") || sender instanceof ConsoleCommandSender){
						reset(sender);
					}else{
						no_perm(sender);
					}
					return true;
				}else if(args[0].toLowerCase().equals("disable")){
					if(sender.hasPermission("jackpot.cmd.disable") || sender instanceof ConsoleCommandSender){
						disable(sender);
					}else{
						no_perm(sender);
					}
					return true;
				}
			}
			
			if(sender.hasPermission("jackpot.cmd.help") || sender instanceof ConsoleCommandSender){
				help(sender);
			}else{
				no_perm(sender);
			}
			
			return true;
		}

		protected void broadcast(String message){
			getServer().broadcastMessage(message);
		}

		protected void sendMessage(CommandSender sender, String message) {
			if(sender instanceof Player){
				if(!ignorelist.contains(((Player)sender).getUniqueId())){
					sender.sendMessage(message);
				}
			}else{
				System.out.println(ChatColor.stripColor(message));
			}
		}

		protected String getMessage(String path) {
			return ChatColor.translateAlternateColorCodes('&', config.getString("messages.prefix") + config.getString(path));
		}

		protected void disable(CommandSender sender) {
			setEnabled(false);
			if(sender instanceof ConsoleCommandSender){
				sendMessage(sender, getMessage("messages.disable_plugin").replace("<Trigger>", "Plugin"));
				for(Player p : getServer().getOnlinePlayers()){
					sendMessage(p, getMessage("messages.disable_plugin").replace("<Trigger>", "Plugin"));
				}
			}else{
				sendMessage(sender, getMessage("messages.disable_plugin").replace("<Trigger>", "CommandExecutor: " + sender.getName()));
				sendMessage(getServer().getConsoleSender(), getMessage("messages.disable_plugin").replace("<Trigger>", "CommandExecutor: " + sender.getName()));
			}
		}

		protected void help(CommandSender sender) {
			if(sender instanceof Player){
				for(String s : config.getStringList("messages.help")){
					sendMessage(sender, ChatColor.translateAlternateColorCodes('&', s));
				}
			}else{
				for(String s : config.getStringList("messages.help")){
					sendMessage(sender, ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', s)));
				}
			}
		}

		private void cancel(CommandSender sender) {
			jackpot.cancelRound(sender);
		}

		private void deposit(CommandSender sender, String[] args) {
			if(sender instanceof Player && args.length == 2 && args[1].toLowerCase().equals("all")){
				if(sender.hasPermission("jackpot.cmd.deposit.all")){
					args[1] = jackpot.getAllMoney(sender);
				}else{
					no_perm(sender);
				}
			}
			jackpot.deposit(sender, args, sender.hasPermission("jackpot.min-bypass"), sender.hasPermission("jackpot.max-bypass"), sender.hasPermission("jackpot.tax-bypass"), sender.hasPermission("jackpot.mult-bypass"));
		}

		private void reload(CommandSender sender) {
			reloadConfig();
			config = getConfig();
			config.options().copyDefaults(true);
			saveConfig();
			
			sendMessage(sender, getMessage("messages.reload_config"));
		}

		private void reset(CommandSender sender) {
			config = null;
			File configFile = new File(getDataFolder(), "config.yml");
			configFile.delete();
			saveDefaultConfig();
			reloadConfig();
			
			config = getConfig();						
			sendMessage(sender, getMessage("messages.reset_config"));
		}

		private void info(CommandSender sender, String[] args) {
			if(sender instanceof Player && args.length == 2 && args[1].toLowerCase().equals("all")){
				if(sender.hasPermission("jackpot.cmd.info.all")){
					sendMessage(sender, jackpot.getInfoA(sender));
				}else{
					no_perm(sender);
				}
			}else{
				sendMessage(sender, jackpot.getInfoP(sender));
				sendMessage(sender, jackpot.getInfoM(sender));
				sendMessage(sender, jackpot.getInfoC(sender));
			}
		}

		private void ignore(CommandSender sender) {
			if(sender instanceof Player){
				Player p = (Player)sender;
				if(p.hasPermission("jackpot.ignore")){
					if(ignorelist.contains(p.getUniqueId())){
						ignorelist.remove(p.getUniqueId());
						sendMessage(p, getMessage("messages.ignore_false"));
					}else{
						sendMessage(p, getMessage("messages.ignore_true"));
						ignorelist.add(p.getUniqueId());
					}
				}
			}else{
				sendMessage(sender, getMessage("messages.console_not_allowed"));
			}
		}
		
		private void stats(CommandSender sender, String[] args) {
			if(sender instanceof Player){
				if(args.length == 2){
					if(sender.hasPermission("jackpot.cmd.stats.others")){
						jackpot.getStatsOthers(sender, args);
					}else{
						no_perm(sender);
					}
				}else{
					jackpot.getStats(sender);
				}
			}else{
				sendMessage(sender, getMessage("messages.console_not_allowed"));
			}
		}
		
		private void start(CommandSender sender) {
			jackpot.forceRound(sender);
		}
		
		private void no_perm(CommandSender sender) {
			sendMessage(sender, getMessage("messages.no_permission"));
		}

	}
