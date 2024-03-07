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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * PDF export configuration options.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Role
public interface PDFExportConfiguration
{
    /**
     * The default host used by the headless Chrome to access XWiki.
     */
    String DEFAULT_XWIKI_HOST = "host.xwiki.internal";

    /**
     * The default URI used by the headless Chrome to access XWiki.
     */
    String DEFAULT_XWIKI_URI = "//" + DEFAULT_XWIKI_HOST;

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
     * @return the name or id of the Docker network to add the Chrome Docker container to; this is useful when XWiki
     *         itself runs inside a Docker container and you want to have the Chrome container in the same network in
     *         order for them to communicate, see {@link #getXWikiURI()}; defaults to "{@code bridge}" the default
     *         Docker network
     * @see #getXWikiURI()
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
     * @return the number of seconds to wait for the Chrome remote debugging service to responde before giving up
     * @since 14.10.16
     * @since 15.5.2
     * @since 15.7
     */
    default int getChromeRemoteDebuggingTimeout()
    {
        return 10;
    }

    /**
     * @return the base URI that the headless Chrome browser should use to access the XWiki instance (i.e. the print
     *         preview page); the host (domain or IP address) is mandatory but the scheme and port number are optional
     *         (they default on the scheme and port number used when triggering the PDF export); defaults to
     *         "{@code host.xwiki.internal}" which means the host running the Docker daemon; if XWiki runs itself inside
     *         a Docker container then you should use the assigned network alias, provided both containers (XWiki and
     *         Chrome) are in the same Docker network, specified by {@link #getDockerNetwork()};
     * @throws URISyntaxException if the specified URI is not valid
     * @since 14.10.15
     * @since 15.5.2
     * @since 15.7RC1
     */
    URI getXWikiURI() throws URISyntaxException;

    /**
     * @return {@code true} if the XWiki URI is specified in the configuration, {@code false} if the default XWiki URI
     *         is used
     */
    default boolean isXWikiURISpecified()
    {
        try {
            return !getXWikiURI().toString().equals(DEFAULT_XWIKI_URI);
        } catch (URISyntaxException e) {
            // If the XWiki URI cannot be parsed then most likely it is specified (basically we expect the default XWiki
            // URI to be valid).
            return true;
        }
    }

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

    /**
     * @return the list of PDF export templates the user can choose from
     * @since 14.9RC1
     */
    default List<DocumentReference> getTemplates()
    {
        return Collections.emptyList();
    }

    /**
     * @return the number of seconds to wait for the web page to be ready (for print) before timing out
     * @since 14.9
     */
    default int getPageReadyTimeout()
    {
        return 60;
    }

    /**
     * @return the maximum content size, in kilobytes (KB), an user is allowed to export to PDF; in order to compute the
     *         content size we sum the size of the HTML rendering for each of the XWiki documents included in the
     *         export; the size of external resources, such as images, style sheets, JavaScript code is not taken into
     *         account; {@code 0} means no limit; defaults to {@code 5MB}
     * @since 14.10
     */
    default int getMaxContentSize()
    {
        return 5000;
    }

    /**
     * @return the maximum number of PDF exports that can be executed in parallel (each PDF export needs a separate
     *         thread); defaults to {@code 3}
     * @since 14.10
     */
    default int getThreadPoolSize()
    {
        return 3;
    }

    /**
     * @return whether to replace or not the old PDF export based on Apache Formatting Objects Processor (FOP)
     * @since 14.10
     */
    default boolean isReplacingFOP()
    {
        return true;
    }
}
