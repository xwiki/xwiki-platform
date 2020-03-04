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

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.xwiki.user.internal.document.DocumentUser.USERS_CLASS_REFERENCE;

/**
 * Unit tests for {@link DocumentUser}.
 *
 * @version $Id$
 */
@ComponentTest
public class DocumentUserTest
{
    @MockComponent
    @Named("user")
    private ConfigurationSource userConfigurationSource;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<EntityReference> currentReferenceResolver;

    @MockComponent
    private EntityReferenceProvider entityReferenceProvider;

    @Test
    void isGlobal()
    {
        DocumentReference reference = new DocumentReference("mainwiki", "space", "user");
        when(this.entityReferenceProvider.getDefaultReference(EntityType.WIKI)).thenReturn(
            new WikiReference("mainwiki"));
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.currentReferenceResolver,
            this.entityReferenceProvider, this.userConfigurationSource);
        assertTrue(user.isGlobal());
    }

    @Test
    void isActive()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.currentReferenceResolver,
            this.entityReferenceProvider, this.userConfigurationSource);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when(this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("active")).thenReturn(new Integer(1));
        assertTrue(user.isActive());

        when(this.userConfigurationSource.getProperty("active")).thenReturn(new Integer(0));
        assertFalse(user.isActive());

        when(this.userConfigurationSource.getProperty("active")).thenReturn(null);
        assertFalse(user.isActive());
    }

    @Test
    void displayHiddenDocuments()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.currentReferenceResolver,
            this.entityReferenceProvider, this.userConfigurationSource);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("displayHiddenDocuments")).thenReturn(new Integer(1));
        assertTrue(user.displayHiddenDocuments());

        when(this.userConfigurationSource.getProperty("displayHiddenDocuments")).thenReturn(new Integer(0));
        assertFalse(user.displayHiddenDocuments());

        when(this.userConfigurationSource.getProperty("displayHiddenDocuments")).thenReturn(null);
        assertFalse(user.displayHiddenDocuments());
    }

    @Test
    void getFirstName()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.currentReferenceResolver,
            this.entityReferenceProvider, this.userConfigurationSource);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("first_name")).thenReturn("John");
        assertEquals("John", user.getFirstName());
    }

    @Test
    void getLastName()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.currentReferenceResolver,
            this.entityReferenceProvider, this.userConfigurationSource);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("last_name")).thenReturn("Doe");
        assertEquals("Doe", user.getLastName());
    }

    @Test
    void getEmail()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.currentReferenceResolver,
            this.entityReferenceProvider, this.userConfigurationSource);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("email")).thenReturn("john@doe.com");
        assertEquals("john@doe.com", user.getEmail());
    }

    @Test
    void getUserType()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.currentReferenceResolver,
            this.entityReferenceProvider, this.userConfigurationSource);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("usertype")).thenReturn("advanced");
        assertEquals(UserType.ADVANCED, user.getType());
    }

    @Test
    void getProperty()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.currentReferenceResolver,
            this.entityReferenceProvider, this.userConfigurationSource);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("property")).thenReturn("value");
        assertEquals("value", user.getProperty("property"));
    }

    @Test
    void isEmailChecked()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.currentReferenceResolver,
            this.entityReferenceProvider, this.userConfigurationSource);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("email_checked")).thenReturn(new Integer(1));
        assertTrue(user.isEmailChecked());

        when(this.userConfigurationSource.getProperty("email_checked")).thenReturn(new Integer(0));
        assertFalse(user.isEmailChecked());

        when(this.userConfigurationSource.getProperty("email_checked")).thenReturn(null);
        assertFalse(user.isEmailChecked());
    }

    @Test
    void getReference()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.currentReferenceResolver,
            this.entityReferenceProvider, this.userConfigurationSource);
        assertEquals(reference, user.getUserReference().getReference());
    }
}
