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
package com.xpn.xwiki.store.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.PropertyNotFoundException;
import org.hibernate.property.BasicPropertyAccessor;
import org.hibernate.property.Getter;
import org.hibernate.property.PropertyAccessor;
import org.hibernate.property.Setter;

/**
 * Hibernate property accessor that allows using the deprecated <code>doc.web</code> in HQL queries, while using the
 * new <code>space</code> getter and setter for the actual object access.
 * 
 * @version $Id$
 */
public class DocumentWebPropertyAccessor extends BasicPropertyAccessor implements PropertyAccessor
{
    /** Logging helper object. */
    private static final Log LOG = LogFactory.getLog(DocumentWebPropertyAccessor.class);

    /**
     * {@inheritDoc}
     * 
     * @see PropertyAccessor#getGetter(Class, String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Getter getGetter(Class theClass, String propertyName) throws PropertyNotFoundException
    {
        LOG.warn("Deprecated usage of doc.web property in HQL query (get). Use doc.space instead.");
        return super.getGetter(theClass, "space");
    }

    /**
     * {@inheritDoc}
     * 
     * @see PropertyAccessor#getSetter(Class, String)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Setter getSetter(Class theClass, String propertyName) throws PropertyNotFoundException
    {
        LOG.warn("Deprecated usage of doc.web property in HQL query (set). Use doc.space instead.");
        return super.getSetter(theClass, "space");
    }

}
