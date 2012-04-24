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
package com.xpn.xwiki.objects;

import java.io.Serializable;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.internal.merge.MergeUtils;
import com.xpn.xwiki.util.Util;
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
    protected EntityReferenceSerializer<String> localEntityReferenceSerializer = Utils.getComponent(
        EntityReferenceSerializer.TYPE_STRING, "local");

    /**
     * Used to build uid string for the getId() hash.
     */
    private EntityReferenceSerializer<String> localUidStringEntityReferenceSerializer;

    @Override
    public R getReference()
    {
        if (this.referenceCache == null) {
            this.referenceCache = createReference();
        }

        return this.referenceCache;
    }

    /**
     * @since 3.2M1
     */
    protected R createReference()
    {
        return null;
    }

    @Override
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
    @Override
    public String getName()
    {
        // If the name is null then serialize the reference as a string.
        if (this.name == null && this.documentReference != null) {
            this.name = this.localEntityReferenceSerializer.serialize(this.documentReference);
        }

        return this.name;
    }

    @Override
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
    @Override
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
     * @return return the LocalUidStringEntityReferenceSerializer to compute ids.
     * @since 4.0M1
     */
    protected EntityReferenceSerializer<String> getLocalUidStringEntityReferenceSerializer()
    {
        if (this.localUidStringEntityReferenceSerializer == null) {
            this.localUidStringEntityReferenceSerializer =
                Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local/uid");
        }

        return this.localUidStringEntityReferenceSerializer;
    }

    /**
     * @return a unique identifier representing this element reference to be used for {@code hashCode()}.
     * @since 4.0M1
     */
    protected String getLocalKey()
    {
        // The R40000XWIKI6990DataMigration use the same algorithm to compute object id. It should be properly synced.
        return getLocalUidStringEntityReferenceSerializer().serialize(getReference());
    }

    /**
     * Return an truncated MD5 hash of the local key computed in {@link #getLocalKey()}.
     * 
     * @return the identifier used by hibernate for storage.
     * @since 4.0M1
     */
    public long getId()
    {
        // The R40000XWIKI6990DataMigration use the same algorithm to compute object id. It should be properly synced.
        return Util.getHash(getLocalKey());
    }

    /**
     * Dummy function, do hibernate is always happy.
     * 
     * @param id the identifier assigned by hibernate.
     * @since 4.0M1
     */
    public void setId(long id)
    {
    }

    @Override
    public int hashCode()
    {
        return (int) Util.getHash(getLocalKey());
    }

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

    @Override
    public BaseElement clone()
    {
        BaseElement element;
        try {
            element = (BaseElement) super.clone();

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

    @Override
    public void merge(ElementInterface previousElement, ElementInterface newElement, MergeConfiguration configuration,
        XWikiContext context, MergeResult mergeResult)
    {
        setPrettyName(MergeUtils.mergeString(((BaseElement) previousElement).getPrettyName(),
            ((BaseElement) newElement).getPrettyName(), getPrettyName(), mergeResult));
    }
}
