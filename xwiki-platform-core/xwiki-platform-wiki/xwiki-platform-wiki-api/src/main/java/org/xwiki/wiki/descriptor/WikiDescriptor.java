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
package org.xwiki.wiki.descriptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.wiki.properties.WikiPropertyGroup;

/**
 * This class is a descriptor for wiki.
 *
 * @version $Id$
 * @since 5.3M2
 */
public class WikiDescriptor implements Cloneable
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
    private List<String> aliases = new ArrayList<>();

    /**
     * Pretty name.
     */
    private String prettyName;

    /**
     * Default page.
     */
    private LocalDocumentReference mainPageReference = new LocalDocumentReference("Main", "WebHome");

    /**
     * The owner of the wiki.
     */
    private String ownerId;

    /**
     * Hidden.
     */
    private boolean isHidden;

    /**
     * Description.
     */
    private String description;

    /**
     * @see #isSecure()
     */
    private Boolean secure;

    /**
     * @see #getPort()
     */
    private int port = -1;

    /**
     * Properties groups that new modules can use to store their own value in the wiki descriptor.
     */
    private Map<String, WikiPropertyGroup> propertyGroups;

    /**
     * Constructor.
     *
     * @param id Unique Id of the wiki
     * @param defaultAlias Default alias of the wiki
     */
    public WikiDescriptor(String id, String defaultAlias)
    {
        this.id = id;
        this.aliases = new ArrayList<>();
        this.propertyGroups = new HashMap<>();
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
        if (aliases.isEmpty()) {
            aliases.add(alias);
        } else {
            aliases.set(DEFAULT_ALIAS_INDEX, alias);
        }
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
     * @return the Id of the owner of the wiki
     */
    public String getOwnerId()
    {
        return ownerId;
    }

    /**
     * @param ownerId the Id of the owner of the wiki
     */
    public void setOwnerId(String ownerId)
    {
        this.ownerId = ownerId;
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
        return new DocumentReference(mainPageReference, new WikiReference(getId()));
    }

    /**
     * @param reference Reference to the main page of the wiki
     */
    public void setMainPageReference(DocumentReference reference)
    {
        this.mainPageReference = new LocalDocumentReference(reference);
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
     *
     * @param hidden if the wiki is hidden or not
     */
    public void setHidden(boolean hidden)
    {
        this.isHidden = hidden;
    }

    /**
     * @return the wiki description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return true if the wiki should be accessed trough a secure connection (HTTPS), null means default
     * @since 10.7RC1
     */
    public Boolean isSecure()
    {
        return this.secure;
    }

    /**
     * @param secure true if the wiki should be accessed trough a secure connection (HTTPS), null means default
     * @since 10.7RC1
     */
    public void setSecure(Boolean secure)
    {
        this.secure = secure;
    }

    /**
     * @return the port to use when generating external URL for the wiki, -1 means default
     * @since 10.7RC1
     */
    public int getPort()
    {
        return this.port;
    }

    /**
     * @param port the port to use when generating external URL for the wiki, -1 means default
     * @since 10.7RC1
     */
    public void setPort(int port)
    {
        this.port = port;
    }

    /**
     * @param propertyGroupId the id of the property group to retrieve
     * @return the property group corresponding to the id, or null if no property group correspond to that Id.
     */
    public WikiPropertyGroup getPropertyGroup(String propertyGroupId)
    {
        return propertyGroups.get(propertyGroupId);
    }

    /**
     * Add a property group to the wiki.
     *
     * @param group property group to add
     */
    public void addPropertyGroup(WikiPropertyGroup group)
    {
        propertyGroups.put(group.getId(), group);
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(3, 3).append(getDefaultAlias()).append(getId()).toHashCode();
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
        return new EqualsBuilder().append(getDefaultAlias(), rhs.getDefaultAlias()).append(getId(), rhs.getId())
            .isEquals();
    }

    @Override
    public WikiDescriptor clone()
    {
        WikiDescriptor descriptor;
        try {
            descriptor = (WikiDescriptor) super.clone();
        } catch (CloneNotSupportedException e) {
            // Supposed to be impossible
            descriptor = new WikiDescriptor(getDescription(), getDefaultAlias());
        }

        // Clone aliases
        descriptor.aliases = new ArrayList<>(this.aliases);

        // Clone properties
        descriptor.propertyGroups = new HashMap<>(this.propertyGroups.size());
        for (WikiPropertyGroup group : this.propertyGroups.values()) {
            descriptor.propertyGroups.put(group.getId(), group.clone());
        }

        return descriptor;
    }
}
