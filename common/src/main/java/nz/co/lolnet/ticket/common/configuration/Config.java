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

package nz.co.lolnet.ticket.common.configuration;

import nz.co.lolnet.ticket.common.configuration.category.CommandCategory;
import nz.co.lolnet.ticket.common.configuration.category.StorageCategory;
import nz.co.lolnet.ticket.common.configuration.category.TicketCategory;

import java.util.UUID;

public class Config {
    
    private boolean debug = false;
    private String proxyId = UUID.randomUUID().toString();
    private long loginDelay = 2500L;
    private CommandCategory command = new CommandCategory();
    private TicketCategory ticket = new TicketCategory();
    private StorageCategory storage = new StorageCategory();
    
    public boolean isDebug() {
        return debug;
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    public String getProxyId() {
        return proxyId;
    }
    
    public long getLoginDelay() {
        return loginDelay;
    }
    
    public CommandCategory getCommand() {
        return command;
    }
    
    public TicketCategory getTicket() {
        return ticket;
    }
    
    public StorageCategory getStorage() {
        return storage;
    }
}