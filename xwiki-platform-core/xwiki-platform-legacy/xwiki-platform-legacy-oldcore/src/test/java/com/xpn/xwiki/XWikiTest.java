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
package com.xpn.xwiki;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;

import org.xwiki.test.annotation.AllComponents;

import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.web.XWikiServletRequestStub;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for legacy methods of {@link com.xpn.xwiki.XWiki}.
 *
 * @version $Id$
 * @since 5.1M1
 */
@OldcoreTest
@AllComponents
class XWikiTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private XWiki xwiki;

    @BeforeEach
    void setUp() throws MalformedURLException
    {
        this.oldcore.getXWikiContext().setRequest(new XWikiServletRequestStub());
        this.oldcore.getXWikiContext().setURL(new URL("http://localhost:8080/xwiki/bin/view/MilkyWay/Fidis"));
        this.xwiki = this.oldcore.getSpyXWiki();
    }

    @Test
    void getDocumentNameFromPath()
    {
        assertEquals("Main.WebHome", this.xwiki.getDocumentNameFromPath("", this.oldcore.getXWikiContext()));
        assertEquals("Main.WebHome", this.xwiki.getDocumentNameFromPath("/", this.oldcore.getXWikiContext()));
        assertEquals("Main.Document", this.xwiki.getDocumentNameFromPath("/Document", this.oldcore.getXWikiContext()));
        assertEquals("Space.WebHome", this.xwiki.getDocumentNameFromPath("/Space/", this.oldcore.getXWikiContext()));
        assertEquals("Space.Document",
            this.xwiki.getDocumentNameFromPath("/Space/Document", this.oldcore.getXWikiContext()));
        assertEquals("Space.WebHome",
            this.xwiki.getDocumentNameFromPath("/view/Space/", this.oldcore.getXWikiContext()));
        assertEquals("Space.Document",
            this.xwiki.getDocumentNameFromPath("/view/Space/Document", this.oldcore.getXWikiContext()));
        assertEquals("Space.Document",
            this.xwiki.getDocumentNameFromPath("/view/Space/Document/", this.oldcore.getXWikiContext()));
        assertEquals("Space.Document", this.xwiki.getDocumentNameFromPath("/view/Space/Document/some/ignored/paths",
            this.oldcore.getXWikiContext()));

        // Test URL encoding and verify an encoded forward slash ("/" - encoded as %2F) works too.
        assertEquals("My Space.My/Document",
            this.xwiki.getDocumentNameFromPath("/My%20Space/My%2FDocument", this.oldcore.getXWikiContext()));
    }
}
