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
package org.xwiki.url;

import java.util.Collections;
import java.util.List;

import org.xwiki.component.annotation.Role;

/**
 * Configuration options for the URL module.
 *
 * @version $Id$
 * @since 5.3M1
 */
@Role
public interface URLConfiguration
{
    /**
     * @return the id of the URL format to use when converting a URL to a Resource. This allows to plug in different
     *         implementations and thus allows to completely control the format of XWiki URLs.
     */
    String getURLFormatId();

    /**
     * @return true means that we'll check for the last modification date of a resource to put it as a query parameter
     *         of the URL. This mechanism allows to avoid cache issues with the browser when modifying a file.
     *         false means we'll fallback on the wiki version to handle this cache issue.
     *         Its default value is true.
     * @since 11.1RC1
     */
    default boolean useResourceLastModificationDate()
    {
        return true;
    }

    /**
     * Specify the list of domains that are considered as trusted by the administrators of the wiki: those domains can
     * be used safely for redirections from the wiki or for performing other requests on them.
     * @return the list of trusted domains that can be used in the wiki.
     * @since 13.3RC1
     * @since 12.10.7
     */
    default List<String> getTrustedDomains()
    {
        return Collections.emptyList();
    }

    /**
     * Define if the trusted domains check should be performed or not. This option is provided only to allow bypassing
     * security checks globally on the wiki in case of problems.
     * @return {@code true} if the security check on domains should be performed. {@code false} otherwise.
     * @since 13.3RC1
     * @since 12.10.7
     */
    default boolean isTrustedDomainsEnabled()
    {
        return true;
    }

    /**
     * Define which URI schemes should be trusted when checking if an URI can be trusted or not.
     * Note that the list of defined schemes might not be enough if the scheme protocol is not supported (by default,
     * only http, https, ftp and files are supported).
     *
     * @return a list of supported schemes for checking trusted URI
     * @since 14.10.4
     * @since 15.0
     */
    default List<String> getTrustedSchemes()
    {
        return List.of("http", "https", "ftp");
    }
}
