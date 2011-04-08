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
package org.xwiki.gwt.wysiwyg.client.plugin.separator.exec;

import org.xwiki.gwt.user.client.ui.rta.RichTextArea;
import org.xwiki.gwt.user.client.ui.rta.cmd.internal.InsertBlockHTMLExecutable;

/**
 * Inserts a horizontal rule in place of the current selection. It should be noted that hr, being a block level element,
 * must be added under a flow container. Because of this, we split the DOM tree up to the nearest flow container and
 * insert the hr element between the two parts.
 * 
 * @version $Id$
 */
public class InsertHRExecutable extends InsertBlockHTMLExecutable
{
    /**
     * Creates a new executable that can be used to insert a horizontal rule in the specified rich text area.
     * 
     * @param rta the execution target
     */
    public InsertHRExecutable(RichTextArea rta)
    {
        super(rta);
    }

    /**
     * {@inheritDoc}
     * 
     * @see InsertBlockHTMLExecutable#execute(String)
     */
    public boolean execute(String param)
    {
        return execute(rta.getDocument().createHRElement());
    }
}
