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
package com.xpn.xwiki.wysiwyg.client.ui.cmd.internal;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.ui.Button;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.Command;

/**
 * Mock command manager to be used on unit tests.
 */
public class MockCommandManager extends AbstractCommandManager
{
    private final Map<String, String> history;

    public MockCommandManager()
    {
        super(new Button("fakeTextArea"));

        history = new HashMap<String, String>();
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#execCommandAssumingFocus(String, String)
     */
    protected boolean execCommandAssumingFocus(String cmd, String param)
    {
        history.put(cmd, param);
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#queryCommandEnabledAssumingFocus(String)
     */
    protected boolean queryCommandEnabledAssumingFocus(String cmd)
    {
        if (Command.OUTDENT.toString().equals(cmd)) {
            return history.containsKey(Command.INDENT.toString());
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#queryCommandIndetermAssumingFocus(String)
     */
    protected boolean queryCommandIndetermAssumingFocus(String cmd)
    {
        return !history.containsKey(cmd);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#queryCommandStateAssumingFocus(String)
     */
    protected boolean queryCommandStateAssumingFocus(String cmd)
    {
        return history.containsKey(cmd);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#queryCommandSupportedAssumingFocus(String)
     */
    protected boolean queryCommandSupportedAssumingFocus(String cmd)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCommandManager#queryCommandValueAssumingFocus(String)
     */
    protected String queryCommandValueAssumingFocus(String cmd)
    {
        return history.get(cmd);
    }
}
