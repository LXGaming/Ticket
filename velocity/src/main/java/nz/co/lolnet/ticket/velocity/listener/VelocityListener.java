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

package nz.co.lolnet.ticket.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import net.kyori.text.TextComponent;
import net.kyori.text.format.TextColor;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.TicketData;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.common.TicketImpl;
import nz.co.lolnet.ticket.common.configuration.Config;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.storage.mysql.MySQLQuery;
import nz.co.lolnet.ticket.common.util.Toolbox;
import nz.co.lolnet.ticket.velocity.VelocityPlugin;
import nz.co.lolnet.ticket.velocity.util.VelocityToolbox;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;
import java.util.concurrent.TimeUnit;

public class VelocityListener {
    
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        VelocityPlugin.getInstance().getProxy().getScheduler().buildTask(VelocityPlugin.getInstance(), () -> {
            if (!event.getPlayer().isActive()) {
                return;
            }
            
            UserData user = DataManager.getUser(event.getPlayer().getUniqueId()).orElse(null);
            if (user == null) {
                return;
            }
            
            if (!StringUtils.equals(user.getName(), event.getPlayer().getUsername())) {
                Ticket.getInstance().getLogger().debug("Updating username: {} -> {}", user.getName(), event.getPlayer().getUsername());
                user.setName(event.getPlayer().getUsername());
                if (!MySQLQuery.updateUser(user)) {
                    Ticket.getInstance().getLogger().warn("Failed to update {} ({})", user.getName(), user.getUniqueId());
                }
            }
            
            Set<TicketData> tickets = DataManager.getUnreadTickets(user.getUniqueId()).orElse(null);
            if (tickets == null || tickets.isEmpty()) {
                return;
            }
            
            event.getPlayer().sendMessage(VelocityToolbox.getTextPrefix()
                    .append(TextComponent.of("You have " + tickets.size() + " unread " + Toolbox.formatUnit(tickets.size(), "ticket", "tickets"), TextColor.GOLD)));
        }).delay(TicketImpl.getInstance().getConfig().map(Config::getLoginDelay).orElse(0L), TimeUnit.MILLISECONDS).schedule();
    }
    
    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        // Forces the expiry to be recalculated
        DataManager.getCachedUser(event.getPlayer().getUniqueId());
        DataManager.getCachedUnreadTickets(event.getPlayer().getUniqueId());
    }
}