/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.gwt.user.client.ui.rta.cmd;

import java.util.List;

/**
 * A {@link CommandListener} that has a name and logs it whenever it is notified.
 * 
 * @version $Id$
 */
public class LoggingCommandListener implements CommandListener
{
    /**
     * The name of this listener.
     */
    private final String name;

    /**
     * The list of log messages.
     */
    private final List<String> log;

    /**
     * Creates a new {@link CommandListener} that logs its name.
     * 
     * @param name the name of the log
     * @param log the list where to add log messages
     */
    public LoggingCommandListener(String name, List<String> log)
    {
        this.name = name;
        this.log = log;
    }

    @Override
    public boolean onBeforeCommand(CommandManager sender, Command command, String param)
    {
        log.add(name + " before(" + command + ',' + param + ')');
        return false;
    }

    @Override
    public void onCommand(CommandManager sender, Command command, String param)
    {
        log.add(name + " after(" + command + ',' + param + ')');
    }
}
