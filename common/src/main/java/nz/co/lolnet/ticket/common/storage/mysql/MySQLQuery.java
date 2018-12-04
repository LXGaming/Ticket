/*
 * Copyright 2018 lolnet.co.nz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nz.co.lolnet.ticket.common.storage.mysql;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.CommentData;
import nz.co.lolnet.ticket.api.data.LocationData;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.common.util.Toolbox;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class MySQLQuery {
    
    public static boolean createTables() {
        try (MySQLStorageAdapter storageAdapter = new MySQLStorageAdapter()) {
            storageAdapter.createConnection().prepareStatement("CREATE TABLE IF NOT EXISTS `user` ("
                    + "`unique_id` CHAR(36) NOT NULL,"
                    + "`name` VARCHAR(16) NOT NULL,"
                    + "`banned` TINYINT(1) UNSIGNED NOT NULL DEFAULT ?,"
                    + "PRIMARY KEY (`unique_id`));");
            storageAdapter.getPreparedStatement().setInt(1, 0);
            storageAdapter.execute();
            
            storageAdapter.prepareStatement("CREATE TABLE IF NOT EXISTS `ticket` ("
                    + "`id` INT(11) NOT NULL AUTO_INCREMENT,"
                    + "`user` CHAR(36) NOT NULL,"
                    + "`timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "`location` TEXT NOT NULL,"
                    + "`text` TEXT NOT NULL,"
                    + "`status` TINYINT(1) NOT NULL DEFAULT ?,"
                    + "`read` TINYINT(1) NOT NULL DEFAULT ?,"
                    + "PRIMARY KEY (`id`),"
                    + "FOREIGN KEY (`user`) REFERENCES `user` (`unique_id`));");
            storageAdapter.getPreparedStatement().setInt(1, 0);
            storageAdapter.getPreparedStatement().setInt(2, 0);
            storageAdapter.execute();
            
            storageAdapter.prepareStatement("CREATE TABLE IF NOT EXISTS `comment` ("
                    + "`id` INT(11) NOT NULL AUTO_INCREMENT,"
                    + "`ticket` INT(11) NOT NULL,"
                    + "`user` CHAR(36) NOT NULL,"
                    + "`timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "`text` TEXT NOT NULL,"
                    + "PRIMARY KEY (`id`),"
                    + "FOREIGN KEY (`ticket`) REFERENCES `ticket` (`id`),"
                    + "FOREIGN KEY (`user`) REFERENCES `user` (`unique_id`));");
            storageAdapter.execute();
            return true;
        } catch (SQLException ex) {
            Ticket.getInstance().getLogger().error("Encountered an error processing MySQLQuery::createTables");
            ex.printStackTrace();
            return false;
        }
    }
    
    public static CommentData createComment(int ticketId, UUID uniqueId, Instant timestamp, String text) throws SQLException {
        try (MySQLStorageAdapter storageAdapter = new MySQLStorageAdapter()) {
            storageAdapter.createConnection().prepareStatement("INSERT INTO `comment`(`ticket`, `user`, `timestamp`, `text`) VALUE (?, ?, ?, ?)", true);
            storageAdapter.getPreparedStatement().setInt(1, ticketId);
            storageAdapter.getPreparedStatement().setString(2, uniqueId.toString());
            storageAdapter.getPreparedStatement().setTimestamp(3, Timestamp.from(timestamp));
            storageAdapter.getPreparedStatement().setString(4, text);
            ResultSet resultSet = storageAdapter.execute();
            if (!resultSet.next()) {
                throw new SQLException("Failed to create Comment");
            }
            
            CommentData comment = new CommentData();
            comment.setId(resultSet.getInt(1));
            comment.setTicket(ticketId);
            comment.setUser(uniqueId);
            comment.setTimestamp(timestamp);
            comment.setText(text);
            return comment;
        }
    }
    
    public static TicketData createTicket(UUID uniqueId, Instant timestamp, LocationData location, String text) throws SQLException {
        try (MySQLStorageAdapter storageAdapter = new MySQLStorageAdapter()) {
            storageAdapter.createConnection().prepareStatement("INSERT INTO `ticket`(`user`, `timestamp`, `location`, `text`) VALUE (?, ?, ?, ?)", true);
            storageAdapter.getPreparedStatement().setString(1, uniqueId.toString());
            storageAdapter.getPreparedStatement().setTimestamp(2, Timestamp.from(timestamp));
            storageAdapter.getPreparedStatement().setString(3, new Gson().toJson(location));
            storageAdapter.getPreparedStatement().setString(4, text);
            ResultSet resultSet = storageAdapter.execute();
            if (!resultSet.next()) {
                throw new SQLException("Failed to create Ticket");
            }
            
            TicketData ticket = new TicketData();
            ticket.setId(resultSet.getInt(1));
            ticket.setUser(uniqueId);
            ticket.setTimestamp(timestamp);
            ticket.setLocation(location);
            ticket.setText(text);
            ticket.setComments(Sets.newHashSet());
            return ticket;
        }
    }
    
    public static UserData createUser(UUID uniqueId) throws SQLException {
        String username = Ticket.getInstance().getPlatform().getUsername(uniqueId).orElse("Unknown");
        try (MySQLStorageAdapter storageAdapter = new MySQLStorageAdapter()) {
            storageAdapter.createConnection().prepareStatement("INSERT INTO `user`(`unique_id`, `name`) VALUE (?, ?)");
            storageAdapter.getPreparedStatement().setString(1, uniqueId.toString());
            storageAdapter.getPreparedStatement().setString(2, username);
            if (storageAdapter.getPreparedStatement().executeUpdate() == 0) {
                throw new SQLException("Failed to create User");
            }
            
            UserData user = new UserData();
            user.setUniqueId(uniqueId);
            user.setName(username);
            return user;
        }
    }
    
    public static Optional<Set<CommentData>> getComments(int ticketId) throws SQLException {
        try (MySQLStorageAdapter storageAdapter = new MySQLStorageAdapter()) {
            storageAdapter.createConnection().prepareStatement("SELECT * FROM `comment` WHERE `ticket` = ?");
            storageAdapter.getPreparedStatement().setInt(1, ticketId);
            ResultSet resultSet = storageAdapter.execute();
            
            Set<CommentData> comments = Sets.newHashSet();
            while (resultSet.next()) {
                CommentData comment = new CommentData();
                comment.setId(resultSet.getInt("id"));
                comment.setTicket(resultSet.getInt("ticket"));
                comment.setUser(UUID.fromString(resultSet.getString("user")));
                comment.setTimestamp(resultSet.getTimestamp("timestamp").toInstant());
                comment.setText(resultSet.getString("text"));
                comments.add(comment);
            }
            
            return Optional.of(comments);
        }
    }
    
    public static Optional<TicketData> getTicket(int ticketId) throws SQLException {
        try (MySQLStorageAdapter storageAdapter = new MySQLStorageAdapter()) {
            storageAdapter.createConnection().prepareStatement("SELECT * FROM `ticket` WHERE `id` = ? LIMIT 0, 1");
            storageAdapter.getPreparedStatement().setInt(1, ticketId);
            ResultSet resultSet = storageAdapter.execute();
            if (!resultSet.next()) {
                return Optional.empty();
            }
            
            TicketData ticket = new TicketData();
            ticket.setId(resultSet.getInt("id"));
            ticket.setUser(UUID.fromString(resultSet.getString("user")));
            ticket.setTimestamp(resultSet.getTimestamp("timestamp").toInstant());
            ticket.setLocation(Toolbox.parseJson(resultSet.getString("location"), LocationData.class).orElse(null));
            ticket.setText(resultSet.getString("text"));
            ticket.setStatus(resultSet.getInt("status"));
            ticket.setRead(resultSet.getBoolean("read"));
            ticket.setComments(Sets.newHashSet());
            return Optional.of(ticket);
        }
    }
    
    public static Optional<Set<TicketData>> getOpenTickets() throws SQLException {
        try (MySQLStorageAdapter storageAdapter = new MySQLStorageAdapter()) {
            storageAdapter.createConnection().prepareStatement("SELECT * FROM `ticket` WHERE `status` = ?");
            storageAdapter.getPreparedStatement().setInt(1, 0);
            ResultSet resultSet = storageAdapter.execute();
            
            Set<TicketData> tickets = Sets.newHashSet();
            while (resultSet.next()) {
                TicketData ticket = new TicketData();
                ticket.setId(resultSet.getInt("id"));
                ticket.setUser(UUID.fromString(resultSet.getString("user")));
                ticket.setTimestamp(resultSet.getTimestamp("timestamp").toInstant());
                ticket.setLocation(Toolbox.parseJson(resultSet.getString("location"), LocationData.class).orElse(null));
                ticket.setText(resultSet.getString("text"));
                ticket.setStatus(resultSet.getInt("status"));
                ticket.setRead(resultSet.getBoolean("read"));
                ticket.setComments(Sets.newHashSet());
                tickets.add(ticket);
            }
            
            return Optional.of(tickets);
        }
    }
    
    public static Optional<Set<TicketData>> getUnreadTickets(UUID uniqueId) throws SQLException {
        try (MySQLStorageAdapter storageAdapter = new MySQLStorageAdapter()) {
            storageAdapter.createConnection().prepareStatement("SELECT * FROM `ticket` WHERE `user` = ? AND `status` = ? AND `read` = ?");
            storageAdapter.getPreparedStatement().setString(1, uniqueId.toString());
            storageAdapter.getPreparedStatement().setInt(2, 1);
            storageAdapter.getPreparedStatement().setInt(3, 0);
            ResultSet resultSet = storageAdapter.execute();
            
            Set<TicketData> tickets = Sets.newHashSet();
            while (resultSet.next()) {
                TicketData ticket = new TicketData();
                ticket.setId(resultSet.getInt("id"));
                ticket.setUser(UUID.fromString(resultSet.getString("user")));
                ticket.setTimestamp(resultSet.getTimestamp("timestamp").toInstant());
                ticket.setLocation(Toolbox.parseJson(resultSet.getString("location"), LocationData.class).orElse(null));
                ticket.setText(resultSet.getString("text"));
                ticket.setStatus(resultSet.getInt("status"));
                ticket.setRead(resultSet.getBoolean("read"));
                ticket.setComments(Sets.newHashSet());
                tickets.add(ticket);
            }
            
            return Optional.of(tickets);
        }
    }
    
    public static Optional<UserData> getUser(UUID uniqueId) throws SQLException {
        try (MySQLStorageAdapter storageAdapter = new MySQLStorageAdapter()) {
            storageAdapter.createConnection().prepareStatement("SELECT * FROM `user` WHERE `unique_id` = ? LIMIT 0, 1");
            storageAdapter.getPreparedStatement().setString(1, uniqueId.toString());
            ResultSet resultSet = storageAdapter.execute();
            if (!resultSet.next()) {
                return Optional.empty();
            }
            
            UserData user = new UserData();
            user.setUniqueId(UUID.fromString(resultSet.getString("unique_id")));
            user.setName(resultSet.getString("name"));
            user.setBanned(resultSet.getBoolean("banned"));
            return Optional.of(user);
        }
    }
    
    public static Optional<Set<UserData>> getUsers(String name) throws SQLException {
        try (MySQLStorageAdapter storageAdapter = new MySQLStorageAdapter()) {
            storageAdapter.createConnection().prepareStatement("SELECT * FROM `user` WHERE `name` = ? LIMIT 0, 1");
            storageAdapter.getPreparedStatement().setString(1, name);
            ResultSet resultSet = storageAdapter.execute();
            
            Set<UserData> users = Sets.newHashSet();
            while (resultSet.next()) {
                UserData user = new UserData();
                user.setUniqueId(UUID.fromString(resultSet.getString("unique_id")));
                user.setName(resultSet.getString("name"));
                user.setBanned(resultSet.getBoolean("banned"));
                users.add(user);
            }
            
            return Optional.of(users);
        }
    }
    
    public static boolean updateTicket(TicketData ticket) {
        try (MySQLStorageAdapter storageAdapter = new MySQLStorageAdapter()) {
            storageAdapter.createConnection().prepareStatement("UPDATE `ticket` SET `read` = ?, `status` = ? WHERE `id` = ?");
            storageAdapter.getPreparedStatement().setBoolean(1, ticket.isRead());
            storageAdapter.getPreparedStatement().setInt(2, ticket.getStatus());
            storageAdapter.getPreparedStatement().setInt(3, ticket.getId());
            return storageAdapter.getPreparedStatement().executeUpdate() != 0;
        } catch (SQLException ex) {
            Ticket.getInstance().getLogger().error("Encountered an error processing MySQLQuery::updateTicket");
            ex.printStackTrace();
            return false;
        }
    }
    
    public static boolean updateUser(UserData user) {
        try (MySQLStorageAdapter storageAdapter = new MySQLStorageAdapter()) {
            storageAdapter.createConnection().prepareStatement("UPDATE `user` SET `banned` = ?, `name` = ? WHERE `unique_id` = ?");
            storageAdapter.getPreparedStatement().setBoolean(1, user.isBanned());
            storageAdapter.getPreparedStatement().setString(2, user.getName());
            storageAdapter.getPreparedStatement().setString(3, user.getUniqueId().toString());
            return storageAdapter.getPreparedStatement().executeUpdate() != 0;
        } catch (SQLException ex) {
            Ticket.getInstance().getLogger().error("Encountered an error processing MySQLQuery::updateUser");
            ex.printStackTrace();
            return false;
        }
    }
}