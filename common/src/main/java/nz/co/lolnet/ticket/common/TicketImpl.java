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

package nz.co.lolnet.ticket.common;

import nz.co.lolnet.ticket.api.Platform;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.util.Reference;
import nz.co.lolnet.ticket.common.configuration.Config;
import nz.co.lolnet.ticket.common.configuration.Configuration;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.storage.mysql.MySQLQuery;
import nz.co.lolnet.ticket.common.util.LoggerImpl;

import java.util.Optional;
import java.util.Set;

public class TicketImpl extends Ticket {
    
    private final Configuration configuration;
    
    public TicketImpl(Platform platform) {
        super();
        this.platform = platform;
        this.logger = new LoggerImpl();
        this.configuration = new Configuration();
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
        
        if (!MySQLQuery.createTables()) {
            return false;
        }
        
        DataManager.getTicketCache().invalidateAll();
        DataManager.getUserCache().invalidateAll();
        Set<TicketData> tickets = DataManager.getOpenTickets().orElse(null);
        if (tickets != null && !tickets.isEmpty()) {
            tickets.forEach(ticket -> DataManager.getUser(ticket.getUser()));
            getLogger().info("Loaded {} open tickets", tickets.size());
        }
        
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
}