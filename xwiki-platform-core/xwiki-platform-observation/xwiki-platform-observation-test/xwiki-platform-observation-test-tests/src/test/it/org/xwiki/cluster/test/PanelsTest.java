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
package org.xwiki.cluster.test;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.panels.internal.PanelClassDocumentInitializer;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.cluster.test.framework.AbstractClusterHttpTest;
import org.xwiki.test.ui.AbstractTest;

import static org.junit.Assert.assertEquals;
import static org.xwiki.test.ui.TestUtils.RestTestUtils.object;
import static org.xwiki.test.ui.TestUtils.RestTestUtils.property;

/**
 * Verify modifying a panel affect the other members of the cluster.
 * 
 * @version $Id$
 */
public class PanelsTest extends AbstractClusterHttpTest
{
    private static LocalDocumentReference PANEL_REFERENCE = new LocalDocumentReference("Test", "SharedPanel");

    private static LocalDocumentReference HOME_REFERENCE = new LocalDocumentReference("Main", "WebHome");

    @Test
    public void testModifyPanel() throws Exception
    {
        String panelContent = "Hey I'm here !";
        String modifiedPanelContent = "Hey I'm still here !";
        String id = "testpanel";

        // Create panel on node 0
        AbstractTest.getUtil().switchExecutor(0);

        Page panelPage = AbstractTest.getUtil().rest().page(PANEL_REFERENCE);
        panelPage.setObjects(new Objects());
        org.xwiki.rest.model.jaxb.Object panelObject = object(PanelClassDocumentInitializer.CLASS_REFERENCE_STRING);
        panelObject.getProperties().add(property("content", "(%id='" + id + "'%)" + panelContent));
        panelPage.getObjects().getObjectSummaries().add(panelObject);
        AbstractTest.getUtil().rest().save(panelPage);

        // Add the panel to wiki right panels
        AbstractTest.getUtil().setWikiPreference("rightPanels", "Test.SharedPanel");

        AbstractTest.getUtil().gotoPage(HOME_REFERENCE);

        Assert.assertEquals(panelContent, AbstractTest.getUtil().getDriver().findElement(By.id(id)).getText());

        // Display panel on node 1
        AbstractTest.getUtil().switchExecutor(1);

        AbstractTest.getUtil().gotoPage(HOME_REFERENCE);
        Assert.assertEquals(panelContent, AbstractTest.getUtil().getDriver().findElement(By.id(id)).getText());

        // Modify panel on node 1
        panelObject = AbstractTest.getUtil().rest().object(PANEL_REFERENCE, PanelClassDocumentInitializer.CLASS_REFERENCE_STRING);
        panelObject.getProperties().add(property("content", "(%id='" + id + "'%)" + modifiedPanelContent));
        AbstractTest.getUtil().rest().update(panelObject);

        // Reload the page after modifying the panel
        AbstractTest.getUtil().getDriver().navigate().refresh();
        Assert.assertEquals(modifiedPanelContent, AbstractTest.getUtil().getDriver().findElement(By.id(id)).getText());

        // Verify panel rendering on node 0
        // Since it can take time for the Cluster to propagate the change, we need to wait and set up a timeout.
        AbstractTest.getUtil().switchExecutor(0);
        AbstractTest.getUtil().gotoPage(HOME_REFERENCE);
        long t1 = System.currentTimeMillis();
        long t2;
        String result;
        while (!(result = AbstractTest.getUtil().getDriver().findElement(By.id(id)).getText())
            .equalsIgnoreCase(modifiedPanelContent)) {
            t2 = System.currentTimeMillis();
            if (t2 - t1 > 10000L) {
                Assert.fail("Content should have been [" + modifiedPanelContent + "] but was [" + result + "]");
            }
            Thread.sleep(100);
            AbstractTest.getUtil().getDriver().navigate().refresh();
        }
    }
}
