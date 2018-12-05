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

package nz.co.lolnet.ticket.common.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.collect.Sets;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.CommentData;
import nz.co.lolnet.ticket.api.data.LocationData;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.common.cache.TicketExpiry;
import nz.co.lolnet.ticket.common.cache.UserExpiry;
import nz.co.lolnet.ticket.common.storage.mysql.MySQLQuery;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class DataManager {
    
    private static final Cache<Integer, TicketData> TICKET_CACHE = Caffeine.newBuilder().expireAfter(new TicketExpiry()).build();
    private static final Cache<UUID, UserData> USER_CACHE = Caffeine.newBuilder().expireAfter(new UserExpiry()).build();
    
    public static Optional<UserData> getCachedUser(UUID uniqueId) {
        return Optional.ofNullable(getUserCache().getIfPresent(uniqueId));
    }
    
    public static Optional<UserData> getUser(UUID uniqueId) {
        return Optional.ofNullable(getUserCache().get(uniqueId, key -> {
            try {
                return MySQLQuery.getUser(key).orElse(null);
            } catch (SQLException ex) {
                Ticket.getInstance().getLogger().error("Encountered an error processing DataManager::getUser", ex);
                return null;
            }
        }));
    }
    
    public static Optional<Set<UserData>> getUsers(String name) {
        try {
            Set<UUID> uniqueIds = MySQLQuery.getUsers(name).orElse(null);
            if (uniqueIds == null || uniqueIds.isEmpty()) {
                return Optional.empty();
            }
            
            Set<UserData> users = Sets.newHashSet();
            uniqueIds.forEach(uniqueId -> getUser(uniqueId).map(users::add));
            return Optional.of(users);
        } catch (SQLException ex) {
            return Optional.empty();
        }
    }
    
    public static Optional<UserData> getOrCreateUser(UUID uniqueId) {
        return Optional.ofNullable(getUserCache().get(uniqueId, key -> {
            try {
                UserData user = MySQLQuery.getUser(key).orElse(null);
                if (user != null) {
                    return user;
                }
                
                return MySQLQuery.createUser(key);
            } catch (SQLException ex) {
                Ticket.getInstance().getLogger().error("Encountered an error processing DataManager::getOrCreateUser", ex);
                return null;
            }
        }));
    }
    
    public static Set<TicketData> getCachedOpenTickets(UUID uniqueId) {
        Set<TicketData> tickets = Sets.newHashSet();
        for (TicketData ticket : getTicketCache().asMap().values()) {
            if (ticket.getUser().equals(uniqueId) && ticket.getStatus() == 0) {
                tickets.add(ticket);
            }
        }
        
        return tickets;
    }
    
    public static Set<TicketData> getCachedOpenTickets() {
        Set<TicketData> tickets = Sets.newHashSet();
        for (TicketData ticket : getTicketCache().asMap().values()) {
            if (ticket.getStatus() == 0) {
                tickets.add(ticket);
            }
        }
        
        return tickets;
    }
    
    public static Set<TicketData> getCachedUnreadTickets(UUID uniqueId) {
        Set<TicketData> tickets = Sets.newHashSet();
        for (TicketData ticket : getTicketCache().asMap().values()) {
            if (ticket.getUser().equals(uniqueId) && ticket.getStatus() == 1 && !ticket.isRead()) {
                tickets.add(ticket);
            }
        }
        
        return tickets;
    }
    
    public static Optional<TicketData> getCachedTicket(int ticketId) {
        return Optional.ofNullable(getTicketCache().getIfPresent(ticketId));
    }
    
    public static Optional<Set<TicketData>> getOpenTickets() {
        try {
            Set<Integer> ticketIds = MySQLQuery.getOpenTickets().orElse(null);
            if (ticketIds == null || ticketIds.isEmpty()) {
                return Optional.empty();
            }
            
            Set<TicketData> tickets = Sets.newHashSet();
            ticketIds.forEach(ticketId -> getTicket(ticketId).map(tickets::add));
            return Optional.of(tickets);
        } catch (SQLException ex) {
            return Optional.empty();
        }
    }
    
    public static Optional<Set<TicketData>> getUnreadTickets(UUID uniqueId) {
        try {
            Set<Integer> ticketIds = MySQLQuery.getUnreadTickets(uniqueId).orElse(null);
            if (ticketIds == null || ticketIds.isEmpty()) {
                return Optional.empty();
            }
            
            Set<TicketData> tickets = Sets.newHashSet();
            ticketIds.forEach(ticketId -> getTicket(ticketId).map(tickets::add));
            return Optional.of(tickets);
        } catch (SQLException ex) {
            return Optional.empty();
        }
    }
    
    public static Optional<TicketData> getTicket(int ticketId) {
        return Optional.ofNullable(getTicketCache().get(ticketId, key -> {
            try {
                TicketData ticket = MySQLQuery.getTicket(key).orElse(null);
                if (ticket != null) {
                    MySQLQuery.getComments(ticket.getId()).ifPresent(ticket::setComments);
                }
                
                return ticket;
            } catch (SQLException ex) {
                Ticket.getInstance().getLogger().error("Encountered an error processing DataManager::getTicket", ex);
                return null;
            }
        }));
    }
    
    public static Optional<TicketData> createTicket(UUID uniqueId, Instant timestamp, LocationData location, String text) {
        try {
            TicketData ticket = MySQLQuery.createTicket(uniqueId, timestamp, location, text);
            getTicketCache().put(ticket.getId(), ticket);
            return Optional.of(ticket);
        } catch (SQLException ex) {
            return Optional.empty();
        }
    }
    
    public static Optional<CommentData> createComment(int ticketId, UUID uniqueId, Instant timestamp, String text) {
        try {
            TicketData ticket = getTicket(ticketId).orElse(null);
            if (ticket == null) {
                return Optional.empty();
            }
            
            CommentData comment = MySQLQuery.createComment(ticketId, uniqueId, timestamp, text);
            ticket.getComments().add(comment);
            return Optional.of(comment);
        } catch (SQLException ex) {
            return Optional.empty();
        }
    }
    
    public static Cache<Integer, TicketData> getTicketCache() {
        return TICKET_CACHE;
    }
    
    public static Cache<UUID, UserData> getUserCache() {
        return USER_CACHE;
    }
}