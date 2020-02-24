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
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DocumentUser}.
 *
 * @version $Id$
 */
@ComponentTest
public class DocumentUserTest
{
    private static final EntityReference USERS_CLASS_REFERENCE =
        new EntityReference("XWikiUsers", EntityType.SPACE,
            new EntityReference("XWiki", EntityType.DOCUMENT));

    @MockComponent
    private DocumentAccessBridge dab;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<EntityReference> currentReferenceResolver;

    @MockComponent
    private
    EntityReferenceProvider entityReferenceProvider;

    @Test
    void isGuest()
    {
        DocumentUser user = new DocumentUser(new DocumentUserReference(new DocumentReference("wiki", "space", "user")),
            this.dab, this.currentReferenceResolver, this.entityReferenceProvider);
        assertFalse(user.isGuest());

        user = new DocumentUser(new DocumentUserReference(new DocumentReference("wiki", "space", "XWikiGuest")),
            this.dab, this.currentReferenceResolver, this.entityReferenceProvider);
        assertTrue(user.isGuest());

        user = new DocumentUser(new DocumentUserReference(new DocumentReference("wiki", "space", "XwIkIGuEsT")),
            this.dab, this.currentReferenceResolver, this.entityReferenceProvider);
        assertTrue(user.isGuest());
    }

    @Test
    void isSuperAdmin()
    {
        DocumentUser user = new DocumentUser(new DocumentUserReference(new DocumentReference("wiki", "space", "user")),
            this.dab, this.currentReferenceResolver, this.entityReferenceProvider);
        assertFalse(user.isSuperAdmin());

        user = new DocumentUser(new DocumentUserReference(new DocumentReference("wiki", "space", "superadmin")),
            this.dab, this.currentReferenceResolver, this.entityReferenceProvider);
        assertTrue(user.isSuperAdmin());

        user = new DocumentUser(new DocumentUserReference(new DocumentReference("wiki", "space", "SuPeRAdMiN")),
            this.dab, this.currentReferenceResolver, this.entityReferenceProvider);
        assertTrue(user.isSuperAdmin());
    }

    @Test
    void isActive()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.dab,
            this.currentReferenceResolver, this.entityReferenceProvider);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.dab.getProperty(reference, classReference, "active")).thenReturn(new Integer(1));
        assertTrue(user.isActive());

        when(this.dab.getProperty(reference, classReference, "active")).thenReturn(new Integer(0));
        assertFalse(user.isActive());

        when(this.dab.getProperty(reference, classReference, "active")).thenReturn(null);
        assertFalse(user.isActive());

        reference = new DocumentReference("wiki", "space", "superadmin");
        user = new DocumentUser(new DocumentUserReference(reference), this.dab, this.currentReferenceResolver,
            this.entityReferenceProvider);
        assertTrue(user.isActive());

        reference = new DocumentReference("wiki", "space", "XWikiGuest");
        user = new DocumentUser(new DocumentUserReference(reference), this.dab, this.currentReferenceResolver,
            this.entityReferenceProvider);
        assertTrue(user.isActive());
    }

    @Test
    void displayHiddenDocuments()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.dab, this.currentReferenceResolver,
            this.entityReferenceProvider);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.dab.getProperty(reference, classReference, "displayHiddenDocuments")).thenReturn(new Integer(1));
        assertTrue(user.displayHiddenDocuments());

        when(this.dab.getProperty(reference, classReference, "displayHiddenDocuments")).thenReturn(new Integer(0));
        assertFalse(user.displayHiddenDocuments());

        when(this.dab.getProperty(reference, classReference, "displayHiddenDocuments")).thenReturn(null);
        assertFalse(user.displayHiddenDocuments());

        reference = new DocumentReference("wiki", "space", "superadmin");
        user = new DocumentUser(new DocumentUserReference(reference), this.dab, this.currentReferenceResolver,
            this.entityReferenceProvider);
        assertTrue(user.displayHiddenDocuments());

        reference = new DocumentReference("wiki", "space", "XWikiGuest");
        user = new DocumentUser(new DocumentUserReference(reference), this.dab, this.currentReferenceResolver,
            this.entityReferenceProvider);
        assertFalse(user.displayHiddenDocuments());
    }

    @Test
    void getFirstName()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.dab,
            this.currentReferenceResolver, this.entityReferenceProvider);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.dab.getProperty(reference, classReference, "first_name")).thenReturn("John");
        assertEquals("John", user.getFirstName());
    }

    @Test
    void getLastName()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.dab,
            this.currentReferenceResolver, this.entityReferenceProvider);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.dab.getProperty(reference, classReference, "last_name")).thenReturn("Doe");
        assertEquals("Doe", user.getLastName());
    }

    @Test
    void getEmail()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.dab,
            this.currentReferenceResolver, this.entityReferenceProvider);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.dab.getProperty(reference, classReference, "email")).thenReturn("john@doe.com");
        assertEquals("john@doe.com", user.getEmail());
    }

    @Test
    void getUserType()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.dab, this.currentReferenceResolver,
            this.entityReferenceProvider);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.dab.getProperty(reference, classReference, "usertype")).thenReturn("advanced");
        assertEquals(UserType.ADVANCED, user.getType());
    }

    @Test
    void getProperty()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.dab,
            this.currentReferenceResolver, this.entityReferenceProvider);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.dab.getProperty(reference, classReference, "property")).thenReturn("value");
        assertEquals("value", user.getProperty("property"));
    }

    @Test
    void isEmailChecked()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.dab,
            this.currentReferenceResolver, this.entityReferenceProvider);
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.dab.getProperty(reference, classReference, "email_checked")).thenReturn(new Integer(1));
        assertTrue(user.isEmailChecked());

        when(this.dab.getProperty(reference, classReference, "email_checked")).thenReturn(new Integer(0));
        assertFalse(user.isEmailChecked());

        when(this.dab.getProperty(reference, classReference, "email_checked")).thenReturn(null);
        assertFalse(user.isEmailChecked());
    }

    @Test
    void getReference()
    {
        DocumentReference reference = new DocumentReference("wiki", "space", "user");
        DocumentUser user = new DocumentUser(new DocumentUserReference(reference), this.dab,
            this.currentReferenceResolver, this.entityReferenceProvider);
        assertEquals(reference, user.getUserReference().getReference());
    }
}
