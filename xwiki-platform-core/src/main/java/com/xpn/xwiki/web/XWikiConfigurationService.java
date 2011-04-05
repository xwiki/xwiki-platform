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
 *
 */
package com.xpn.xwiki.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;

/**
 * Provide access to the configuration.
 * <p>
 * Do not use it if you have access to {@link XWiki#getXWikiPreference(String, com.xpn.xwiki.XWikiContext)}, this is
 * just a hack for rare use case where we don't have access to the XWikiContext or ExecutionContext.
 * 
 * @version $Id$
 * @since 1.9.1
 */
class XWikiConfigurationService
{
    private static final Log LOG = LogFactory.getLog(XWikiConfigurationService.class);

    private static XWikiConfig config;

    public synchronized static String getProperty(String propertyKey, String defaultValue, ServletContext context)
    {
        if (config == null) {
            // Make XWikiRequestProcessor own configuration loader since XWiki of Configuration component are not
            // initialized at this time
            InputStream xwikicfgis = null;

            String configurationLocation;
            try {
                configurationLocation = XWiki.getConfigPath();

                // First try loading from a file.
                File f = new File(configurationLocation);
                try {
                    if (f.exists()) {
                        xwikicfgis = new FileInputStream(f);
                    }
                } catch (Exception e) {
                    // Error loading the file. Most likely, the Security Manager prevented it.
                    // We'll try loading it as a resource below.
                    LOG.debug("Failed to load the file [" + configurationLocation + "] using direct "
                        + "file access. The error was [" + e.getMessage() + "]. Trying to load it "
                        + "as a resource using the Servlet Context...");
                }
                // Second, try loading it as a resource using the Servlet Context
                if (xwikicfgis == null) {
                    xwikicfgis = context.getResourceAsStream(configurationLocation);
                    LOG.debug("Failed to load the file [" + configurationLocation + "] as a resource "
                        + "using the Servlet Context. Trying to load it as classpath resource...");
                }

                // Third, try loading it from the classloader used to load this current class
                if (xwikicfgis == null) {
                    xwikicfgis = XWiki.class.getClassLoader().getResourceAsStream("xwiki.cfg");
                }

                config = new XWikiConfig(xwikicfgis);
            } catch (Exception e) {
                LOG.error("Faile to lod configuration", e);

                config = new XWikiConfig();
            }
        }

        return config.getProperty(propertyKey, defaultValue);
    }
}
