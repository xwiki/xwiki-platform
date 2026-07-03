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
package org.xwiki.image.style.test.ui;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.jupiter.params.ParameterizedTest;
import org.xwiki.image.style.rest.ImageStylesResource;
import org.xwiki.image.style.test.po.ImageStyleAdministrationPage;
import org.xwiki.image.style.test.po.ImageStyleConfigurationForm;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.WikisSource;
import org.xwiki.test.ui.TestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Docker tests for the image style administration, tests the user interface of the administation as well as the rest
 * endpoints.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@UITest
class ImageStyleIT
{
    @ParameterizedTest
    @WikisSource(extensions = {
        "org.xwiki.platform:xwiki-platform-image-style-ui",
        "org.xwiki.platform:xwiki-platform-administration-ui"
    })
    void imageStyleAdministration(WikiReference wikiReference, TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        // Make sure that an icon theme is configured.
        testUtils.setWikiPreference("iconTheme", "IconThemes.Silk");
        String wikiName = wikiReference.getName();

        String defaultName = String.format("default-%s", wikiName);
        String defaultPrettyName = String.format("Default-%s", wikiName);
        String type = String.format("default-class-%s", wikiName);

        testUtils.deletePage(
            new DocumentReference(wikiName, List.of("Image", "Style", "Code", "ImageStyles"), defaultName));
        testUtils.updateObject(new DocumentReference(wikiName, List.of("Image", "Style", "Code"), "Configuration"),
            "Image.Style.Code.ConfigurationClass", 0, "defaultStyle", "");

        assertEquals(Map.of(), getDefaultFromRest(testUtils, wikiReference));

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<styles xmlns=\"http://www.xwiki.org/imageStyle\"/>", getImageStylesFromRest(testUtils, wikiReference));

        ImageStyleAdministrationPage imageStyleAdministrationPage =
            ImageStyleAdministrationPage.getToAdminPage(wikiReference);
        ImageStyleConfigurationForm imageStyleConfigurationForm =
            imageStyleAdministrationPage.submitNewImageStyleForm(defaultName);
        imageStyleConfigurationForm
            .setPrettyName(defaultPrettyName)
            .setType(type)
            .clickSaveAndView(true);
        imageStyleAdministrationPage = imageStyleConfigurationForm.clickBackToTheAdministration();
        assertEquals("", imageStyleAdministrationPage.getDefaultStyle());
        imageStyleAdministrationPage.submitDefaultStyleForm(defaultName);
        assertEquals(defaultName, imageStyleAdministrationPage.getDefaultStyle());

        TableLayoutElement tableLayout = new LiveDataElement("imageStyles").getTableLayout();
        assertEquals(1, tableLayout.countRows());
        assertEquals(defaultName, tableLayout.getCell("Identifier", 1).getText());
        assertEquals(defaultPrettyName, tableLayout.getCell("Pretty Name", 1).getText());
        assertEquals(type, tableLayout.getCell("Type", 1).getText());

        assertEquals(Map.of("forceDefaultStyle", "false", "defaultStyle", defaultName),
            getDefaultFromRest(testUtils, wikiReference));

        assertEquals(String.format("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<styles xmlns=\"http://www.xwiki.org/imageStyle\">"
            + "<imageStyle>"
            + "<identifier>%s</identifier>"
            + "<prettyName>%s</prettyName>"
            + "<type>%s</type>"
            + "<adjustableSize>false</adjustableSize>"
            + "<defaultWidth xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=\"true\"/>"
            + "<defaultHeight xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:nil=\"true\"/>"
            + "<adjustableBorder>false</adjustableBorder>"
            + "<defaultBorder>false</defaultBorder>"
            + "<adjustableAlignment>false</adjustableAlignment>"
            + "<defaultAlignment>none</defaultAlignment>"
            + "<adjustableTextWrap>false</adjustableTextWrap>"
            + "<defaultTextWrap>false</defaultTextWrap>"
            + "</imageStyle>"
            + "</styles>", defaultName, defaultPrettyName, type), getImageStylesFromRest(testUtils, wikiReference));
    }

    private Map<?, ?> getDefaultFromRest(TestUtils testUtils, WikiReference wikiReference) throws Exception
    {
        URI imageStylesResourceURI =
            testUtils.rest().createUri(ImageStylesResource.class, new HashMap<>(), wikiReference.getName());
        GetMethod getMethod = testUtils.rest().executeGet(UriBuilder.fromUri(imageStylesResourceURI)
            .segment("default")
            .queryParam("media", "json")
            .build());
        return new ObjectMapper().readValue(getMethod.getResponseBodyAsString(), Map.class);
    }

    private String getImageStylesFromRest(TestUtils testUtils, WikiReference wikiReference) throws Exception
    {
        URI imageStylesResourceURI =
            testUtils.rest().createUri(ImageStylesResource.class, new HashMap<>(), wikiReference.getName());
        GetMethod getMethod =
            testUtils.rest().executeGet(imageStylesResourceURI);
        return getMethod.getResponseBodyAsString().trim();
    }
}
