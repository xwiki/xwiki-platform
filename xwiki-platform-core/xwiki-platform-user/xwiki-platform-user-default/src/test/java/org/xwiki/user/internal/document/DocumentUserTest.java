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
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.Editor;
import org.xwiki.user.UserType;

import com.xpn.xwiki.XWikiContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    private DocumentReference reference;

    private DocumentUser user;

    private XWikiContext xcontext;

    @BeforeEach
    void setup()
    {
        this.reference = new DocumentReference("mainwiki", "space", "user");
        this.user = new DocumentUser(new DocumentUserReference(this.reference, this.entityReferenceProvider),
            this.contextProvider, this.userConfigurationSource);

        this.xcontext = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.xcontext);
    }

    @Test
    void isActive()
    {
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when(this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("active", Boolean.class, false)).thenReturn(true);
        assertTrue(this.user.isActive());

        when(this.userConfigurationSource.getProperty("active", Boolean.class, false)).thenReturn(false);
        assertFalse(this.user.isActive());
    }

    @Test
    void displayHiddenDocuments()
    {
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("displayHiddenDocuments", Boolean.class, false))
            .thenReturn(true);
        assertTrue(user.displayHiddenDocuments());

        when(this.userConfigurationSource.getProperty("displayHiddenDocuments", Boolean.class, false))
            .thenReturn(false);
        assertFalse(user.displayHiddenDocuments());
    }

    @Test
    void getFirstName()
    {
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("first_name")).thenReturn("John");
        assertEquals("John", this.user.getFirstName());
    }

    @Test
    void getLastName()
    {
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("last_name")).thenReturn("Doe");
        assertEquals("Doe", this.user.getLastName());
    }

    @Test
    void getEmail()
    {
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("email")).thenReturn("john@doe.com");
        assertEquals("john@doe.com", this.user.getEmail());
    }

    @Test
    void getUserType()
    {
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("usertype")).thenReturn("advanced");
        assertEquals(UserType.ADVANCED, this.user.getType());
    }

    @Test
    void getEditor()
    {
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("editor")).thenReturn("Wysiwyg");
        assertEquals(Editor.WYSIWYG, this.user.getEditor());
    }

    @Test
    void getProperty()
    {
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("property")).thenReturn("value");
        assertEquals("value", this.user.getProperty("property"));

        // Verify that the user has been put in the xwiki context so that we don't mess with the current user.
        verify(this.xcontext).setUserReference(this.reference);
    }

    @Test
    void isEmailChecked()
    {
        DocumentReference classReference = new DocumentReference("xwiki", "XWiki", "XWikiUsers");
        when (this.currentReferenceResolver.resolve(USERS_CLASS_REFERENCE)).thenReturn(classReference);

        when(this.userConfigurationSource.getProperty("email_checked", Boolean.class, false)).thenReturn(true);
        assertTrue(this.user.isEmailChecked());

        when(this.userConfigurationSource.getProperty("email_checked", Boolean.class, false)).thenReturn(false);
        assertFalse(this.user.isEmailChecked());
    }

    @Test
    void getReference()
    {
        assertEquals(reference, this.user.getUserReference().getReference());
    }
}
