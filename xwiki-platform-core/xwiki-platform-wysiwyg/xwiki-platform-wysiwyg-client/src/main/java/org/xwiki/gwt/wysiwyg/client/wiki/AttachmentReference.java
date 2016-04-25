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
package org.xwiki.gwt.wysiwyg.client.wiki;

import org.xwiki.gwt.user.client.StringUtils;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityReference.EntityType;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * An attachment reference.
 * 
 * @version $Id$
 */
public class AttachmentReference implements IsSerializable
{
    /**
     * The component that stores the name of the file that hosts the referenced entity.
     */
    public static final String FILE_NAME = "fileName";

    /**
     * The underlying, untyped, entity reference.
     */
    private EntityReference entityReference;

    /**
     * A reference to the wiki page that holds the referenced attachment.
     */
    private WikiPageReference wikiPageReference;

    /**
     * Default constructor.
     */
    public AttachmentReference()
    {
        this(new EntityReference());
        entityReference.setType(EntityType.ATTACHMENT);
    }

    /**
     * Creates a typed attachment reference.
     * 
     * @param entityReference an entity reference
     */
    public AttachmentReference(EntityReference entityReference)
    {
        this.entityReference = entityReference;
        wikiPageReference = new WikiPageReference(entityReference);
    }

    /**
     * Creates a new typed reference to the specified attachment.
     * 
     * @param fileName the file name
     * @param wikiPageReference a reference to the wiki page that holds the attachment
     */
    public AttachmentReference(String fileName, WikiPageReference wikiPageReference)
    {
        this(wikiPageReference.getEntityReference().clone());
        entityReference.setType(EntityType.ATTACHMENT);
        setFileName(fileName);
    }

    /**
     * @return the name of the file that hosts the referenced entity
     */
    public String getFileName()
    {
        return entityReference.getComponent(FILE_NAME);
    }

    /**
     * Sets the name of the file that hosts the referenced entity.
     * 
     * @param fileName the name of the file that hosts the referenced entity
     */
    public void setFileName(String fileName)
    {
        entityReference.setComponent(FILE_NAME, fileName);
    }

    /**
     * @return a reference to the wiki page that holds the referenced attachment
     */
    public WikiPageReference getWikiPageReference()
    {
        return wikiPageReference;
    }

    /**
     * @return the underlying, untyped, entity reference
     */
    public EntityReference getEntityReference()
    {
        return entityReference;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        String fileName = getFileName();
        result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
        result = prime * result + ((wikiPageReference == null) ? 0 : wikiPageReference.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof AttachmentReference)) {
            return false;
        }
        AttachmentReference other = (AttachmentReference) obj;
        return StringUtils.areEqual(getFileName(), other.getFileName()) && wikiPageReference == null
            ? other.wikiPageReference == null : wikiPageReference.equals(other.wikiPageReference);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#clone()
     */
    public AttachmentReference clone()
    {
        return new AttachmentReference(entityReference.clone());
    }
}
