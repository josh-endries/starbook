package starbook.nodes.index;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Set;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import starbook.common.User;

public enum DNS {
	Instance;
	
	private DataSource dataSource;
	private static final Logger log = Logger.getLogger(DNS.class);
	
	public void initialize() throws NamingException, SQLException {
		/*
		 * Create a database connection.
		 */
		Context initial = new InitialContext();
		Context env = (Context) initial.lookup("java:comp/env");
		dataSource = (DataSource) env.lookup("jdbc/bind");
		
		Connection connection = null;
		PreparedStatement ps = null;
		
		try {
			connection = dataSource.getConnection();
			String sql = "select id from records limit 1";
			ps = connection.prepareStatement(sql);

			/*
			 * Test the connection.
			 */
			ResultSet rs = ps.executeQuery();
			if (!rs.first()) throw new SQLException("Unable to query the database.");
		} finally {
			if (connection != null) connection.close();
			if (ps != null) ps.close();
		}
	}
	
	
	
	/**
	 * Update the list of index nodes in the DNS database to the specified set of addresses. This
	 * operation first deletes the records and then re-adds them, meaning there is a gap of time
	 * wherein there are zero or an incorrect list of index nodes in the list.
	 * 
	 * @param addresses The addresses to which the index records will be set.
	 */
	public void modifyIndexNodes(Set<InetAddress> addresses) {
		Connection connection = null;
		PreparedStatement ps = null;
		
		try {
			connection = dataSource.getConnection();
			
			String sql1 = "delete from records where type='A' and host='@' and zone='starbook.l'";
			connection.createStatement().executeUpdate(sql1);
			
			String sql = String.format("insert into records (type,host,data,zone,ttl) values ('A', '@', ?, 'starbook.l', 60)");
			ps = connection.prepareStatement(sql);
			
			for (InetAddress a: addresses) {
				ps.setString(1, a.getHostAddress());
				ps.executeUpdate();
			}
			
			log.debug(String.format("Index list set to: %s", addresses));
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (connection != null) connection.close();
				if (ps != null) ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	public void updateUser(User user, Set<InetAddress> addresses) {
		Connection conn = null;
		PreparedStatement ps = null;

		try {
			conn = dataSource.getConnection();

			String sql = "select id from records where host = ?";
			ps = conn.prepareStatement(sql);
			ps.setString(1, user.getName());
			log.debug("Executing query: " + ps);
			ResultSet rs = ps.executeQuery();

			Integer userDatabaseID = null;
			if (rs.first()) {
				/*
				 * A result was found.
				 */
				userDatabaseID = rs.getInt("id");
				log.debug("Found an existing record: " + userDatabaseID);
			} else {
				/*
				 * There isn't a database record for this user.
				 */
				log.debug(String.format("User %s was not found in the database.", user));
			}

			/*
			 * Delete the current entries.
			 */
			sql = "delete from records where host = ?";
			ps = conn.prepareStatement(sql);
			ps.setString(1, user.getName());
			log.debug("Executing query: " + ps);
			ps.executeUpdate();

			if (userDatabaseID == null) {
				/*
				 * The user ID doesn't exist in the database.
				 */
				if (addresses == null || addresses.size() < 1) {
					/*
					 * There are no addresses for this user, so the fact that it's not in the database is
					 * okay. Don't do anything.
					 */
					log.debug(String.format("User %s was not found in the database and there are no addresses to assign.", user));
				} else {
					/*
					 * This user exists on the network, but not in the database. Create the record(s) for
					 * it.
					 */
					Iterator<InetAddress> i = addresses.iterator();
					ps = conn.prepareStatement("insert into records (type,ttl,zone,host,data) values (?,?,?,?,?)");
					while (i.hasNext()) {
						ps.setString(1, "A");
						ps.setInt(2, 60);
						ps.setString(3, "starbook.l");
						ps.setString(4, user.getName());
						ps.setString(5, i.next().getHostAddress());
						log.debug("Executing query: " + ps);
						ps.executeUpdate();
					}
				}
			} else {
				/*
				 * The user does exist in the database.
				 */
				if (addresses == null || addresses.size() < 1) {
					/*
					 * The user exists in the database, but doesn't have any nodes on the network
					 * associated with it. Since we already cleared the database entries for this user,
					 * leave it alone.
					 */
					log.debug(String.format("User %s was found in the database but there are no nodes to assign."));
				} else {
					/*
					 * The user exists in the database and there are nodes on the network associated with
					 * it. Reset what's in the database to match the user list.
					 */
					Iterator<InetAddress> i = addresses.iterator();
					ps = conn.prepareStatement("insert into records (type,ttl,zone,host,data) values (?,?,?,?,?)");
					while (i.hasNext()) {
						ps.setString(1, "A");
						ps.setInt(2, 60);
						ps.setString(3, "starbook.l");
						ps.setString(4, user.getName());
						ps.setString(5, i.next().getHostAddress());
						log.debug("Executing query: " + ps);
						ps.executeUpdate();
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (ps != null)
					ps.close();
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

	}
}
