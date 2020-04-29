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
import org.mockito.Mock;
import org.xwiki.configuration.ConfigurationRight;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.configuration.ConfigurationSourceAuthorization;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link SecureUserDocumentUserPropertiesResolver}.
 *
 * @version $Id$
 * @since 12.4RC1
 */
@ComponentTest
public class SecureUserDocumentUserPropertiesResolverTest
{
    @InjectMockComponents
    private SecureUserDocumentUserPropertiesResolver resolver;

    @MockComponent
    private EntityReferenceProvider entityReferenceProvider;

    @MockComponent
    @Named("normaluser")
    private ConfigurationSourceAuthorization authorization;

    @MockComponent
    @Named("user")
    private ConfigurationSource configurationSource;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private AuthorizationManager authorizationManager;

    @Mock
    private XWikiContext xcontext;

    @BeforeEach
    void setup()
    {
        when(this.contextProvider.get()).thenReturn(this.xcontext);
    }

    @Test
    void resolveWhenNotAuthorized()
    {
        DocumentReference userDocumentReference = new DocumentReference("wiki", "space", "user");
        UserReference userReference = new DocumentUserReference(userDocumentReference, this.entityReferenceProvider);
        when(this.authorization.hasAccess("first_name", userReference, ConfigurationRight.READ)).thenReturn(false);
        when(this.authorization.hasAccess("active", userReference, ConfigurationRight.READ)).thenReturn(false);

        UserProperties properties = this.resolver.resolve(userReference);
        // Verify default value when a string property is asked
        assertNull(properties.getFirstName());// Verify default value when a boolean property is asked
        assertFalse(properties.isActive());
    }

    @Test
    void resolveWhenAuthorized()
    {
        DocumentReference userDocumentReference = new DocumentReference("wiki", "space", "user");
        UserReference userReference = new DocumentUserReference(userDocumentReference, this.entityReferenceProvider);
        when(this.authorization.hasAccess("first_name", userReference, ConfigurationRight.READ)).thenReturn(true);
        when(this.configurationSource.getProperty("first_name")).thenReturn("John");

        UserProperties properties = this.resolver.resolve(userReference);
        assertEquals("John", properties.getFirstName());
    }

    @Test
    void resolveWhenEmailPropertyAndLastAuhorHasntPR()
    {
        DocumentReference userDocumentReference = new DocumentReference("wiki", "space", "user");
        UserReference userReference = new DocumentUserReference(userDocumentReference, this.entityReferenceProvider);
        when(this.authorization.hasAccess("email", userReference, ConfigurationRight.READ)).thenReturn(true);
        when(this.configurationSource.getProperty("email")).thenReturn("john@doe.com");

        UserProperties properties = this.resolver.resolve(userReference);
        assertEquals("j...@doe.com", properties.getEmail());
    }

    @Test
    void resolveWhenEmailPropertyAndLastAuhorHasPR()
    {
        DocumentReference userDocumentReference = new DocumentReference("wiki", "space", "user");
        UserReference userReference = new DocumentUserReference(userDocumentReference, this.entityReferenceProvider);
        when(this.authorization.hasAccess("email", userReference, ConfigurationRight.READ)).thenReturn(true);
        when(this.configurationSource.getProperty("email")).thenReturn("john@doe.com");

        // Set up mocks for the last current doc author has PR
        XWikiDocument currentDocument = mock(XWikiDocument.class);
        DocumentReference lastAuthorReference = new DocumentReference("wiki", "space", "lastauthor");
        when(currentDocument.getAuthorReference()).thenReturn(lastAuthorReference);
        when(this.xcontext.getDoc()).thenReturn(currentDocument);
        DocumentReference currentDocumentReference = new DocumentReference("wiki", "space", "currentdoc");
        when(currentDocument.getDocumentReference()).thenReturn(currentDocumentReference);
        when(this.authorizationManager.hasAccess(Right.PROGRAM, lastAuthorReference, currentDocumentReference))
            .thenReturn(true);

        UserProperties properties = this.resolver.resolve(userReference);
        assertEquals("john@doe.com", properties.getEmail());
    }
}
