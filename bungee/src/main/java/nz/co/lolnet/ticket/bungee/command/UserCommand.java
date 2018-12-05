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
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.api.util.Reference;
import nz.co.lolnet.ticket.bungee.util.BungeeToolbox;
import nz.co.lolnet.ticket.common.command.AbstractCommand;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.util.Toolbox;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class UserCommand extends AbstractCommand {
    
    public UserCommand() {
        addAlias("user");
        setPermission("ticket.user.base");
        setUsage("<User>");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSender sender = (CommandSender) object;
        if (arguments.size() != 1) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Invalid arguments: " + getUsage()).color(ChatColor.RED).create());
            return;
        }
        
        String data = arguments.remove(0);
        if (data.length() < 3 || (data.length() > 16 && data.length() != 36)) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Invalid argument length").color(ChatColor.RED).create());
            return;
        }
        
        if (data.length() == 36) {
            UUID uniqueId = Toolbox.parseUUID(data).orElse(null);
            if (uniqueId == null) {
                sender.sendMessage(BungeeToolbox.getTextPrefix().append("Failed to parse unique id").color(ChatColor.RED).create());
                return;
            }
            
            UserData user = DataManager.getUser(uniqueId).orElse(null);
            if (user == null) {
                sender.sendMessage(BungeeToolbox.getTextPrefix().append("Failed to find user").color(ChatColor.RED).create());
                return;
            }
            
            sender.sendMessage(buildUserInfo(user));
            return;
        }
        
        Set<UserData> users = DataManager.getUsers(data).orElse(null);
        if (users == null || users.isEmpty()) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Fail to find users by the name of ").color(ChatColor.RED).append(data).color(ChatColor.YELLOW).create());
            return;
        }
        
        if (users.size() == 1) {
            sender.sendMessage(buildUserInfo(users.iterator().next()));
            return;
        }
        
        sender.sendMessage(new ComponentBuilder("")
                .append("----------").color(ChatColor.GREEN).strikethrough(true)
                .append(" Users ").color(ChatColor.GREEN).strikethrough(false)
                .append("----------").color(ChatColor.GREEN).strikethrough(true)
                .create());
        for (UserData user : users) {
            ComponentBuilder componentBuilder = new ComponentBuilder("");
            componentBuilder.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + Reference.ID + " " + getPrimaryAlias().orElse("unknown") + " " + user.getUniqueId()));
            componentBuilder.append("> ").color(ChatColor.BLUE);
            
            if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
                componentBuilder.append(user.getName()).color(ChatColor.GREEN);
            } else {
                componentBuilder.append(user.getName()).color(ChatColor.RED);
            }
            
            componentBuilder.append(" (").color(ChatColor.DARK_GRAY).append(user.getUniqueId().toString()).color(ChatColor.GRAY).append(")").color(ChatColor.DARK_GRAY);
            sender.sendMessage(componentBuilder.create());
        }
    }
    
    private BaseComponent[] buildUserInfo(UserData user) {
        ComponentBuilder componentBuilder = new ComponentBuilder("");
        componentBuilder.append("----------").color(ChatColor.GREEN).strikethrough(true);
        componentBuilder.append(" User Info: ").color(ChatColor.GREEN).strikethrough(false);
        componentBuilder.append(user.getName() + " ").color(ChatColor.YELLOW);
        componentBuilder.append("----------").color(ChatColor.GREEN).strikethrough(true);
        componentBuilder.append("\n", ComponentBuilder.FormatRetention.NONE);
        
        componentBuilder.append("UUID").color(ChatColor.AQUA).append(": " + user.getUniqueId()).color(ChatColor.WHITE);
        componentBuilder.append("\n");
        
        componentBuilder.append("Online").color(ChatColor.AQUA).append(": ").color(ChatColor.WHITE);
        if (Ticket.getInstance().getPlatform().isOnline(user.getUniqueId())) {
            componentBuilder.append("Yes").color(ChatColor.GREEN);
        } else {
            componentBuilder.append("No").color(ChatColor.RED);
        }
        
        componentBuilder.append("\n");
        componentBuilder.append("Banned").color(ChatColor.AQUA).append(": ").color(ChatColor.WHITE);
        if (user.isBanned()) {
            componentBuilder.append("Yes").color(ChatColor.RED);
        } else {
            componentBuilder.append("No").color(ChatColor.GREEN);
        }
        
        componentBuilder.append("\n\n");
        componentBuilder.append(new ComponentBuilder("")
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + Reference.ID + " ban " + user.getUniqueId()))
                .append("[").color(ChatColor.GOLD).append("Ban").color(ChatColor.RED).append("]").color(ChatColor.GOLD)
                .create());
        
        componentBuilder.append(" ", ComponentBuilder.FormatRetention.NONE);
        componentBuilder.append(new ComponentBuilder("")
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + Reference.ID + " pardon " + user.getUniqueId()))
                .append("[").color(ChatColor.GOLD).append("Pardon").color(ChatColor.GREEN).append("]").color(ChatColor.GOLD)
                .create());
        
        componentBuilder.append("", ComponentBuilder.FormatRetention.NONE);
        return componentBuilder.create();
    }
}