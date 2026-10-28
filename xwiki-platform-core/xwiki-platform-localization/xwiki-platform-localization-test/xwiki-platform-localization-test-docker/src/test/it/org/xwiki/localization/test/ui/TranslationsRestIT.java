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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.localization.rest.TranslationsResource;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Functional tests of the translations REST endpoint.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@UITest
class TranslationsRestIT
{
    @BeforeEach
    void setUp(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();
    }

    @Test
    @Order(1)
    void translationsNoKeyParam(TestUtils testUtils) throws Exception
    {
        Map<String, Object[]> queryParams = new HashMap<>();
        String body = testUtils.rest().getString(TranslationsResource.class, queryParams, "xwiki");
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<translations xmlns=\"http://www.xwiki.org/localization\"/>"
            , body);
    }

    @Test
    @Order(2)
    void translationsKeyMissing(TestUtils testUtils) throws Exception
    {
        Map<String, Object[]> queryParams = new HashMap<>();
        queryParams.put("key", new Object[] {"key1"});
        String body = testUtils.rest().getString(TranslationsResource.class, queryParams, "xwiki");
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<translations xmlns=\"http://www.xwiki.org/localization\">"
            + "<translation><key>key1</key></translation>"
            + "</translations>"
            , body);
    }

    @Test
    @Order(3)
    void translations(TestUtils testUtils, TestReference testReference) throws Exception
    {
        // Creates a translation page to have some results to return when calling the REST endpoint.
        testUtils.deletePage(testReference);
        Map<String, Object> properties = new HashMap<>();
        properties.put("scope", "WIKI");
        testUtils.addObject(testReference, "XWiki.TranslationDocumentClass", properties);
        testUtils.rest().savePage(testReference, "key1=value1 {0}", "translation");

        // Call the REST endpoint and request the translation keys created above.
        Map<String, Object[]> queryParams = new HashMap<>();
        queryParams.put("key", new Object[] {"key1"});
        String body = testUtils.rest().getString(TranslationsResource.class, queryParams, "xwiki");
        assertEquals(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<translations xmlns=\"http://www.xwiki.org/localization\">"
                + "<translation><key>key1</key><rawSource>value1 {0}</rawSource></translation>"
                + "</translations>"
                , body);
    }
}
