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

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * References a server side entity.
 * 
 * @version $Id$
 */
public class EntityReference implements IsSerializable
{
    /**
     * Represents a type of entity that can be referenced from the client.
     */
    public static enum EntityType
    {
        /**
         * Represents a Document Entity.
         */
        DOCUMENT,

        /**
         * Represents an Attachment Entity.
         */
        ATTACHMENT,

        /**
         * Represents an Image entity.
         */
        IMAGE;

        /**
         * @param expected the expected type
         * @param actual the actual type
         * @return {@code true} if the given types are equal, {@code false} otherwise
         */
        public static boolean areEqual(EntityType expected, EntityType actual)
        {
            return expected == actual || (expected != null && expected.equals(actual));
        }
    }

    /**
     * The type of entity being referenced.
     */
    private EntityType type;

    /**
     * The name of the wiki that hosts the referenced entity.
     */
    private String wikiName;

    /**
     * The name of the space that hosts the referenced entity.
     */
    private String spaceName;

    /**
     * The name of the page that hosts the referenced entity.
     */
    private String pageName;

    /**
     * The name of the file that hosts the referenced entity.
     */
    private String fileName;

    /**
     * @return the type of entity being referenced
     */
    public EntityType getType()
    {
        return type;
    }

    /**
     * Sets the type of entity being referenced.
     * 
     * @param type the entity type
     */
    public void setType(EntityType type)
    {
        this.type = type;
    }

    /**
     * @return the name of the wiki that hosts the referenced entity
     */
    public String getWikiName()
    {
        return wikiName;
    }

    /**
     * Sets the name of the wiki that hosts the referenced entity.
     * 
     * @param wikiName the name of the wiki that hosts the referenced entity
     */
    public void setWikiName(String wikiName)
    {
        this.wikiName = wikiName;
    }

    /**
     * @return the name of the space that hosts the referenced entity
     */
    public String getSpaceName()
    {
        return spaceName;
    }

    /**
     * Sets the name of the space that hosts the referenced entity.
     * 
     * @param spaceName the name of the space that hosts the referenced entity
     */
    public void setSpaceName(String spaceName)
    {
        this.spaceName = spaceName;
    }

    /**
     * @return the name of the page that hosts the referenced entity
     */
    public String getPageName()
    {
        return pageName;
    }

    /**
     * Sets the name of the page that hosts the referenced entity.
     * 
     * @param pageName the name of the page that hosts the referenced entity
     */
    public void setPageName(String pageName)
    {
        this.pageName = pageName;
    }

    /**
     * @return the name of the file that hosts the referenced entity
     */
    public String getFileName()
    {
        return fileName;
    }

    /**
     * Sets the name of the file that hosts the referenced entity.
     * 
     * @param fileName the name of the file that hosts the referenced entity
     */
    public void setFileName(String fileName)
    {
        this.fileName = fileName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
        result = prime * result + ((pageName == null) ? 0 : pageName.hashCode());
        result = prime * result + ((spaceName == null) ? 0 : spaceName.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        result = prime * result + ((wikiName == null) ? 0 : wikiName.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof EntityReference)) {
            return false;
        }
        EntityReference other = (EntityReference) obj;
        return EntityType.areEqual(type, other.type) && StringUtils.areEqual(fileName, other.fileName)
            && StringUtils.areEqual(wikiName, other.wikiName) && StringUtils.areEqual(spaceName, other.spaceName)
            && StringUtils.areEqual(pageName, other.pageName) && StringUtils.areEqual(fileName, other.fileName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Object#clone()
     */
    public EntityReference clone()
    {
        EntityReference clone = new EntityReference();
        clone.setType(type);
        clone.setWikiName(wikiName);
        clone.setSpaceName(spaceName);
        clone.setPageName(pageName);
        clone.setFileName(fileName);
        return clone;
    }
}
