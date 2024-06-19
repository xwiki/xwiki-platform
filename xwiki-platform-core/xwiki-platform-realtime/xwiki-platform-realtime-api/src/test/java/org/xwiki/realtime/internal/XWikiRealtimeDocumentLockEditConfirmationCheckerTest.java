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

package org.xwiki.realtime.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;
import java.util.Optional;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.validation.edit.EditConfirmationCheckerResult;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

@ComponentTest
class XWikiRealtimeDocumentLockEditConfirmationCheckerTest
{

    private static final String WYSIWYG = "wysiwyg";

    // We use the Spy decorator on the class to test because we need to mock the
    // super call.
    @Spy
    @InjectMockComponents
    private XWikiRealtimeDocumentLockEditConfirmationChecker xwikiRealtimeDocumentLockEditConfirmationChecker;

    @MockComponent
    private RealtimeEditorManager realtimeEditorManager;

    @MockComponent
    private Provider<XWikiContext> xwikiContextProvider;

    @Mock
    private XWikiContext xwikiContext;

    @Mock
    private XWikiDocument document;

    @Mock
    private DocumentReference reference;

    @Mock
    private EditConfirmationCheckerResult editConfirmationCheckerResult;

    @BeforeEach
    void setup()
    {
        when(document.getRealLocale()).thenReturn(Locale.ENGLISH);
        when(document.getDocumentReference()).thenReturn(reference);
        when(xwikiContext.get("tdoc")).thenReturn(document);
        when(xwikiContextProvider.get()).thenReturn(xwikiContext);
 
        // We consider the active session case the default, but we test both.
        when(realtimeEditorManager.getSelectedEditor()).thenReturn(WYSIWYG);
        when(realtimeEditorManager.sessionIsActive(reference, Locale.ENGLISH, WYSIWYG)).thenReturn(true);
        
        // Provide a dummy result for the super call.
        doReturn(Optional.of(mock(EditConfirmationCheckerResult.class))).when(xwikiRealtimeDocumentLockEditConfirmationChecker).parentCheck();
    }

    @Test
    void checkWhenActive()
    {
        assertTrue(xwikiRealtimeDocumentLockEditConfirmationChecker.check().isEmpty());
        // When the session is active, we should override the behavior of the parent class.
        // Thus the parent check should *not* be called.
        verify(xwikiRealtimeDocumentLockEditConfirmationChecker, times(0)).parentCheck();
    }

    @Test
    void checkWhenInactive()
    {
        when(realtimeEditorManager.sessionIsActive(reference, Locale.ENGLISH, WYSIWYG)).thenReturn(false);
        assertTrue(xwikiRealtimeDocumentLockEditConfirmationChecker.check().isPresent());
        // When the session is not active, we should use the behavior of the parent class.
        // The parent check should be called.
        verify(xwikiRealtimeDocumentLockEditConfirmationChecker, times(1)).parentCheck();
    }
}
