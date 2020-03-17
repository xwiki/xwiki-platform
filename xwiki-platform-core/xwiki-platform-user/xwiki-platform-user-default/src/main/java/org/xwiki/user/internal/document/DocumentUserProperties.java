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

import java.util.function.Supplier;

import javax.inject.Provider;

import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.user.internal.DefaultUserProperties;

import com.xpn.xwiki.XWikiContext;

/**
 * Document-based implementation of a XWiki user properties.
 *
 * Always go through a {@link org.xwiki.user.UserPropertiesResolver} to get a
 * {@link DocumentUserProperties} object (this is why this class is package-protected). The reason is because the
 * resolvers know how to handle Guest and SuperAdmin users properly.
 *
 * @version $Id$
 * @since 12.2RC1
 */
class DocumentUserProperties extends DefaultUserProperties
{
    static final LocalDocumentReference USERS_CLASS_REFERENCE = new LocalDocumentReference("XWiki", "XWikiUsers");

    private DocumentUserReference userReference;

    private Provider<XWikiContext> contextProvider;

    /**
     * @param userReference the user reference
     * @param contextProvider the component to get/set the current user in the context
     * @param userConfigurationSource the component to get the user properties
     * @param
     */
    DocumentUserProperties(DocumentUserReference userReference, Provider<XWikiContext> contextProvider,
        ConfigurationSource userConfigurationSource)
    {
        super(userConfigurationSource);

        this.userReference = userReference;
        this.contextProvider = contextProvider;
    }

    @Override
    protected <T> T execute(Supplier<T> supplier)
    {
        XWikiContext xcontext = this.contextProvider.get();
        DocumentReference originalUserReference = xcontext.getUserReference();
        try {
            xcontext.setUserReference(getInternalReference());
            return supplier.get();
        } finally {
            xcontext.setUserReference(originalUserReference);
        }
    }

    private DocumentUserReference getUserReference()
    {
        return this.userReference;
    }

    private DocumentReference getInternalReference()
    {
        return getUserReference().getReference();
    }
}
