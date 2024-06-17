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
package org.xwiki.netflux.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.netflux.internal.EntityChange.ScriptLevel;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;

/**
 * Unit tests for {@link EffectiveAuthorSetterListener}.
 *
 * @version $Id$
 */
@ComponentTest
class EffectiveAuthorSetterListenerTest
{
    @InjectMockComponents
    private EffectiveAuthorSetterListener listener;

    @MockComponent
    private EntityChannelScriptAuthorTracker scriptAuthorTracker;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private Container container;

    @Mock
    private Request request;

    @Mock
    private UserReference effectiveAuthor;

    @BeforeEach
    void beforeEach()
    {
        when(this.container.getRequest()).thenReturn(this.request);
    }

    @Test
    void onActionExecutingEvent() throws Exception
    {
        DocumentReference currentDocumentReference = new DocumentReference("test", "Some", "Page");
        when(this.documentAccessBridge.getCurrentDocumentReference()).thenReturn(currentDocumentReference);
        DocumentModelBridge tdoc = mock(DocumentModelBridge.class);
        when(this.documentAccessBridge.getTranslatedDocumentInstance(currentDocumentReference)).thenReturn(tdoc);
        when(tdoc.getRealLanguage()).thenReturn("fr");

        UserReference otherUserReference = mock(UserReference.class);
        // An entity change that targets a different document.
        EntityChange entityChangeTwo =
            new EntityChange(new DocumentReference("test", "Other", "Page"), otherUserReference, ScriptLevel.SCRIPT);
        // An entity change that targets the current document translation, but with a higher script level.
        EntityChange entityChangeThree =
            new EntityChange(new DocumentReference(currentDocumentReference, Locale.FRENCH), otherUserReference,
                ScriptLevel.PROGRAMMING);
        // An entity change that targets the current document (access rights are checked at document level) with a lower
        // script level. The lower script level should win.
        EntityChange entityChangeFour = new EntityChange(new AttachmentReference("file.txt", currentDocumentReference),
            this.effectiveAuthor, ScriptLevel.SCRIPT);

        when(this.request.getProperties("netfluxChannel"))
            .thenReturn(Arrays.asList("", "one", null, "two", "three", "four"));
        when(this.scriptAuthorTracker.getScriptAuthor("two")).thenReturn(Optional.of(entityChangeTwo));
        when(this.scriptAuthorTracker.getScriptAuthor("three")).thenReturn(Optional.of(entityChangeThree));
        when(this.scriptAuthorTracker.getScriptAuthor("four")).thenReturn(Optional.of(entityChangeFour));

        this.listener.onEvent(new ActionExecutingEvent(), null, null);

        verify(this.request).setProperty("com.xpn.xwiki.web.XWikiRequest#effectiveAuthor", this.effectiveAuthor);
    }
}
