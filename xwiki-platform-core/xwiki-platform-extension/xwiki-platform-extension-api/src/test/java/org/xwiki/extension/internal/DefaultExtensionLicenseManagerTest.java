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
package org.xwiki.extension.internal;

import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.xwiki.extension.ExtensionLicense;
import org.xwiki.extension.ExtensionLicenseManager;
import org.xwiki.test.AbstractComponentTestCase;

public class DefaultExtensionLicenseManagerTest extends AbstractComponentTestCase
{
    private ExtensionLicenseManager licenseManager;

    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.licenseManager = getComponentManager().lookup(ExtensionLicenseManager.class);
    }

    @Test
    public void testGetLicenses()
    {
        Assert.assertTrue(this.licenseManager.getLicenses().size() > 0);
    }

    @Test
    public void testGetLicense() throws IOException
    {
        ExtensionLicense license = this.licenseManager.getLicense("Apache License 2.0");

        List<String> content =
            IOUtils.readLines(getClass().getResourceAsStream("/extension/licenses/Apache License 2.0.txt"));
        content = content.subList(4, content.size());

        Assert.assertNotNull(license);
        Assert.assertEquals("Apache License 2.0", license.getName());
        Assert.assertEquals(content, license.getContent());

        license = this.licenseManager.getLicense("ASL");

        Assert.assertNotNull(license);
        Assert.assertEquals("Apache License 2.0", license.getName());
        Assert.assertEquals(content, license.getContent());
    }
}
