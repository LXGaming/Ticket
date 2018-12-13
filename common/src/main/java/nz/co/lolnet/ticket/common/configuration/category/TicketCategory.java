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

package nz.co.lolnet.ticket.common.configuration.category;

public class TicketCategory {
    
    private String dateFormat = "dd/MM/yyyy 'at' HH:mm:ss z";
    private long commentDelay = 30000L;
    private long openDelay = 60000L;
    private int maximumTickets = 3;
    private int minimumWords = 2;
    
    public String getDateFormat() {
        return dateFormat;
    }
    
    public long getCommentDelay() {
        return commentDelay;
    }
    
    public long getOpenDelay() {
        return openDelay;
    }
    
    public int getMaximumTickets() {
        return maximumTickets;
    }
    
    public int getMinimumWords() {
        return minimumWords;
    }
}