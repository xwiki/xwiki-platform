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
package org.xwiki.livedata.test.ui;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.editor.ClassEditPage;
import org.xwiki.test.ui.po.editor.StaticListClassEditElement;
import org.xwiki.text.StringUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of the Live Data macro, in view and edit modes.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@UITest
class LiveDataIT
{
    private static final String NAME_COLUMN = "name";

    private static final String CHOICE_COLUMN = "choice";

    private static final String NAME_LYNDA = "Lynda";

    private static final String NAME_ESTHER = "Esther";

    private static final String NAME_CHARLY = "Charly";

    private static final String CHOICE_A = "value1";

    private static final String CHOICE_B = "value2";

    private static final String CHOICE_C = "value3";

    private static final String BIRTHDAY_COLUMN = "birthday";

    private static final String BIRTHDAY_DATETIME = "11/05/2021 16:00:00";

    /**
     * Test the view and edition of the cells of a live data in table layout with a liveTable source. Creates an XClass
     * and two XObjects, then edit the XObjects properties from the live data.
     */
    @Test
    @Order(1)
    void livedataLivetableTableLayout(TestUtils testUtils, TestReference testReference) throws Exception
    {
        // Login as super admin because guest user cannot remove pages.
        testUtils.loginAsSuperAdmin();
        // Wipes the test space.
        testUtils.deletePage(testReference, true);
        // Become guest because the tests does not need specific rights. 
        testUtils.forceGuestUser();

        DocumentReference o1 = new DocumentReference("O1", (SpaceReference) testReference.getParent());
        DocumentReference o2 = new DocumentReference("O2", (SpaceReference) testReference.getParent());
        String className = testUtils.serializeReference(testReference);

        // Initializes the page content.
        createsLiveDataPage(testUtils, testReference, className);

        // Creates the XClass.
        createXClass(testUtils, testReference);

        // Creates corresponding XObjects.
        addXObject(testUtils, o1, className, NAME_LYNDA, CHOICE_A);
        addXObject(testUtils, o2, className, NAME_ESTHER, CHOICE_B);

        testUtils.gotoPage(testReference);

        TableLayoutElement tableLayout = new LiveDataElement("test").getTableLayout();
        assertEquals(2, tableLayout.countRows());
        tableLayout.assertRow(NAME_COLUMN, NAME_LYNDA);
        tableLayout.assertRow(NAME_COLUMN, NAME_ESTHER);
        tableLayout.assertRow(CHOICE_COLUMN, CHOICE_A);
        tableLayout.assertRow(CHOICE_COLUMN, CHOICE_B);
        tableLayout.editCell(NAME_COLUMN, 1, NAME_COLUMN, NAME_CHARLY);
        tableLayout.editCell(CHOICE_COLUMN, 2, CHOICE_COLUMN, CHOICE_C);
        tableLayout.editCell(BIRTHDAY_COLUMN, 1, BIRTHDAY_COLUMN, BIRTHDAY_DATETIME);
        assertEquals(2, tableLayout.countRows());
        tableLayout.assertRow(NAME_COLUMN, NAME_CHARLY);
        tableLayout.assertRow(NAME_COLUMN, NAME_LYNDA);
        tableLayout.assertRow(CHOICE_COLUMN, CHOICE_B);
        tableLayout.assertRow(CHOICE_COLUMN, CHOICE_C);
        tableLayout.assertRow(BIRTHDAY_COLUMN, BIRTHDAY_DATETIME);
    }

    private void addXObject(TestUtils testUtils, DocumentReference documentReference,
        String className, String name, String choice)
    {
        Map<String, Object> properties = new HashMap<>();
        properties.put(NAME_COLUMN, name);
        properties.put(CHOICE_COLUMN, choice);
        testUtils.addObject(documentReference, className, properties);
    }

    private void createXClass(TestUtils testUtils, TestReference testReference)
    {
        testUtils.addClassProperty(testReference, NAME_COLUMN, "String");
        testUtils.addClassProperty(testReference, CHOICE_COLUMN, "StaticList");
        ClassEditPage classEditPage = new ClassEditPage();
        StaticListClassEditElement propertyList = classEditPage.getStaticListClassEditElement(CHOICE_COLUMN);
        propertyList.setValues(StringUtils.joinWith("|", CHOICE_A, CHOICE_B, CHOICE_C));
        classEditPage.clickSaveAndView();
        testUtils.addClassProperty(testReference, BIRTHDAY_COLUMN, "Date");
    }

    private void createsLiveDataPage(TestUtils testUtils, TestReference testReference, String className)
        throws Exception
    {
        TestUtils.RestTestUtils rest = testUtils.rest();
        Page page = rest.page(testReference);
        page.setContent("{{velocity}}\n"
            + "{{liveData\n"
            + "  id=\"test\"\n"
            + "  properties=\"" + NAME_COLUMN + "," + CHOICE_COLUMN + "," + BIRTHDAY_COLUMN + "\"\n"
            + "  source=\"liveTable\"\n"
            + "  sourceParameters=\"className=" + className.replace("xwiki:", "") + "\"\n"
            + "}}{{/liveData}}\n"
            + "{{/velocity}}");
        rest.save(page);
    }
}
