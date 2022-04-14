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

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.jupiter.api.Test;
import org.xwiki.image.style.rest.ImageStylesResource;
import org.xwiki.image.style.test.po.ImageStyleAdministrationPage;
import org.xwiki.image.style.test.po.ImageStyleConfigurationForm;
import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.livedata.test.po.TableLayoutElement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;

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
    @Test
    void imageStyleAdministration(TestUtils testUtils) throws Exception
    {
        testUtils.loginAsSuperAdmin();
        // Make sure that an icon theme is configured.
        testUtils.setWikiPreference("iconTheme", "IconThemes.Silk");
        testUtils.deletePage(
            new DocumentReference("xwiki", List.of("Image", "Style", "Code", "ImageStyles"), "default"));
        testUtils.updateObject(new DocumentReference("xwiki", List.of("Image", "Style", "Code"), "Configuration"),
            "Image.Style.Code.ConfigurationClass", 0, "defaultStyle", "");

        assertEquals("<MapN/>", getDefaultFromRest(testUtils));

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<styles xmlns=\"http://www.xwiki.org/imageStyle\"/>", getImageStylesFromRest(testUtils));

        ImageStyleAdministrationPage imageStyleAdministrationPage = ImageStyleAdministrationPage.getToAdminPage();
        ImageStyleConfigurationForm imageStyleConfigurationForm =
            imageStyleAdministrationPage.submitNewImageStyleForm("default");
        imageStyleConfigurationForm
            .setPrettyName("Default")
            .setType("default-class")
            .clickSaveAndView(true);
        imageStyleAdministrationPage = imageStyleConfigurationForm.clickBackToTheAdministration();
        assertEquals("", imageStyleAdministrationPage.getDefaultStyle());
        imageStyleAdministrationPage.submitDefaultStyleForm("default");
        assertEquals("default", imageStyleAdministrationPage.getDefaultStyle());

        TableLayoutElement tableLayout = new LiveDataElement("imageStyles").getTableLayout();
        assertEquals(1, tableLayout.countRows());
        assertEquals("default", tableLayout.getCell("Identifier", 1).getText());
        assertEquals("Default", tableLayout.getCell("Pretty Name", 1).getText());
        assertEquals("default-class", tableLayout.getCell("Type", 1).getText());

        assertEquals("<Map1><defaultStyle>default</defaultStyle></Map1>", getDefaultFromRest(testUtils));

        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<styles xmlns=\"http://www.xwiki.org/imageStyle\">"
            + "<imageStyle>"
            + "<identifier>default</identifier>"
            + "<prettyName>Default</prettyName>"
            + "<type>default-class</type>"
            + "<adjustableSize>false</adjustableSize>"
            + "<defaultWidth xsi:nil=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>"
            + "<defaultHeight xsi:nil=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>"
            + "<adjustableBorder>false</adjustableBorder>"
            + "<defaultBorder>false</defaultBorder>"
            + "<adjustableAlignment>false</adjustableAlignment>"
            + "<defaultAlignment>none</defaultAlignment>"
            + "<adjustableTextWrap>false</adjustableTextWrap>"
            + "<defaultTextWrap>false</defaultTextWrap>"
            + "</imageStyle>"
            + "</styles>", getImageStylesFromRest(testUtils));
    }

    private String getDefaultFromRest(TestUtils testUtils) throws Exception
    {
        URI imageStylesResourceURI = testUtils.rest().createUri(ImageStylesResource.class, new HashMap<>(), "xwiki");
        GetMethod getMethod =
            testUtils.rest().executeGet(UriBuilder.fromUri(imageStylesResourceURI).segment("default").build());
        return getMethod.getResponseBodyAsString();
    }

    private String getImageStylesFromRest(TestUtils testUtils) throws Exception
    {
        URI imageStylesResourceURI = testUtils.rest().createUri(ImageStylesResource.class, new HashMap<>(), "xwiki");
        GetMethod getMethod =
            testUtils.rest().executeGet(imageStylesResourceURI);
        return getMethod.getResponseBodyAsString().trim();
    }
}
