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
import java.util.concurrent.TimeoutException;

import org.xwiki.component.annotation.Role;

/**
 * Manages the web browser used for printing to PDF.
 * 
 * @version $Id$
 * @since 14.8
 */
@Role
public interface BrowserManager extends AutoCloseable
{
    /**
     * Attempts to connect to the web browser that runs on the specified host, behind the specified port.
     * 
     * @param host the host running the web browser, specified either as an IP address or a host name
     * @param port the port number to connect to
     * @throws TimeoutException if the connection timeouts
     */
    void connect(String host, int port) throws TimeoutException;

    /**
     * @return {@code true} if the web browser can be accessed, {@code false} otherwise
     */
    boolean isConnected();

    /**
     * Opens a new web browser tab that uses a separate browser context (profile).
     * 
     * @return the create tab
     */
    BrowserTab createIncognitoTab() throws IOException;

    @Override
    void close();
}
