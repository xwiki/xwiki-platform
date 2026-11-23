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
package org.xwiki.livedata.internal.livetable;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.observation.internal.DefaultObservationManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.UserDeletingDocumentEvent;
import com.xpn.xwiki.internal.store.StoreConfiguration;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the document deletion of {@link ModelBridge} against the real {@link com.xpn.xwiki.XWiki} delete.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList(DefaultObservationManager.class)
class ModelBridgeDeleteTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "MyApp", "mydoc");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectMockComponents
    private ModelBridge modelBridge;

    @MockComponent
    private StoreConfiguration storeConfiguration;

    private XWikiContext xcontext;

    @BeforeEach
    void setUp() throws Exception
    {
        this.xcontext = this.oldcore.getXWikiContext();
        this.xcontext.setUserReference(new DocumentReference("xwiki", "XWiki", "Alice"));

        // MockitoOldcore stubs the delete, run the real one instead.
        doCallRealMethod().when(this.oldcore.getSpyXWiki()).deleteDocument(any(XWikiDocument.class), anyBoolean(),
            anyBoolean(), any(XWikiContext.class));
        doCallRealMethod().when(this.oldcore.getSpyXWiki()).checkDeletingDocument(any(DocumentReference.class),
            any(XWikiDocument.class), any(XWikiContext.class));

        this.oldcore.getSpyXWiki().saveDocument(new XWikiDocument(DOCUMENT_REFERENCE), this.xcontext);
    }

    @Test
    void delete() throws Exception
    {
        this.modelBridge.delete(DOCUMENT_REFERENCE);

        assertFalse(this.oldcore.getSpyXWiki().exists(DOCUMENT_REFERENCE, this.xcontext));
    }

    @Test
    void deleteWhenCanceled() throws Exception
    {
        EventListener listener = mock(EventListener.class);
        when(listener.getName()).thenReturn("cancelUserDelete");
        when(listener.getEvents()).thenReturn(List.of(new UserDeletingDocumentEvent()));
        doAnswer(invocation -> {
            invocation.<CancelableEvent>getArgument(0).cancel("denied");
            return null;
        }).when(listener).onEvent(any(), any(), any());
        this.oldcore.getObservationManager().addListener(listener);

        XWikiException exception =
            assertThrows(XWikiException.class, () -> this.modelBridge.delete(DOCUMENT_REFERENCE));

        assertEquals(XWikiException.ERROR_XWIKI_ACCESS_DENIED, exception.getCode());
        assertTrue(this.oldcore.getSpyXWiki().exists(DOCUMENT_REFERENCE, this.xcontext));
    }
}
