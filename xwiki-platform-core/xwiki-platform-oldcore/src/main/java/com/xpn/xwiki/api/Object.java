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

import java.util.Map;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.evaluation.ObjectEvaluator;
import org.xwiki.evaluation.ObjectEvaluatorException;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * API representation of an XObject.
 * This class has been created to allow manipulating xobjects in scripts safely.
 * @version $Id$
 */
public class Object extends Collection
{
    /**
     * Default constructor.
     * @param obj the wrapped object
     * @param context the context to manipulate the object
     */
    public Object(BaseObject obj, XWikiContext context)
    {
        super(obj, context);
    }

    protected BaseObject getBaseObject()
    {
        return (BaseObject) getCollection();
    }

    /**
     * Retrieve the wrapped object when the user has programming rights.
     * @return the wrapped object or {@code null}.
     */
    public BaseObject getXWikiObject()
    {
        if (hasProgrammingRights()) {
            return (BaseObject) getCollection();
        } else {
            return null;
        }
    }

    /**
     * @return the wrapped object guid.
     * @see BaseObject#getGuid()
     */
    public String getGuid()
    {
        return getBaseObject().getGuid();
    }

    /**
     * Set the wrapped object guid.
     * @param guid the guid to set
     * @see BaseObject#setGuid(String)
     */
    public void setGuid(String guid)
    {
        getBaseObject().setGuid(guid);
    }

    /**
     * Display the passed field.
     * <p>
     * This method's name is misleading since it doesn't return the Object's property value; it"s equivalent to
     * {@link #display(String, String)} (with {@code type} equals to the context action ("view" or "edit" usually). The
     * right method to get the field value is {@link #getValue(String)}.
     * @param name the name of the field for which to display the value.
     * @return a string representing the display output or {@code null} in case of problem
     */
    public java.lang.Object get(String name)
    {
        try {
            XWikiDocument doc = getBaseObject().getOwnerDocument();
            if (doc == null) {
                doc =
                    getXWikiContext().getWiki().getDocument(getBaseObject().getDocumentReference(), getXWikiContext());
            }

            return doc.display(name, this.getBaseObject(), getXWikiContext());
        } catch (XWikiException e) {
            return null;
        }
    }

    /**
     * Display the property with the passed name in the context of its own document.
     * 
     * @param name the name of the property
     * @param mode the edit mode in which the property should be displayed ("view", "edit", etc.)
     * @return the result of the display, generally HTML
     */
    public java.lang.Object display(String name, String mode)
    {
        return display(name, mode, true);
    }

    /**
     * Display the property with the passed name in the context of the current document or its own document.
     * 
     * @param name the name of the property
     * @param mode the edit mode in which the property should be displayed ("view", "edit", etc.)
     * @param isolated true if the property should be displayed in it's own document context
     * @return a string representing the display output or {@code null} in case of problem
     * @since 13.0
     */
    public java.lang.Object display(String name, String mode, boolean isolated)
    {
        return display(name, mode, isolated, true);
    }

    /**
     * Display the property with the passed name in the context of the current document or its own document.
     * 
     * @param name the name of the property
     * @param mode the edit mode in which the property should be displayed ("view", "edit", etc.)
     * @param isolated true if the property should be displayed in it's own document context
     * @param number true if the number you be part of the input name, false otherwise
     * @return a string representing the display output or {@code null} in case of problem
     * @since 17.3.0RC1
     */
    @Unstable
    public java.lang.Object display(String name, String mode, boolean isolated, boolean number)
    {
        try {
            XWikiDocument doc = getBaseObject().getOwnerDocument();
            if (doc == null) {
                doc =
                    getXWikiContext().getWiki().getDocument(getBaseObject().getDocumentReference(), getXWikiContext());
            }

            return doc.display(name, mode, getBaseObject(), isolated, number, getXWikiContext());
        } catch (XWikiException e) {
            return null;
        }
    }

    @Override
    public boolean equals(java.lang.Object arg0)
    {
        if (!(arg0 instanceof Object)) {
            return false;
        }
        Object o = (Object) arg0;
        return o.getXWikiContext().equals(getXWikiContext()) && this.element.equals(o.element);
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
            .append(getXWikiContext())
            .append(this.element)
            .toHashCode();
    }

    /**
     * Set the defined property with the given value in the current object.
     * The given value might be a {@link String} or a type supported by the property. If a {@link String} is given
     * then {@link com.xpn.xwiki.objects.classes.PropertyClassInterface#fromString(String)} will be used.
     * Note that this method also set the author of the document to match the object owner.
     * @param fieldname the name of the property to set
     * @param value the value to set
     * @throws XWikiException in case of problem when parsing the value
     * @see BaseObject#set(String, java.lang.Object, XWikiContext)
     */
    public void set(String fieldname, java.lang.Object value) throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();

        getBaseObject().set(fieldname, value, xcontext);

        // Temporary set as author of the document the current script author (until the document is saved)
        Document.updateAuthor(getBaseObject().getOwnerDocument(), xcontext);
    }

    @Override
    public BaseObjectReference getReference()
    {
        return getBaseObject().getReference();
    }

    /**
     * Helper method used to obtain the reference of an object property even when the object might not have the property
     * (e.g. because it's a computed property which doesn't have a stored value so it's not saved on the object). This
     * is a safe alternative to {@code getProperty(propertyName).getReference()} when you're not sure whether the object
     * has the specified property or not.
     * 
     * @param propertyName the property name
     * @return the object property reference
     * @see <a href="https://jira.xwiki.org/browse/XWIKI-19031">XWIKI-19031: Computed fields are stored as empty string
     *      properties in the database</a>
     * @since 12.10.11
     * @since 13.4.6
     * @since 13.10RC1
     */
    public ObjectPropertyReference getPropertyReference(String propertyName)
    {
        return new ObjectPropertyReference(propertyName, getReference());
    }

    /**
     * Evaluates the properties of an object using a matching implementation of {@link ObjectEvaluator}.
     *
     * @return a Map storing the evaluated properties
     * @since 14.10.21
     * @since 15.5.5
     * @since 15.10.2
     */
    public Map<String, String> evaluate() throws ObjectEvaluatorException
    {
        return getBaseObject().evaluate();
    }
}
