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
package org.xwiki.annotation.reference;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectReference;

/**
 * Object reference implementation for object names generated in {@code className[objectNumber]} format. It provides
 * helper functions to extract the class name as specified by the caller, and object number. <br>
 * Accepted formats for the object name are:
 * <dl>
 * <dt>className[objectNumber]
 * <dd>interpreted as the object of class className with number objectNumber. refers the object returned by
 * XWikiDocument.getObject(String className, int objectNumber). In this case, className is obtained by calling
 * {@link #getClassName()} and object index by calling {@link #getObjectNumber()}.
 * <dt>className
 * <dd>interpreted as the first object of class className. refers the object returned by XWikiDocument.getObject(String
 * className). In this case, {@link #getObjectNumber()} will return {@code null} and {@code className} is obtained by
 * calling {@link #getClassName()}.
 * </dl>
 * 
 * @version $Id$
 * @since 2.3M1
 */
public class IndexedObjectReference extends ObjectReference
{
    /**
     * The class name of this object, as set by the caller.
     */
    protected String className;

    /**
     * The number of this object, as set by the caller.
     */
    protected Integer objectNumber;

    /**
     * Constructor which would raise exceptions if the source entity reference does not have the appropriate type or
     * parent, etc.
     * 
     * @param reference the raw reference to build this object reference from
     */
    public IndexedObjectReference(EntityReference reference)
    {
        super(reference);
    }

    /**
     * Builds an indexed object reference for the object of class {@code className} with index {@code objectNumber} in
     * the document referenced by {@code parent}.
     * 
     * @param className the name of the class of the object
     * @param objectNumber the number of the object in the document, or {@code null} if the default object should be
     *            referenced
     * @param parent reference to the parent document where the object is
     */
    public IndexedObjectReference(String className, Integer objectNumber, EntityReference parent)
    {
        // crap, i'm building the string here to only parse it back in setName. But it shouldn't be that much processing
        super(objectNumber != null ? className + "[" + objectNumber + "]" : className, new DocumentReference(parent));
    }

    /**
     * @return the name of the class of this object, as it was set by caller. I.e. no resolving is done from the value
     *         set in the name of the object.
     */
    public String getClassName()
    {
        return className != null ? className : getName();
    }

    /**
     * @return the number of this object among the objects of the same class in the document, as set by the caller in
     *         [objectNumber] format after the class name (i.e. no resolving is done, existence of this object is not
     *         guaranteed). If no number can be parsed (i.e. [number] cannot be parsed) this function returns {@code
     *         null} and object should be interpreted as the first object of this class in the document.
     */
    public Integer getObjectNumber()
    {
        return objectNumber;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Overridden to always compute the class name and the object number.
     * </p>
     * 
     * @see org.xwiki.model.reference.EntityReference#setName(java.lang.String)
     */ 
    @Override
    protected void setName(String name)
    {
        super.setName(name);

        // set fields to default values
        className = name;
        objectNumber = null;

        // find last encounter of ] (which should be the last character), then last encounter of [, parse in between as
        // number. If anything in this fails, then everything is className
        int closePosition = name.lastIndexOf(']');
        // if there is no ] or is not on the last position
        if (closePosition < 0 || closePosition != name.length() - 1) {
            return;
        }
        // if there is no [
        int openPosition = name.lastIndexOf('[');
        if (openPosition < 0) {
            return;
        }
        // parse the string between the two as object number
        String numberString = name.substring(openPosition + 1, closePosition);
        try {
            objectNumber = Integer.parseInt(numberString);
            className = name.substring(0, openPosition);
        } catch (NumberFormatException e) {
            // number could not be parsed, which means className stays name, and object stays null
            return;
        }
    }
}
