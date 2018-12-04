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
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.storage.mysql.MySQLQuery;

public class VelocityListener {
    
    @Subscribe
    public void onPostLogin(PostLoginEvent event) {
        Ticket.getInstance().getLogger().info("onPostLogin");
        UserData user = DataManager.getUser(event.getPlayer().getUniqueId()).orElse(null);
        if (user == null) {
            return;
        }
        
        if (!user.getName().equals(event.getPlayer().getUsername())) {
            user.setName(event.getPlayer().getUsername());
            MySQLQuery.updateUser(user);
        }
        
        // TODO Handle Open Tickets.
    }
    
    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        Ticket.getInstance().getLogger().info("onPlayerDisconnect");
        DataManager.getCachedUser(event.getPlayer().getUniqueId()).orElse(null);
    }
}