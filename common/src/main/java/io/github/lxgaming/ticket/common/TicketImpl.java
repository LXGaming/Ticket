/*
 * Copyright 2018 Alex Thomson
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

package io.github.lxgaming.ticket.common;

import com.google.common.collect.Maps;
import io.github.lxgaming.ticket.api.Platform;
import io.github.lxgaming.ticket.api.Ticket;
import io.github.lxgaming.ticket.api.util.Reference;
import io.github.lxgaming.ticket.common.configuration.Config;
import io.github.lxgaming.ticket.common.configuration.Configuration;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.storage.Storage;
import io.github.lxgaming.ticket.common.storage.mysql.MySQLStorage;
import io.github.lxgaming.ticket.common.util.LoggerImpl;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;

public class TicketImpl extends Ticket {
    
    private final Configuration configuration;
    private final Storage storage;
    private final Map<String, String> legacyCommands;
    
    public TicketImpl(Platform platform) {
        super();
        this.platform = platform;
        this.logger = new LoggerImpl();
        this.configuration = new Configuration();
        this.storage = new MySQLStorage();
        this.legacyCommands = Maps.newHashMap();
    }
    
    public void loadTicket() {
        getLogger().info("Initializing...");
        reloadTicket();
        getLogger().info("{} v{} has loaded", Reference.NAME, Reference.VERSION);
    }
    
    public boolean reloadTicket() {
        getConfiguration().loadConfiguration();
        if (!getConfig().isPresent()) {
            return false;
        }
        
        getConfiguration().saveConfiguration();
        if (getConfig().map(Config::isDebug).orElse(false)) {
            getLogger().debug("Debug mode enabled");
        } else {
            getLogger().info("Debug mode disabled");
        }
        
        getLegacyCommands().clear();
        getConfig().map(Config::getCommand).ifPresent(command -> {
            if (!command.isLegacy()) {
                return;
            }
            
            getLegacyCommands().put(StringUtils.defaultIfBlank(command.getCloseTicket(), ""), "close");
            getLegacyCommands().put(StringUtils.defaultIfBlank(command.getCommentTicket(), ""), "comment");
            getLegacyCommands().put(StringUtils.defaultIfBlank(command.getOpenTicket(), ""), "open");
            getLegacyCommands().put(StringUtils.defaultIfBlank(command.getReadTicket(), ""), "read");
            getLegacyCommands().put(StringUtils.defaultIfBlank(command.getReopenTicket(), ""), "reopen");
        });
        
        try {
            if (!getStorage().connect()) {
                getLogger().error("Connection failed");
                return false;
            }
            
            if (!getStorage().getQuery().createTables()) {
                getLogger().error("Failed to create tables");
                return false;
            }
        } catch (Exception ex) {
            getLogger().error("Encountered an error while connecting to {}", getStorage().getClass().getSimpleName(), ex);
            return false;
        }
        
        DataManager.getTicketCache().invalidateAll();
        DataManager.getUserCache().invalidateAll();
        DataManager.getOpenTickets().ifPresent(tickets -> {
            tickets.forEach(ticket -> DataManager.getUser(ticket.getUser()));
            getLogger().info("Loaded {} open tickets", tickets.size());
        });
        
        return true;
    }
    
    public static TicketImpl getInstance() {
        return (TicketImpl) Ticket.getInstance();
    }
    
    public Configuration getConfiguration() {
        return configuration;
    }
    
    public Optional<? extends Config> getConfig() {
        if (getConfiguration() != null) {
            return Optional.ofNullable(getConfiguration().getConfig());
        }
        
        return Optional.empty();
    }
    
    public Storage getStorage() {
        return storage;
    }
    
    public Map<String, String> getLegacyCommands() {
        return legacyCommands;
    }
}