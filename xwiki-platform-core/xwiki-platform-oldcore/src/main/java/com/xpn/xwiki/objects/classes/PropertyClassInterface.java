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
    /**
     * Display the string representation of a property value.
     * @param property the property for which to display the value
     * @return the string representation of the property value or an empty string.
     * @see BaseProperty#toText()
     */
    String toString(BaseProperty property);

    /**
     * Create a new property of the current class and parse the given value to set the new property with it.
     * @param value the string representation of the value to use in the new property
     * @return a new property with a value set or {@code null} if the class cannot create a new property instance.
     * @throws XWikiException in case of problem when parsing the value
     */
    BaseProperty fromString(String value) throws XWikiException;

    /**
     * Create a new property of the current class and set the new property value with the given value.
     * @param value the value to use in the new property
     * @return a new property with a value set or {@code null} if the class cannot create a new property instance.
     */
    BaseProperty fromValue(Object value);

    /**
     * Display a hidden input for the given property of the given object in the given buffer.
     * @param buffer where to write the output
     * @param name the name of the property to display
     * @param prefix the prefix to use for the name of the field
     * @param object the object where to find the property to display
     * @param context the wiki context to use for computing the values
     * @see com.xpn.xwiki.objects.classes.PropertyClassInterface#displayHidden(StringBuffer, String, String,
     * BaseCollection, XWikiContext)
     */
    void displayHidden(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context);

    /**
     * Display the value of the given property in the given buffer.
     * @param buffer where to write the output
     * @param name the name of the property to display
     * @param prefix the prefix to use for the name of the field
     * @param object the object where to find the property to display
     * @param context the wiki context to use for computing the values
     * @see com.xpn.xwiki.objects.classes.PropertyClassInterface#displayView(StringBuffer, String, String,
     * BaseCollection, XWikiContext)
     */
    void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context);

    /**
     * Display the value of the given property in the given buffer.
     * @param buffer where to write the output
     * @param name the name of the property to display
     * @param prefix the prefix to use for the name of the field
     * @param object the object where to find the property to display
     * @param isolated true if the content should be executed in this document's context
     * @param context the wiki context to use for computing the values
     * @see com.xpn.xwiki.objects.classes.PropertyClassInterface#displayView(StringBuffer, String, String,
     * BaseCollection, XWikiContext)
     * @since 13.0
     */
    default void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, boolean isolated,
        XWikiContext context)
    {
        displayView(buffer, name, prefix, object, context);
    }

    /**
     * Display an edit input of the given property in the given buffer.
     * @param buffer where to write the output
     * @param name the name of the property to display
     * @param prefix the prefix to use for the name of the field
     * @param object the object where to find the property to display
     * @param context the wiki context to use for computing the values
     * @see com.xpn.xwiki.objects.classes.PropertyClassInterface#displayEdit(StringBuffer, String, String,
     * BaseCollection, XWikiContext)
     */
    void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context);

    /**
     * Create a new property of the current class.
     * @return a new property instance or {@code null} if the class cannot create new properties.
     */
    BaseProperty newProperty();

    /**
     * Flush the caches that the class might maintain.
     */
    void flushCache();
}
