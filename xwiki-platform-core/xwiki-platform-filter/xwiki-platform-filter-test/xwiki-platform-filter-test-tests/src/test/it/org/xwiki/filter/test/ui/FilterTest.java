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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.xwiki.panels.test.po.ApplicationsPanel;
import org.xwiki.test.ui.AbstractTest;
import org.xwiki.test.ui.SuperAdminAuthenticationRule;
import org.xwiki.test.ui.po.ViewPage;

/**
 * UI tests for the Filter application.
 *
 * @version $Id$
 */
// TODO: provide PO APIs when Filter application is in a more final state
public class FilterTest extends AbstractTest
{
    // Login as superadmin to have delete rights.
    @Rule
    public SuperAdminAuthenticationRule authenticationRule = new SuperAdminAuthenticationRule(getUtil());

    private ViewPage vp;

    @Before
    public void setUp()
    {
        // Navigate to the Filter app by clicking in the Application Panel.
        // This verifies that the Filter application is registered in the Applications Panel.
        // It also verifies that the Translation is registered properly.
        ApplicationsPanel applicationPanel = ApplicationsPanel.gotoPage();
        this.vp = applicationPanel.clickApplication("Filter Streams Converter");

        // Verify we're on the right page!
        Assert.assertEquals("Filter", this.vp.getMetaDataValue("space"));
        Assert.assertEquals("WebHome", this.vp.getMetaDataValue("page"));
    }

    @Test
    public void testConvertXMLURL() throws IOException, InterruptedException
    {
        URL url = getClass().getResource("/xml/document1.xml");

        // Set input
        WebElement inputType = getDriver().findElement(By.id("filter_input_type"));
        // TODO: make sure the right type is selected
        WebElement inputElement = getDriver().findElement(By.id("filter_input_properties_descriptor_source"));
        inputElement.sendKeys("url:" + url.toString());

        // Set output
        WebElement outputType = getDriver().findElement(By.id("filter_output_type"));
        // TODO: make sure the right type is selected
        File tmp = File.createTempFile("result", ".xml");
        WebElement outputElement = getDriver().findElement(By.id("filter_output_properties_descriptor_target"));
        outputElement.sendKeys("file:" + tmp.getAbsolutePath());

        // Start conversion
        WebElement submit = getDriver().findElement(By.name("convert"));
        submit.click();

        // Give some time to finish the conversion
        // TODO: use a cleaner way to find if the conversion is done
        Thread.sleep(30000);

        Assert.assertEquals(IOUtils.toString(getClass().getResource("/xml/document1.xml")),
            FileUtils.readFileToString(tmp));
    }
}
