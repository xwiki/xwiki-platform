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
 * @since 15.0
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
     * Parse the given string to create a URI that is safe to use.
     * This method returns null if the parsed URI is not safe to use according to
     * {@link URLSecurityManager#isURITrusted(URI)}. It might also throw a {@link URISyntaxException} if the parameter
     * cannot be properly parsed.
     *
     * @param uriRepresentation a string representing a URI that needs to be parsed.
     * @return a URI safe to use or {@code null}
     * @throws URISyntaxException if the given parameter cannot be properly parsed
     * @see URLSecurityManager#parseToSafeURI(String)
     */
    public URI parseToSafeURI(String uriRepresentation) throws URISyntaxException, SecurityException
    {
        try {
            return this.urlSecurityManager.parseToSafeURI(uriRepresentation);
        } catch (SecurityException e)
        {
            this.logger.info("The URI [{}] is considered not safe: [{}]", uriRepresentation, e.getMessage());
            this.logger.debug("Security exception stack trace: ", e);
            return null;
        }
    }
}
