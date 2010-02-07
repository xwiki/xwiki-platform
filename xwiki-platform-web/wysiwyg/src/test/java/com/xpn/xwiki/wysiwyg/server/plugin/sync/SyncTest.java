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
package com.xpn.xwiki.wysiwyg.server.plugin.sync;

import org.jmock.cglib.MockObjectTestCase;
import org.xwiki.gwt.wysiwyg.client.diff.Diff;
import org.xwiki.gwt.wysiwyg.client.diff.Revision;
import org.xwiki.gwt.wysiwyg.client.diff.ToString;
import org.xwiki.gwt.wysiwyg.client.plugin.sync.SyncResult;
import org.xwiki.gwt.wysiwyg.client.plugin.sync.SyncStatus;

import com.xpn.xwiki.wysiwyg.server.plugin.sync.internal.DefaultSyncEngine;

/**
 * Unit tests for the synchronization code.
 * 
 * @version $Id$
 */
public class SyncTest extends MockObjectTestCase
{
    /**
     * The synchronization engine.
     */
    private SyncEngine syncEngine;

    /**
     * {@inheritDoc}
     * 
     * @see MockObjectTestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * {@inheritDoc}
     * 
     * @see MockObjectTestCase#setUp()
     */
    public void setUp() throws Exception
    {
        super.setUp();
        this.syncEngine = new DefaultSyncEngine();
    }

    public void testSimpleChanges() throws Exception
    {
        String currentContent = "";
        SyncStatus syncStatus = new SyncStatus("Test.Test", "1.1", "A\n");
        syncEngine.setSyncStatus("Test.Test", syncStatus);
        SyncResult sync = syncEngine.sync(syncStatus, null, 0);

        Revision rev = sync.getRevision();
        int currentVersion = sync.getVersion();
        assertNotNull("There should be a first revision", rev);

        currentContent = ToString.arrayToString(rev.patch(ToString.stringToArray(currentContent)));
        assertEquals("Content should be A", "A\n", currentContent);

        Revision syncRev = Diff.diff(ToString.stringToArray(currentContent), ToString.stringToArray("A B\n"));
        SyncResult sync2 = syncEngine.sync(syncStatus, syncRev, currentVersion);

        rev = sync2.getRevision();
        currentVersion = sync.getVersion();
        assertNull("There should be a no revision", rev);
        assertEquals("Current version should be 1", 1, sync.getVersion());
    }

    public void testSimpleChanges2() throws Exception
    {
        String currentContent = "";
        SyncStatus syncStatus = new SyncStatus("Test.Test", "1.1", "A\n");
        syncEngine.setSyncStatus("Test.Test", syncStatus);
        SyncResult sync = syncEngine.sync(syncStatus, null, 0);

        Revision rev = sync.getRevision();
        int currentVersion = sync.getVersion();
        assertNotNull("There should be a first revision", rev);

        currentContent = ToString.arrayToString(rev.patch(ToString.stringToArray(currentContent)));
        assertEquals("Content should be A", "A\n", currentContent);

        Revision syncRev = Diff.diff(ToString.stringToArray(currentContent), ToString.stringToArray("A B\n"));
        SyncResult sync2 = syncEngine.sync(syncStatus, syncRev, currentVersion);

        rev = sync2.getRevision();
        currentVersion = sync.getVersion();
        assertNull("There should be a no revision", rev);
        assertEquals("Current version should be 1", 1, sync.getVersion());
    }

    public void testMerge(String original, String change1, String change2, String expected) throws Exception
    {
        String currentContentUser1 = "";
        String currentContentUser2 = "";
        int currentVersionUser1 = 0;
        int currentVersionUser2 = 0;

        SyncStatus syncStatus = new SyncStatus("Test.Test", "1.1", original);
        syncEngine.setSyncStatus("Test.Test", syncStatus);
        assertEquals("Current version should be 1", 1, syncStatus.getCurrentVersionNumber());

        // Client 1 loads the data
        SyncResult syncResultUser1 = syncEngine.sync(syncStatus, null, currentVersionUser1);
        assertEquals("Current version should be 1", 1, syncStatus.getCurrentVersionNumber());
        assertEquals("Sync result version should be 1", 1, syncResultUser1.getVersion());

        Revision syncRevUser1 = syncResultUser1.getRevision();
        currentVersionUser1 = syncResultUser1.getVersion();
        assertNotNull("There should be a first revision", syncRevUser1);

        currentContentUser1 = ToString.arrayToString(syncRevUser1.patch(ToString.stringToArray(currentContentUser1)));
        assertEquals("Content should be A", original, currentContentUser1);

        // Client 2 loads the data
        SyncResult syncResultUser2 = syncEngine.sync(syncStatus, null, currentVersionUser2);
        assertEquals("Current version should be 1", 1, syncStatus.getCurrentVersionNumber());
        assertEquals("Sync result version should be 1", 1, syncResultUser2.getVersion());

        Revision syncRevUser2 = syncResultUser2.getRevision();
        currentVersionUser2 = syncResultUser2.getVersion();
        assertNotNull("There should be a first revision", syncRevUser2);

        currentContentUser2 = ToString.arrayToString(syncRevUser2.patch(ToString.stringToArray(currentContentUser2)));
        assertEquals("Content should be A", original, currentContentUser2);

        // Client 1 sends it's patch
        Revision revUser1 = Diff.diff(ToString.stringToArray(currentContentUser1), ToString.stringToArray(change1));
        syncResultUser1 = syncEngine.sync(syncStatus, revUser1, currentVersionUser1);
        assertEquals("Current version should be 2", 2, syncStatus.getCurrentVersionNumber());

        syncRevUser1 = syncResultUser1.getRevision();
        currentVersionUser1 = syncResultUser1.getVersion();
        assertNull("There should be a no revision", syncRevUser1);
        currentContentUser1 = change1;
        assertEquals("Sync result version should be 2", 2, syncResultUser1.getVersion());

        // Client 2 sends it's patch
        Revision revUser2 = Diff.diff(ToString.stringToArray(currentContentUser2), ToString.stringToArray(change2));
        syncResultUser2 = syncEngine.sync(syncStatus, revUser2, currentVersionUser2);
        assertEquals("Current version should be 3", 3, syncStatus.getCurrentVersionNumber());

        syncRevUser2 = syncResultUser2.getRevision();
        currentVersionUser2 = syncResultUser2.getVersion();
        assertNotNull("There should be a revision", syncRevUser2);
        assertEquals("Sync result version should be 3", 3, syncResultUser2.getVersion());

        currentContentUser2 = ToString.arrayToString(syncRevUser2.patch(ToString.stringToArray(currentContentUser2)));
        assertEquals("New client 2 content should be " + expected, currentContentUser2, expected);

        // Client 1 retrieves it's patch
        syncResultUser1 = syncEngine.sync(syncStatus, null, currentVersionUser1);
        assertEquals("Current version should be 3", 3, syncStatus.getCurrentVersionNumber());

        syncRevUser1 = syncResultUser1.getRevision();
        currentVersionUser1 = syncResultUser1.getVersion();
        assertNotNull("There should be a revision", syncRevUser1);
        assertEquals("Sync result version should be 3", 3, syncResultUser1.getVersion());

        currentContentUser1 = ToString.arrayToString(syncRevUser1.patch(ToString.stringToArray(currentContentUser1)));
        assertEquals("New client 1 content should be " + expected, currentContentUser1, expected);

    }

    public void testSpacedMerge1() throws Exception
    {
        testMerge("A\n", "A B\n", "A C\n", "A C B\n");
    }

    public void testSpacedMerge2() throws Exception
    {
        testMerge("A\n", "A\nB\n", "A\nC\n", "A\nC\nB\n");
    }

    public void testSpacedMerge3() throws Exception
    {
        testMerge("A B C D E\n", "A X B C D E\n", "A B C D Y E\n", "A X B C D Y E\n");
    }

    public void testCloseMerge() throws Exception
    {
        testMerge("A\n", "AB\n", "AC\n", "ACB\n");
    }

    public void testDoubleMerge1() throws Exception
    {
        testMerge("A\n", "XA B\n", "A C\n", "XA C B\n");
    }

    public void testDoubleMerge2() throws Exception
    {
        testMerge("A\n", "XYA B\n", "A C\n", "XYA C B\n");
    }

    public void testDoubleMerge3() throws Exception
    {
        testMerge("A\n", "XYZA B\n", "A C\n", "XYZA C B\n");
    }

    public void testDoubleMerge4() throws Exception
    {
        testMerge("A\n", "XYZTA B\n", "A C\n", "XYZTA C B\n");
    }

    public void testDoubleMergeWithSpace1() throws Exception
    {
        testMerge("A\n", "X A B\n", "A C\n", "X A C B\n");
    }

    public void testDoubleMergeWithSpace2() throws Exception
    {
        testMerge("A\n", "XY A B\n", "A C\n", "XY A C B\n");
    }

    public void testDoubleMergeWithSpace3() throws Exception
    {
        testMerge("A\n", "XYZ A B\n", "A C\n", "XYZ A C B\n");
    }

    public void testDeleteMergeOk1() throws Exception
    {
        testMerge("ABCDEF\n", "ABEF\n", "ABCDEFGH\n", "ABEFGH\n");
    }

    public void failingTestDeleteMergeOk2() throws Exception
    {
        testMerge("ABCD\n", "\n", "ABCDEF\n", "EF\n");
    }

    public void testSameEditMerge() throws Exception
    {
        testMerge("flower\n", "flowers\n", "flowers\n", "flowerss\n");
    }
}
