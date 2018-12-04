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

package nz.co.lolnet.ticket.velocity.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.text.TextComponent;
import net.kyori.text.event.ClickEvent;
import net.kyori.text.format.TextColor;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.api.util.Reference;
import nz.co.lolnet.ticket.common.command.AbstractCommand;
import nz.co.lolnet.ticket.common.manager.DataManager;
import nz.co.lolnet.ticket.common.storage.mysql.MySQLQuery;
import nz.co.lolnet.ticket.common.util.Toolbox;
import nz.co.lolnet.ticket.velocity.VelocityPlugin;
import nz.co.lolnet.ticket.velocity.util.VelocityToolbox;

import java.util.List;
import java.util.Set;

public class PardonCommand extends AbstractCommand {
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSource source = (CommandSource) object;
        if (arguments.size() != 1) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Invalid arguments: " + getUsage(), TextColor.RED)));
            return;
        }
        
        String data = arguments.remove(0);
        if (data.length() == 36) {
            UserData user = Toolbox.parseUUID(data).flatMap(DataManager::getUser).orElse(null);
            if (user == null) {
                source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("User doesn't exist", TextColor.RED)));
                return;
            }
            
            if (!user.isBanned()) {
                source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of(user.getName() + " is not banned", TextColor.RED)));
                return;
            }
            
            user.setBanned(false);
            MySQLQuery.updateUser(user);
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of(user.getName() + " pardoned", TextColor.GREEN)));
            return;
        }
        
        if (data.length() < 3 || data.length() > 16) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("Invalid username length", TextColor.RED)));
            return;
        }
        
        Player player = VelocityPlugin.getInstance().getProxy().getPlayer(data).orElse(null);
        if (player != null) {
            UserData user = DataManager.getCachedUser(player.getUniqueId()).orElse(null);
            if (user == null) {
                source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("User doesn't exist", TextColor.RED)));
                return;
            }
            
            user.setBanned(false);
            MySQLQuery.updateUser(user);
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of(user.getName() + " pardoned", TextColor.GREEN)));
            return;
        }
        
        Set<UserData> users = DataManager.getUsers(data).orElse(null);
        if (users == null || users.isEmpty()) {
            source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of("User doesn't exist", TextColor.GREEN)));
            return;
        }
        
        source.sendMessage(VelocityToolbox.getTextPrefix().append(TextComponent.of(" Users", TextColor.GREEN)));
        for (UserData user : users) {
            TextComponent.Builder textBuilder = TextComponent.builder("");
            textBuilder.clickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + Reference.ID + " " + getPrimaryAlias().orElse("unknown") + " " + user.getUniqueId()));
            textBuilder.append(TextComponent.of("> ", TextColor.BLUE));
            
            if (user.isBanned()) {
                textBuilder.append(TextComponent.of(user.getName() + " (" + user.getUniqueId().toString() + ")", TextColor.RED));
            } else {
                textBuilder.append(TextComponent.of(user.getName() + " (" + user.getUniqueId().toString() + ")", TextColor.GREEN));
            }
        }
    }
}