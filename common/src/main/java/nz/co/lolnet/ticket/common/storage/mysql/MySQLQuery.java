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
import nz.co.lolnet.ticket.common.storage.Query;
import nz.co.lolnet.ticket.common.util.Toolbox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

public class MySQLQuery implements Query {
    
    private final MySQLStorage storage;
    
    MySQLQuery(MySQLStorage storage) {
        this.storage = storage;
    }
    
    @Override
    public boolean createTables() {
        try (Connection connection = storage.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(""
                    + "CREATE TABLE IF NOT EXISTS `user` ("
                    + "`unique_id` CHAR(36) NOT NULL,"
                    + "`name` VARCHAR(16) NOT NULL,"
                    + "`banned` TINYINT(1) UNSIGNED NOT NULL DEFAULT ?,"
                    + "PRIMARY KEY (`unique_id`));")) {
                preparedStatement.setInt(1, 0);
                preparedStatement.execute();
            }
            
            try (PreparedStatement preparedStatement = connection.prepareStatement(""
                    + "CREATE TABLE IF NOT EXISTS `ticket` ("
                    + "`id` INT(11) NOT NULL AUTO_INCREMENT,"
                    + "`user` CHAR(36) NOT NULL,"
                    + "`timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "`location` TEXT NOT NULL,"
                    + "`text` TEXT NOT NULL,"
                    + "`status` TINYINT(1) NOT NULL DEFAULT ?,"
                    + "`read` TINYINT(1) NOT NULL DEFAULT ?,"
                    + "PRIMARY KEY (`id`),"
                    + "FOREIGN KEY (`user`) REFERENCES `user` (`unique_id`));")) {
                preparedStatement.setInt(1, 0);
                preparedStatement.setInt(2, 0);
                preparedStatement.execute();
            }
            
            try (PreparedStatement preparedStatement = connection.prepareStatement(""
                    + "CREATE TABLE IF NOT EXISTS `comment` ("
                    + "`id` INT(11) NOT NULL AUTO_INCREMENT,"
                    + "`ticket` INT(11) NOT NULL,"
                    + "`user` CHAR(36) NOT NULL,"
                    + "`timestamp` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "`text` TEXT NOT NULL,"
                    + "PRIMARY KEY (`id`),"
                    + "FOREIGN KEY (`ticket`) REFERENCES `ticket` (`id`),"
                    + "FOREIGN KEY (`user`) REFERENCES `user` (`unique_id`));")) {
                preparedStatement.execute();
            }
            
            return true;
        } catch (SQLException ex) {
            Ticket.getInstance().getLogger().error("Encountered an error processing MySQLQuery::createTables");
            ex.printStackTrace();
            return false;
        }
    }
    
    @Override
    public CommentData createComment(int ticketId, UUID uniqueId, Instant timestamp, String text) throws SQLException {
        try (Connection connection = storage.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(""
                    + "INSERT INTO `comment`(`ticket`, `user`, `timestamp`, `text`) VALUE (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setInt(1, ticketId);
                preparedStatement.setString(2, uniqueId.toString());
                preparedStatement.setTimestamp(3, Timestamp.from(timestamp));
                preparedStatement.setString(4, text);
                
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
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
        }
    }
    
    public TicketData createTicket(UUID uniqueId, Instant timestamp, LocationData location, String text) throws SQLException {
        try (Connection connection = storage.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(""
                    + "INSERT INTO `ticket`(`user`, `timestamp`, `location`, `text`) VALUE (?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                preparedStatement.setString(1, uniqueId.toString());
                preparedStatement.setTimestamp(2, Timestamp.from(timestamp));
                preparedStatement.setString(3, new Gson().toJson(location));
                preparedStatement.setString(4, text);
                
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        throw new SQLException("Failed to create Ticket");
                    }
                    
                    TicketData ticket = new TicketData();
                    ticket.setId(resultSet.getInt(1));
                    ticket.setUser(uniqueId);
                    ticket.setTimestamp(timestamp);
                    ticket.setLocation(location);
                    ticket.setText(text);
                    ticket.setComments(Sets.newTreeSet());
                    return ticket;
                }
            }
        }
    }
    
    public UserData createUser(UUID uniqueId) throws SQLException {
        String username = Ticket.getInstance().getPlatform().getUsername(uniqueId).orElse("Unknown");
        try (Connection connection = storage.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(""
                    + "INSERT INTO `user`(`unique_id`, `name`) VALUE (?, ?)")) {
                preparedStatement.setString(1, uniqueId.toString());
                preparedStatement.setString(2, username);
                
                if (preparedStatement.executeUpdate() == 0) {
                    throw new SQLException("Failed to create User");
                }
                
                UserData user = new UserData();
                user.setUniqueId(uniqueId);
                user.setName(username);
                return user;
            }
        }
    }
    
    public Collection<CommentData> getComments(int ticketId) throws SQLException {
        try (Connection connection = storage.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(""
                    + "SELECT * FROM `comment` WHERE `ticket` = ?")) {
                preparedStatement.setInt(1, ticketId);
                
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    Collection<CommentData> comments = Sets.newTreeSet();
                    while (resultSet.next()) {
                        CommentData comment = new CommentData();
                        comment.setId(resultSet.getInt("id"));
                        comment.setTicket(resultSet.getInt("ticket"));
                        comment.setUser(UUID.fromString(resultSet.getString("user")));
                        comment.setTimestamp(resultSet.getTimestamp("timestamp").toInstant());
                        comment.setText(resultSet.getString("text"));
                        comments.add(comment);
                    }
                    
                    return comments;
                }
            }
        }
    }
    
    public TicketData getTicket(int ticketId) throws SQLException {
        try (Connection connection = storage.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(""
                    + "SELECT * FROM `ticket` WHERE `id` = ? LIMIT 0, 1")) {
                preparedStatement.setInt(1, ticketId);
                
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    
                    TicketData ticket = new TicketData();
                    ticket.setId(resultSet.getInt("id"));
                    ticket.setUser(UUID.fromString(resultSet.getString("user")));
                    ticket.setTimestamp(resultSet.getTimestamp("timestamp").toInstant());
                    ticket.setLocation(Toolbox.parseJson(resultSet.getString("location"), LocationData.class).orElse(null));
                    ticket.setText(resultSet.getString("text"));
                    ticket.setStatus(resultSet.getInt("status"));
                    ticket.setRead(resultSet.getBoolean("read"));
                    ticket.setComments(Sets.newTreeSet());
                    return ticket;
                }
            }
        }
    }
    
    public Collection<Integer> getOpenTickets() throws SQLException {
        try (Connection connection = storage.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(""
                    + "SELECT `id` FROM `ticket` WHERE `status` = ?")) {
                
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    Collection<Integer> ticketIds = Sets.newTreeSet();
                    while (resultSet.next()) {
                        ticketIds.add(resultSet.getInt("id"));
                    }
                    
                    return ticketIds;
                }
            }
        }
    }
    
    public Collection<Integer> getUnreadTickets(UUID uniqueId) throws SQLException {
        try (Connection connection = storage.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(""
                    + "SELECT `id` FROM `ticket` WHERE `user` = ? AND `status` = ? AND `read` = ?")) {
                preparedStatement.setString(1, uniqueId.toString());
                preparedStatement.setInt(2, 1);
                preparedStatement.setInt(3, 0);
                
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    Collection<Integer> ticketIds = Sets.newTreeSet();
                    while (resultSet.next()) {
                        ticketIds.add(resultSet.getInt("id"));
                    }
                    
                    return ticketIds;
                }
            }
        }
    }
    
    public UserData getUser(UUID uniqueId) throws SQLException {
        try (Connection connection = storage.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(""
                    + "SELECT `name`, `banned` FROM `user` WHERE `unique_id` = ? LIMIT 0, 1")) {
                preparedStatement.setString(1, uniqueId.toString());
                
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    
                    UserData user = new UserData();
                    user.setUniqueId(uniqueId);
                    user.setName(resultSet.getString("name"));
                    user.setBanned(resultSet.getBoolean("banned"));
                    return user;
                }
            }
        }
    }
    
    public Collection<UUID> getUsers(String name) throws SQLException {
        try (Connection connection = storage.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(""
                    + "SELECT `unique_id` FROM `user` WHERE `name` = ?")) {
                preparedStatement.setString(1, name);
                
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    Collection<UUID> uniqueIds = Sets.newHashSet();
                    while (resultSet.next()) {
                        uniqueIds.add(UUID.fromString(resultSet.getString("unique_id")));
                    }
                    
                    return uniqueIds;
                }
            }
        }
    }
    
    public boolean updateTicket(TicketData ticket) {
        try (Connection connection = storage.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(""
                    + "UPDATE `ticket` SET `read` = ?, `status` = ? WHERE `id` = ?")) {
                preparedStatement.setBoolean(1, ticket.isRead());
                preparedStatement.setInt(2, ticket.getStatus());
                preparedStatement.setInt(3, ticket.getId());
                return preparedStatement.executeUpdate() != 0;
            }
        } catch (SQLException ex) {
            Ticket.getInstance().getLogger().error("Encountered an error processing MySQLQuery::updateTicket");
            ex.printStackTrace();
            return false;
        }
    }
    
    public boolean updateUser(UserData user) {
        try (Connection connection = storage.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(""
                    + "UPDATE `user` SET `banned` = ?, `name` = ? WHERE `unique_id` = ?")) {
                preparedStatement.setBoolean(1, user.isBanned());
                preparedStatement.setString(2, user.getName());
                preparedStatement.setString(3, user.getUniqueId().toString());
                return preparedStatement.executeUpdate() != 0;
            }
        } catch (SQLException ex) {
            Ticket.getInstance().getLogger().error("Encountered an error processing MySQLQuery::updateUser");
            ex.printStackTrace();
            return false;
        }
    }
}