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

package io.github.lxgaming.ticket.velocity.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import io.github.lxgaming.ticket.api.Ticket;
import io.github.lxgaming.ticket.api.data.TicketData;
import io.github.lxgaming.ticket.api.data.UserData;
import io.github.lxgaming.ticket.api.util.Reference;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.configuration.Config;
import io.github.lxgaming.ticket.common.manager.DataManager;
import io.github.lxgaming.ticket.common.util.Toolbox;
import io.github.lxgaming.ticket.velocity.VelocityPlugin;
import io.github.lxgaming.ticket.velocity.util.VelocityToolbox;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class VelocityListener {
    
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        VelocityPlugin.getInstance().getProxy().getScheduler().buildTask(VelocityPlugin.getInstance(), () -> {
            if (!event.getPlayer().isActive()) {
                return;
            }
            
            if (event.getPlayer().hasPermission("ticket.read.others")) {
                Collection<TicketData> openTickets = DataManager.getCachedOpenTickets();
                if (!openTickets.isEmpty()) {
                    TextComponent.Builder textBuilder = TextComponent.builder("");
                    textBuilder.append(VelocityToolbox.getTextPrefix());
                    textBuilder.append(TextComponent.builder("")
                            .clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, "/" + Reference.ID + " read"))
                            .append(TextComponent.of("There is currently " + openTickets.size() + " open " + Toolbox.formatUnit(openTickets.size(), "ticket", "tickets"), TextColor.GOLD))
                            .build());
                    
                    event.getPlayer().sendMessage(textBuilder.build());
                }
            }
            
            UserData user = DataManager.getUser(event.getPlayer().getUniqueId()).orElse(null);
            if (user == null) {
                return;
            }
            
            if (!StringUtils.equals(user.getName(), event.getPlayer().getUsername())) {
                Ticket.getInstance().getLogger().debug("Updating username: {} -> {}", user.getName(), event.getPlayer().getUsername());
                user.setName(event.getPlayer().getUsername());
                if (!TicketImpl.getInstance().getStorage().getQuery().updateUser(user)) {
                    Ticket.getInstance().getLogger().warn("Failed to update {} ({})", user.getName(), user.getUniqueId());
                }
            }
            
            Collection<TicketData> tickets = DataManager.getUnreadTickets(user.getUniqueId()).orElse(null);
            if (tickets == null || tickets.isEmpty()) {
                return;
            }
            
            TextComponent.Builder textBuilder = TextComponent.builder("");
            textBuilder.append(VelocityToolbox.getTextPrefix());
            textBuilder.append(TextComponent.builder("")
                    .clickEvent(ClickEvent.of(ClickEvent.Action.RUN_COMMAND, "/" + Reference.ID + " read"))
                    .append(TextComponent.of("You have " + tickets.size() + " unread " + Toolbox.formatUnit(tickets.size(), "ticket", "tickets"), TextColor.GOLD))
                    .build());
            
            event.getPlayer().sendMessage(textBuilder.build());
        }).delay(TicketImpl.getInstance().getConfig().map(Config::getLoginDelay).orElse(0L), TimeUnit.MILLISECONDS).schedule();
    }
    
    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        // Forces the expiry to be recalculated
        DataManager.getCachedUser(event.getPlayer().getUniqueId());
        DataManager.getCachedUnreadTickets(event.getPlayer().getUniqueId());
    }
}