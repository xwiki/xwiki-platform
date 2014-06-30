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

import java.net.URL;

import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.web.XWikiServletRequestStub;

/**
 * Unit tests for legacy methods of {@link com.xpn.xwiki.XWiki}.
 *
 * @version $Id$
 * @since 5.1M1
 */
public class XWikiTest extends AbstractBridgedXWikiComponentTestCase
{
    private XWiki xwiki;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        getContext().setRequest(new XWikiServletRequestStub());
        getContext().setURL(new URL("http://localhost:8080/xwiki/bin/view/MilkyWay/Fidis"));

        this.xwiki = new XWiki(new XWikiConfig(), getContext())
        {
            // Avoid all the error at XWiki initialization
            @Override
            public String getXWikiPreference(String prefname, String defaultValue, XWikiContext context)
            {
                if (prefname.equals("plugins") || prefname.startsWith("macros_")) {
                    return defaultValue;
                } else {
                    return super.getXWikiPreference(prefname, defaultValue, context);
                }
            }
        };
    }

    public void testGetDocumentNameFromPath()
    {
        assertEquals("Main.WebHome", this.xwiki.getDocumentNameFromPath("", getContext()));
        assertEquals("Main.WebHome", this.xwiki.getDocumentNameFromPath("/", getContext()));
        assertEquals("Main.Document", this.xwiki.getDocumentNameFromPath("/Document", getContext()));
        assertEquals("Space.WebHome", this.xwiki.getDocumentNameFromPath("/Space/", getContext()));
        assertEquals("Space.Document", this.xwiki.getDocumentNameFromPath("/Space/Document", getContext()));
        assertEquals("Space.WebHome", this.xwiki.getDocumentNameFromPath("/view/Space/", getContext()));
        assertEquals("Space.Document", this.xwiki.getDocumentNameFromPath("/view/Space/Document", getContext()));
        assertEquals("Space.Document", this.xwiki.getDocumentNameFromPath("/view/Space/Document/", getContext()));
        assertEquals("Space.Document", this.xwiki.getDocumentNameFromPath("/view/Space/Document/some/ignored/paths",
           getContext()));

        // Test URL encoding and verify an encoded forward slash ("/" - encoded as %2F) works too.
        assertEquals("My Space.My/Document",
            this.xwiki.getDocumentNameFromPath("/My%20Space/My%2FDocument", getContext()));
    }
}
