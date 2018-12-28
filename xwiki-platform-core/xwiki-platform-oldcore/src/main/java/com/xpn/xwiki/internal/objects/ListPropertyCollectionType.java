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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

import com.xpn.xwiki.objects.ListProperty.NotifyList;

/**
 * Helper class for hibernate to wrap a persistent collection around a NotifyList.
 *
 * @since 4.3
 * @version $Id$
 */
public class ListPropertyCollectionType implements UserCollectionType
{

    @Override
    public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister)
        throws HibernateException
    {
        return new ListPropertyPersistentList(session);
    }

    @Override
    @SuppressWarnings("unchecked")
    public PersistentCollection wrap(SessionImplementor session, Object collection)
    {
        if (collection instanceof NotifyList) {
            return new ListPropertyPersistentList(session, (NotifyList) collection);
        } else if (collection instanceof NotifyList) {
            return new ListPropertyPersistentList(session, new NotifyList((List<String>) collection));
        } else {
            throw new IllegalArgumentException("Can only wrap ListProperty.NotifyList.");
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Iterator getElementsIterator(Object collection)
    {
        return ((Collection<?>) collection).iterator();
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object collection, Object entity)
    {
        return ((Collection<?>) collection).contains(entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object indexOf(Object collection, Object entity)
    {
        return ((List) collection).indexOf(entity);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object replaceElements(
        Object original,
        Object target,
        CollectionPersister persister,
        Object owner,
        Map copyCache,
        SessionImplementor session)
        throws HibernateException
    {
        Collection cTarget = (Collection) target;
        Collection cOriginal = (Collection) original;

        cTarget.clear();
        cTarget.addAll(cOriginal);

        return cTarget;
    }

    @Override
    public Object instantiate(int anticipatedSize)
    {
        return new NotifyList(new ArrayList<String>());
    }
}
