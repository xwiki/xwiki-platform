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
package com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.internal;

import com.xpn.xwiki.wysiwyg.client.widget.rta.RichTextArea;
import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Executable;

/**
 * Toggles an executable. Alternates the calls to the underlying executable using two parameter values.
 * 
 * @version $Id$
 */
public class ToggleExecutable extends AbstractExecutable
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

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#execute(RichTextArea, String)
     */
    public boolean execute(RichTextArea rta, String parameter)
    {
        return executable.execute(rta, isExecuted(rta) ? offParameter : onParameter);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#isEnabled(RichTextArea)
     */
    public boolean isEnabled(RichTextArea rta)
    {
        return executable.isEnabled(rta);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#isExecuted(RichTextArea)
     */
    public boolean isExecuted(RichTextArea rta)
    {
        String parameter = executable.getParameter(rta);
        return onParameter == parameter || (onParameter != null && onParameter.equals(parameter));
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractExecutable#isSupported(RichTextArea)
     */
    public boolean isSupported(RichTextArea rta)
    {
        return executable.isSupported(rta);
    }
}
