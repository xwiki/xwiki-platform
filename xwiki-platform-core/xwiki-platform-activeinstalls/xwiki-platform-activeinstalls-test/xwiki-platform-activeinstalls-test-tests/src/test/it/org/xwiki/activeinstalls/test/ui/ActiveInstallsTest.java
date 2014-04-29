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
import org.xwiki.test.ui.AbstractGuestTest;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.*;
import static com.github.tlrx.elasticsearch.test.EsSetup.*;

/**
 * Verify the overall Active Installs feature.
 *
 * @version $Id$
 * @since 5.2M2
 */
public class ActiveInstallsTest extends AbstractGuestTest
{
    @Test
    public void verifyPingIsSent() throws Exception
    {
        // When XWiki was started by ElasticSearchRunner from AllTests, a page was checked to verify that the XWiki
        // instance was up. This, in turn, triggered the send of an asynchronous ping to the ES instance (started prior
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

        // In order to verify backward compatibility with the previous Active Install format, we also add an index in
        // the older format.
/*
        ElasticSearchRunner.esSetup.execute(
            createIndex("installs")
                .withMapping("install", fromClassPath("mapping.json"))
                .withData(fromClassPath("data.json")));
*/
        // Navigate to the Active Installs Counter Value page to verify that the ping has been received
        getUtil().gotoPage("ActiveInstalls", "ActiveCounterValue", "view", "query=*");
        ViewPage vp  = new ViewPage();
        assertEquals("1", vp.getContent());

        // The default query doesn't show SNAPSHOT versions and thus we expect 0
        vp = getUtil().gotoPage("ActiveInstalls", "ActiveCounterValue");
        assertEquals("0", vp.getContent());

        // Navigate to the Total Installs Counter Value page to verify that the ping has been received
        getUtil().gotoPage("ActiveInstalls", "TotalCounterValue", "view", "query=*");
        vp = new ViewPage();
        assertEquals("1", vp.getContent());

        // The default query doesn't show SNAPSHOT versions and thus we expect 0
        vp = getUtil().gotoPage("ActiveInstalls", "TotalCounterValue");
        assertEquals("0", vp.getContent());
    }
}
