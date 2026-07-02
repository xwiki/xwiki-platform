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
package org.xwiki.job.store.internal;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.job.JobManagerConfiguration;
import org.xwiki.job.internal.Version3JobStatusFolderResolver;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Component tests for {@link JobStatusIdentifierSerializer}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList(Version3JobStatusFolderResolver.class)
class JobStatusIdentifierSerializerTest
{
    private static final String A = "a";

    private static final String B = "b";

    private static final String DELIMITER = "/";

    private static final String HASH_SEPARATOR = "__";

    private static final String NODE_ID = "node/id";

    private static final String BLOB_KEY_HASH =
        "3d9608c7bcba5e728f982fcc106a3053221d695716d04fbfbdeb78bbc30485c0"
            + "4748443df32c3c4ca3cc84f3ae77af443ef27be13e284e4605d6f1f24f98a34c";

    private static final String DATABASE_KEY_HASH =
        "e4e58e1d0088f1515d869002859fbb5179b21f451c4c59ca1e08317f3869b188"
            + "813568e4ca8a9e6b5860948a4d3e2f9c200f776601a86dbbebc87b80ba72c5fa";

    @InjectMockComponents
    private JobStatusIdentifierSerializer serializer;

    @MockComponent
    private JobManagerConfiguration jobManagerConfiguration;

    @MockComponent
    private RemoteObservationManagerConfiguration remoteObservationManagerConfiguration;

    @BeforeEach
    void setUp()
    {
        when(this.jobManagerConfiguration.getStorage()).thenReturn(new File("target/test/storage"));
        when(this.remoteObservationManagerConfiguration.getId()).thenReturn(NODE_ID);
    }

    @Test
    void getBlobKeyEscapesIdentifierSegments()
    {
        List<String> jobId = Arrays.asList(".hidden", "space inside", "CamelCase", "a/b", null, "file*.txt");

        assertEquals("3/node%2Fid/%2Ehidden/space%20inside/%43amel%43ase/a%2Fb/&null/file%2A.txt",
            this.serializer.getBlobKey(jobId));
    }

    @Test
    void getBlobKeyTruncatesLongEscapedIdentifier()
    {
        List<String> jobId = List.of(A.repeat(600));

        String actual = this.serializer.getBlobKey(jobId);

        assertEquals("3/node%2Fid/" + A.repeat(250) + DELIMITER + A.repeat(119) + HASH_SEPARATOR + BLOB_KEY_HASH,
            actual);
        assertEquals(512, actual.length());
    }

    @Test
    void getDatabaseKeyTruncatesLongJobId()
    {
        List<String> jobId = List.of(A.repeat(300), B.repeat(300));

        String actual = this.serializer.getDatabaseKey(jobId);

        assertEquals(A.repeat(300) + DELIMITER + B.repeat(81) + HASH_SEPARATOR + DATABASE_KEY_HASH,
            actual);
        assertEquals(512, actual.length());
    }
}
