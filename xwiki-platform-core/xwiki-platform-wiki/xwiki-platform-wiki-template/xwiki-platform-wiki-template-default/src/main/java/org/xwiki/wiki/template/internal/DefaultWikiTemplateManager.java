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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.manager.WikiManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.properties.WikiPropertyGroupException;
import org.xwiki.wiki.properties.WikiPropertyGroupProvider;
import org.xwiki.wiki.template.WikiTemplatePropertyGroup;
import org.xwiki.wiki.template.WikiTemplateManager;
import org.xwiki.wiki.template.WikiTemplateManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

@Component
@Singleton
public class DefaultWikiTemplateManager implements WikiTemplateManager
{
    /**
     * Used to access current {@link XWikiContext}.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("default")
    private WikiManager wikiManager;

    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private WikiDescriptorDocumentHelper wikiDescriptorDocumentHelper;

    @Inject
    @Named("template")
    private WikiPropertyGroupProvider templateWikiPropertyGroupProvider;

    @Override
    public Collection<WikiDescriptor> getTemplates() throws WikiTemplateManagerException
    {
        List<WikiDescriptor> result = new ArrayList<WikiDescriptor>();

        try {
            Query query = this.queryManager.createQuery(
                    "from doc.object(XWiki.XWikiServerTemplateClass) as descriptor where doc.name like 'XWikiServer%' "
                    + "and descriptor.iswikitemplate = 1", Query.XWQL);
            query.setWiki(xcontextProvider.get().getMainXWiki());
            List<String> documentNames = query.execute();

            if (documentNames != null && !documentNames.isEmpty()) {
                for (String documentName : documentNames) {
                    String id = documentName.substring("XWiki.XWikiServer".length()).toLowerCase();
                    result.add(wikiDescriptorManager.getById(id));
                }
            }
        } catch (Exception e) {
            throw new WikiTemplateManagerException("Failed to locate XWiki.XWikiServerClass documents", e);
        }

        return result;
    }

    @Override
    public void setTemplate(String wikiId, boolean value) throws WikiTemplateManagerException
    {
        try {
            // Get the descriptor
            WikiDescriptor descriptor = wikiDescriptorManager.getById(wikiId);
            // Get the property group
            WikiTemplatePropertyGroup group = (WikiTemplatePropertyGroup) descriptor.getPropertyGroup(
                    WikiTemplatePropertyGroupProvider.GROUP_NAME);
            // Set the value
            group.setTemplate(value);
            // Save the property groups
            templateWikiPropertyGroupProvider.save(group, wikiId);
        } catch (WikiPropertyGroupException e) {
            throw new WikiTemplateManagerException(String.format("Failed to save the property group [%s]",
                    WikiTemplatePropertyGroupProvider.GROUP_NAME), e);
        } catch (WikiManagerException e) {
            throw new WikiTemplateManagerException(String.format("Failed to get the descriptor for [%s].", wikiId), e);
        }
    }

    @Override
    public boolean isTemplate(String wikiId) throws WikiTemplateManagerException
    {
        try {
            // Get the descriptor
            WikiDescriptor descriptor = wikiDescriptorManager.getById(wikiId);
            // Get the property group
            WikiTemplatePropertyGroup group = (WikiTemplatePropertyGroup) descriptor.getPropertyGroup(
                    WikiTemplatePropertyGroupProvider.GROUP_NAME);
            // Return the value
            return group.isTemplate();
        } catch (WikiManagerException e) {
            throw new WikiTemplateManagerException(String.format("Failed to get the descriptor for [%s].", wikiId), e);
        }
    }

    @Override
    public WikiDescriptor createWikiFromTemplate(String newWikiId, String newWikiAlias,
            WikiDescriptor templateDescriptor) throws WikiTemplateManagerException
    {
        try {
            // First, create the new wiki
            WikiDescriptor descriptor = wikiManager.create(newWikiId, newWikiAlias);

            // Then copy the wiki
            WikiCopy wikiCopy = new WikiCopy();
            wikiCopy.copyWiki(templateDescriptor.getId(), newWikiId, "", xcontextProvider.get());

            // Finally, return the descriptor
            return descriptor;
        } catch (WikiManagerException e) {
            throw new WikiTemplateManagerException(e.getMessage(), e);
        } catch (XWikiException e) {
            throw new WikiTemplateManagerException(e.getMessage(), e);
        }
    }
}
