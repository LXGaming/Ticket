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

package nz.co.lolnet.ticket.common.cache;

import com.github.benmanes.caffeine.cache.Expiry;
import nz.co.lolnet.ticket.api.Ticket;
import nz.co.lolnet.ticket.api.data.UserData;
import nz.co.lolnet.ticket.common.manager.DataManager;

import java.time.Duration;
import java.util.UUID;

public class UserExpiry implements Expiry<UUID, UserData> {
    
    @Override
    public long expireAfterCreate(UUID key, UserData value, long currentTime) {
        if (Ticket.getInstance().getPlatform().isOnline(key) || !DataManager.getCachedOpenTickets(key).isEmpty()) {
            return Long.MAX_VALUE;
        }
        
        return Duration.ofHours(1L).toNanos();
    }
    
    @Override
    public long expireAfterUpdate(UUID key, UserData value, long currentTime, long currentDuration) {
        if (Ticket.getInstance().getPlatform().isOnline(key) || !DataManager.getCachedOpenTickets(key).isEmpty()) {
            return Long.MAX_VALUE;
        }
        
        return Duration.ofHours(1L).toNanos();
    }
    
    @Override
    public long expireAfterRead(UUID key, UserData value, long currentTime, long currentDuration) {
        if (Ticket.getInstance().getPlatform().isOnline(key) || !DataManager.getCachedOpenTickets(key).isEmpty()) {
            Ticket.getInstance().getLogger().debug("UserExpiry::read - Infinite");
            return Long.MAX_VALUE;
        }
        
        Ticket.getInstance().getLogger().debug("UserExpiry::read - 1 Hour");
        return Duration.ofHours(1L).toNanos();
    }
}