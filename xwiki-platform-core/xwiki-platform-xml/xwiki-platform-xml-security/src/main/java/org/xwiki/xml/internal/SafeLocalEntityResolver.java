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
package org.xwiki.xml.internal;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.xml.EntityResolver;

import io.sf.carte.doc.xml.dtd.DefaultEntityResolver;

/**
 * Uses css4j's entity resolver.
 * <p>
 * Provides the following features:
 * <ul>
 *   <li>Security protections against <a href=
 * "https://owasp.org/www-community/attacks/Server_Side_Request_Forgery">SSRF</a>
 * (although better security could be achieved by enabling the whitelist) and
 * {@code jar:} decompression bombs.</li>
 *   <li>Has about all W3C DTDs built-in for fast access.</li>
 *   <li>Enables the usage of the XHTML5 DOCTYPE declaration or even DTD-less
 * documents.</li>
 *   <li>Allows reading preset DTDs from the classpath or modulepath (currently
 * none set).</li>
 * </ul>
 *
 * @version $Id$
 */
@Component
@Singleton
public class SafeLocalEntityResolver extends DefaultEntityResolver implements EntityResolver
{
    /**
     * Construct a subclass of css4j's {@code DefaultEntityResolver} with the
     * whitelist disabled (can connect to any remote {@code http} or {@code https}
     * server, but applying restrictions to the retrieved URL).
     */
    public SafeLocalEntityResolver()
    {
        super(false);
    }
}
