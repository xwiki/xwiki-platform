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
package com.xpn.xwiki.api;

import java.util.List;
import java.util.Map;

import org.xwiki.model.reference.ClassPropertyReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.util.Programming;

/**
 * <p>
 * XProperty definition API.
 * </p>
 * <p>
 * A <strong>property definition</strong> is the instantiation of a {@link com.xpn.xwiki.objects.meta.PropertyMetaClass}
 * for a particular <strong>Object definition</strong> ({@link Class XClass}), that customizes a property type to suit
 * the needs of the class. For example, it can set the number type for a
 * {@link com.xpn.xwiki.objects.classes.NumberClass}, the list of possible values for a
 * {@link com.xpn.xwiki.objects.classes.StaticListClass}, etc.
 * </p>
 *
 * @version $Id$
 */
public class PropertyClass extends Collection implements Comparable<PropertyClass>
{
    /**
     * Default API constructor that creates a wrapper for a {@link com.xpn.xwiki.objects.classes.PropertyClass}, given a
     * {@link com.xpn.xwiki.XWikiContext context}.
     *
     * @param property the property definition to wrap
     * @param context the current request context
     */
    public PropertyClass(com.xpn.xwiki.objects.classes.PropertyClass property, XWikiContext context)
    {
        super(property, context);
    }

    /**
     * Internal access to the wrapped {@link com.xpn.xwiki.objects.classes.PropertyClass}.
     *
     * @return the wrapped property definition
     */
    protected com.xpn.xwiki.objects.classes.PropertyClass getBasePropertyClass()
    {
        return (com.xpn.xwiki.objects.classes.PropertyClass) getCollection();
    }

    @Override
    public ClassPropertyReference getReference()
    {
        return getBasePropertyClass().getReference();
    }

    /**
     * Each type of XClass property is identified by a string that specifies the data type of the property value (e.g.
     * 'String', 'Number', 'Date') without disclosing implementation details. The internal implementation of an XClass
     * property type can change over time but its {@code classType} should not.
     * <p>
     * The {@code classType} can be used as a hint to lookup various components related to this specific XClass property
     * type or it can be used as a property name to retrieve the meta class of this property from
     * {@link com.xpn.xwiki.api.XWiki#getMetaclass()}.
     *
     * @return an identifier for the data type of the property value (e.g. 'String', 'Number', 'Date')
     */
    public String getClassType()
    {
        return getBasePropertyClass().getClassType();
    }

    /**
     * Get the actual type of the wrapped {@link com.xpn.xwiki.objects.classes.PropertyClass}. The returned value is
     * extracted from the class name of the runtime object representing this property definition, and denotes a
     * user-friendly data type name, for example {@code StringClass}, {@code NumberClass} or {@code StaticListClass}.
     *
     * @return the type of this property definition
     * @see #getClassType() {@code getClassType()} if you need the implementation hint of this property
     */
    public String getType()
    {
        return getBasePropertyClass().getClass().getSimpleName();
    }

    /**
     * Get the name of the {@link com.xpn.xwiki.api.Class XClass} (Object Definition) this property definition belongs
     * to. For example, {@code XWiki.XWikiUsers} or {@code Blog.BlogPostClass}.
     *
     * @return the name of the owner XClass
     */
    public String getClassName()
    {
        return getBasePropertyClass().getObject().getName();
    }

    /**
     * Provides access to the wrapped {@link com.xpn.xwiki.objects.classes.PropertyClass} if Programming Rights are
     * present.
     *
     * @return the wrapped property definition
     */
    @Programming
    public com.xpn.xwiki.objects.classes.PropertyClass getPropertyClass()
    {
        if (hasProgrammingRights()) {
            return getBasePropertyClass();
        }
        return null;
    }

    /**
     * Get the untranslated user-friendly name of this property. For example, {@code User type} instead of the internal
     * {@code usertype}.
     *
     * @return the configured pretty name of this property definition
     * @see #getName() {@code getName()} returns the actual property name
     */
    @Override
    public String getPrettyName()
    {
        return getBasePropertyClass().getPrettyName();
    }

    /**
     * Get the translated user-friendly name of this property.
     *
     * @return the configured pretty name of this property definition
     * @see #getName() {@code getName()} returns the actual property name
     */
    public String getTranslatedPrettyName()
    {
        return getBasePropertyClass().getTranslatedPrettyName(this.context);
    }

    /**
     * Get the translated hint string that should be displayed for input fields for instances of this property
     * definition.
     *
     * @return the hint string
     * @since 9.11RC1
     */
    public String getHint()
    {
        return getBasePropertyClass().getHint();
    }

    /**
     * Get the message that should be displayed when a value for an instance of this property definition fails the
     * validation. For example, {@code Please enter a valid IP address}.
     *
     * @return the configured validation message
     * @see #getValidationRegExp() {@code getValidationRegExp()} returns the regular expression used for validating the
     *      property value
     */
    public String getValidationMessage()
    {
        return getBasePropertyClass().getValidationMessage();
    }

    /**
     * Get the regular expression used for validating a value for an instance of this property definition.
     *
     * @return a string representation of the validation regular expression
     * @see #getValidationMessage() {@code getValidationMessage()} returns the message that should be displayed in case
     *      the validation failed
     */
    public String getValidationRegExp()
    {
        return getBasePropertyClass().getValidationRegExp();
    }

    /**
     * Get a tooltip string that should be displayed for input fields for instances of this property definition.
     *
     * @return A raw tooltip string. The value does not escape special HTML characters, so the caller should manually
     *         escape quotes if the tooltip should be used as a value for the HTML {@code title} attribute.
     */
    public String getTooltip()
    {
        return getBasePropertyClass().getTooltip();
    }

    /**
     * See if this property is disabled or not. A disabled property should not be editable, but existing object values
     * are still kept in the database.
     *
     * @return {@code true} if this property is disabled and should not be used, {@code false} otherwise
     * @since 2.4M2
     */
    public boolean isDisabled()
    {
        return getBasePropertyClass().isDisabled();
    }

    /**
     * If the property is a {@link ListClass}, returns the possible values. These are the internal values (keys), and
     * not the user-friendly or translated values that would be displayed to the user.
     *
     * @return the list of possible ({@code String}) values
     * @see #getMapValues() {@code getMapValues()} returns both the keys and their user-friendly displayed values
     **/
    public List<String> getListValues()
    {
        com.xpn.xwiki.objects.classes.PropertyClass pclass = getBasePropertyClass();
        if (pclass instanceof ListClass) {
            return ((ListClass) pclass).getList(this.context);
        } else {
            // Although we prefer to return empty lists from API methods, here returning any kind of list doesn't make
            // sense, since the property does not have a list of possible values at all (like, a number property).
            return null;
        }
    }

    /**
     * If the property is a {@link ListClass}, returns the possible values as a map {@code internal key <-> displayed
     * value}.
     *
     * @return the map of possible ({@code String}) values and their associated ({@code ListItem}) displayed values
     * @see #getListValues() {@code getListValues()} returns only the list of possible internal keys
     **/
    public Map<String, ListItem> getMapValues()
    {
        com.xpn.xwiki.objects.classes.PropertyClass pclass = getBasePropertyClass();
        if (pclass instanceof ListClass) {
            return ((ListClass) pclass).getMap(this.context);
        } else {
            // Although we prefer to return empty maps from API methods, here returning any kind of map doesn't make
            // sense, since the property does not have a list of possible values at all (like, a number property).
            return null;
        }
    }

    /**
     * Compares two property definitions based on their index number.
     *
     * @param other the other property definition to be compared with
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than
     *         the specified object.
     * @see #getNumber()
     * @since 2.4M2
     */
    @Override
    public int compareTo(PropertyClass other)
    {
        return this.getBasePropertyClass().compareTo(other.getBasePropertyClass());
    }
}
