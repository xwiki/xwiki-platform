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
import org.xwiki.gwt.user.client.ui.rta.cmd.Executable;

/**
 * Abstract {@link Executable} whose execution target is a rich text area.
 * 
 * @version $Id$
 */
public abstract class AbstractRichTextAreaExecutable implements Executable
{
    /**
     * The execution target.
     */
    protected final RichTextArea rta;

    /**
     * Creates a new executable to be executed on the specified rich text area.
     * 
     * @param rta the execution target
     */
    public AbstractRichTextAreaExecutable(RichTextArea rta)
    {
        this.rta = rta;
    }

    @Override
    public boolean isEnabled()
    {
        // Note that we check if the rich text area is visible to account for the case when the editor is loaded lazy
        // and the user can switch between view and edit mode without reloading the page (so the rich text area can be
        // attached and enabled but hidden).
        return isSupported() && rta.isEnabled() && rta.getElement().getOffsetWidth() > 0;
    }

    @Override
    public boolean isSupported()
    {
        return rta.isAttached() && rta.getDocument() != null;
    }
}
