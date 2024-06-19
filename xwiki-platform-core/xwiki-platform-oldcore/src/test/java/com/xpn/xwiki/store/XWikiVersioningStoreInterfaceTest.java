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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mock;
import org.mockito.Spy;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.test.junit5.mockito.ComponentTest;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.criteria.impl.Period;
import com.xpn.xwiki.criteria.impl.RangeFactory;
import com.xpn.xwiki.criteria.impl.RevisionCriteria;
import com.xpn.xwiki.criteria.impl.RevisionCriteriaFactory;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeId;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the default methods of {@link XWikiVersioningStoreInterface}.
 *
 * @version $Id$
 */
@ComponentTest
class XWikiVersioningStoreInterfaceTest
{
    @Spy
    private XWikiVersioningStoreInterface versioningStore;

    @Mock
    private XWikiDocumentArchive archive;

    @BeforeEach
    void setUp() throws XWikiException
    {
        SortedMap<Version, XWikiRCSNodeInfo> archiveNodes = new TreeMap<>();
        Stream.of(
            Arguments.of(new Version(1, 1), "Author1", 1000L),
            Arguments.of(new Version(1, 2), "Author2", 2000L),
            Arguments.of(new Version(1, 3), "Author1", 3000L),
            Arguments.of(new Version(2, 1), "Author2", 4000L),
            Arguments.of(new Version(3, 1), "Author1", 5000L),
            Arguments.of(new Version(3, 2), "Author1", 6000L),
            Arguments.of(new Version(4, 1), "Author2", 7000L),
            Arguments.of(new Version(5, 1), "Author1", 8000L),
            Arguments.of(new Version(5, 2), "Author1", 9000L)
        ).forEach(arguments -> {
            XWikiRCSNodeInfo node = new XWikiRCSNodeInfo();
            XWikiRCSNodeId nodeId = new XWikiRCSNodeId(null, 42, (Version) arguments.get()[0]);
            node.setId(nodeId);
            node.setAuthor((String) arguments.get()[1]);
            node.setDate(new Date((Long) arguments.get()[2]));
            archiveNodes.put((Version) arguments.get()[0], node);
        });

        List<XWikiRCSNodeInfo> nodes = new ArrayList<>(archiveNodes.values());
        Collections.sort(nodes);
        when(this.archive.getNodes()).thenReturn(nodes);
        when(this.versioningStore.getXWikiDocumentArchive(any(), any())).thenReturn(this.archive);
    }

    @Test
    void getVersionsDefaultCriteria() throws XWikiException
    {
        RevisionCriteria criteria = new RevisionCriteria();
        Collection<Version> versions = this.versioningStore.getXWikiDocVersions(null, criteria, null);
        long versionsCount = this.versioningStore.getXWikiDocVersionsCount(null, criteria, null);
        assertEquals(5, versions.size());
        assertEquals(5, versionsCount);
        assertIterableEquals(List.of("1.3", "2.1", "3.2", "4.1", "5.2"),
            versions.stream().map(Version::toString).collect(Collectors.toList()));
    }

    @Test
    void getVersionsFilterAuthor() throws XWikiException
    {
        RevisionCriteria criteria = new RevisionCriteriaFactory().createRevisionCriteria("Author1", true);
        Collection<Version> versions = this.versioningStore.getXWikiDocVersions(null, criteria, null);
        long versionsCount = this.versioningStore.getXWikiDocVersionsCount(null, criteria, null);
        assertEquals(6, versions.size());
        assertEquals(6, versionsCount);
        assertIterableEquals(List.of("1.1", "1.3", "3.1", "3.2", "5.1", "5.2"),
            versions.stream().map(Version::toString).collect(Collectors.toList()));
    }

    @Test
    void getVersionsFilterDate() throws XWikiException
    {
        RevisionCriteria criteria = new RevisionCriteriaFactory().createRevisionCriteria(new Period(1999L, 6001L),
            true);
        Collection<Version> versions = this.versioningStore.getXWikiDocVersions(null, criteria, null);
        long versionsCount = this.versioningStore.getXWikiDocVersionsCount(null, criteria, null);
        assertEquals(5, versions.size());
        assertEquals(5, versionsCount);
        assertIterableEquals(List.of("1.2", "1.3", "2.1", "3.1", "3.2"),
            versions.stream().map(Version::toString).collect(Collectors.toList()));
    }

    @Test
    void getLastVersion() throws XWikiException
    {
        RevisionCriteria criteria = new RevisionCriteriaFactory().createRevisionCriteria();
        criteria.setRange(RangeFactory.getLAST());
        Collection<Version> versions = this.versioningStore.getXWikiDocVersions(null, criteria, null);
        assertEquals(1, versions.size());
        assertIterableEquals(List.of("5.2"),
            versions.stream().map(Version::toString).collect(Collectors.toList()));
    }
}
