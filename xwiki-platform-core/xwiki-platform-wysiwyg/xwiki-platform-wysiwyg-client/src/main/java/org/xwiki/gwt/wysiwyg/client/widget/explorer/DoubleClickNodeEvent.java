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
package org.xwiki.gwt.wysiwyg.client.widget.explorer;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Event fired when a node in the tree is double clicked.
 * 
 * @version $Id$
 */
public class DoubleClickNodeEvent extends GwtEvent<DoubleClickNodeHandler>
{
    /**
     * Event type for double click node events. Represents the meta-data associated with this event.
     */
    public static final Type<DoubleClickNodeHandler> TYPE = new Type<DoubleClickNodeHandler>();

    @Override
    public Type<DoubleClickNodeHandler> getAssociatedType()
    {
        return TYPE;
    }

    @Override
    protected void dispatch(DoubleClickNodeHandler handler)
    {
        handler.onDoubleClickNode(this);
    }
}
