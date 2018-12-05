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

package nz.co.lolnet.ticket.bungee.listener;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.bungee.BungeePlugin;
import nz.co.lolnet.ticket.bungee.util.BungeeToolbox;
import nz.co.lolnet.ticket.common.TicketImpl;
import nz.co.lolnet.ticket.common.configuration.Config;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.storage.mysql.MySQLQuery;
import nz.co.lolnet.ticket.common.util.Toolbox;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class BungeeListener implements Listener {
    
    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        BungeePlugin.getInstance().getProxy().getScheduler().schedule(BungeePlugin.getInstance(), () -> {
            if (!event.getPlayer().isConnected()) {
                return;
            }
            
            UserData user = DataManager.getUser(event.getPlayer().getUniqueId()).orElse(null);
            if (user == null) {
                return;
            }
            
            if (!StringUtils.equals(user.getName(), event.getPlayer().getName())) {
                Ticket.getInstance().getLogger().debug("Updating username: {} -> {}", user.getName(), event.getPlayer().getName());
                user.setName(event.getPlayer().getName());
                if (!MySQLQuery.updateUser(user)) {
                    Ticket.getInstance().getLogger().warn("Failed to update {} ({})", user.getName(), user.getUniqueId());
                }
            }
            
            Set<TicketData> tickets = DataManager.getUnreadTickets(user.getUniqueId()).orElse(null);
            if (tickets == null || tickets.isEmpty()) {
                return;
            }
            
            event.getPlayer().sendMessage(BungeeToolbox.getTextPrefix()
                    .append("You have " + tickets.size() + " unread " + Toolbox.formatUnit(tickets.size(), "ticket", "tickets"))
                    .color(ChatColor.GOLD)
                    .create());
        }, TicketImpl.getInstance().getConfig().map(Config::getLoginDelay).orElse(0L), TimeUnit.MILLISECONDS);
    }
    
    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        // Forces the expiry to be recalculated
        DataManager.getCachedUser(event.getPlayer().getUniqueId());
        DataManager.getCachedUnreadTickets(event.getPlayer().getUniqueId());
    }
}