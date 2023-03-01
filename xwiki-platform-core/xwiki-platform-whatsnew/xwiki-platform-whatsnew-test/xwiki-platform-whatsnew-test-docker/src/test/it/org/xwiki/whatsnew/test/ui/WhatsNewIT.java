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
package org.xwiki.whatsnew.test.ui;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.whatsnew.test.po.WhatsNewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verify the What's New template UI.
 *
 * @version $Id$
 */
@ExtendWith(DynamicTestConfigurationExtension.class)
@UITest
class WhatsNewIT
{
    @Test
    void verify(TestUtils setup) throws Exception
    {
        setup.loginAsSuperAdmin();

        // Since the rssreader library used to read RSS doesn't support a "file:" URI scheme for the RSS resource,
        // we create a page in the wiki, into which we copy the content of blogrss.xml, and then we configure the
        // whatsnew URL to be the URL to that wiki page.
        String rss = IOUtils.toString(WhatsNewIT.class.getClassLoader().getResourceAsStream("blogrss.xml"));
        EntityReference reference = new DocumentReference("xwiki", "Main", "WhatsNewRSS");
        setup.rest().savePage(reference, rss, "What's New RSS");

        WhatsNewPage page = WhatsNewPage.gotoPage();

        assertEquals(10, page.getNewsItemCount());
        // Assert the first news content
        assertEquals("XWiki 15.0 Release Candidate 1 Released", page.getNewsItemTitle(0));
        assertEquals("The XWiki development team is proud to announce the availability of the first release candidate "
            + "of XWiki 15.0. This release consists mostly of dependency upgrades and bug fixes including security "
            + "fixes with some small new features for admins and developers. ...", page.getNewsItemDescription(0));
        // TODO: Fix the displayed date to use the date format defined in XWiki
        assertEquals("2023-01-23T05:34:15+01:00", page.getNewsItemDate(0));
    }
}
