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
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.GuestUserReference;
import org.xwiki.user.SuperAdminUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link UserPreferencesConfigurationSource}.
 *
 * @version $Id$
 */
@ComponentTest
public class UserPreferencesConfigurationSourceTest
{
    @InjectMockComponents
    private UserPreferencesConfigurationSource source;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    @Named("normaluser")
    private ConfigurationSource normalUserConfigurationSource;

    @MockComponent
    @Named("superadminuser")
    private ConfigurationSource superAdminConfigurationSource;

    @MockComponent
    @Named("guestuser")
    private ConfigurationSource guestConfigurationSource;

    @Test
    void getPropertyWhenGuest()
    {
        DocumentReference userDocumentReference = new DocumentReference("wiki", "space", "XWikiGuest");
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(userDocumentReference);
        when(this.userReferenceResolver.resolve(userDocumentReference)).thenReturn(GuestUserReference.INSTANCE);

        this.source.getProperty("key");

        // Verify the right CS is called.
        verify(this.guestConfigurationSource).getProperty("key");
    }

    @Test
    void getPropertyWhenSuperAdmin()
    {
        DocumentReference userDocumentReference = new DocumentReference("wiki", "space", "superadmin");
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(userDocumentReference);
        when(this.userReferenceResolver.resolve(userDocumentReference)).thenReturn(SuperAdminUserReference.INSTANCE);

        this.source.getProperty("key");

        // Verify the right CS is called.
        verify(this.superAdminConfigurationSource).getProperty("key");
    }

    @Test
    void getPropertyWhenNormalUser()
    {
        DocumentReference userDocumentReference = new DocumentReference("wiki", "space", "user");
        when(this.documentAccessBridge.getCurrentUserReference()).thenReturn(userDocumentReference);
        when(this.userReferenceResolver.resolve(userDocumentReference)).thenReturn(mock(UserReference.class));

        this.source.getProperty("key");

        // Verify the right CS is called.
        verify(this.normalUserConfigurationSource).getProperty("key");
    }
}
