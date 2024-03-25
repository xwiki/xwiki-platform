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
package org.xwiki.filter.test.ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.xwiki.filter.test.po.ApplicationFilterHomePage;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.rest.model.jaxb.Attachment;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.Assert.assertEquals;

/**
 * UI tests for the Filter application.
 *
 * @version $Id$
 */
public class FilterIT extends AbstractTest
{
    private final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z");

    // Login as superadmin to have delete rights.
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    @Test
    public void applicationRegistration()
    {
        // Navigate to the Filter app by clicking in the Application Panel.
        // This verifies that the Filter application is registered in the Applications Panel.
        // It also verifies that the Translation is registered properly.
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        ViewPage page = applicationPanel.clickApplication("Filter Streams Converter");

        // Verify we're on the right page!
        assertEquals("Filter", page.getMetaDataValue("space"));
        assertEquals("WebHome", page.getMetaDataValue("page"));
    }

    @Test
    public void testDefaultVersionPreservedValue()
    {
        ApplicationFilterHomePage page = ApplicationFilterHomePage.gotoPage();

        Select outputType = new Select(getDriver().findElement(By.id("filter_output_type")));
        outputType.selectByValue("xwiki+instance");
        WebElement outputElement = page.getOutputField("versionPreserved");
        assertEquals("true", outputElement.getAttribute("value"));
    }

    @Test
    public void testConvertXMLURL() throws IOException
    {
        ApplicationFilterHomePage page = ApplicationFilterHomePage.gotoPage();

        URL url = getClass().getResource("/xml/document1.xml");

        // Set input
        page.setInputFilter("filter+xml");
        page.setSource("url:" + url.toString());

        // Set output
        page.setOutputFilter("filter+xml");
        File tmp = File.createTempFile("result", ".xml");
        page.setTarget("file:" + tmp.getAbsolutePath());

        // Start conversion
        page.convert();

        assertEquals(IOUtils.toString(getClass().getResource("/xml/document1.xml"), StandardCharsets.UTF_8),
            FileUtils.readFileToString(tmp, StandardCharsets.UTF_8));
    }

    @Test
    public void testImportDocument() throws Exception
    {
        ApplicationFilterHomePage filterApp = ApplicationFilterHomePage.gotoPage();

        URL url = getClass().getResource("/xml/document1.xml");

        // Set input
        filterApp.setInputFilter("filter+xml");
        filterApp.setSource("url:" + url.toString());

        // Set output
        filterApp.setOutputFilter("xwiki+instance");

        // Start conversion
        filterApp.convert();

        Page page = getUtil().rest().get(new LocalDocumentReference(List.of("space", "nestedspace"), "document"),
            Map.of("objects", new Object[] {"true"}, "class", new Object[] {"true"}, "attachments",
                new Object[] {"true"}));

        assertEquals("1.52", page.getVersion());

        assertEquals(toDate("2000-01-03 00:00:00.0 UTC"), page.getModified().getTime());
        assertEquals(toDate("2000-01-01 00:00:00.0 UTC"), page.getCreated().getTime());

        Attachment attachment = page.getAttachments().getAttachments().get(0);

        assertEquals(toDate("2000-01-05 00:00:00.0 UTC"), attachment.getDate().getTime());
        assertEquals("25.1", attachment.getVersion());
    }

    private Date toDate(String datePattern) throws ParseException
    {
        return DATE_FORMAT.parse(datePattern);
    }
}
