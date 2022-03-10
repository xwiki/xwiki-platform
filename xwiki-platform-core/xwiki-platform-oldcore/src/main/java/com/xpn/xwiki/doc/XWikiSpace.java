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
package com.xpn.xwiki.doc;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.internal.reference.LocalUidStringEntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.SpaceReferenceResolver;

import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

/**
 * Represent a space.
 *
 * @version $Id$
 * @since 7.2M1
 */
public class XWikiSpace
{
    private static final String CURRENT = "current";

    private static final LocalStringEntityReferenceSerializer SERIALIZER =
        new LocalStringEntityReferenceSerializer(new DefaultSymbolScheme());

    private XWikiStoreInterface store;

    /**
     * Synthetic id.
     */
    private Long id;

    private String name;

    private String parent;

    private boolean hidden;

    /**
     * @see #getReference()
     */
    private SpaceReference reference;

    /**
     * Required by Hibernate.
     */
    protected XWikiSpace()
    {
        this(null, null);
    }

    /**
     * @param reference the complete reference of the space
     */
    public XWikiSpace(SpaceReference reference)
    {
        this(reference, null);
    }

    /**
     * @param reference the complete reference of the space
     * @param hidden true if all the documents in the space are hidden
     */
    public XWikiSpace(SpaceReference reference, boolean hidden)
    {
        this(reference, null);

        setHidden(hidden);
    }

    /**
     * @param reference the complete reference of the space
     * @param store the store where the {@link XWikiSpace} instance come from
     */
    public XWikiSpace(SpaceReference reference, XWikiStoreInterface store)
    {
        this.reference = reference;
        this.store = store;
    }

    private static SpaceReferenceResolver<String> getCurrentSpaceResolver()
    {
        return Utils.getComponent(SpaceReferenceResolver.TYPE_STRING, CURRENT);
    }

    /**
     * @return the store where the {@link XWikiSpace} instance come from
     */
    public XWikiStoreInterface getStore()
    {
        return this.store;
    }

    /**
     * Set the store of the space.
     *
     * @param store the store to set.
     * @since 12.5RC1
     */
    public void setStore(XWikiStoreInterface store)
    {
        this.store = store;
    }

    /**
     * Compute a XWikiSpace ID based on a reference.
     *
     * @param spaceReference the reference from which to compute the ID
     * @return an ID that can be used to retrieve the space in DB.
     * @since 12.5RC1
     */
    public static long getId(SpaceReference spaceReference)
    {
        return Util.getHash(LocalUidStringEntityReferenceSerializer.INSTANCE.serialize(spaceReference));
    }

    /**
     * @return the synthetic id of this deleted document. unique only for document.
     */
    public long getId()
    {
        if (this.id == null) {
            this.id = getId(getSpaceReference());
        }

        return this.id;
    }

    /**
     * Required by Hibernate.
     * 
     * @param id the synthetic id to set.
     */
    protected void setId(long id)
    {
        this.id = id;
    }

    /**
     * @return the name of the space.
     */
    public String getName()
    {
        if (this.name == null && this.reference != null) {
            this.name = this.reference.getName();
        }

        return this.name;
    }

    /**
     * Required by Hibernate.
     * 
     * @param name the name of the space
     */
    protected void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the reference of the space parent
     */
    public String getParent()
    {
        if (this.parent == null && this.reference != null) {
            this.parent =
                this.reference.getParent().getType() == EntityType.SPACE ? SERIALIZER.serialize(this.reference
                    .getParent()) : "";
        }

        return StringUtils.isEmpty(this.parent) ? null : this.parent;
    }

    /**
     * Required by Hibernate.
     * 
     * @param parent the reference of the space parent
     */
    protected void setParent(String parent)
    {
        this.parent = StringUtils.isEmpty(parent) ? "" : parent;
    }

    /**
     * @return the complete reference of the space
     */
    public String getReference()
    {
        return SERIALIZER.serialize(this.reference);
    }

    /**
     * @return true if all the documents in the space are hidden
     */
    public boolean isHidden()
    {
        return this.hidden;
    }

    /**
     * @param hidden true if all the documents in the space are hidden
     */
    public void setHidden(boolean hidden)
    {
        this.hidden = hidden;
    }

    /**
     * Required by Hibernate.
     * 
     * @param reference
     */
    protected void setReference(String reference)
    {
        this.reference = getCurrentSpaceResolver().resolve(reference);
    }

    /**
     * @return the complete reference of the space
     */
    public SpaceReference getSpaceReference()
    {
        return this.reference;
    }

    @Override
    public String toString()
    {
        return getSpaceReference().toString();
    }
}
