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

package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ObjectInterface;
import com.xpn.xwiki.objects.PropertyInterface;

/**
 * The interface implemented by all XClass properties. An XClass property is at the same time a property (implements
 * {@link PropertyInterface}) and an instance (object) of a meta class (implements {@link ObjectInterface}), where the
 * meta class defines the meta properties of an XClass property (e.g. "relational storage", "display type", "separator",
 * "multiple selection", etc.)
 *
 * @version $Id$
 */
public interface PropertyClassInterface extends ObjectInterface, PropertyInterface
{
    String toString(BaseProperty property);

    @Deprecated
    BaseProperty fromString(String value);

    default BaseProperty parseString(String value) throws XWikiException
    {
        return fromString(value);
    }

    BaseProperty fromValue(Object value);

    void displayHidden(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context);

    void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context);

    /**
     * @since 13.0
     */
    default void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, boolean isolated,
        XWikiContext context)
    {
        displayView(buffer, name, prefix, object, context);
    }

    void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context);

    BaseProperty newProperty();

    void flushCache();
}
