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

import org.junit.jupiter.api.BeforeEach;
import org.mockito.stubbing.Answer;
import org.xwiki.environment.Environment;
import org.xwiki.test.page.PageTest;
import org.xwiki.text.StringUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Template tests should extends this class.
 *
 * @version $Id$
 */
public class TemplateTest extends PageTest
{
    @BeforeEach
    public void templateSetup() throws Exception
    {
        // Environment resources
        Environment environment = oldcore.getMocker().getInstance(Environment.class);
        when(environment.getResource(any(String.class))).thenAnswer(
            (Answer) invocation -> {
                String templateName = (String) invocation.getArguments()[0];
                // Try to load the resource from the CP first and if not found load it from src/main/webapp/templates
                // This is to support the skin.properties template resource coming from the PageTest module.
                URL url = getClass().getResource(templateName);
                if (url == null) {
                    String templatePath = getResourcePath(templateName);
                    url = new File(templatePath).toURI().toURL();
                }
                return url;
            });
        when(environment.getResourceAsStream(any(String.class))).thenAnswer(
            (Answer) invocation -> {
                String templateName = (String) invocation.getArguments()[0];
                // Try to load the resource from the CP first and if not found load it from src/main/webapp/templates
                // This is to support the skin.properties template resource coming from the PageTest module.
                InputStream is = getClass().getResourceAsStream(templateName);
                if (is == null) {
                    String templatePath = getResourcePath(templateName);
                    is = new FileInputStream(templatePath);
                }
                return is;
            });
    }

    private String getResourcePath(String templateName)
    {
        // Extract the part after the /skins/flamingo/ and look for it in src/main/webapp/templates instead
        String suffix = StringUtils.substringAfter(templateName, "/skins/flamingo/");
        return String.format("src/main/webapp/templates/%s", suffix);
    }
}
