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
package org.xwiki.user.internal.document;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.user.User;
import org.xwiki.user.UserType;

/**
 * Document-based implementation of a XWiki user.
 *
 * Always go through a {@link org.xwiki.user.UserResolver} to get a
 * {@link DocumentUser} object (this is why this class is package-protected). The reason is because the resolvers
 * know how to handle Guest and SuperAdmin users properly.
 *
 * @version $Id$
 * @since 12.2RC1
 */
class DocumentUser implements User
{
    static final LocalDocumentReference USERS_CLASS_REFERENCE = new LocalDocumentReference("XWiki", "XWikiUsers");

    private DocumentUserReference userReference;

    private DocumentReferenceResolver<EntityReference> currentReferenceResolver;

    private DocumentAccessBridge dab;

    private EntityReferenceProvider entityReferenceProvider;

    /**
     * @param userReference the user reference
     * @param dab the component to retrieve user properties stored as xproperties
     * @param currentReferenceResolver the component to resolve user xclass for the current wiki
     * @param entityReferenceProvider the component to check if the current wiki is the main wiki
     */
    DocumentUser(DocumentUserReference userReference, DocumentAccessBridge dab,
        DocumentReferenceResolver<EntityReference> currentReferenceResolver,
        EntityReferenceProvider entityReferenceProvider)
    {
        this.userReference = userReference;
        this.dab = dab;
        this.currentReferenceResolver = currentReferenceResolver;
        this.entityReferenceProvider = entityReferenceProvider;
    }

    @Override
    public boolean displayHiddenDocuments()
    {
        Integer preference = (Integer) getProperty("displayHiddenDocuments");
        return preference != null && preference == 1;
    }

    @Override
    public boolean isActive()
    {
        boolean active = true;
        // Default value of active should be 1 (i.e. active) if not set
        Integer value = (Integer) getProperty("active");
        if (value == null || value != 1) {
            active = false;
        }
        return active;
    }

    @Override
    public String getFirstName()
    {
        return (String) getProperty("first_name");
    }

    @Override
    public String getLastName()
    {
        return (String) getProperty("last_name");
    }

    @Override
    public String getEmail()
    {
        return (String) getProperty("email");
    }

    @Override
    public UserType getType()
    {
        return UserType.fromString((String) getProperty("usertype"));
    }

    @Override
    public boolean isGlobal()
    {
        return this.entityReferenceProvider.getDefaultReference(EntityType.WIKI).equals(
            getInternalReference().getWikiReference());
    }

    @Override
    public Object getProperty(String propertyName)
    {
        return this.dab.getProperty(getInternalReference(), getUserClassReference(), propertyName);
    }

    @Override
    public DocumentUserReference getUserReference()
    {
        return this.userReference;
    }

    @Override
    public boolean isEmailChecked()
    {
        boolean emailChecked = true;
        // Default value of email_checked should be 1 (i.e. checked) if not set.
        Integer value = (Integer) getProperty("email_checked");
        if (value == null || value != 1) {
            emailChecked = false;
        }
        return emailChecked;
    }

    private DocumentReference getInternalReference()
    {
        return getUserReference().getReference();
    }

    private DocumentReference getUserClassReference()
    {
        return this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE);
    }
}
