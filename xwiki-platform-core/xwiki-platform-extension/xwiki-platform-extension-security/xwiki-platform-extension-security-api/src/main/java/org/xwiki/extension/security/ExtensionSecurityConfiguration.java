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
package org.xwiki.extension.security;

import org.xwiki.component.annotation.Role;

/**
 * Provide the configuration values for the extension security.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Role
public interface ExtensionSecurityConfiguration
{
    /**
     * @return {@code true} when the security scan is enabled, {@code false} otherwise. When the security scan is
     *     disabled, no security scan is performed and the list of security vulnerabilities is not displayed in the
     *     administration. The default value is {@code true}
     */
    boolean isSecurityScanEnabled();

    /**
     * @return the delay before starting a new security scan after the last one has finished. The default value is 24
     *     hours.
     */
    int getScanDelay();

    /**
     * The returned url must match what's documented in <a href="https://google.github.io/osv.dev/api/">osv.dev API
     * documentation</a>.
     *
     * @return the url to use for the rest api endpoint. The default value is <a
     *     href="https://api.osv.dev/v1/query">https://api.osv.dev/v1/query</a>
     */
    String getScanURL();

    /**
     * @return the URL where the reviews are fetched
     * @since 15.6RC1
     */
    String getReviewsURL();
}
