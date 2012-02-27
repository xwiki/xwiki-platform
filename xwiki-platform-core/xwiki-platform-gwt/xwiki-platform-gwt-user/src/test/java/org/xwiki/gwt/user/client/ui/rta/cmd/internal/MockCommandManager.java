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
package org.xwiki.gwt.user.client.ui.rta.cmd.internal;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.gwt.user.client.ui.rta.cmd.Command;

/**
 * Mock command manager to be used on unit tests.
 * 
 * @version $Id$
 */
public class MockCommandManager extends AbstractCommandManager
{
    /**
     * The commands that have been executed by this manager.
     */
    private final Map<Command, String> history;

    /**
     * Creates a new mock command manager.
     */
    public MockCommandManager()
    {
        history = new HashMap<Command, String>();
    }

    @Override
    public boolean execute(Command cmd, String param)
    {
        history.put(cmd, param);
        return true;
    }

    @Override
    public boolean isEnabled(Command cmd)
    {
        if (Command.OUTDENT.equals(cmd)) {
            return history.containsKey(Command.INDENT);
        }
        return true;
    }

    @Override
    public boolean isExecuted(Command cmd)
    {
        return history.containsKey(cmd);
    }

    @Override
    public boolean isSupported(Command cmd)
    {
        return true;
    }

    @Override
    public String getStringValue(Command cmd)
    {
        return history.get(cmd);
    }
}
