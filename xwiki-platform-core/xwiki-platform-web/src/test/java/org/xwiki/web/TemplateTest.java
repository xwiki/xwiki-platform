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
package org.xwiki.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import org.xwiki.test.page.PageTest;
import org.xwiki.text.StringUtils;

/**
 * Template tests in this module should extends this class. It makes the template located in {@code src/main/webapp}
 * available as resource environments for the tests. See {@link PageTest#getEnvironmentResource(String)} and
 * {@link PageTest#getEnvironmentResourceAsStream(String)}.
 *
 * @version $Id$
 */
public class TemplateTest extends PageTest
{
    @Override
    protected URL getEnvironmentResource(String resourceName) throws Exception
    {
        String templatePath = getResourcePath(resourceName);
        return new File(templatePath).toURI().toURL();
    }

    @Override
    protected InputStream getEnvironmentResourceAsStream(String resourceName) throws Exception
    {
        String templatePath = getResourcePath(resourceName);
        return new FileInputStream(templatePath);
    }

    private String getResourcePath(String templateName)
    {
        // Extract the part after the /skins/flamingo/ and look for it in src/main/webapp/templates instead
        String suffix = StringUtils.substringAfter(templateName, "/skins/flamingo/");
        return String.format("src/main/webapp/templates/%s", suffix);
    }
}
