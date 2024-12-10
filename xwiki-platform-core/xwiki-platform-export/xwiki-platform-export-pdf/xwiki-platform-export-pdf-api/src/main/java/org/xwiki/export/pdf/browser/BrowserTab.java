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
package org.xwiki.export.pdf.browser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.servlet.http.Cookie;

import org.xwiki.stability.Unstable;

/**
 * Represents a web browser tab.
 * 
 * @version $Id$
 * @since 14.8
 */
@Unstable
public interface BrowserTab extends AutoCloseable
{
    /**
     * Sets the extra HTTP headers to use when requesting web pages in this browser tab.
     * 
     * @param headers the extra HTTP headers to use when requesting web pages in this browser tab
     * @since 15.10.16
     * @since 16.4.6
     * @since 16.10.2
     * @since 17.0.0RC1
     */
    default void setExtraHTTPHeaders(Map<String, List<String>> headers)
    {
        // Do nothing by default.
    }

    /**
     * Navigates to the specified web page, optionally waiting for it to be ready (fully loaded).
     * 
     * @param url the URL of the web page we are going to navigate to
     * @param cookies the cookies to use when loading the specified web page
     * @param wait {@code true} to wait for the page to be ready, {@code false} otherwise
     * @param timeout the number of seconds to wait for the web page to be ready before timing out
     * @return {@code true} if the navigation was successful, {@code false} otherwise
     * @throws IOException if navigating to the specified web page fails
     * @since 14.9
     */
    boolean navigate(URL url, Cookie[] cookies, boolean wait, int timeout) throws IOException;

    /**
     * Navigates to the specified web page, optionally waiting for it to be ready (fully loaded).
     *
     * @param url the URL of the web page we are going to navigate to
     * @param cookies the cookies to use when loading the specified web page
     * @param wait {@code true} to wait for the page to be ready, {@code false} otherwise
     * @return {@code true} if the navigation was successful, {@code false} otherwise
     * @throws IOException if navigating to the specified web page fails
     */
    default boolean navigate(URL url, Cookie[] cookies, boolean wait) throws IOException
    {
        return navigate(url, cookies, wait, 60);
    }

    /**
     * Navigates to the specified web page, optionally waiting for it to be ready (fully loaded).
     * 
     * @param url the URL of the web page we are going to navigate to
     * @param wait {@code true} to wait for the page to be ready, {@code false} otherwise
     * @return {@code true} if the navigation was successful, {@code false} otherwise
     * @throws IOException if navigating to the specified web page fails
     */
    default boolean navigate(URL url, boolean wait) throws IOException
    {
        return navigate(url, null, wait);
    }

    /**
     * Navigates to the specified web page, without waiting for it to be fully loaded.
     * 
     * @param url the URL of the web page we are going to navigate to
     * @return {@code true} if the navigation was successful, {@code false} otherwise
     * @throws IOException if navigating to the specified web page fails
     */
    default boolean navigate(URL url) throws IOException
    {
        return navigate(url, false);
    }

    /**
     * @return the source of the web page loaded in this browser tab
     * @since 14.10
     */
    String getSource();

    /**
     * Print the current web page to PDF.
     * 
     * @param cleanup the code to execute after the PDF was generated, useful for performing cleanup
     * @return the PDF input stream
     */
    InputStream printToPDF(Runnable cleanup);

    @Override
    void close();
}
