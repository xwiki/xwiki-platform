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
package org.xwiki.wysiwyg.server.internal.plugin.sync;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xwiki.gwt.wysiwyg.client.diff.Diff;
import org.xwiki.gwt.wysiwyg.client.diff.Revision;
import org.xwiki.gwt.wysiwyg.client.diff.ToString;
import org.xwiki.gwt.wysiwyg.client.plugin.sync.SyncResult;
import org.xwiki.gwt.wysiwyg.client.plugin.sync.SyncStatus;
import org.xwiki.test.AbstractComponentTestCase;
import org.xwiki.wysiwyg.server.plugin.sync.SyncEngine;

/**
 * Unit tests for {@link DefaultSyncEngine}.
 * 
 * @version $Id$
 */
public class DefaultSyncEngineTest extends AbstractComponentTestCase
{
    /**
     * The synchronization engine.
     */
    private SyncEngine syncEngine;

    @Before
    @Override
    public void setUp() throws Exception
    {
        this.syncEngine = getComponentManager().lookup(SyncEngine.class);
    }

    @Test
    public void testSimpleChanges() throws Exception
    {
        String currentContent = "";
        SyncStatus syncStatus = new SyncStatus("Test.Test", "1.1", "A\n");
        syncEngine.setSyncStatus("Test.Test", syncStatus);
        SyncResult sync = syncEngine.sync(syncStatus, null, 0);

        Revision rev = sync.getRevision();
        int currentVersion = sync.getVersion();
        Assert.assertNotNull("There should be a first revision", rev);

        currentContent = ToString.arrayToString(rev.patch(ToString.stringToArray(currentContent)));
        Assert.assertEquals("Content should be A", "A\n", currentContent);

        Revision syncRev = Diff.diff(ToString.stringToArray(currentContent), ToString.stringToArray("A B\n"));
        SyncResult sync2 = syncEngine.sync(syncStatus, syncRev, currentVersion);

        rev = sync2.getRevision();
        currentVersion = sync.getVersion();
        Assert.assertNull("There should be a no revision", rev);
        Assert.assertEquals("Current version should be 1", 1, sync.getVersion());
    }

    @Test
    public void testSimpleChanges2() throws Exception
    {
        String currentContent = "";
        SyncStatus syncStatus = new SyncStatus("Test.Test", "1.1", "A\n");
        syncEngine.setSyncStatus("Test.Test", syncStatus);
        SyncResult sync = syncEngine.sync(syncStatus, null, 0);

        Revision rev = sync.getRevision();
        int currentVersion = sync.getVersion();
        Assert.assertNotNull("There should be a first revision", rev);

        currentContent = ToString.arrayToString(rev.patch(ToString.stringToArray(currentContent)));
        Assert.assertEquals("Content should be A", "A\n", currentContent);

        Revision syncRev = Diff.diff(ToString.stringToArray(currentContent), ToString.stringToArray("A B\n"));
        SyncResult sync2 = syncEngine.sync(syncStatus, syncRev, currentVersion);

        rev = sync2.getRevision();
        currentVersion = sync.getVersion();
        Assert.assertNull("There should be a no revision", rev);
        Assert.assertEquals("Current version should be 1", 1, sync.getVersion());
    }

    @Test
    public void testSpacedMerge1() throws Exception
    {
        testMerge("A\n", "A B\n", "A C\n", "A C B\n");
    }

    @Test
    public void testSpacedMerge2() throws Exception
    {
        testMerge("A\n", "A\nB\n", "A\nC\n", "A\nC\nB\n");
    }

    @Test
    public void testSpacedMerge3() throws Exception
    {
        testMerge("A B C D E\n", "A X B C D E\n", "A B C D Y E\n", "A X B C D Y E\n");
    }

    @Test
    public void testCloseMerge() throws Exception
    {
        testMerge("A\n", "AB\n", "AC\n", "ACB\n");
    }

    @Test
    public void testDoubleMerge1() throws Exception
    {
        testMerge("A\n", "XA B\n", "A C\n", "XA C B\n");
    }

    @Test
    public void testDoubleMerge2() throws Exception
    {
        testMerge("A\n", "XYA B\n", "A C\n", "XYA C B\n");
    }

    @Test
    public void testDoubleMerge3() throws Exception
    {
        testMerge("A\n", "XYZA B\n", "A C\n", "XYZA C B\n");
    }

    @Test
    public void testDoubleMerge4() throws Exception
    {
        testMerge("A\n", "XYZTA B\n", "A C\n", "XYZTA C B\n");
    }

    @Test
    public void testDoubleMergeWithSpace1() throws Exception
    {
        testMerge("A\n", "X A B\n", "A C\n", "X A C B\n");
    }

    @Test
    public void testDoubleMergeWithSpace2() throws Exception
    {
        testMerge("A\n", "XY A B\n", "A C\n", "XY A C B\n");
    }

    @Test
    public void testDoubleMergeWithSpace3() throws Exception
    {
        testMerge("A\n", "XYZ A B\n", "A C\n", "XYZ A C B\n");
    }

    @Test
    public void testDeleteMergeOk1() throws Exception
    {
        testMerge("ABCDEF\n", "ABEF\n", "ABCDEFGH\n", "ABEFGH\n");
    }

    @Ignore
    public void failingTestDeleteMergeOk2() throws Exception
    {
        testMerge("ABCD\n", "\n", "ABCDEF\n", "EF\n");
    }

    @Test
    public void testSameEditMerge() throws Exception
    {
        testMerge("flower\n", "flowers\n", "flowers\n", "flowerss\n");
    }

    private void testMerge(String original, String change1, String change2, String expected) throws Exception
    {
        String currentContentUser1 = "";
        String currentContentUser2 = "";
        int currentVersionUser1 = 0;
        int currentVersionUser2 = 0;

        SyncStatus syncStatus = new SyncStatus("Test.Test", "1.1", original);
        syncEngine.setSyncStatus("Test.Test", syncStatus);
        Assert.assertEquals("Current version should be 1", 1, syncStatus.getCurrentVersionNumber());

        // Client 1 loads the data
        SyncResult syncResultUser1 = syncEngine.sync(syncStatus, null, currentVersionUser1);
        Assert.assertEquals("Current version should be 1", 1, syncStatus.getCurrentVersionNumber());
        Assert.assertEquals("Sync result version should be 1", 1, syncResultUser1.getVersion());

        Revision syncRevUser1 = syncResultUser1.getRevision();
        currentVersionUser1 = syncResultUser1.getVersion();
        Assert.assertNotNull("There should be a first revision", syncRevUser1);

        currentContentUser1 = ToString.arrayToString(syncRevUser1.patch(ToString.stringToArray(currentContentUser1)));
        Assert.assertEquals("Content should be A", original, currentContentUser1);

        // Client 2 loads the data
        SyncResult syncResultUser2 = syncEngine.sync(syncStatus, null, currentVersionUser2);
        Assert.assertEquals("Current version should be 1", 1, syncStatus.getCurrentVersionNumber());
        Assert.assertEquals("Sync result version should be 1", 1, syncResultUser2.getVersion());

        Revision syncRevUser2 = syncResultUser2.getRevision();
        currentVersionUser2 = syncResultUser2.getVersion();
        Assert.assertNotNull("There should be a first revision", syncRevUser2);

        currentContentUser2 = ToString.arrayToString(syncRevUser2.patch(ToString.stringToArray(currentContentUser2)));
        Assert.assertEquals("Content should be A", original, currentContentUser2);

        // Client 1 sends it's patch
        Revision revUser1 = Diff.diff(ToString.stringToArray(currentContentUser1), ToString.stringToArray(change1));
        syncResultUser1 = syncEngine.sync(syncStatus, revUser1, currentVersionUser1);
        Assert.assertEquals("Current version should be 2", 2, syncStatus.getCurrentVersionNumber());

        syncRevUser1 = syncResultUser1.getRevision();
        currentVersionUser1 = syncResultUser1.getVersion();
        Assert.assertNull("There should be a no revision", syncRevUser1);
        currentContentUser1 = change1;
        Assert.assertEquals("Sync result version should be 2", 2, syncResultUser1.getVersion());

        // Client 2 sends it's patch
        Revision revUser2 = Diff.diff(ToString.stringToArray(currentContentUser2), ToString.stringToArray(change2));
        syncResultUser2 = syncEngine.sync(syncStatus, revUser2, currentVersionUser2);
        Assert.assertEquals("Current version should be 3", 3, syncStatus.getCurrentVersionNumber());

        syncRevUser2 = syncResultUser2.getRevision();
        currentVersionUser2 = syncResultUser2.getVersion();
        Assert.assertNotNull("There should be a revision", syncRevUser2);
        Assert.assertEquals("Sync result version should be 3", 3, syncResultUser2.getVersion());

        currentContentUser2 = ToString.arrayToString(syncRevUser2.patch(ToString.stringToArray(currentContentUser2)));
        Assert.assertEquals("New client 2 content should be " + expected, currentContentUser2, expected);

        // Client 1 retrieves it's patch
        syncResultUser1 = syncEngine.sync(syncStatus, null, currentVersionUser1);
        Assert.assertEquals("Current version should be 3", 3, syncStatus.getCurrentVersionNumber());

        syncRevUser1 = syncResultUser1.getRevision();
        currentVersionUser1 = syncResultUser1.getVersion();
        Assert.assertNotNull("There should be a revision", syncRevUser1);
        Assert.assertEquals("Sync result version should be 3", 3, syncResultUser1.getVersion());

        currentContentUser1 = ToString.arrayToString(syncRevUser1.patch(ToString.stringToArray(currentContentUser1)));
        Assert.assertEquals("New client 1 content should be " + expected, currentContentUser1, expected);
    }
}
