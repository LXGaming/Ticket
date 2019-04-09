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

package nz.co.lolnet.ticket.bungee;

import com.imaginarycode.minecraft.redisbungee.RedisBungee;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;
import nz.co.lolnet.ticket.api.Platform;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.util.Logger;
import nz.co.lolnet.ticket.api.util.Reference;
import nz.co.lolnet.ticket.bungee.command.BanCommand;
import nz.co.lolnet.ticket.bungee.command.CloseCommand;
import nz.co.lolnet.ticket.bungee.command.CommentCommand;
import nz.co.lolnet.ticket.bungee.command.DebugCommand;
import nz.co.lolnet.ticket.bungee.command.HelpCommand;
import nz.co.lolnet.ticket.bungee.command.OpenCommand;
import nz.co.lolnet.ticket.bungee.command.PardonCommand;
import nz.co.lolnet.ticket.bungee.command.ReadCommand;
import nz.co.lolnet.ticket.bungee.command.ReloadCommand;
import nz.co.lolnet.ticket.bungee.command.ReopenCommand;
import nz.co.lolnet.ticket.bungee.command.TicketCommand;
import nz.co.lolnet.ticket.bungee.command.UserCommand;
import nz.co.lolnet.ticket.bungee.listener.BungeeListener;
import nz.co.lolnet.ticket.bungee.listener.RedisListener;
import nz.co.lolnet.ticket.common.TicketImpl;
import nz.co.lolnet.ticket.common.configuration.Config;
import nz.co.lolnet.ticket.common.manager.CommandManager;

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