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
package org.xwiki.export.pdf;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * PDF export configuration options.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Role
@Unstable
public interface PDFExportConfiguration
{
    /**
     * @return the Docker image used to create the Docker container running the headless Chrome web browser; defaults to
     *         "{@code zenika/alpine-chrome:latest}"
     */
    String getChromeDockerImage();

    /**
     * @return the name of the Docker container running the headless Chrome web browser used to print web pages to PDF;
     *         defaults to "{@code headless-chrome-pdf-printer}"
     */
    String getChromeDockerContainerName();

    /**
     * @return {@code true} if the Docker container running the headless Chrome web browser can be reused across XWiki
     *         restarts, {@code false} to remove the container each time XWiki is stopped / restarted; defaults to
     *         {@code false}
     */
    boolean isChromeDockerContainerReusable();

    /**
     * @return the name or id of the Docker network to add the Chrome Docker container to; this is useful when XWiki
     *         itself runs inside a Docker container and you want to have the Chrome container in the same network in
     *         order for them to communicate, see {@link #getXWikiHost()}; defaults to "{@code bridge}" the default
     *         Docker network
     * @see #getXWikiHost()
     */
    String getDockerNetwork();

    /**
     * @return the host running the headless Chrome web browser, specified either by its name or by its IP address; this
     *         allows you to use a remote Chrome instance, running on a separate machine, rather than a Chrome instance
     *         running in a Docker container on the same machine; defaults to empty value, meaning that by default the
     *         PDF export is done using the Chrome instance running in the Docker container specified by
     *         {@link #getChromeDockerContainerName()}
     */
    String getChromeHost();

    /**
     * @return the port number used for communicating with the headless Chrome web browser running on the host specified
     *         by {@link #getChromeHost()}; defaults to {@code 9222}
     */
    int getChromeRemoteDebuggingPort();

    /**
     * @return the host name or IP address that the headless Chrome browser should use to access the XWiki instance
     *         (i.e. the print preview page); defaults to "{@code host.xwiki.internal}" which means the host running the
     *         Docker daemon; if XWiki runs itself inside a Docker container then you should use the assigned network
     *         alias, provided both containers (XWiki and Chrome) are in the same Docker network, specified by
     *         {@link #getDockerNetwork()};
     */
    String getXWikiHost();

    /**
     * @return {@code true} if the PDF export should be performed server-side, e.g. using a headless Chrome web browser
     *         running inside a Docker container, {@code false} if the user's web browser should be used instead;
     *         defaults to client-side PDF generation
     * @since 14.4.3
     * @since 14.5.1
     * @since 14.6RC1
     */
    default boolean isServerSide()
    {
        return false;
    }
}
