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
package org.xwiki.platform.flavor.internal;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.search.ExtensionQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.platform.flavor.FlavorFilter;
import org.xwiki.platform.flavor.FlavorManagerException;
import org.xwiki.platform.flavor.FlavorQuery;
import org.xwiki.repository.internal.XWikiRepositoryModel;
import org.xwiki.text.StringUtils;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Implement a filter based on a white list.
 *  
 * @since 7.2M1 
 * @version $Id$
 */
@Component
@Singleton
@Named("WhiteList")
public class WhiteListFlavorFilter implements FlavorFilter
{
    private static final String FLAVOR_CONFIGURATION_SPACE = "Flavors";
    
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public boolean isFlavorAuthorized(ExtensionId flavor) throws FlavorManagerException
    {
        return getAuthorizedFlavorsId().contains(flavor.getId());
    }

    @Override
    public void addFilterToQuery(FlavorQuery flavorQuery) throws FlavorManagerException
    {
        for (String extensionId : getAuthorizedFlavorsId()) {
            flavorQuery.addFilter(XWikiRepositoryModel.PROP_EXTENSION_ID, extensionId, ExtensionQuery.COMPARISON.EQUAL);
        }
    }
    
    private List<String> getAuthorizedFlavorsId() throws FlavorManagerException
    {
        List<String> results = new ArrayList<>();
        
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        DocumentReference documentReference = new DocumentReference(context.getMainXWiki(), FLAVOR_CONFIGURATION_SPACE,
            "WhiteList");
        try {
            XWikiDocument document = xwiki.getDocument(documentReference, context);
            List<BaseObject> objects = document.getXObjects(
                    new DocumentReference(context.getMainXWiki(), FLAVOR_CONFIGURATION_SPACE, "WhiteListClass"));
            for (BaseObject obj : objects) {
                if (obj != null) {
                    String extensionId = obj.getStringValue("extensionId");
                    if (StringUtils.isNotBlank(extensionId)) {
                        results.add(extensionId);
                    }
                }
            }

            return results;
        } catch (XWikiException e) {
            throw new FlavorManagerException("Failed to load the flavors white list.", e);
        }
    }
    
}
