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
package org.xwiki.security.authservice.internal;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Validate {@link AuthServiceConfiguration} and {@link AuthServiceConfigurationInvalidator}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
public class AuthServiceConfigurationTest
{
    @InjectMockComponents
    private AuthServiceConfiguration configuration;

    @InjectMockComponents
    private AuthServiceConfigurationInvalidator invalidator;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.ERROR);

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private void setAuthServiceId(String authService, String wiki) throws XWikiException
    {
        WikiReference currentWiki = this.oldcore.getXWikiContext().getWikiReference();

        try {
            this.oldcore.getXWikiContext().setWikiId(wiki);

            this.configuration.setAuthServiceId(authService);
        } finally {
            this.oldcore.getXWikiContext().setWikiReference(currentWiki);
        }

        this.invalidator.onEvent(
            new XObjectPropertyUpdatedEvent(
                new DocumentReference(wiki, List.of("XWiki", "AuthService"), "XWikiAuthServiceConfiguration")),
            null, null);
    }

    @Test
    void getsetAuthService() throws XWikiException
    {
        assertNull(this.configuration.getAuthService());

        setAuthServiceId("main", "xwiki");

        this.oldcore.getXWikiContext().setWikiId("xwiki");

        assertEquals("main", this.configuration.getAuthService());

        this.oldcore.getXWikiContext().setWikiId("wiki1");

        assertEquals("main", this.configuration.getAuthService());

        setAuthServiceId("wiki1", "wiki1");

        assertEquals("wiki1", this.configuration.getAuthService());
    }

    @Test
    void getAuthServiceWhenSubwikifail() throws XWikiException
    {
        setAuthServiceId("main", "xwiki");
        setAuthServiceId("wiki1", "wiki1");

        this.oldcore.getXWikiContext().setWikiId("wiki1");

        assertEquals("wiki1", this.configuration.getAuthService());

        when(this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference(AuthServiceConfiguration.DOC_REFERENCE,
                this.oldcore.getXWikiContext().getWikiReference()), this.oldcore.getXWikiContext()))
                    .thenThrow(new XWikiException()).thenThrow(new XWikiException());

        this.configuration.invalidate("wiki1");

        assertEquals("main", this.configuration.getAuthService());

        assertEquals(
            "Failed to load the authentication service configured for wiki [wiki1]. Falling back on main wiki.",
            this.logCapture.getMessage(0));
    }
}
