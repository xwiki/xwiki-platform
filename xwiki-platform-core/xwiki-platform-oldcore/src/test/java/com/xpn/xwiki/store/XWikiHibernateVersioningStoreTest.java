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
package com.xpn.xwiki.store;

import java.util.List;

import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Spy;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.criteria.impl.RevisionCriteria;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xpn.xwiki.store.hibernate.query.VersioningStoreQueryFactory;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link XWikiHibernateVersioningStore}.
 *
 * @version $Id$
 */
@ReferenceComponentList
@OldcoreTest
public class XWikiHibernateVersioningStoreTest
{
    @Spy
    @InjectMockComponents
    private XWikiHibernateVersioningStore versioningStore;

    @MockComponent
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Mock
    private Query<XWikiRCSNodeInfo> nodeInfoQuery;

    @Mock
    private XWikiDocument document;

    @Mock
    private DocumentAuthors authors;

    @Captor
    private ArgumentCaptor<RevisionCriteria> revisionCriteria;

    @BeforeEach
    void setUp() throws XWikiException
    {
        when(this.nodeInfoQuery.getResultList()).thenReturn(List.of());

        DocumentReference documentReference = new DocumentReference("xwiki", "test", "TestPage");
        when(this.document.getDocumentReference()).thenReturn(documentReference);
        when(this.document.getAuthors()).thenReturn(this.authors);
        when(this.document.toXML(null)).thenReturn("<");
    }

    /**
     * This test checks that whenever the archive gets updated, only a single revision is ever fetched from the
     * versioning store.
     */
    @Test
    void checkLoadedRevisionsDuringArchiveUpdate() throws XWikiException
    {
        doNothing().when(this.versioningStore).saveXWikiDocArchive(any(), anyBoolean(), any());

        try (MockedStatic<?> mockedQueryFactory = mockStatic(VersioningStoreQueryFactory.class)) {
            mockedQueryFactory.when(
                () -> VersioningStoreQueryFactory.getRCSNodeInfoQuery(any(), anyLong(), this.revisionCriteria.capture())
            ).thenReturn(this.nodeInfoQuery);

            this.versioningStore.updateXWikiDocArchive(this.document, false, null);
            assertEquals(1, this.revisionCriteria.getValue().getRange().getAbsoluteSize());
        }
    }
}
