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
package org.xwiki.icon.test.ui;

import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;

import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBContext;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.icon.rest.IconThemesResource;
import org.xwiki.icon.rest.model.jaxb.Icon;
import org.xwiki.icon.rest.model.jaxb.Icons;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

import static java.util.Collections.singletonList;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Functional tests of the icon REST endpoint.
 *
 * @version $Id$
 * @since 13.4RC1
 */
@UITest
class IconThemesRestIT
{
    @BeforeEach
    void setUp(TestUtils testUtils)
    {
        // Login as superadmin to define the preferences.
        testUtils.loginAsSuperAdmin();
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "XWikiPreferences");
        testUtils.updateObject(documentReference, "XWiki.XWikiPreferences", 0, "iconTheme", "IconThemes.FontAwesome");
        // Lougout because the rest of the test must pass as a guest user.
        testUtils.forceGuestUser();
    }

    @Test
    @Order(1)
    void iconsNoParamKeys(TestUtils testUtils) throws Exception
    {
        URI rootIconThemeURI = testUtils.rest().createUri(IconThemesResource.class, new HashMap<>(), "xwiki");
        String body = testUtils.rest().getString(UriBuilder.fromUri(rootIconThemeURI).segment("icons").build());
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<icons xmlns=\"http://www.xwiki.org/icon\"/>", body);
    }

    @Test
    @Order(2)
    void iconsUnknownWiki(TestUtils testUtils) throws Exception
    {
        URI rootIconThemeURI = testUtils.rest().createUri(IconThemesResource.class, new HashMap<>(), "nowiki");
        try (CloseableHttpResponse response =
            testUtils.rest().executeGet(UriBuilder.fromUri(rootIconThemeURI).segment("icons").build())) {
            int statusCode = response.getCode();
            assertEquals(NOT_FOUND.getStatusCode(), statusCode);
        }
    }

    @Test
    @Order(3)
    void icons(TestUtils testUtils) throws Exception
    {
        URI rootIconThemeURI = testUtils.rest().createUri(IconThemesResource.class, new HashMap<>(), "xwiki");

        URI uri = UriBuilder.fromUri(rootIconThemeURI).segment("icons")
            .queryParam("name", "add", "unknown", "arrow_undo").build();
        String body = testUtils.rest().getString(uri);
        // @formatter:off
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<icons xmlns=\"http://www.xwiki.org/icon\">"
            + "<icon>"
            + "<name>add</name>"
            + "<iconSetType>FONT</iconSetType>"
            + "<iconSetName>Font Awesome</iconSetName>"
            + "<cssClass>fa fa-plus</cssClass>"
            + "</icon>"
            + "<icon>"
            + "<name>arrow_undo</name>"
            + "<iconSetType>FONT</iconSetType>"
            + "<iconSetName>Font Awesome</iconSetName>"
            + "<cssClass>fa fa-undo</cssClass>"
            + "</icon>"
            + "<missingIcons>unknown</missingIcons></icons>"
            , body);
        // @formatter:on
    }

    @Test
    @Order(4)
    void iconsUnknownTheme(TestUtils testUtils) throws Exception
    {
        URI rootIconThemeURI = testUtils.rest().createUri(IconThemesResource.class, new HashMap<>(), "xwiki");

        URI uri = UriBuilder.fromUri(rootIconThemeURI).segment("notheme").segment("icons")
            .queryParam("name", "add", "unknown", "arrow_undo").build();
        try (CloseableHttpResponse response = testUtils.rest().executeGet(uri)) {
            int statusCode = response.getCode();
            assertEquals(NOT_FOUND.getStatusCode(), statusCode);
        }
    }

    @Test
    @Order(5)
    void iconsWithTheme(TestUtils testUtils) throws Exception
    {
        URI rootIconThemeURI = testUtils.rest().createUri(IconThemesResource.class, new HashMap<>(), "xwiki");

        URI uri = UriBuilder.fromUri(rootIconThemeURI).segment("Silk").segment("icons")
            .queryParam("name", "add", "unknown", "arrow_undo").build();
        String body = testUtils.rest().getString(uri);
        JAXBContext jaxbContext = JAXBContext.newInstance(Icons.class);

        // We need to parse the response body to analyse the icons attributes more finely. This is useful in
        // particular to assert the icon's url attribute while ignoring the cache-version get parameter value.
        Icons icons = (Icons) jaxbContext.createUnmarshaller().unmarshal(new StringReader(body));

        assertEquals(singletonList("unknown"), icons.getMissingIcons());
        assertEquals(2, icons.getIcons().size());

        Icon iconAdd = icons.getIcons().get(0);
        assertEquals("add", iconAdd.getName());
        assertEquals("IMAGE", iconAdd.getIconSetType());
        assertEquals("Silk", iconAdd.getIconSetName());
        assertNull(iconAdd.getCssClass());
        assertEquals("/xwiki/resources/icons/silk/add.png", URI.create(iconAdd.getUrl()).getPath());

        Icon iconArrowUndo = icons.getIcons().get(1);
        assertEquals("arrow_undo", iconArrowUndo.getName());
        assertEquals("IMAGE", iconArrowUndo.getIconSetType());
        assertEquals("Silk", iconArrowUndo.getIconSetName());
        assertNull(iconArrowUndo.getCssClass());
        assertEquals("/xwiki/resources/icons/silk/arrow_undo.png", URI.create(iconArrowUndo.getUrl()).getPath());

    }
}
