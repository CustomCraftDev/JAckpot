package net.Animes4Me.Jackpot;
	import java.sql.Connection;
	import java.sql.DriverManager;
	import java.sql.PreparedStatement;
	import java.sql.ResultSet;
	import java.sql.Statement;
	
	public class SQLite {
		private Connection sql;
		private boolean connection = false;
		
		public SQLite(Plugin plugin) {			
			try {
		    	Class.forName("org.sqlite.JDBC");
		    	sql = DriverManager.getConnection("jdbc:sqlite:plugins/Jackpot/stats.db");
		    	connection = true;
			}catch(Exception ex) {}
			
			if (connection) {
				setupDB();
			}else {
				plugin.sendMessage(plugin.getServer().getConsoleSender(), plugin.getMessage("messages.database_missing"));
				plugin.disable(plugin.getServer().getConsoleSender());
			}
		}
	
		private void setupDB() {
			try {
				Statement stmt = sql.createStatement();
				String query = "CREATE TABLE IF NOT EXISTS stats" +
		                       "(UUID         CHAR(40), " +
		                       " MONEY       CHAR(500), " + 
		                       ");";
				stmt.executeUpdate(query);
				stmt.close();
			}catch(Exception ex) {}
		}
	
		public void saveStatistic(String uuid, String money) {
			if(connection){
				try {
					PreparedStatement ps = sql.prepareStatement("INSERT INTO stats VALUES(?, ?);"); 
						ps.setString(1, uuid);
			            ps.setString(2, money);
			            ps.executeUpdate(); 
			            ps.close();
				}catch(Exception ex) {}
			}
		}
	
		public ResultSet getStatistic(String uuid) {
			if(connection){
				try {
					Statement stmt = sql.createStatement();
					ResultSet rs = stmt.executeQuery("SELECT * FROM stats WHERE UUID='" + uuid + "' LIMIT 10;");
					stmt.close();
					return rs;
				}catch(Exception ex) {}
			}
			return null;
		}
	
	}
