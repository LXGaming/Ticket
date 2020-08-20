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

package io.github.lxgaming.ticket.bungee;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import io.github.lxgaming.ticket.api.Platform;
import io.github.lxgaming.ticket.api.Ticket;
import io.github.lxgaming.ticket.api.util.Logger;
import io.github.lxgaming.ticket.api.util.Reference;
import io.github.lxgaming.ticket.bungee.command.BanCommand;
import io.github.lxgaming.ticket.bungee.command.CloseCommand;
import io.github.lxgaming.ticket.bungee.command.CommentCommand;
import io.github.lxgaming.ticket.bungee.command.DebugCommand;
import io.github.lxgaming.ticket.bungee.command.HelpCommand;
import io.github.lxgaming.ticket.bungee.command.OpenCommand;
import io.github.lxgaming.ticket.bungee.command.PardonCommand;
import io.github.lxgaming.ticket.bungee.command.ReadCommand;
import io.github.lxgaming.ticket.bungee.command.ReloadCommand;
import io.github.lxgaming.ticket.bungee.command.ReopenCommand;
import io.github.lxgaming.ticket.bungee.command.TicketCommand;
import io.github.lxgaming.ticket.bungee.command.UserCommand;
import io.github.lxgaming.ticket.bungee.listener.BungeeListener;
import io.github.lxgaming.ticket.bungee.listener.RedisListener;
import io.github.lxgaming.ticket.common.TicketImpl;
import io.github.lxgaming.ticket.common.configuration.Config;
import io.github.lxgaming.ticket.common.manager.CommandManager;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class BungeePlugin extends Plugin implements Platform {
    
    private static BungeePlugin instance;
    
    @Override
    public void onEnable() {
        instance = this;
        TicketImpl ticket = new TicketImpl(this);
        ticket.getLogger()
                .add(Logger.Level.INFO, getLogger()::info)
                .add(Logger.Level.WARN, getLogger()::warning)
                .add(Logger.Level.ERROR, getLogger()::severe)
                .add(Logger.Level.DEBUG, message -> {
                    if (TicketImpl.getInstance().getConfig().map(Config::isDebug).orElse(false)) {
                        getLogger().info(message);
                    }
                });
        
        ticket.loadTicket();
        
        CommandManager.registerCommand(BanCommand.class);
        CommandManager.registerCommand(CloseCommand.class);
        CommandManager.registerCommand(CommentCommand.class);
        CommandManager.registerCommand(DebugCommand.class);
        CommandManager.registerCommand(HelpCommand.class);
        CommandManager.registerCommand(OpenCommand.class);
        CommandManager.registerCommand(PardonCommand.class);
        CommandManager.registerCommand(ReadCommand.class);
        CommandManager.registerCommand(ReloadCommand.class);
        CommandManager.registerCommand(ReopenCommand.class);
        CommandManager.registerCommand(UserCommand.class);
        getProxy().getPluginManager().registerCommand(getInstance(), new TicketCommand());
        getProxy().getPluginManager().registerListener(getInstance(), new BungeeListener());
        
        if (getProxy().getPluginManager().getPlugin("RedisBungee") != null) {
            Ticket.getInstance().getLogger().info("RedisBungee detected");
            getProxy().getPluginManager().registerListener(getInstance(), new RedisListener());
            RedisBungee.getApi().registerPubSubChannels(Reference.ID);
        }
    }
    
    @Override
    public void onDisable() {
        if (getProxy().getPluginManager().getPlugin("RedisBungee") != null) {
            RedisBungee.getApi().unregisterPubSubChannels(Reference.ID);
        }
        
        TicketImpl.getInstance().getStorage().close();
        Ticket.getInstance().getLogger().info("{} v{} unloaded", Reference.NAME, Reference.VERSION);
    }
    
    @Override
    public boolean isOnline(UUID uniqueId) {
        if (uniqueId == Platform.CONSOLE_UUID) {
            return true;
        }
        
        return Optional.ofNullable(ProxyServer.getInstance().getPlayer(uniqueId)).map(ProxiedPlayer::isConnected).orElse(false);
    }
    
    @Override
    public Optional<String> getUsername(UUID uniqueId) {
        if (uniqueId == Platform.CONSOLE_UUID) {
            return Optional.of("CONSOLE");
        }
        
        return Optional.ofNullable(ProxyServer.getInstance().getPlayer(uniqueId)).map(ProxiedPlayer::getName);
    }
    
    @Override
    public Path getPath() {
        return getDataFolder().toPath();
    }
    
    public static BungeePlugin getInstance() {
        return instance;
    }
}