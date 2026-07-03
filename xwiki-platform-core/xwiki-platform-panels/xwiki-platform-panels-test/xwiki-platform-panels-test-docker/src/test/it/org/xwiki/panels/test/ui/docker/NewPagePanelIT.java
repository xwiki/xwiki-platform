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
package org.xwiki.panels.test.ui.docker;

import org.junit.jupiter.api.Test;
import org.xwiki.panels.test.po.NewPagePanel;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.CreatePagePage;
import org.xwiki.test.ui.po.editor.WikiEditPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test page creation using the NewPage Panel.
 * 
 * @version $Id$
 * @since 2.5RC1
 */
@UITest
public class NewPagePanelIT
{
    /**
     * Tests if a new page can be created using the create page panel.
     */
    @Test
    public void createPageFromPanel(TestUtils testUtils, TestReference testReference)
    {
        NewPagePanel newPagePanel = NewPagePanel.gotoPage();

        String pageName = testReference.getLastSpaceReference().getName();
        String spaceName = testReference.getLastSpaceReference().getParent().getName();
        CreatePagePage createPagePage = newPagePanel.createPage(spaceName, pageName);
        createPagePage.clickCreate();
        WikiEditPage editPage = new WikiEditPage();

        assertEquals(pageName, editPage.getDocumentTitle());
        assertEquals("WebHome", editPage.getMetaDataValue("page"));
        assertEquals(spaceName + "." + pageName, editPage.getMetaDataValue("space"));
    }
}
