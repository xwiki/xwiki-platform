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

package com.xpn.xwiki.objects;

import java.io.Serializable;

import org.suigeneris.jrcs.diff.Diff;
import org.suigeneris.jrcs.diff.Revision;
import org.suigeneris.jrcs.util.ToString;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.web.Utils;

/**
 * Base class for representing an element having a name (either a reference of a free form name) and a pretty name.
 * 
 * @version $Id$
 */
public abstract class BaseElement<R extends EntityReference> implements ElementInterface, Serializable
{
    /**
     * Full reference of this element.
     * 
     * @since 3.2M1
     */
    protected R referenceCache;

    /**
     * Reference to the document in which this element is defined (for elements where this make sense, for example for
     * an XClass or a XObject).
     */
    private DocumentReference documentReference;

    /**
     * Free form name (for elements which don't point to a reference, for example for instances of {@link BaseProperty}
     * ).
     */
    private String name;

    private String prettyName;

    /**
     * Used to convert a proper Document Reference to a string but without the wiki name.
     */
    private EntityReferenceSerializer<String> localEntityReferenceSerializer = Utils.getComponent(
        EntityReferenceSerializer.class, "local");

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.ElementInterface#getReference()
     */
    public R getReference()
    {
        if (this.referenceCache == null) {
            this.referenceCache = createReference();
        }

        return this.referenceCache;
    }

    /**
     * @since 3.2M1.2M1
     */
    protected R createReference()
    {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.ElementInterface#getDocumentReference()
     * @since 2.2M2
     */
    public DocumentReference getDocumentReference()
    {
        // Object using name without setting a reference are not allowed to retrieve the reference
        if (this.documentReference == null && this.name != null) {
            throw new IllegalStateException(
                "BaseElement#getDocumentReference could not be called when a non-reference Name has been set.");
        }

        return this.documentReference;
    }

    /**
     * Note that this method is used by Hibernate for saving an element. {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.ElementInterface#getName()
     */
    public String getName()
    {
        // If the name is null then serialize the reference as a string.
        if (this.name == null && this.documentReference != null) {
            this.name = this.localEntityReferenceSerializer.serialize(this.documentReference);
        }

        return this.name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.ElementInterface#setDocumentReference(DocumentReference)
     * @since 2.2M2
     */
    public void setDocumentReference(DocumentReference reference)
    {
        // If the name is already set then reset it since we're now using a reference
        this.documentReference = reference;
        this.name = null;
        this.referenceCache = null;
    }

    /**
     * Note that this method is used by Hibernate for loading an element. {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.ElementInterface#setName(java.lang.String)
     */
    public void setName(String name)
    {
        // If a reference is already set, then you cannot set a name
        if (this.documentReference != null) {
            throw new IllegalStateException("BaseElement#setName could not be called when a reference has been set.");
        }

        this.name = name;
        this.referenceCache = null;
    }

    public String getPrettyName()
    {
        return this.prettyName;
    }

    public void setPrettyName(String name)
    {
        this.prettyName = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object el)
    {
        // Same Java object, they sure are equal
        if (this == el) {
            return true;
        }

        if (el == null || !(el.getClass().equals(this.getClass()))) {
            return false;
        }

        BaseElement element = (BaseElement) el;

        if (element.documentReference != null) {
            if (!element.documentReference.equals(this.documentReference)) {
                return false;
            }
        } else {
            if (this.documentReference != null) {
                return false;
            }
            if (element.name == null) {
                if (this.name != null) {
                    return false;
                }
            } else if (!element.name.equals(this.name)) {
                return false;
            }
        }

        if (element.getPrettyName() == null) {
            if (getPrettyName() != null) {
                return false;
            }
        } else if (!element.getPrettyName().equals(getPrettyName())) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone()
    {
        BaseElement element;
        try {
            element = getClass().newInstance();

            // Make sure we clone either the reference or the name depending on which one is used.
            if (this.documentReference != null) {
                element.setDocumentReference(getDocumentReference());
            } else if (this.name != null) {
                element.setName(getName());
            }

            element.setPrettyName(getPrettyName());
        } catch (Exception e) {
            // This should not happen
            element = null;
        }

        return element;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.ElementInterface#merge(com.xpn.xwiki.objects.ElementInterface,
     *      com.xpn.xwiki.objects.ElementInterface, com.xpn.xwiki.XWikiContext, com.xpn.xwiki.doc.merge.MergeResult)
     */
    @Override
    public void merge(ElementInterface previousElement, ElementInterface newElement, XWikiContext context,
        MergeResult mergeResult)
    {
        setPrettyName(mergeString(((BaseElement) previousElement).getPrettyName(),
            ((BaseElement) newElement).getPrettyName(), getPrettyName(), mergeResult));
    }

    /**
     * Try to apply a 3 ways merge of provided String.
     * 
     * @param previousStr previous version of the string
     * @param newStr new version of the string
     * @param currentStr current version of the string
     * @param mergeResult merge report
     * @return the merged value of the string
     * @since 3.2M1
     */
    protected String mergeString(String previousStr, String newStr, String currentStr, MergeResult mergeResult)
    {
        String result = currentStr;

        try {
            Revision revision = Diff.diff(ToString.stringToArray(previousStr), ToString.stringToArray(newStr));
            if (revision.size() > 0) {
                result = ToString.arrayToString(revision.patch(ToString.stringToArray(currentStr)));

                mergeResult.setModified(true);
            }
        } catch (Exception e) {
            mergeResult.getErrors().add(e);
        }

        return result;
    }
}
