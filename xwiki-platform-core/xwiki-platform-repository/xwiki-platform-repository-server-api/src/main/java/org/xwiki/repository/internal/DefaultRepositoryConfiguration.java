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
package org.xwiki.repository.internal;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of {@link RepositoryConfiguration}.
 * 
 * @version $Id$
 * @since 4.5M1
 */
@Component
@Singleton
public class DefaultRepositoryConfiguration implements RepositoryConfiguration
{
    /**
     * Gives access to the current {@link XWikiContext}.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used to create an absolute reference from a relative one.
     */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> resolver;

    /**
     * @return the XWiki object containing the configuration
     * @throws XWikiException when failing to get the object
     */
    private BaseObject getConfigurationObject() throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        XWikiDocument document =
            xcontext.getWiki().getDocument(this.resolver.resolve(XWikiRepositoryModel.CONFIGURATION_REFERENCE),
                xcontext);

        return document.getXObject(XWikiRepositoryModel.CONFIGURATION_CLASSREFERENCE);
    }

    @Override
    public String getDefaultIdPrefix() throws XWikiException
    {
        BaseObject obj = getConfigurationObject();

        return obj != null ? obj.getStringValue(XWikiRepositoryModel.PROP_CONFIGURATION_DEFAULTIDPREFIX) : "";
    }

    @Override
    public List<String> getValidTypes() throws XWikiException
    {
        BaseObject obj = getConfigurationObject();

        return obj != null ? obj.getListValue(XWikiRepositoryModel.PROP_CONFIGURATION_VALIDTYPES) : Collections
            .<String> emptyList();
    }

    @Override
    public boolean isValidType(String type) throws XWikiException
    {
        List<String> types = getValidTypes();

        return types.isEmpty() || types.contains(StringUtils.defaultString(type));
    }

}
