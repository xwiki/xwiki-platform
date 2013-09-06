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
package org.xwiki.activeinstalls.test.ui;

import org.junit.*;
import org.xwiki.test.ui.AbstractAdminAuthenticatedTest;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.*;

/**
 * Verify the overall Active Installs feature.
 *
 * @version $Id$
 * @since 5.2M2
 */
public class ActiveInstallsTest extends AbstractAdminAuthenticatedTest
{
    @Test
    public void verifyPingIsSent() throws Exception
    {
        // When XWiki was started by ElasticSearchRunner from AllTests, a page was checked to verify that the XWiki
        // instance was up. This, in turn, triggered the send of an asynchrnous ping to the ES instance (started prior
        // to the XWiki start in ElasticSearchRunner).
        //
        // Since the ping may take some time to be sent to our ES instance, we wait till we have 1 index in ES or
        // till the timeout expires.
        long count = 0;
        long time = System.currentTimeMillis();
        while (count != 1 && (System.currentTimeMillis() - time) < 5000L) {
            count = ElasticSearchRunner.esSetup.countAll();
            Thread.sleep(100L);
        }

        // Navigate to the Active Installs home page to verify that we can see how ping
        ViewPage vp  = getUtil().gotoPage("ActiveInstalls", "WebHome");
        String expected = "Total number of XWiki installations\n1\nActive XWiki installations (i.e. we've received a ping "
            + "in the past month)\n1";
        assertEquals(expected, vp.getContent());
    }
}
