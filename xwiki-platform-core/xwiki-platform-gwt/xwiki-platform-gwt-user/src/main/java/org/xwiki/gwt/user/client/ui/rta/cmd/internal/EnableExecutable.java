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
 * Enables or disables the rich text area. When the rich text area is disabled its content is not submitted.
 * 
 * @version $Id$
 */
public class EnableExecutable extends AbstractRichTextAreaExecutable
{
    /**
     * Creates a new executable that can be used to enable and disable the specified rich text area.
     * 
     * @param rta the execution target
     */
    public EnableExecutable(RichTextArea rta)
    {
        super(rta);
    }

    @Override
    public boolean execute(String parameter)
    {
        rta.setEnabled(Boolean.parseBoolean(parameter));
        return true;
    }

    @Override
    public String getParameter()
    {
        return Boolean.toString(rta.isEnabled());
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
        return rta.isEnabled();
    }

    @Override
    public boolean isSupported()
    {
        // Always supported.
        return true;
    }
}
