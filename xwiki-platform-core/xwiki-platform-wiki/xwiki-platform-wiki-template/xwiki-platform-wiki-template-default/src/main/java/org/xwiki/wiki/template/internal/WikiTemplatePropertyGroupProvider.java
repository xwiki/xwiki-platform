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
package org.xwiki.wiki.template.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.properties.WikiPropertyGroup;
import org.xwiki.wiki.properties.WikiPropertyGroupException;
import org.xwiki.wiki.properties.WikiPropertyGroupProvider;
import org.xwiki.wiki.template.WikiTemplatePropertyGroup;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Implementation of {@link WikiPropertyGroupProvider} for the property groups concerning the templates.
 *
 * @since 5.3M2
 * @version $Id$
 */
@Component
@Named(WikiTemplatePropertyGroupProvider.GROUP_NAME)
@Singleton
public class WikiTemplatePropertyGroupProvider implements WikiPropertyGroupProvider
{
    /**
     * Name of the property group.
     */
    public static final String GROUP_NAME = "template";

    private static final String ERROR_MESSAGE_NO_DESCRIPTOR_DOCUMENT = "Unable to load descriptor "
            + "document for wiki [%s].";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private WikiDescriptorDocumentHelper wikiDescriptorDocumentHelper;

    @Override
    public WikiPropertyGroup get(String wikiId) throws WikiPropertyGroupException
    {
        WikiTemplatePropertyGroup group = new WikiTemplatePropertyGroup(GROUP_NAME);

        try {
            XWikiDocument descriptorDocument = wikiDescriptorDocumentHelper.getDocumentFromWikiId(wikiId);
            // Get the object
            BaseObject object = descriptorDocument.getXObject(WikiTemplateClassDocumentInitializer.SERVER_CLASS);
            if (object != null) {
                group.setTemplate(
                        object.getIntValue(WikiTemplateClassDocumentInitializer.FIELD_ISWIKITEMPLATE, 0) != 0);
            }
        } catch (WikiManagerException e) {
            throw new WikiPropertyGroupException(String.format(ERROR_MESSAGE_NO_DESCRIPTOR_DOCUMENT, wikiId), e);
        }

        return group;
    }

    @Override
    public void save(WikiPropertyGroup group, String wikiId) throws WikiPropertyGroupException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();

        WikiTemplatePropertyGroup templateGroup = (WikiTemplatePropertyGroup) group;

        try {
            XWikiDocument descriptorDocument = wikiDescriptorDocumentHelper.getDocumentFromWikiId(wikiId);

            // Avoid modifying the cached document
            descriptorDocument = descriptorDocument.clone();

            BaseObject object = descriptorDocument.getXObject(WikiTemplateClassDocumentInitializer.SERVER_CLASS,
                    true, context);
            object.setIntValue(WikiTemplateClassDocumentInitializer.FIELD_ISWIKITEMPLATE,
                    templateGroup.isTemplate() ? 1 : 0);
            // The document must have a creator
            if (descriptorDocument.getCreatorReference() == null) {
                descriptorDocument.setCreatorReference(context.getUserReference());
            }
            // The document must have an author
            if (descriptorDocument.getAuthorReference() == null) {
                descriptorDocument.setAuthorReference(context.getUserReference());
            }
            xwiki.saveDocument(descriptorDocument, String.format("Changed property group [%s].", GROUP_NAME), context);
        } catch (WikiManagerException e) {
            throw new WikiPropertyGroupException(String.format(ERROR_MESSAGE_NO_DESCRIPTOR_DOCUMENT, wikiId), e);
        } catch (XWikiException e) {
            throw new WikiPropertyGroupException("Unable to save descriptor document.", e);
        }
    }

}
