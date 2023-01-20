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
package org.xwiki.url.script;

import java.net.URI;
import java.net.URISyntaxException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.url.URLSecurityManager;

/**
 * Script service for security related checks from URL and URI.
 *
 * @version $Id$
 * @since 14.10.4
 * @since 15.0RC1
 */
@Component
@Named("security.url")
@Singleton
@Unstable
public class URLSecurityScriptService implements ScriptService
{
    @Inject
    private URLSecurityManager urlSecurityManager;

    @Inject
    private Logger logger;

    /**
     * Check if the given URI representation can be trusted.
     * The trustfulness of a URI is defined by {@link URLSecurityManager#isURITrusted(URI)}.
     * If the given parameter cannot be parsed as a URI, then it's automatically considered not to be trusted.
     *
     * @param uriRepresentation the {@code String} representation of an URI to check
     * @return {@code true} if the {@link URLSecurityManager} determined that this URI can be trusted
     * @see URLSecurityManager#isURITrusted(URI)
     */
    public boolean isURITrusted(String uriRepresentation)
    {
        boolean result;
        try {
            URI uri = new URI(uriRepresentation);
            result = this.urlSecurityManager.isURITrusted(uri);
        } catch (URISyntaxException e) {
            this.logger.warn("Trying to check if [{}] is a trusted URI returned false because URI parsing failed: "
                + "[{}]", uriRepresentation, ExceptionUtils.getRootCauseMessage(e));
            result = false;
        }
        return result;
    }
}
