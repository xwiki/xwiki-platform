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
 * A reference to a wiki page.
 * 
 * @version $Id$
 */
public class WikiPageReference implements IsSerializable
{
    /**
     * The component that stores the name of the wiki that hosts the referenced entity.
     */
    public static final String WIKI_NAME = "wikiName";

    /**
     * The component that stores the name of the space that hosts the referenced entity.
     */
    public static final String SPACE_NAME = "spaceName";

    /**
     * The component that stores the name of the page that hosts the referenced entity.
     */
    public static final String PAGE_NAME = "pageName";

    /**
     * The underlying, untyped, entity reference.
     */
    private EntityReference entityReference;

    /**
     * Default constructor.
     */
    public WikiPageReference()
    {
        this(new EntityReference());
        entityReference.setType(EntityType.DOCUMENT);
    }

    /**
     * Creates a typed wiki page reference.
     * 
     * @param entityReference an entity reference
     */
    public WikiPageReference(EntityReference entityReference)
    {
        this.entityReference = entityReference;
    }

    /**
     * Creates a reference to the specified wiki page.
     * 
     * @param wikiName the name of the wiki that hosts the page
     * @param spaceName the name of the space that hosts the page
     * @param pageName the name of the page
     */
    public WikiPageReference(String wikiName, String spaceName, String pageName)
    {
        this(new EntityReference());
        entityReference.setType(EntityType.DOCUMENT);
        setWikiName(wikiName);
        setSpaceName(spaceName);
        setPageName(pageName);
    }

    /**
     * @return the name of the wiki that hosts the referenced entity
     */
    public String getWikiName()
    {
        return entityReference.getComponent(WIKI_NAME);
    }

    /**
     * Sets the name of the wiki that hosts the referenced entity.
     * 
     * @param wikiName the name of the wiki that hosts the referenced entity
     */
    public void setWikiName(String wikiName)
    {
        entityReference.setComponent(WIKI_NAME, wikiName);
    }

    /**
     * @return the name of the space that hosts the referenced entity
     */
    public String getSpaceName()
    {
        return entityReference.getComponent(SPACE_NAME);
    }

    /**
     * @return the space pretty name
     */
    public native String getSpacePrettyName()
    /*-{
        var localSpaceReference = this.@org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference::getSpaceName()();
        var spaceReference = $wnd.XWiki.Model.resolve(localSpaceReference, $wnd.XWiki.EntityType.SPACE);
        var spaceNames = [];
        while (spaceReference) {
          spaceNames.push(spaceReference.name);
          spaceReference = spaceReference.parent;
        }
        return spaceNames.reverse().join(' \u00BB ');
    }-*/;

    /**
     * Sets the name of the space that hosts the referenced entity.
     * 
     * @param spaceName the name of the space that hosts the referenced entity
     */
    public void setSpaceName(String spaceName)
    {
        entityReference.setComponent(SPACE_NAME, spaceName);
    }

    /**
     * @return the name of the page that hosts the referenced entity
     */
    public String getPageName()
    {
        return entityReference.getComponent(PAGE_NAME);
    }

    /**
     * Sets the name of the page that hosts the referenced entity.
     * 
     * @param pageName the name of the page that hosts the referenced entity
     */
    public void setPageName(String pageName)
    {
        entityReference.setComponent(PAGE_NAME, pageName);
    }

    /**
     * @return the underlying, untyped, entity reference
     */
    public EntityReference getEntityReference()
    {
        return entityReference;
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
        String wikiName = getWikiName();
        result = prime * result + ((wikiName == null) ? 0 : wikiName.hashCode());
        String spaceName = getSpaceName();
        result = prime * result + ((spaceName == null) ? 0 : spaceName.hashCode());
        String pageName = getPageName();
        result = prime * result + ((pageName == null) ? 0 : pageName.hashCode());
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
        if (!(obj instanceof WikiPageReference)) {
            return false;
        }
        WikiPageReference other = (WikiPageReference) obj;
        return StringUtils.areEqual(getWikiName(), other.getWikiName())
            && StringUtils.areEqual(getSpaceName(), other.getSpaceName())
            && StringUtils.areEqual(getPageName(), other.getPageName());
    }
}
