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
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.api.util.Reference;
import nz.co.lolnet.ticket.bungee.BungeePlugin;
import nz.co.lolnet.ticket.bungee.util.BungeeToolbox;
import nz.co.lolnet.ticket.common.command.AbstractCommand;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.storage.mysql.MySQLQuery;
import nz.co.lolnet.ticket.common.util.Toolbox;

import java.util.List;
import java.util.Set;

public class PardonCommand extends AbstractCommand {
    
    public PardonCommand() {
        addAlias("pardon");
        addAlias("unban");
        setPermission("ticket.command.pardon");
        setUsage("<Player>");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSender sender = (CommandSender) object;
        if (arguments.size() != 1) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Invalid arguments: " + getUsage()).color(ChatColor.RED).create());
            return;
        }
        
        String data = arguments.remove(0);
        if (data.length() == 36) {
            UserData user = Toolbox.parseUUID(data).flatMap(DataManager::getUser).orElse(null);
            if (user == null) {
                sender.sendMessage(BungeeToolbox.getTextPrefix().append("User doesn't exist").color(ChatColor.RED).create());
                return;
            }
            
            if (!user.isBanned()) {
                sender.sendMessage(BungeeToolbox.getTextPrefix().append(user.getName() + " is not banned").color(ChatColor.RED).create());
                return;
            }
            
            user.setBanned(false);
            MySQLQuery.updateUser(user);
            sender.sendMessage(BungeeToolbox.getTextPrefix().append(user.getName() + " pardoned").color(ChatColor.GREEN).create());
            return;
        }
        
        if (data.length() < 3 || data.length() > 16) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Invalid username length").color(ChatColor.RED).create());
            return;
        }
        
        ProxiedPlayer player = BungeePlugin.getInstance().getProxy().getPlayer(data);
        if (player != null) {
            UserData user = DataManager.getCachedUser(player.getUniqueId()).orElse(null);
            if (user == null) {
                sender.sendMessage(BungeeToolbox.getTextPrefix().append("User doesn't exist").color(ChatColor.RED).create());
                return;
            }
            
            user.setBanned(false);
            MySQLQuery.updateUser(user);
            sender.sendMessage(BungeeToolbox.getTextPrefix().append(user.getName() + " pardoned").color(ChatColor.GREEN).create());
            return;
        }
        
        Set<UserData> users = DataManager.getUsers(data).orElse(null);
        if (users == null || users.isEmpty()) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("User doesn't exist").color(ChatColor.GREEN).create());
            return;
        }
        
        sender.sendMessage(BungeeToolbox.getTextPrefix().append(" Users").color(ChatColor.GREEN).create());
        for (UserData user : users) {
            ComponentBuilder componentBuilder = new ComponentBuilder("");
            componentBuilder.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + Reference.ID + " " + getPrimaryAlias().orElse("unknown") + " " + user.getUniqueId()));
            componentBuilder.append("> ").color(ChatColor.BLUE);
            
            componentBuilder.append(user.getName()).append(" (").append(user.getUniqueId().toString()).append(")");
            if (user.isBanned()) {
                componentBuilder.color(ChatColor.RED);
            } else {
                componentBuilder.color(ChatColor.GREEN);
            }
        }
    }
}