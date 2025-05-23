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

import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
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
     * Serialize the given property as a string.
     * @param property the property to serialize.
     * @return the serialized property.
     */
    String toString(BaseProperty property);

    /**
     * Creates a new property and set its value based on the given string.
     * @param value the value to set in the new property.
     * @return the created property
     */
    BaseProperty fromString(String value);

    /**
     * Sets the value of the given property from the given string parameter. Note that if the given property is
     * {@code null} then the method should create a new property and set its value.
     * @param value the value to set in the new property.
     * @param baseProperty the property instance to set the value for, or {@code null} to create a new property.
     * @return the modified property
     * @since 17.4.0RC1
     */
    @Unstable
    default BaseProperty fromString(String value, BaseProperty baseProperty)
    {
        return fromString(value);
    }

    /**
     * Creates a new property and set its value based on the given parameter.
     * @param value the value to set in the new property.
     * @return the created property
     */
    BaseProperty fromValue(Object value);

    /**
     * Sets the value of the given property from the given parameter. Note that if the given property is {@code null}
     * then the method should create a new property and set its value.
     *
     * @param value the value to set in the new property.
     * @param baseProperty the property instance to set the value for, or {@code null} to create a new property.
     * @return the modified property
     * @since 17.4.0RC1
     */
    @Unstable
    default BaseProperty fromValue(Object value, BaseProperty baseProperty)
    {
        return fromValue(value);
    }

    /**
     * Output in the given buffer, hidden form elements for the property identified by the name in the given object.
     * @param buffer the buffer where to output the result
     * @param name the name of the property for which to output a form
     * @param prefix the prefix to use for the form id and name elements
     * @param object the object where to find the property
     * @param context the context to use
     */
    void displayHidden(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context);

    /**
     * Output in the given buffer, a visual representation for the property identified by the name in the given object.
     * @param buffer the buffer where to output the result
     * @param name the name of the property for which to output a view of the property
     * @param prefix the prefix to use for the form id and name elements
     * @param object the object where to find the property
     * @param context the context to use
     */
    void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context);

    /**
     * Output in the given buffer, a visual representation for the property identified by the name in the given object.
     * @param buffer the buffer where to output the result
     * @param name the name of the property for which to output a view of the property
     * @param prefix the prefix to use for the form id and name elements
     * @param object the object where to find the property
     * @param isolated {@code true} if the representation should be computed outside of the context of the current
     * document reference.
     * @param context the context to use
     * @since 13.0
     */
    default void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, boolean isolated,
        XWikiContext context)
    {
        displayView(buffer, name, prefix, object, context);
    }

    /**
     * Output in the given buffer, a representation allowing to edit the property identified by the name in the given
     * object.
     * @param buffer the buffer where to output the result
     * @param name the name of the property for which to output a view of the property
     * @param prefix the prefix to use for the form id and name elements
     * @param object the object where to find the property
     * document reference.
     * @param context the context to use
     */
    void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context);

    /**
     * @return a new property corresponding to the current class.
     */
    BaseProperty newProperty();

    /**
     * Invalidate all caches used in the implementations (e.g. the cache of displayers)
     */
    void flushCache();
}
