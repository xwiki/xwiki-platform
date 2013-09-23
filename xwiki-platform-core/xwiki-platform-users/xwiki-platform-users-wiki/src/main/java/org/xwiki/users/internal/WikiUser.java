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

import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.users.AbstractUser;

/**
 * Class representing a XWiki User based on wiki documents holding {@code XWiki.XWikiUsers} XObjects.
 * 
 * @version $Id$
 * @since 5.3M1
 */
public class WikiUser extends AbstractUser
{
    /** A stub for the reference to the user profile class. */
    private static final EntityReference USER_CLASS =
        new EntityReference("XWikiUsers", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    /** A link to the XClass defining user profiles in the user's wiki. */
    private DocumentReference classReference;

    /** Model access, used for reading profile properties. */
    private DocumentAccessBridge bridge;

    /** Entity reference resolver, used for obtaining a full reference to the user profile class. */
    private EntityReferenceResolver<EntityReference> resolver;

    /**
     * Constructor.
     * 
     * @param reference reference to the user profile document
     * @param serializer the entity reference serializer to use
     * @param bridge the model access bridge to use
     * @param resolver the reference resolver to use
     */
    public WikiUser(DocumentReference reference, EntityReferenceSerializer<String> serializer,
        DocumentAccessBridge bridge, EntityReferenceResolver<EntityReference> resolver)
    {
        this.profileReference = reference;
        this.serializer = serializer;
        this.bridge = bridge;
        this.resolver = resolver;
        if (this.profileReference != null) {
            this.classReference =
                new DocumentReference(this.resolver.resolve(USER_CLASS, EntityType.DOCUMENT, this.profileReference));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.users.User#exists()
     */
    public boolean exists()
    {
        return (this.profileReference == null) ? false : this.bridge.exists(this.profileReference);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.users.User#getName()
     */
    public String getName()
    {
        if (this.profileReference == null) {
            return "";
        }
        String result = this.bridge.getProperty(this.profileReference, this.classReference, "first_name") + "";
        result = StringUtils.trim(result) + " ";
        result +=
            StringUtils.trim("" + this.bridge.getProperty(this.profileReference, this.classReference, "last_name"));
        if (StringUtils.isBlank(result)) {
            result = getUsername();
        }
        return result.trim();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.users.User#getAttribute(String)
     */
    public Object getAttribute(String attributeName)
    {
        if (this.profileReference == null) {
            return null;
        }
        return this.bridge.getProperty(this.profileReference, this.classReference, attributeName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.users.User#getProfileURI()
     */
    public URI getProfileURI()
    {
        if (this.profileReference != null) {
            try {
                return new URI(this.bridge.getDocumentURL(this.profileReference, "view", null, null));
            } catch (URISyntaxException ex) {
                // Shouldn't happen, bug in the model bridge
            }
        }
        return null;
    }
}
