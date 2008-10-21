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

import com.xpn.xwiki.wysiwyg.client.util.Document;

/**
 * Executes a predefined command on a specific document.
 * 
 * @version $Id$
 */
public class DefaultExecutable extends AbstractExecutable
{
    /**
     * The predefined command executed by this executable.
     */
    protected String command;

    /**
     * Creates a new instance that will execute the specified command.
     * 
     * @param command A predefined command to be executed by this executable.
     */
    public DefaultExecutable(String command)
    {
        this.command = command;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#execute(Document, String)
     */
    public boolean execute(Document doc, String parameter)
    {
        return execute(doc, command, parameter);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#getParameter(Document)
     */
    public String getParameter(Document doc)
    {
        return getParameter(doc, command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#isEnabled(Document)
     */
    public boolean isEnabled(Document doc)
    {
        return isEnabled(doc, command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#isExecuted(Document)
     */
    public boolean isExecuted(Document doc)
    {
        return isExecuted(doc, command);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#isSupported(Document)
     */
    public boolean isSupported(Document doc)
    {
        return isSupported(doc, command);
    }
}
