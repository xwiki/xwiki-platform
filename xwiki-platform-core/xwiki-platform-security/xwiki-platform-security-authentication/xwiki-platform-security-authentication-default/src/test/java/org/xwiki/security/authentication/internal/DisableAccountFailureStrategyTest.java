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
package org.xwiki.security.authentication.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authentication.AuthenticationFailureManager;
import org.xwiki.security.authentication.AuthenticationFailureStrategy;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Unit tests for {@link DisableAccountFailureStrategy}.
 * 
 * @version $Id$
 * @since 11.8RC1
 */
@ComponentTest
class DisableAccountFailureStrategyTest
{
    @InjectMockComponents(role = AuthenticationFailureStrategy.class)
    private DisableAccountFailureStrategy disableStrategy;

    @MockComponent
    private Provider<AuthenticationFailureManager> authenticationFailureManagerProvider;

    @Mock(name = "updated")
    private XWikiDocument updatedDocument;

    @BeforeEach
    public void configure()
    {
        DocumentReference documentReference = new DocumentReference("test", "Some", "Page");

        XWikiDocument originalDocument = mock(XWikiDocument.class, "original");
        BaseObject originalUserObject = mock(BaseObject.class, "original");
        when(originalDocument.getXObject(DisableAccountFailureStrategy.USER_CLASS_REFERENCE))
            .thenReturn(originalUserObject);

        BaseObject updatedUserObject = mock(BaseObject.class, "updated");
        when(this.updatedDocument.getXObject(DisableAccountFailureStrategy.USER_CLASS_REFERENCE))
            .thenReturn(updatedUserObject);
        when(this.updatedDocument.getOriginalDocument()).thenReturn(originalDocument);
        when(this.updatedDocument.getDocumentReference()).thenReturn(documentReference);

        AuthenticationFailureManager authenticationFailureManager = mock(AuthenticationFailureManager.class);
        when(this.authenticationFailureManagerProvider.get()).thenReturn(authenticationFailureManager);
    }

    @Test
    void resetAuthenticationFailureCounterWhenAccountIsActivated()
    {
        when(this.updatedDocument.getOriginalDocument().getXObject(DisableAccountFailureStrategy.USER_CLASS_REFERENCE)
            .getIntValue("active")).thenReturn(0);
        when(this.updatedDocument.getXObject(DisableAccountFailureStrategy.USER_CLASS_REFERENCE).getIntValue("active"))
            .thenReturn(1);

        disableStrategy.onEvent(new DocumentUpdatedEvent(), this.updatedDocument, null);

        verify(this.authenticationFailureManagerProvider.get())
            .resetAuthenticationFailureCounter(this.updatedDocument.getDocumentReference());
    }

    @Test
    void dontResetAuthenticationFailureCounterWhenAccountRemainsInactive()
    {
        when(this.updatedDocument.getOriginalDocument().getXObject(DisableAccountFailureStrategy.USER_CLASS_REFERENCE)
            .getIntValue("active")).thenReturn(0);
        when(this.updatedDocument.getXObject(DisableAccountFailureStrategy.USER_CLASS_REFERENCE).getIntValue("active"))
            .thenReturn(0);

        disableStrategy.onEvent(new DocumentUpdatedEvent(), this.updatedDocument, null);

        verify(this.authenticationFailureManagerProvider.get(), never())
            .resetAuthenticationFailureCounter(this.updatedDocument.getDocumentReference());
    }

    @Test
    void dontResetAuthenticationFailureCounterWhenAccountRemainsActive()
    {
        when(this.updatedDocument.getOriginalDocument().getXObject(DisableAccountFailureStrategy.USER_CLASS_REFERENCE)
            .getIntValue("active")).thenReturn(1);
        when(this.updatedDocument.getXObject(DisableAccountFailureStrategy.USER_CLASS_REFERENCE).getIntValue("active"))
            .thenReturn(1);

        disableStrategy.onEvent(new DocumentUpdatedEvent(), this.updatedDocument, null);

        verify(this.authenticationFailureManagerProvider.get(), never())
            .resetAuthenticationFailureCounter(this.updatedDocument.getDocumentReference());
    }

    @Test
    void dontResetAuthenticationFailureCounterWhenAccountIsDeactivated()
    {
        when(this.updatedDocument.getOriginalDocument().getXObject(DisableAccountFailureStrategy.USER_CLASS_REFERENCE)
            .getIntValue("active")).thenReturn(1);
        when(this.updatedDocument.getXObject(DisableAccountFailureStrategy.USER_CLASS_REFERENCE).getIntValue("active"))
            .thenReturn(0);

        disableStrategy.onEvent(new DocumentUpdatedEvent(), this.updatedDocument, null);

        verify(this.authenticationFailureManagerProvider.get(), never())
            .resetAuthenticationFailureCounter(this.updatedDocument.getDocumentReference());
    }

    @Test
    void onDocumentUpdatedNoUserAccount()
    {
        when(this.updatedDocument.getXObject(DisableAccountFailureStrategy.USER_CLASS_REFERENCE)).thenReturn(null);

        disableStrategy.onEvent(new DocumentUpdatedEvent(), updatedDocument, null);

        verify(this.authenticationFailureManagerProvider, never()).get();
    }

    @Test
    void onDocumentUpdatedNoUserAccountStateChange()
    {
        disableStrategy.onEvent(new DocumentUpdatedEvent(), updatedDocument, null);

        verify(this.authenticationFailureManagerProvider, never()).get();
    }

    @Test
    void validateFormReturnsFalseWhenUserNotFound()
    {
        assertFalse(this.disableStrategy.validateForm("Foo", (javax.servlet.http.HttpServletRequest) null));
    }
}
