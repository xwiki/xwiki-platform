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
package org.xwiki.user.document;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.user.User;
import org.xwiki.user.UserType;

import com.xpn.xwiki.user.api.XWikiRightService;

/**
 * Document-based implementation of a XWiki user.
 *
 * @version $Id$
 * @since 12.2RC1
 */
public class DocumentUser implements User<DocumentReference>
{
    private static final EntityReference USERS_CLASS_REFERENCE =
        new EntityReference("XWikiUsers", EntityType.SPACE,
            new EntityReference("XWiki", EntityType.DOCUMENT));

    private DocumentReference userReference;

    private DocumentReferenceResolver<EntityReference> currentReferenceResolver;

    private DocumentAccessBridge dab;

    private EntityReferenceProvider entityReferenceProvider;

    private boolean isGlobal;

    /**
     * @param userReference the user reference
     * @param dab the component to retrieve user properties stored as xproperties
     * @param currentReferenceResolver the component to resolve user xclass for the current wiki
     * @param entityReferenceProvider the component to check if the current wiki is the main wiki
     */
    public DocumentUser(DocumentReference userReference, DocumentAccessBridge dab,
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

        // These users are necessarily active. Note that superadmin might be main-wiki-prefixed when in a subwiki.
        if (!isGuest() && !isSuperAdmin()) {
            // Default value of active should be 1 (i.e. active) if not set
            Integer value = (Integer) getProperty("active");
            if (value == null || value != 1) {
                active = false;
            }
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
    public boolean isGuest()
    {
        return XWikiRightService.isGuest(getReference());
    }

    @Override
    public boolean isSuperAdmin()
    {
        return XWikiRightService.isSuperAdmin(getReference());
    }

    @Override
    public boolean isGlobal()
    {
        return this.entityReferenceProvider.getDefaultReference(EntityType.WIKI).equals(getReference());
    }

    @Override
    public Object getProperty(String propertyName)
    {
        return this.dab.getProperty(getReference(), getUserClassReference(), propertyName);
    }

    @Override
    public DocumentReference getReference()
    {
        return this.userReference;
    }

    @Override
    public boolean isEmailChecked()
    {
        boolean emailChecked = true;

        // These users always have their emails checked
        if (!isGuest() && !isSuperAdmin()) {
            // Default value of email_checked should be 1 (i.e. checked) if not set.
            Integer value = (Integer) getProperty("email_checked");
            if (value == null || value != 1) {
                emailChecked = false;
            }
        }

        return emailChecked;
    }

    private DocumentReference getUserClassReference()
    {
        return this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE);
    }
}
