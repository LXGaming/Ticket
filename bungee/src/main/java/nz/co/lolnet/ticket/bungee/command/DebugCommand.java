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

package nz.co.lolnet.ticket.bungee.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.bungee.util.BungeeToolbox;
import nz.co.lolnet.ticket.common.TicketImpl;
import nz.co.lolnet.ticket.common.command.AbstractCommand;
import nz.co.lolnet.ticket.common.configuration.Config;
import nz.co.lolnet.ticket.common.manager.DataManager;

import java.util.List;

public class DebugCommand extends AbstractCommand {
    
    public DebugCommand() {
        addAlias("debug");
        setDescription("For debugging purposes");
        setPermission("ticket.debug.base");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSender sender = (CommandSender) object;
        Config config = TicketImpl.getInstance().getConfig().orElse(null);
        if (config == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Configuration error").color(ChatColor.RED).create());
            return;
        }
        
        if (arguments.isEmpty()) {
            if (config.isDebug()) {
                config.setDebug(false);
                sender.sendMessage(BungeeToolbox.getTextPrefix().append("Debugging disabled").color(ChatColor.RED).create());
            } else {
                config.setDebug(true);
                sender.sendMessage(BungeeToolbox.getTextPrefix().append("Debugging enabled").color(ChatColor.GREEN).create());
            }
            
            return;
        }
        
        String command = arguments.remove(0);
        if (command.equalsIgnoreCase("cache")) {
            Ticket.getInstance().getLogger().debug("Ticket Cache:");
            Ticket.getInstance().getLogger().debug("- Average Load Penalty: {}", DataManager.getTicketCache().stats().averageLoadPenalty());
            Ticket.getInstance().getLogger().debug("- Eviction Count: {}", DataManager.getTicketCache().stats().evictionCount());
            Ticket.getInstance().getLogger().debug("- Eviction Weight: {}", DataManager.getTicketCache().stats().evictionWeight());
            Ticket.getInstance().getLogger().debug("- Hit Count: {}", DataManager.getTicketCache().stats().hitCount());
            Ticket.getInstance().getLogger().debug("- Hit Rate: {}", DataManager.getTicketCache().stats().hitRate());
            Ticket.getInstance().getLogger().debug("- Load Count: {}", DataManager.getTicketCache().stats().loadCount());
            Ticket.getInstance().getLogger().debug("- Load Failure Count: {}", DataManager.getTicketCache().stats().loadFailureCount());
            Ticket.getInstance().getLogger().debug("- Load Failure Rate: {}", DataManager.getTicketCache().stats().loadFailureRate());
            Ticket.getInstance().getLogger().debug("- Load Success Count: {}", DataManager.getTicketCache().stats().loadSuccessCount());
            Ticket.getInstance().getLogger().debug("- Miss Count: {}", DataManager.getTicketCache().stats().missCount());
            Ticket.getInstance().getLogger().debug("- Miss Rate: {}", DataManager.getTicketCache().stats().missRate());
            Ticket.getInstance().getLogger().debug("- Request Count: {}", DataManager.getTicketCache().stats().requestCount());
            Ticket.getInstance().getLogger().debug("- Total Load Time: {}", DataManager.getTicketCache().stats().totalLoadTime());
            
            Ticket.getInstance().getLogger().debug("User Cache:");
            Ticket.getInstance().getLogger().debug("- Average Load Penalty: {}", DataManager.getUserCache().stats().averageLoadPenalty());
            Ticket.getInstance().getLogger().debug("- Eviction Count: {}", DataManager.getUserCache().stats().evictionCount());
            Ticket.getInstance().getLogger().debug("- Eviction Weight: {}", DataManager.getUserCache().stats().evictionWeight());
            Ticket.getInstance().getLogger().debug("- Hit Count: {}", DataManager.getUserCache().stats().hitCount());
            Ticket.getInstance().getLogger().debug("- Hit Rate: {}", DataManager.getUserCache().stats().hitRate());
            Ticket.getInstance().getLogger().debug("- Load Count: {}", DataManager.getUserCache().stats().loadCount());
            Ticket.getInstance().getLogger().debug("- Load Failure Count: {}", DataManager.getUserCache().stats().loadFailureCount());
            Ticket.getInstance().getLogger().debug("- Load Failure Rate: {}", DataManager.getUserCache().stats().loadFailureRate());
            Ticket.getInstance().getLogger().debug("- Load Success Count: {}", DataManager.getUserCache().stats().loadSuccessCount());
            Ticket.getInstance().getLogger().debug("- Miss Count: {}", DataManager.getUserCache().stats().missCount());
            Ticket.getInstance().getLogger().debug("- Miss Rate: {}", DataManager.getUserCache().stats().missRate());
            Ticket.getInstance().getLogger().debug("- Request Count: {}", DataManager.getUserCache().stats().requestCount());
            Ticket.getInstance().getLogger().debug("- Total Load Time: {}", DataManager.getUserCache().stats().totalLoadTime());
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Cache information dumped to console.").color(ChatColor.AQUA).create());
        }
    }
}