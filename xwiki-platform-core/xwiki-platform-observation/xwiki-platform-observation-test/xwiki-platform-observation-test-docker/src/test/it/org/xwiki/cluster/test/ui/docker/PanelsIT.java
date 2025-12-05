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
package org.xwiki.cluster.test.ui.docker;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.panels.internal.PanelClassDocumentInitializer;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.Instances;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.xwiki.test.ui.TestUtils.RestTestUtils.object;
import static org.xwiki.test.ui.TestUtils.RestTestUtils.property;

/**
 * Verify modifying a panel affect the other members of the cluster.
 * 
 * @version $Id$
 */
@UITest(instances = @Instances(2))
class PanelsIT
{
    private static final LocalDocumentReference PANEL_REFERENCE = new LocalDocumentReference("Test", "SharedPanel");

    private static final LocalDocumentReference HOME_REFERENCE = new LocalDocumentReference("Main", "WebHome");

    @Test
    void testModifyPanel(TestUtils setup) throws Exception
    {
        String panelContent = "Hey I'm here !";
        String modifiedPanelContent = "Hey I'm still here !";
        String id = "testpanel";

        // Create panel on node 0
        setup.switchExecutor(0);

        Page panelPage = setup.rest().page(PANEL_REFERENCE);
        panelPage.setObjects(new Objects());
        org.xwiki.rest.model.jaxb.Object panelObject = object(PanelClassDocumentInitializer.CLASS_REFERENCE_STRING);
        panelObject.getProperties().add(property("content", "(%id='" + id + "'%)" + panelContent));
        panelPage.getObjects().getObjectSummaries().add(panelObject);
        setup.rest().save(panelPage);

        // Add the panel to wiki right panels
        setup.setWikiPreference("rightPanels", "Test.SharedPanel");

        setup.gotoPage(HOME_REFERENCE);

        assertEquals(panelContent, setup.getDriver().findElement(By.id(id)).getText());

        // Display panel on node 1
        setup.switchExecutor(1);

        setup.gotoPage(HOME_REFERENCE);
        assertEquals(panelContent, setup.getDriver().findElement(By.id(id)).getText());

        // Modify panel on node 1
        panelObject = setup.rest().object(PANEL_REFERENCE, PanelClassDocumentInitializer.CLASS_REFERENCE_STRING);
        panelObject.getProperties().add(property("content", "(%id='" + id + "'%)" + modifiedPanelContent));
        setup.rest().update(panelObject);

        // Reload the page after modifying the panel
        setup.getDriver().navigate().refresh();
        assertEquals(modifiedPanelContent, setup.getDriver().findElement(By.id(id)).getText());

        // Verify panel rendering on node 0
        // Since it can take time for the Cluster to propagate the change, we need to wait and set up a timeout.
        setup.switchExecutor(0);
        setup.gotoPage(HOME_REFERENCE);
        long t1 = System.currentTimeMillis();
        long t2;
        String result;
        while (!(result = setup.getDriver().findElement(By.id(id)).getText()).equalsIgnoreCase(modifiedPanelContent)) {
            t2 = System.currentTimeMillis();
            if (t2 - t1 > 10000L) {
                fail("Content should have been [" + modifiedPanelContent + "] but was [" + result + "]");
            }
            Thread.sleep(100);
            setup.getDriver().navigate().refresh();
        }
    }
}
