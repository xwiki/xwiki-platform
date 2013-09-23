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

import org.xwiki.gwt.user.client.ui.rta.RichTextArea;

/**
 * Executes a predefined command on a specific document.
 * 
 * @version $Id$
 */
public class DefaultExecutable extends AbstractRichTextAreaExecutable
{
    /**
     * The predefined command executed by this executable.
     */
    protected String command;

    /**
     * Creates a new instance that will execute the specified command.
     * 
     * @param rta the execution target
     * @param command A predefined command to be executed by this executable.
     */
    public DefaultExecutable(RichTextArea rta, String command)
    {
        super(rta);
        this.command = command;
    }

    @Override
    public boolean execute(String parameter)
    {
        return rta.getDocument().execCommand(command, parameter);
    }

    @Override
    public String getParameter()
    {
        return rta.getDocument().queryCommandValue(command);
    }

    @Override
    public boolean isEnabled()
    {
        return super.isEnabled() && rta.getDocument().queryCommandEnabled(command);
    }

    @Override
    public boolean isExecuted()
    {
        return rta.getDocument().queryCommandState(command);
    }

    @Override
    public boolean isSupported()
    {
        return super.isSupported() && rta.getDocument().queryCommandSupported(command);
    }
}
