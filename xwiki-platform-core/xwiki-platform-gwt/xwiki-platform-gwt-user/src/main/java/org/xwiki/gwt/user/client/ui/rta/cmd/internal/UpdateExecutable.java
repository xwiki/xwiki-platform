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

import org.xwiki.gwt.user.client.ui.rta.cmd.Executable;

/**
 * An executable that only notifies the command listeners that they should update their states.
 * 
 * @version $Id$
 */
public class UpdateExecutable implements Executable
{
    @Override
    public boolean execute(String param)
    {
        // Always return true. Just notify all the command listeners that they need to update their state.
        return true;
    }

    @Override
    public String getParameter()
    {
        // Always return null, because this executable has no parameter.
        return null;
    }

    @Override
    public boolean isEnabled()
    {
        // Always enabled.
        return true;
    }

    @Override
    public boolean isExecuted()
    {
        // Never executed, because this executable has no state.
        return false;
    }

    @Override
    public boolean isSupported()
    {
        // Always supported.
        return true;
    }
}
