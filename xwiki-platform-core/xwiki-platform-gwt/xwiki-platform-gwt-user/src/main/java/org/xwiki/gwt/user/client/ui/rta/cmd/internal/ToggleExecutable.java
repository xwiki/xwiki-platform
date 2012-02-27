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
 * Toggles an executable. Alternates the calls to the underlying executable using two parameter values.
 * 
 * @version $Id$
 */
public class ToggleExecutable implements Executable
{
    /**
     * The underlying executable that is being toggled.
     */
    private final Executable executable;

    /**
     * The parameter used to toggle on the underlying executable.
     */
    private final String onParameter;

    /**
     * The parameter used to toggle off the underlying executable.
     */
    private final String offParameter;

    /**
     * Creates a new executable that toggles the given executable.
     * 
     * @param executable the executable to be toggled
     * @param onParameter the parameter used to toggle on the given executable
     * @param offParameter the parameter used to toggle off the given executable
     */
    public ToggleExecutable(Executable executable, String onParameter, String offParameter)
    {
        this.executable = executable;
        this.onParameter = onParameter;
        this.offParameter = offParameter;
    }

    @Override
    public boolean execute(String parameter)
    {
        return executable.execute(isExecuted() ? offParameter : onParameter);
    }

    @Override
    public boolean isEnabled()
    {
        return executable.isEnabled();
    }

    @Override
    public boolean isExecuted()
    {
        String parameter = executable.getParameter();
        return onParameter == parameter || (onParameter != null && onParameter.equals(parameter));
    }

    @Override
    public boolean isSupported()
    {
        return executable.isSupported();
    }

    @Override
    public String getParameter()
    {
        return null;
    }
}
