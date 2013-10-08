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
package org.xwiki.wiki;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.stability.Unstable;
import org.xwiki.wiki.properties.WikiPropertiesGroup;

/**
 * This class is a descriptor for wiki.
 *
 * @version $Id$
 * @since 5.3M1
 */
@Unstable
public class WikiDescriptor
{
    /**
     * Default alias index.
     */
    private static final int DEFAULT_ALIAS_INDEX = 0;

    /**
     * The ID is the unique identifier that designate this wiki.
     */
    private String id;

    /**
     * Alias are names that can be used to designate this wiki in several places, like the URL.
     */
    private List<String> aliases = new ArrayList<String>();

    /**
     * Pretty name.
     */
    private String prettyName;

    /**
     * Default page.
     */
    private LocalDocumentReference mainPageReference;

    /**
     * The owner of the wiki.
     */
    private String ownerId;

    /**
     * Hidden.
     */
    private boolean isHidden;

    /**
     * Properties groups that new modules can use to store their own value in the wiki descriptor.
     */
    private Map<String, WikiPropertiesGroup> propertiesGroups;

    /**
     * Constructor.
     *
     * @param id Unique Id of the wiki
     * @param defaultAlias Default alias of the wiki
     */
    public WikiDescriptor(String id, String defaultAlias)
    {
        this.id = id;
        this.aliases = new ArrayList<String>();
        setDefaultAlias(defaultAlias);
    }

    /**
     * @return the unique Id of the wiki.
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * The default alias is the alias used to generate URL for that wiki.
     *
     * @return the default alias.
     */
    public String getDefaultAlias()
    {
        return aliases.get(DEFAULT_ALIAS_INDEX);
    }

    /**
     * Set the default alias.
     *
     * @param alias the new default alias
     */
    public void setDefaultAlias(String alias)
    {
        aliases.set(DEFAULT_ALIAS_INDEX, alias);
    }

    /**
     * @param alias the new alias to add
     */
    public void addAlias(String alias)
    {
        aliases.add(alias);
    }

    /**
     * @return all aliases
     */
    public List<String> getAliases()
    {
        return aliases;
    }

    /**
     * @return the pretty name of the wiki
     */
    public String getPrettyName()
    {
        return prettyName;
    }

    /**
     * @param prettyName the new pretty name
     */
    public void setPrettyName(String prettyName)
    {
        this.prettyName = prettyName;
    }

    /**
     * @return a reference to that wiki
     */
    public WikiReference getReference()
    {
        return new WikiReference(getId());
    }

    /**
     * @return a reference to the main page of the wiki
     */
    public DocumentReference getMainPageReference()
    {
        return new DocumentReference(getId(), mainPageReference.getParent().getName(), mainPageReference.getName());
    }

    /**
     * @return the Id of the owner of the wiki
     */
    public String getOwnerId()
    {
        return ownerId;
    }

    /**
     * @return if the wiki is hidden
     */
    public boolean isHidden()
    {
        return isHidden;
    }

    /**
     * Set if the wiki is hidden.
     * @param hidden if the wiki is hidden or not
     */
    public void setHidden(boolean hidden)
    {
        this.isHidden = hidden;
    }

    /**
     * @param propertiesGroupId the id of the properties group to retrieve
     * @return the properties group corresponding to the id, or null if no properties group correspond to that Id.
     */
    public WikiPropertiesGroup getPropertiesGroup(String propertiesGroupId)
    {
        return propertiesGroups.get(propertiesGroupId);
    }

    /**
     * Add a properties group to the wiki.
     *
     * @param group properties group to add
     */
    public void addPropertiesGroup(WikiPropertiesGroup group)
    {
        propertiesGroups.put(group.getId(), group);
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(3, 3)
                .append(getDefaultAlias())
                .append(getId())
                .toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        WikiDescriptor rhs = (WikiDescriptor) object;
        return new EqualsBuilder()
                .append(getDefaultAlias(), rhs.getDefaultAlias())
                .append(getId(), rhs.getId())
                .isEquals();
    }
}
