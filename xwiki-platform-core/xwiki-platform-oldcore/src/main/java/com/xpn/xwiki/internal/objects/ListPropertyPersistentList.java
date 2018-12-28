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
package com.xpn.xwiki.internal.objects;

import java.util.ArrayList;

import org.hibernate.collection.internal.PersistentList;
import org.hibernate.engine.spi.SessionImplementor;

import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.ListProperty.NotifyList;

/**
 * Implementation of a persistent list holder for list property values.
 *
 * @since 4.3
 * @version $Id$
 */
public class ListPropertyPersistentList extends PersistentList
{

    /**
     * @param session The session implementor. {@link PersistentList}.
     * @param list The notify list to wrap with this list holder.
     */
    public ListPropertyPersistentList(SessionImplementor session, NotifyList list)
    {
        super(session, list);
    }

    /**
     * @param session The session implementor. {@link PersistentList}.
     */
    public ListPropertyPersistentList(SessionImplementor session)
    {
        super(session);
    }

    /**
     * @param owner The owner list property.
     */
    @SuppressWarnings("unchecked")
    public void setOwner(ListProperty owner)
    {
        if (this.list == null) {
            this.list = new NotifyList(new ArrayList<String>());
        }
        ((NotifyList) this.list).setOwner(owner);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean isWrapper(Object collection)
    {
        return collection == this.list || ((NotifyList) this.list).isWrapper(collection);
    }
}
