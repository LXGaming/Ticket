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

package io.github.lxgaming.ticket.bungee.listener;

import com.google.common.collect.Lists;
import io.github.lxgaming.ticket.api.Ticket;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.api.util.Reference;
import io.github.lxgaming.ticket.bungee.BungeePlugin;
import io.github.lxgaming.ticket.bungee.util.BungeeToolbox;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import io.github.lxgaming.ticket.common.configuration.Config;
import io.github.lxgaming.ticket.common.manager.CommandManager;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.util.Toolbox;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class BungeeListener implements Listener {
    
    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        BungeePlugin.getInstance().getProxy().getScheduler().schedule(BungeePlugin.getInstance(), () -> {
            if (!event.getPlayer().isConnected()) {
                return;
            }
            
            if (event.getPlayer().hasPermission("ticket.read.others")) {
                Collection<TicketData> openTickets = DataManager.getCachedOpenTickets();
                if (!openTickets.isEmpty()) {
                    ComponentBuilder componentBuilder = new ComponentBuilder("");
                    componentBuilder.append(BungeeToolbox.getTextPrefix().create());
                    componentBuilder.append(new ComponentBuilder("")
                            .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + Reference.ID + " read"))
                            .append("There " + Toolbox.formatUnit(openTickets.size(), "is", "are") + " currently " + openTickets.size() + " open " + Toolbox.formatUnit(openTickets.size(), "ticket", "tickets")).color(ChatColor.GOLD)
                            .create());
                    
                    event.getPlayer().sendMessage(componentBuilder.create());
                }
            }
            
            UserData user = DataManager.getUser(event.getPlayer().getUniqueId()).orElse(null);
            if (user == null) {
                return;
            }
            
            if (!StringUtils.equals(user.getName(), event.getPlayer().getName())) {
                Ticket.getInstance().getLogger().debug("Updating username: {} -> {}", user.getName(), event.getPlayer().getName());
                user.setName(event.getPlayer().getName());
                if (!TicketImpl.getInstance().getStorage().getQuery().updateUser(user)) {
                    Ticket.getInstance().getLogger().warn("Failed to update {} ({})", user.getName(), user.getUniqueId());
                }
            }
            
            Collection<TicketData> tickets = DataManager.getUnreadTickets(user.getUniqueId()).orElse(null);
            if (tickets == null || tickets.isEmpty()) {
                return;
            }
            
            ComponentBuilder componentBuilder = new ComponentBuilder("");
            componentBuilder.append(BungeeToolbox.getTextPrefix().create());
            componentBuilder.append(new ComponentBuilder("")
                    .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + Reference.ID + " read"))
                    .append("You have " + tickets.size() + " unread " + Toolbox.formatUnit(tickets.size(), "ticket", "tickets")).color(ChatColor.GOLD)
                    .create());
            
            event.getPlayer().sendMessage(componentBuilder.create());
        }, TicketImpl.getInstance().getConfig().map(Config::getLoginDelay).orElse(0L), TimeUnit.MILLISECONDS);
    }
    
    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        // Forces the expiry to be recalculated
        DataManager.getCachedUser(event.getPlayer().getUniqueId());
        DataManager.getCachedUnreadTickets(event.getPlayer().getUniqueId());
    }
    
    @EventHandler
    public void onPlayerChat(ChatEvent event) {
        if (!event.isCommand() || !(event.getSender() instanceof ProxiedPlayer)) {
            return;
        }
        
        ProxiedPlayer sender = (ProxiedPlayer) event.getSender();
        List<String> arguments = Lists.newArrayList(StringUtils.split(StringUtils.removeStartIgnoreCase(event.getMessage(), "/"), " "));
        if (arguments.isEmpty()) {
            return;
        }
        
        String alias = TicketImpl.getInstance().getLegacyCommands().get(arguments.remove(0));
        if (StringUtils.isBlank(alias)) {
            return;
        }
        
        arguments.add(0, alias);
        AbstractCommand command = CommandManager.getChildCommand(arguments).orElse(null);
        if (command == null) {
            return;
        }
        
        event.setCancelled(true);
        if (StringUtils.isBlank(command.getPermission()) || !sender.hasPermission(command.getPermission())) {
            sender.sendMessage(new ComponentBuilder("You do not have permission to execute this command!").color(ChatColor.RED).create());
            return;
        }
        
        Ticket.getInstance().getLogger().debug("Processing {}", command.getPrimaryAlias().orElse("Unknown"));
        
        try {
            command.execute(sender, arguments);
        } catch (Throwable throwable) {
            Ticket.getInstance().getLogger().error("Encountered an error while executing {}", command.getClass().getSimpleName(), throwable);
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error has occurred. Details are available in console.").color(ChatColor.RED).create());
        }
    }
}