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

package io.github.lxgaming.ticket.bungee.command;

import io.github.lxgaming.ticket.api.Ticket;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.bungee.util.BungeeToolbox;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import io.github.lxgaming.ticket.common.configuration.Configuration;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.util.Toolbox;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

import java.util.List;
import java.util.UUID;

public class PardonCommand extends AbstractCommand {
    
    public PardonCommand() {
        addAlias("pardon");
        addAlias("unban");
        setDescription("Pardons a user allowing them to create Tickets");
        setPermission("ticket.pardon.base");
        setUsage("<UniqueId>");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSender sender = (CommandSender) object;
        if (arguments.size() != 1) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Invalid arguments: " + getUsage()).color(ChatColor.RED).create());
            return;
        }
        
        String data = arguments.remove(0);
        if (data.length() != 36) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Invalid argument length").color(ChatColor.RED).create());
            return;
        }
        
        UUID uniqueId = Toolbox.parseUUID(data).orElse(null);
        if (uniqueId == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Failed to parse unique id").color(ChatColor.RED).create());
            return;
        }
        
        if (BungeeToolbox.getUniqueId(sender).equals(uniqueId)) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("You cannot pardon yourself").color(ChatColor.RED).create());
            return;
        }
        
        UserData user = DataManager.getUser(uniqueId).orElse(null);
        if (user == null) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Failed to find user").color(ChatColor.RED).create());
            return;
        }
        
        if (!user.isBanned()) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append(user.getName()).color(ChatColor.YELLOW).append(" is not banned").color(ChatColor.GREEN).create());
            return;
        }
        
        user.setBanned(false);
        if (TicketImpl.getInstance().getStorage().getQuery().updateUser(user)) {
            BungeeToolbox.sendRedisMessage("UserPardon", jsonObject -> {
                jsonObject.add("user", Configuration.getGson().toJsonTree(user));
                jsonObject.addProperty("by", Ticket.getInstance().getPlatform().getUsername(BungeeToolbox.getUniqueId(sender)).orElse("Unknown"));
            });
            
            BungeeToolbox.broadcast(null, "ticket.pardon.notify", BungeeToolbox.getTextPrefix()
                    .append(user.getName()).color(ChatColor.YELLOW)
                    .append(" was pardoned by ").color(ChatColor.GREEN)
                    .append(Ticket.getInstance().getPlatform().getUsername(BungeeToolbox.getUniqueId(sender)).orElse("Unknown")).color(ChatColor.YELLOW).create());
        } else {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Failed to update ").color(ChatColor.RED).append(user.getName()).color(ChatColor.YELLOW).create());
        }
    }
}