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

package org.xwiki.test.ui;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TestUtils}.
 *
 * @version $Id$
 * @since 16.10.0RC1
 * @since 16.4.6
 */
class TestUtilsTest
{
    @Test
    void getBaseBinURL()
    {
        TestUtils testUtils = new TestUtils();
        assertEquals("http://localhost:8080/xwiki/bin/", testUtils.getBaseBinURL(""));
        assertEquals("http://localhost:8080/xwiki/bin/", testUtils.getBaseBinURL(null));
        assertEquals("http://localhost:8080/xwiki/wiki/foo/", testUtils.getBaseBinURL("foo"));
        testUtils.setCurrentWiki("myWiki");
        assertEquals("http://localhost:8080/xwiki/wiki/myWiki/", testUtils.getBaseBinURL(""));
        assertEquals("http://localhost:8080/xwiki/wiki/myWiki/", testUtils.getBaseBinURL(null));
        assertEquals("http://localhost:8080/xwiki/wiki/foo/", testUtils.getBaseBinURL("foo"));
        testUtils.setCurrentWiki("");
        assertEquals("http://localhost:8080/xwiki/bin/", testUtils.getBaseBinURL(""));
        assertEquals("http://localhost:8080/xwiki/bin/", testUtils.getBaseBinURL(null));
        assertEquals("http://localhost:8080/xwiki/wiki/foo/", testUtils.getBaseBinURL("foo"));
    }

    @Test
    void getURL()
    {
        TestUtils testUtils = new TestUtils();
        EntityReference myReference = new DocumentReference("fooWiki", "MySpace", "MyPage");
        assertEquals("http://localhost:8080/xwiki/wiki/fooWiki/view/MySpace/MyPage", testUtils.getURL(myReference));

        myReference = new DocumentReference("xwiki", List.of("Space1", "Space2", "Foo"), "WebHome");
        assertEquals("http://localhost:8080/xwiki/bin/view/Space1/Space2/Foo/WebHome", testUtils.getURL(myReference));

        testUtils.setSecretToken("myToken");

        assertEquals("http://localhost:8080/xwiki/bin/edit/Space1/Space2/Foo/WebHome?form_token=myToken&editor=wiki",
            testUtils.getURL(myReference, "edit", "editor=wiki"));

        myReference = new LocalDocumentReference("MySpace", "MyPage");
        assertEquals("http://localhost:8080/xwiki/bin/view/MySpace/MyPage", testUtils.getURL(myReference));
    }
}