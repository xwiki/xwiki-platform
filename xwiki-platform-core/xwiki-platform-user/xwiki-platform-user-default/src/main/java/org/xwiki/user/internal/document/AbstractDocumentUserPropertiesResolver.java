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
package org.xwiki.user.internal.document;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.user.UserProperties;
import org.xwiki.user.UserPropertiesResolver;
import org.xwiki.user.UserReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Commons code for all Document-based user properties resolvers.
 *
 * @version $Id$
 * @since 12.2RC1
 */
public abstract class AbstractDocumentUserPropertiesResolver implements UserPropertiesResolver
{
    @Inject
    protected Provider<XWikiContext> contextProvider;

    @Override
    public UserProperties resolve(UserReference userReference, Object... parameters)
    {
        if (!(userReference instanceof DocumentUserReference)) {
            throw new IllegalArgumentException(String.format("You need to pass a user reference of type [%s]",
                DocumentUserReference.class.getName()));
        }
        DocumentUserReference documentUserReference = (DocumentUserReference) userReference;
        return new DocumentUserProperties(documentUserReference, this.contextProvider, getConfigurationSource());
    }

    /**
     * @return the configuration source from which to resolve properties from
     */
    protected abstract ConfigurationSource getConfigurationSource();
}
