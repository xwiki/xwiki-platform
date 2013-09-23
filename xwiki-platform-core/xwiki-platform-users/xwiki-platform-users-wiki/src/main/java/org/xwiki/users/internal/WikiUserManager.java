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
package org.xwiki.users.internal;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.users.AbstractUserManager;
import org.xwiki.users.User;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

/**
 * User manager based on wiki documents holding {@code XWiki.XWikiUsers} XObjects.
 * 
 * @version $Id$
 * @since 3.1M2
 */
@Component
@Named("wiki")
@Singleton
public class WikiUserManager extends AbstractUserManager implements Initializable
{
    /** Configuration, used for reading the default wiki and space where new users should be stored. */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    /** Model configuration, provides the default wiki. */
    @Inject
    private ModelConfiguration modelConfiguration;

    /** Model access, used for determining if a document exists or not, and to get the current wiki. */
    @Inject
    private DocumentAccessBridge bridge;

    /** Entity reference serializer to pass to {@link WikiUser} instances. */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /** Entity reference resolver, used for converting usernames into proper document references. */
    @Inject
    @Named("explicit")
    private EntityReferenceResolver<String> nameResolver;

    /** Entity reference resolver to pass to {@link WikiUser} instances. */
    @Inject
    @Named("explicit")
    private EntityReferenceResolver<EntityReference> explicitResolver;

    /** The configured default wiki/space where user profiles are stored. */
    private SpaceReference globalUserReferenceBase;

    /** The configured default space where user profiles are stored. */
    private String defaultSpace;

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize() throws InitializationException
    {
        this.defaultSpace = this.configuration.getProperty("users.defaultUserSpace", "XWiki");
        this.globalUserReferenceBase = new SpaceReference(this.defaultSpace, new WikiReference(
            this.modelConfiguration.getDefaultReferenceValue(EntityType.WIKI)));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.users.UserManager#getUser(String, boolean)
     */
    @Override
    public User getUser(String identifier, boolean force)
    {
        if (StringUtils.isBlank(identifier)) {
            return null;
        }
        DocumentReference reference = getLocalReference(identifier);
        if (!this.bridge.exists(reference)) {
            reference = getDefaultReference(identifier);
        }
        if (!this.bridge.exists(reference)) {
            reference = getGlobalReference(identifier);
        }
        if (this.bridge.exists(reference)) {
            return new WikiUser(reference, this.serializer, this.bridge, this.explicitResolver);
        }
        return force ? new WikiUser(getDefaultReference(identifier), this.serializer, this.bridge,
            this.explicitResolver) : null;
    }

    /**
     * Transform a username into a document reference, belonging to the current wiki.
     * 
     * @param identifier the user identifier to resolve
     * @return a document reference
     */
    private DocumentReference getLocalReference(String identifier)
    {
        WikiReference currentWiki = this.bridge.getCurrentDocumentReference().getWikiReference();
        return new DocumentReference(this.nameResolver.resolve(identifier, EntityType.DOCUMENT,
            new EntityReference(this.defaultSpace, EntityType.SPACE, currentWiki)));
    }

    /**
     * Transform a username into a document reference, belonging to the default wiki where user profiles should be
     * stored.
     * 
     * @param identifier the user identifier to resolve
     * @return a document reference
     */
    private DocumentReference getDefaultReference(String identifier)
    {
        String currentWiki = this.bridge.getCurrentDocumentReference().getWikiReference().getName();
        WikiReference defaultWiki = new WikiReference(this.configuration.getProperty("users.defaultWiki", currentWiki));
        return new DocumentReference(this.nameResolver.resolve(identifier, EntityType.DOCUMENT,
            new EntityReference(this.defaultSpace, EntityType.SPACE, defaultWiki)));
    }

    /**
     * Transform a username into a document reference, belonging to the main wiki.
     * 
     * @param identifier the user identifier to resolve
     * @return a document reference
     */
    private DocumentReference getGlobalReference(String identifier)
    {
        return new DocumentReference(this.nameResolver.resolve(identifier, EntityType.DOCUMENT,
            this.globalUserReferenceBase));
    }
}
