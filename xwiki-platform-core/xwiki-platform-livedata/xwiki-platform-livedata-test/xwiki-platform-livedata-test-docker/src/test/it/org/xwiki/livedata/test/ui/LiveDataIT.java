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

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.node.ArrayNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.node.ObjectNode;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests of the Live Data macro.
 *
 * @version $Id$
 * @since 13.4RC1
 * @since 12.10.9
 */
@UITest
class LiveDataIT
{
    @Test
    @Order(1)
    void livedataLivetableTableLayoutResultPage(TestUtils testUtils, TestReference testReference) throws Exception
    {
        // Login as super admin because guest user cannot remove pages.
        testUtils.loginAsSuperAdmin();
        // Wipes the test space.
        testUtils.deletePage(testReference, true);

        DocumentReference resultPageDocumentReference =
            new DocumentReference("resultPage", testReference.getLastSpaceReference());

        initResultPage(testUtils, resultPageDocumentReference);

        String resultPage = testUtils.serializeReference(resultPageDocumentReference).replaceFirst("xwiki:", "");

        createResultPageLiveDataPage(testUtils, testReference, resultPage);

        testUtils.gotoPage(testReference);
        TableLayoutElement tableLayout = new LiveDataElement("test").getTableLayout();
        assertEquals(1, tableLayout.countRows());
        tableLayout.assertRow("count", "1");
        tableLayout.assertRow("label", "first result");
    }

    private void createResultPageLiveDataPage(TestUtils testUtils, TestReference testReference, String resultPage)
        throws Exception
    {
        TestUtils.RestTestUtils rest = testUtils.rest();
        Page page = rest.page(testReference);
        page.setContent("{{velocity}}\n"
            + "#set ($liveDataConfig={'meta': {'propertyDescriptors':[{'id': 'count', 'type': 'Number'}]}})\n"
            + "\n"
            + "{{liveData\n"
            + "  id=\"test\"\n"
            + "  properties=\"count,label\"\n"
            + "  source=\"liveTable\"\n"
            + "  sourceParameters=\"translationPrefix=&resultPage=" + resultPage + "\"\n"
            + "}}$jsontool.serialize($liveDataConfig){{/liveData}}\n"
            + "{{/velocity}}");
        rest.save(page);
    }

    private void initResultPage(TestUtils testUtils, DocumentReference resultPageDocumentReference)
        throws JsonProcessingException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode objectNode = objectMapper.createObjectNode();
        objectNode.put("totalrows", 1);
        objectNode.put("returnedrows", 1);
        objectNode.put("offset", 1);
        objectNode.put("reqNo", 1);
        ArrayNode rows = objectNode.putArray("rows");
        ObjectNode jsonNodes = rows.addObject();
        jsonNodes.put("count", 1);
        jsonNodes.put("label", "first result");
        testUtils.createPage(resultPageDocumentReference, objectMapper.writeValueAsString(objectNode));
    }
}
