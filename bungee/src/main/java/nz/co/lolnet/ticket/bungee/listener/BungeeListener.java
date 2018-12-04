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

import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.storage.mysql.MySQLQuery;

public class BungeeListener implements Listener {
    
    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        Ticket.getInstance().getLogger().info("onPostLogin");
        UserData user = DataManager.getUser(event.getPlayer().getUniqueId()).orElse(null);
        if (user == null) {
            return;
        }
        
        if (!user.getName().equals(event.getPlayer().getName())) {
            user.setName(event.getPlayer().getName());
            MySQLQuery.updateUser(user);
        }
        
        // TODO Handle Open Tickets.
    }
    
    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        Ticket.getInstance().getLogger().info("onPlayerDisconnect");
        DataManager.getCachedUser(event.getPlayer().getUniqueId()).orElse(null);
    }
}