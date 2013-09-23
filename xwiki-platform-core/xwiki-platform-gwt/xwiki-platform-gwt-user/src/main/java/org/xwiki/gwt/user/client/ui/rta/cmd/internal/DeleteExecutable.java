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
import org.xwiki.gwt.user.client.ui.rta.cmd.Command;

import com.google.gwt.core.client.GWT;

/**
 * Deletes the current selection.
 * 
 * @version $Id$
 */
public class DeleteExecutable extends DefaultExecutable
{
    /**
     * Browser specific implementation required by this executable.
     */
    private DeleteExecutableImpl impl = GWT.create(DeleteExecutableImpl.class);

    /**
     * Creates a new executable that can be used to delete the current selection in the specified rich text area.
     * 
     * @param rta the execution target
     */
    public DeleteExecutable(RichTextArea rta)
    {
        super(rta, Command.DELETE.toString());
    }

    @Override
    public boolean execute(String parameter)
    {
        return impl.deleteSelection(rta.getDocument().getSelection()) || super.execute(parameter);
    }
}
