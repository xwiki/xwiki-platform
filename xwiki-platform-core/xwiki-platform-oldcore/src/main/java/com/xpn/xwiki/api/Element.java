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

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseElement;

/**
 * Element is a superclass for any XWiki Class, Object, or Property which might be stored in the database.
 *
 * @version $Id$
 */
public class Element extends Api
{
    /** The internal element which this wraps. */
    protected BaseElement element;

    /**
     * The Constructor. Create a new element wrapping the given internal BaseElement.
     *
     * @param element the internal BaseElement to wrap.
     * @param context the XWikiContext which may be used to get information about the current request.
     */
    public Element(BaseElement element, XWikiContext context)
    {
        super(context);
        this.element = element;
    }

    /**
     * @return the internal BaseElement which this Element wraps.
     */
    protected BaseElement getBaseElement()
    {
        return this.element;
    }

    /**
     * Get the name of this element.
     * If the Element is an XWiki {@link com.xpn.xwiki.api.Object} then it will be the name of the Document
     * containing the Object, if it's an XWiki {@link com.xpn.xwiki.api.Class} it will be the full name of the
     * {@link com.xpn.xwiki.api.Document} where the class is defined, if it's an XWiki 
     * {@link com.xpn.xwiki.api.Property} then it will be the name of the property.
     *
     * @return the name of this Element.
     */
    public String getName()
    {
        return this.element.getName();
    }

    /**
     * @return the reference of the element
     * @since 7.3M1
     */
    public EntityReference getReference()
    {
        return this.element.getReference();
    }

    /**
     * @return the reference to the document
     * @since 10.0
     */
    public DocumentReference getDocumentReference()
    {
        return this.element.getDocumentReference();
    }
}
