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
package org.xwiki.repository.internal;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProjectUpdaterListener}.
 *
 * @version $Id$
 */
@ComponentTest
class ProjectUpdaterListenerTest
{
    private static final DocumentReference PROJECT_REFERENCE =
        new DocumentReference("wiki", List.of("Extension", "MyProject"), "WebHome");

    private static final DocumentReference VERSION_REFERENCE =
        new DocumentReference("wiki", List.of("Extension", "MyProject", "Versions", "1.0"), "WebHome");

    @InjectMockComponents
    private ProjectUpdaterListener listener;

    @MockComponent
    private RepositoryManager repositoryManager;

    @MockComponent
    private ObservationContext observationContext;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    private XWikiDocument versionDocument;

    @BeforeEach
    void beforeEach()
    {
        this.versionDocument = mock();
        when(this.versionDocument.getDocumentReference()).thenReturn(VERSION_REFERENCE);
        when(this.versionDocument.getXObject(XWikiRepositoryModel.PROJECTVERSION_CLASSREFERENCE)).thenReturn(mock(BaseObject.class));
    }

    @Test
    void updateLastVersionOnLocalVersionPageEvent() throws Exception
    {
        this.listener.onEvent(new DocumentCreatedEvent(VERSION_REFERENCE), this.versionDocument, null);

        verify(this.repositoryManager).updateLastProjectVersion(PROJECT_REFERENCE);
    }

    @Test
    void ignoreVersionPageEventDuringImport() throws Exception
    {
        when(this.observationContext.isIn(any(ProjectImportStartingEvent.class))).thenReturn(true);

        this.listener.onEvent(new DocumentCreatedEvent(VERSION_REFERENCE), this.versionDocument, null);

        verify(this.repositoryManager, never()).updateLastProjectVersion(any(DocumentReference.class));
    }

    @Test
    void ignoreRemoteVersionPageEvent() throws Exception
    {
        when(this.remoteObservationManagerContext.isRemoteState()).thenReturn(true);

        this.listener.onEvent(new DocumentCreatedEvent(VERSION_REFERENCE), this.versionDocument, null);

        verify(this.repositoryManager, never()).updateLastProjectVersion(any(DocumentReference.class));
    }
}
