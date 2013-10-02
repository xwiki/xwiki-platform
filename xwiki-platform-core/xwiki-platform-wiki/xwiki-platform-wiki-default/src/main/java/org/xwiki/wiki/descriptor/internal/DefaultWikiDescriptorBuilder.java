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
package org.xwiki.wiki.descriptor.internal;

import java.util.List;

import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorAlias;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component
@Singleton
public class DefaultWikiDescriptorBuilder implements WikiDescriptorBuilder
{
    static final String VALID_PAGE_PREFIX = "XWikiServer";

    @Override
    public WikiDescriptor build(List<BaseObject> serverClassObjects, XWikiDocument document, XWikiContext context)
    {
        // Create a WikiDescriptor object with the first XWikiServerClass object
        WikiDescriptor descriptor = extractWikiDescriptor(serverClassObjects.get(0), document);

        if (descriptor != null) {
            // Create WikiDescriptorAlias instances for the other XWikiServerClass objects
            for (int i = 1; i < serverClassObjects.size(); i++) {
                WikiDescriptorAlias descriptorAlias = extractWikiDescriptorAlias(serverClassObjects.get(i));
                descriptor.addDescriptorAlias(descriptorAlias);
            }
        }

        return descriptor;
    }

    private DefaultWikiDescriptor extractWikiDescriptor(BaseObject serverClassObject, XWikiDocument document)
    {
        DefaultWikiDescriptor descriptor = null;

        // If the server property is empty then consider we have an invalid WikiDescriptor
        String serverProperty = extractWikiAlias(serverClassObject);
        if (!StringUtils.isBlank(serverProperty)) {
            // If the page name doesn't start with "XWikiServer" then consider we have an invalid WikiDescriptor
            String wikiId = extractWikiId(document);
            if (wikiId != null) {
                descriptor = new DefaultWikiDescriptor(wikiId, serverProperty, document);
            }
        }

        return descriptor;
    }

    private WikiDescriptorAlias extractWikiDescriptorAlias(BaseObject serverClassObject)
    {
        return new WikiDescriptorAlias(extractWikiAlias(serverClassObject));
    }

    private String extractWikiAlias(BaseObject serverClassObject)
    {
        return serverClassObject.getStringValue(XWikiServerClassDocumentInitializer.FIELD_SERVER);
    }

    private String extractWikiId(XWikiDocument document)
    {
        String wikiId = null;
        String pageName = document.getDocumentReference().getName();
        if (pageName.startsWith(VALID_PAGE_PREFIX)) {
            wikiId = StringUtils.removeStart(pageName, "XWikiServer").toLowerCase();
        }
        return wikiId;
    }
}
