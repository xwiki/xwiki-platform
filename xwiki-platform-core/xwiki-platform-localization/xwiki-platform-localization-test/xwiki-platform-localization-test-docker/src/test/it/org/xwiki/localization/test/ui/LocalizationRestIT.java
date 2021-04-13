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
package org.xwiki.localization.test.ui;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.localization.rest.LocalizationSource;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Functional tests of the localization REST endpoint.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@UITest
class LocalizationRestIT
{
    @BeforeEach
    void setUp(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
    }

    @Test
    @Order(1)
    void translationKeyMissing(TestUtils testUtils) throws Exception
    {
        Map<String, Object[]> queryParams = new HashMap<>();
        queryParams.put("key", new Object[] { "key1" });
        GetMethod getMethod = testUtils.rest().executeGet(LocalizationSource.class, queryParams, "xwiki");
        String body = getMethod.getResponseBodyAsString();
        assertEquals("<ObjectNode><key1/></ObjectNode>", body);
    }

    @Test
    @Order(2)
    void translationOnXWiki(TestUtils testUtils, TestReference testReference) throws Exception
    {
        testUtils.deletePage(testReference);
        Map<String, Object> properties = new HashMap<>();
        properties.put("scope", "WIKI");
        testUtils.addObject(testReference, "XWiki.TranslationDocumentClass", properties);
        testUtils.rest().savePage(testReference, "key1=value1 {0}", "translation");

        Map<String, Object[]> queryParams = new HashMap<>();
        queryParams.put("key", new Object[] { "key1" });
        GetMethod getMethod = testUtils.rest().executeGet(LocalizationSource.class, queryParams, "xwiki");
        String body = getMethod.getResponseBodyAsString();
        assertEquals("<ObjectNode><key1>value1 {0}</key1></ObjectNode>", body);
    }
}
