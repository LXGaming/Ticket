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
import nz.co.lolnet.ticket.api.data.TicketData;

import java.time.Duration;

public class TicketExpiry implements Expiry<Integer, TicketData> {
    
    @Override
    public long expireAfterCreate(Integer key, TicketData value, long currentTime) {
        return getExpiry(value).toNanos();
    }
    
    @Override
    public long expireAfterUpdate(Integer key, TicketData value, long currentTime, long currentDuration) {
        return getExpiry(value).toNanos();
    }
    
    @Override
    public long expireAfterRead(Integer key, TicketData value, long currentTime, long currentDuration) {
        return getExpiry(value).toNanos();
    }
    
    private Duration getExpiry(TicketData ticket) {
        if (ticket.getStatus() == 0) {
            Ticket.getInstance().getLogger().debug("TicketExpiry - #{} Infinite", ticket.getId());
            return Duration.ofNanos(Long.MAX_VALUE);
        }
        
        if (ticket.getStatus() == 1 && !ticket.isRead() && Ticket.getInstance().getPlatform().isOnline(ticket.getUser())) {
            Ticket.getInstance().getLogger().debug("TicketExpiry - #{} Infinite", ticket.getId());
            return Duration.ofNanos(Long.MAX_VALUE);
        }
        
        Ticket.getInstance().getLogger().debug("TicketExpiry - #{} 1 Hour", ticket.getId());
        return Duration.ofHours(1L);
    }
}