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
package org.xwiki.url.internal.standard;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.WikiReference;

/**
 * Resolves the wiki name based on the passed domain name (of the form {@code wikiname.domain.domaintype}, eg
 * {@code mywiki.xwiki.org}).
 *
 * @version $Id$
 * @since 5.1M1
 */
@Component
@Named("domain")
@Singleton
public class DomainWikiReferenceResolver implements WikiReferenceResolver
{
    /**
     * Used to get the main wiki name.
     * @todo replace that with a proper API to get the main wiki reference
     */
    @Inject
    private EntityReferenceValueProvider entityReferenceValueProvider;

    @Override
    public WikiReference resolve(String host)
    {
        String wiki;

        // If the following conditions are met we consider that the host is pointing to the main wiki:
        // - Equals to "localhost"
        // - Is defined as an IP address
        if ("localhost".equals(host) || host.matches("[0-9]{1,3}(?:\\.[0-9]{1,3}){3}")) {
            wiki = this.entityReferenceValueProvider.getDefaultValue(EntityType.WIKI);
        } else {
            int pos = host.indexOf('.');
            if (pos > -1) {
                wiki = host.substring(0, pos);
            } else {
                wiki = host;
            }
        }

        return new WikiReference(wiki);
    }
}
