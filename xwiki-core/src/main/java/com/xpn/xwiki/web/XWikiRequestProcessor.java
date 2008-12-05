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
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;

public class XWikiRequestProcessor extends org.apache.struts.action.RequestProcessor
{
    protected static final Log LOG = LogFactory.getLog(XWikiRequestProcessor.class);

    private XWikiConfig config;

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.struts.action.RequestProcessor#processPath(javax.servlet.http.HttpServletRequest,
     *      javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected String processPath(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
        throws IOException
    {
        String result = super.processPath(httpServletRequest, httpServletResponse);

        if ("1".equals(getProperty("xwiki.virtual.usepath", "0"))) {
            // Remove /wikiname part if the struts action is /wiki
            if (httpServletRequest.getServletPath().equals(
                "/" + getProperty("xwiki.virtual.usepath.servletpath", "wiki"))) {
                int wikiNameIndex = result.indexOf("/", 1);
                if (wikiNameIndex == -1) {
                    result = "";
                } else {
                    result = result.substring(result.indexOf("/", 1));
                }
            }
        }

        if (StringUtils.countMatches(result, "/") <= 2) {
            if (result.startsWith("/xmlrpc/")) {
                return "/xmlrpc/";
            } else {
                return "/view/";
            }
        } else {
            return result.substring(0, result.indexOf("/", 1) + 1);
        }
    }

    private String getProperty(String propertyKey, String defaultValue)
    {
        synchronized (this) {
            if (this.config == null) {
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
                        xwikicfgis = getServletContext().getResourceAsStream(configurationLocation);
                        LOG.debug("Failed to load the file [" + configurationLocation + "] as a resource "
                            + "using the Servlet Context. Trying to load it as classpath resource...");
                    }

                    // Third, try loading it from the classloader used to load this current class
                    if (xwikicfgis == null) {
                        xwikicfgis = XWiki.class.getClassLoader().getResourceAsStream("xwiki.cfg");
                    }

                    this.config = new XWikiConfig(xwikicfgis);
                } catch (Exception e) {
                    LOG.error("Faile to lod configuration", e);

                    this.config = new XWikiConfig();
                }
            }
        }

        return this.config.getProperty(propertyKey, defaultValue);
    }
}
