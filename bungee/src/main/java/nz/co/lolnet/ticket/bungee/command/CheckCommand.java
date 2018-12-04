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
import net.md_5.bungee.api.chat.ComponentBuilder;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.CommentData;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.bungee.util.BungeeToolbox;
import nz.co.lolnet.ticket.common.command.AbstractCommand;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.util.Toolbox;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Set;

public class CheckCommand extends AbstractCommand {
    
    public CheckCommand() {
        addAlias("check");
        setPermission("ticket.command.check");
        setUsage("[Id]");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSender sender = (CommandSender) object;
        
        if (arguments.isEmpty()) {
            Set<TicketData> tickets = DataManager.getCachedOpenTickets();
            if (tickets.isEmpty()) {
                sender.sendMessage(BungeeToolbox.getTextPrefix().append("There are no open tickets").color(ChatColor.YELLOW).create());
                return;
            }
            
            for (TicketData ticket : tickets) {
                UserData user = DataManager.getCachedUser(ticket.getUser()).orElse(null);
                if (user == null) {
                    sender.sendMessage(new ComponentBuilder("User is not cached").color(ChatColor.RED).create());
                    continue;
                }
                
                ComponentBuilder componentBuilder = new ComponentBuilder("")
                        .append("#" + ticket.getId()).color(ChatColor.GOLD)
                        .append(" " + Toolbox.getShortTimeString(System.currentTimeMillis() - ticket.getTimestamp().toEpochMilli())).color(ChatColor.GREEN)
                        .append(" by ").color(ChatColor.GOLD).append(user.getName());
                
                if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
                    componentBuilder.color(ChatColor.GREEN);
                } else {
                    componentBuilder.color(ChatColor.RED);
                }
                
                componentBuilder.append(" - ").color(ChatColor.GOLD);
                componentBuilder.append(Toolbox.substring(ticket.getText(), 20)).color(ChatColor.GRAY);
                sender.sendMessage(componentBuilder.create());
            }
            
            return;
        }
        
        Integer ticketId = Toolbox.parseInteger(arguments.remove(0)).orElse(null);
        if (ticketId == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Failed to parse argument").color(ChatColor.RED).create());
            return;
        }
        
        TicketData ticket = DataManager.getTicket(ticketId).orElse(null);
        if (ticket == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Ticket doesn't exist").color(ChatColor.RED).create());
            return;
        }
        
        ComponentBuilder componentBuilder = new ComponentBuilder("");
        componentBuilder.append("---[").color(ChatColor.GRAY);
        componentBuilder.append("Ticket #" + ticket.getId()).color(ChatColor.GOLD);
        componentBuilder.append("]---").color(ChatColor.GRAY);
        
        componentBuilder.append("\n");
        
        componentBuilder.append("Status: ").color(ChatColor.WHITE);
        
        if (ticket.getStatus() == 0) {
            componentBuilder.append("Open").color(ChatColor.GREEN);
        } else if (ticket.getStatus() == 1) {
            componentBuilder.append("Closed").color(ChatColor.RED);
        }
        
        componentBuilder.append("\n");
        componentBuilder.append("Location: ").color(ChatColor.WHITE);
        
        if (ticket.getLocation().getX() != null && ticket.getLocation().getY() != null && ticket.getLocation().getZ() != null) {
            componentBuilder.append("" + Toolbox.formatDecimal(ticket.getLocation().getX(), 3)).color(ChatColor.GRAY).append(", ").color(ChatColor.DARK_GRAY);
            componentBuilder.append("" + Toolbox.formatDecimal(ticket.getLocation().getY(), 3)).color(ChatColor.GRAY).append(", ").color(ChatColor.DARK_GRAY);
            componentBuilder.append("" + Toolbox.formatDecimal(ticket.getLocation().getZ(), 3)).color(ChatColor.GRAY).append(" @ ").color(ChatColor.DARK_GRAY);
        }
        
        if (ticket.getLocation().getDimension() != null && StringUtils.isNotBlank(ticket.getLocation().getServer())) {
            componentBuilder.append(ticket.getLocation().getServer()).color(ChatColor.GRAY);
            componentBuilder.append(" (").color(ChatColor.DARK_GRAY).append("" + ticket.getLocation().getDimension()).color(ChatColor.GRAY).append(")").color(ChatColor.DARK_GRAY);
        }
        
        componentBuilder.append("\n");
        
        componentBuilder.append("Message: ").color(ChatColor.WHITE).append(ticket.getText()).color(ChatColor.GRAY);
        componentBuilder.append("\n");
        
        componentBuilder.append("Comments: ").color(ChatColor.WHITE);
        for (CommentData comment : ticket.getComments()) {
            UserData user = DataManager.getUser(ticket.getUser()).orElse(null);
            if (user == null) {
                // TODO Error Handling
                continue;
            }
            
            componentBuilder.append("\n");
            componentBuilder.append(Toolbox.getShortTimeString(System.currentTimeMillis() - comment.getTimestamp().toEpochMilli())).color(ChatColor.GRAY);
            componentBuilder.append(" by ").color(ChatColor.DARK_GRAY);
            componentBuilder.append(user.getName());
            if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
                componentBuilder.color(ChatColor.GREEN);
            } else {
                componentBuilder.color(ChatColor.RED);
            }
            
            componentBuilder.append(": ").append(comment.getText()).color(ChatColor.GRAY);
        }
        
        sender.sendMessage(componentBuilder.create());
    }
}