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

import com.xpn.xwiki.wysiwyg.client.widget.rta.cmd.Command;

/**
 * Inserts an HTML fragment in place of the current selection. We've added this class because Internet Explorer doesn't
 * support this command and we need to use deferred binding for loading the custom implementation.
 * 
 * @version $Id$
 */
public class InsertHTMLExecutable extends DefaultExecutable
{
    /**
     * Creates a new executable of this type.
     */
    public InsertHTMLExecutable()
    {
        super(Command.INSERT_HTML.toString());
    }
}
