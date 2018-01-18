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
import org.xwiki.activeinstalls.test.po.ActiveInstallsHomePage;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.*;
import static com.github.tlrx.elasticsearch.test.EsSetup.*;

/**
 * Verify the overall Active Installs feature.
 *
 * @version $Id$
 * @since 5.2M2
 */
public class ActiveInstallsIT extends AbstractTest
{
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void verifyActiveInstalls() throws Exception
    {
        // Note that we verify that the ES Runner has been initialized as this allows us to more easily debug the test
        // by manually starting an XWiki instance and an ES instance prior to running this test (in this case we don't
        // provision ES).
        if (ElasticSearchRunner.esSetup != null) {
            // When XWiki was started by ElasticSearchRunner from AllITs, a page was checked to verify that the XWiki
            // instance was up. This, in turn, triggered the send of an asynchronous ping to the ES instance
            // (started prior to the XWiki start in ElasticSearchRunner).
            //
            // Since the ping may take some time to be sent to our ES instance, we wait till we have 1 index in ES or
            // till the timeout expires.
            long count = 0;
            long time = System.currentTimeMillis();
            while (count != 1 && (System.currentTimeMillis() - time) < 5000L) {
                count = ElasticSearchRunner.esSetup.countAll();
                Thread.sleep(100L);
            }
            assertEquals("AS ping wasn't sent by the XWiki instance", 1, count);

            // In order to verify backward compatibility with the previous Active Install format, we also add an index
            // in the older format.
            ElasticSearchRunner.esSetup.execute(index("installs", "install", "156231f3-705b-44c6-afe3-e191bcc4b746")
                .withSource("{ \"formatVersion\": \"1.0\", \"distributionVersion\": \"5.2\", "
                    + "\"distributionId\": \"org.xwiki.platform:xwiki-platform-web\", "
                    + "\"date\": \"2013-09-16T20:00:34.277Z\", \"extensions\": [ ] }"));
        }

        // Navigate to the ActiveInstalls app by clicking in the Application Panel.
        // This verifies that the ActiveInstalls application is registered in the Applications Panel.
        // It also verifies that the Translation is registered properly.
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage vp = applicationPanel.clickApplication("Active Installs");

        // Verify we're on the right page!
        Assert.assertEquals(ActiveInstallsHomePage.getSpace(), vp.getMetaDataValue("space"));
        Assert.assertEquals(ActiveInstallsHomePage.getPage(), vp.getMetaDataValue("page"));

        // Configure the Active Installs feature to count "org.xwiki.*" distribution ids.
        getUtil().updateObject("ActiveInstalls", "ActiveInstallsConfig", "ActiveInstalls.ActiveInstallsConfig", 0,
            "distributionId", "org.xwiki.*");

        // By default we don't show SNAPSHOTs, verify that!

        // The default query doesn't show SNAPSHOT versions and thus we expect 0
        getUtil().gotoPage("ActiveInstalls", "ActiveCounterValue2");
        vp  = new ViewPage();
        assertEquals("0", vp.getContent());

        // The default query doesn't show SNAPSHOT versions and thus we expect 0
        getUtil().gotoPage("ActiveInstalls", "TotalCounterValue2");
        vp = new ViewPage();
        assertEquals("0", vp.getContent());

        // Configure the Active Installs feature to count SNAPSHOTs and to count
        // "org.xwiki.platform:xwiki-platform-web" distribution ids.
        getUtil().updateObject("ActiveInstalls", "ActiveInstallsConfig", "ActiveInstalls.ActiveInstallsConfig", 0,
            "snapshots", true);

        // Navigate to the Active Installs Counter Value page to verify that the ping has been received
        getUtil().gotoPage("ActiveInstalls", "ActiveCounterValue2");
        vp  = new ViewPage();
        assertEquals("1", vp.getContent());

        // Also verify the Active Installs Counter for the old format
        getUtil().gotoPage("ActiveInstalls", "ActiveCounterValue1");
        vp  = new ViewPage();
        assertEquals("0", vp.getContent());

        // Navigate to the Total Installs Counter Value page to verify that the ping has been received
        getUtil().gotoPage("ActiveInstalls", "TotalCounterValue2");
        vp = new ViewPage();
        assertEquals("1", vp.getContent());

        // Also verify the Total Installs Counter for the old format
        getUtil().gotoPage("ActiveInstalls", "TotalCounterValue1");
        vp  = new ViewPage();
        assertEquals("1", vp.getContent());

        // Verify JavaVersion data
        getUtil().gotoPage("ActiveInstalls", "JavaVersionsData");
        vp  = new ViewPage();
        assertTrue("Got [" + vp.getContent() + "]",
            vp.getContent().matches("Java Version Active Installs Count\\r?\\n1\\.[0-9_\\.]* 1"));

        // Verify Databases data
        getUtil().gotoPage("ActiveInstalls", "DatabasesData");
        vp  = new ViewPage();
        assertTrue("Got [" + vp.getContent() + "]",
            vp.getContent().matches("Database Active Installs Count\\r?\\nHSQL Database Engine 1"));

        // Verify Distribution data
        getUtil().gotoPage("ActiveInstalls", "DistributionData");
        vp  = new ViewPage();
        assertTrue("Got [" + vp.getContent() + "]",
            vp.getContent().matches("Distributions Active Installs Count\\r?\\n"
                + "org.xwiki.platform:xwiki-platform-web 1"));

        // Verify top 10 XWiki versions data
        getUtil().gotoPage("ActiveInstalls", "XWikiVersionsData");
        vp  = new ViewPage();
        assertTrue("Got [" + vp.getContent() + "]",
            vp.getContent().matches("XWiki Version Active Installs Count\\r?\\n[0-9]+\\.[0-9]+.* 1"));

        // Verify XWiki Cycle versions data
        getUtil().gotoPage("ActiveInstalls", "XWikiVersionsCycleData");
        vp  = new ViewPage();
        assertTrue("Got [" + vp.getContent() + "]",
            vp.getContent().matches("XWiki Version Active Installs Count\\r?\\n[0-9]+\\.x 1"));

        // Verify ServletContainers data
        getUtil().gotoPage("ActiveInstalls", "ServletContainersData");
        vp  = new ViewPage();
        assertTrue("Got [" + vp.getContent() + "]",
            vp.getContent().matches("Servlet Container Active Installs Count\\r?\\njetty 1"));

        // Verify Extension Count
        // Create a page calling the Extension Count macro
        String content  = "{{include reference=\"ActiveInstalls.ExtensionCount\"/}}\n"
            + "\n"
            + "{{velocity}}\n"
            + "#set ($extensionIds = [\n"
            + "  'org.xwiki.contrib:xwiki-totem-application',\n"
            + "  'jsimard:event-reporter-application',\n"
            + "  'mouhb:likeapplication'\n"
            + "])\n"
            + "|=Extension Id|=Count\n"
            + "#foreach($extensionId in $extensionIds)\n"
            + "  #countActiveInstallsUsingExtension($extensionId $count)\n"
            + "  |$extensionId|$count\n"
            + "#end\n"
            + "{{/velocity}}";
        vp = getUtil().createPage(getTestClassName(), "ExtensionCountExample", content, "Example");
        assertTrue("Got [" + vp.getContent() + "]", vp.getContent().matches("Extension Id Count\n"
            + "org.xwiki.contrib:xwiki-totem-application 0\n"
            + "jsimard:event-reporter-application 0\n"
            + "mouhb:likeapplication 0"));
    }
}
