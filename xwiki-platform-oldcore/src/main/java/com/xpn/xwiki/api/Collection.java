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
 *
 */
package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;

public abstract class Collection extends Element
{
    public Collection(BaseCollection collection, XWikiContext context)
    {
        super(collection, context);
    }

    protected BaseCollection getCollection()
    {
        return (BaseCollection) this.element;
    }

    public Class getxWikiClass()
    {
        return new Class(getCollection().getxWikiClass(getXWikiContext()), getXWikiContext());
    }

    @Override
    public String getName()
    {
        return getCollection().getName();
    }

    public String getPrettyName()
    {
        return getCollection().getPrettyName();
    }

    public int getNumber()
    {
        return getCollection().getNumber();
    }

    public java.lang.Object[] getPropertyNames()
    {
        return getCollection().getPropertyNames();
    }

    public Element[] getProperties()
    {
        @SuppressWarnings("unchecked")
        java.util.Collection<BaseProperty> coll = getCollection().getFieldList();
        if (coll == null) {
            return null;
        }
        Property[] properties = new Property[coll.size()];
        int i = 0;
        for (BaseProperty prop : coll) {
            properties[i++] = new Property(prop, getXWikiContext());
        }
        return properties;
    }

    public Property getProperty(String name)
    {
        try {
            PropertyInterface prop = getCollection().get(name);
            if (prop == null) {
                return null;
            }

            return new Property((BaseProperty) prop, getXWikiContext());
        } catch (Exception e) {
            return null;
        }
    }
}
