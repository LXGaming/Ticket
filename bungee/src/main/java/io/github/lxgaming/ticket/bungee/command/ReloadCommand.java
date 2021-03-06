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

import io.github.lxgaming.ticket.bungee.util.BungeeToolbox;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.command.AbstractCommand;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;

import java.util.List;

public class ReloadCommand extends AbstractCommand {
    
    public ReloadCommand() {
        addAlias("reload");
        setDescription("Reload Ticket");
        setPermission("ticket.reload.base");
    }
    
    @Override
    public void execute(Object object, List<String> arguments) {
        CommandSender sender = (CommandSender) object;
        if (TicketImpl.getInstance().reloadTicket()) {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("Configuration reloaded").color(ChatColor.GREEN).create());
        } else {
            sender.sendMessage(BungeeToolbox.getTextPrefix().append("An error occurred. Please check the console").color(ChatColor.RED).create());
        }
    }
}