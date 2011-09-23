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
package com.xpn.xwiki.web.includeservletasstring;

import java.io.IOException;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IncludeServletAsString
{
    private static final Logger LOGGER = LoggerFactory.getLogger(IncludeServletAsString.class);

    /**
     * Creates a new instance of IncludeServletAsString
     */
    private IncludeServletAsString()
    {
    }

    static public String invokeServletAndReturnAsString(String url, HttpServletRequest servletRequest,
        HttpServletResponse servletResponse) throws IOException, ServletException
    {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Including url \"" + url + "\"...");
        }

        RequestDispatcher requestDispatcher = servletRequest.getRequestDispatcher(url);

        if (requestDispatcher == null) {
            IllegalArgumentException iae =
                new IllegalArgumentException("Failed to get RequestDispatcher for url: " + url);
            LOGGER.error(iae.getMessage(), iae);
            throw iae;
        }

        BufferedResponse bufferedResponse = new BufferedResponse(servletResponse);

        requestDispatcher.include(servletRequest, bufferedResponse);

        byte[] buffer = bufferedResponse.getBufferAsByteArray();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Buffer returned with " + buffer.length + " bytes.");
        }

        String bufferString = new String(buffer, servletResponse.getCharacterEncoding());

        return bufferString;
    }

}
