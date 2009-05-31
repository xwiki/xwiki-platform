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
package org.xwiki.rest;

import java.io.InputStream;
import java.util.logging.LogManager;

import javax.servlet.ServletException;

import com.noelios.restlet.ext.servlet.ServerServlet;

/**
 * $Id$
 */
public class XWikiRestletServlet extends ServerServlet
{
    private static final String JAVA_LOGGING_PROPERTY_FILE = "java-logging.properties";

    private static final long serialVersionUID = 9148448182654390153L;

    @Override
    public void init() throws ServletException
    {
        super.init();

        try {
            /* Try first in WEB-INF */
            InputStream is =
                getServletContext().getResourceAsStream(String.format("/WEB-INF/%s", JAVA_LOGGING_PROPERTY_FILE));

            /* If nothing is there then try in the current jar */
            if (is == null) {
                is = getClass().getClassLoader().getResourceAsStream(JAVA_LOGGING_PROPERTY_FILE);
            }

            if (is != null) {
                LogManager.getLogManager().readConfiguration(is);
                is.close();
            }
        } catch (Exception e) {
            log("Unable to initialize Java logging framework. Using defaults", e);
        }
    }

}
