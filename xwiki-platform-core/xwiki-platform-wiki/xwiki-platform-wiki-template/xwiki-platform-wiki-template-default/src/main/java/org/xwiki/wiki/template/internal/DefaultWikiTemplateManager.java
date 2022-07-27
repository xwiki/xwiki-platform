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
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.properties.WikiPropertyGroupException;
import org.xwiki.wiki.properties.WikiPropertyGroupProvider;
import org.xwiki.wiki.provisioning.WikiProvisioningJob;
import org.xwiki.wiki.provisioning.WikiProvisioningJobException;
import org.xwiki.wiki.provisioning.WikiProvisioningJobExecutor;
import org.xwiki.wiki.template.WikiTemplateManager;
import org.xwiki.wiki.template.WikiTemplateManagerException;
import org.xwiki.wiki.template.WikiTemplatePropertyGroup;

import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation for {@link WikiTemplateManager}.
 *
 * @version $Id$
 * @since 5.3M2
 */
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

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    private WikiProvisioningJobExecutor wikiProvisionerExecutor;

    @Inject
    @Named("template")
    private WikiPropertyGroupProvider templateWikiPropertyGroupProvider;

    private String errorMessageNoDescriptor = "Failed to get the descriptor for [%s].";

    @Override
    public Collection<WikiDescriptor> getTemplates() throws WikiTemplateManagerException
    {
        List<WikiDescriptor> result = new ArrayList<WikiDescriptor>();

        try {
            Query query = this.queryManager.createQuery(
                    "from doc.object(WikiManager.WikiTemplateClass) as descriptor where doc.name like 'XWikiServer%' "
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
            if (group != null) {
                // Set the value
                group.setTemplate(value);
                // Save the property groups
                templateWikiPropertyGroupProvider.save(group, wikiId);
            }
        } catch (WikiPropertyGroupException e) {
            throw new WikiTemplateManagerException(String.format("Failed to save the property group [%s]",
                    WikiTemplatePropertyGroupProvider.GROUP_NAME), e);
        } catch (WikiManagerException e) {
            throw new WikiTemplateManagerException(String.format(errorMessageNoDescriptor, wikiId), e);
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
            throw new WikiTemplateManagerException(String.format(errorMessageNoDescriptor, wikiId), e);
        }
    }

    @Override
    @Deprecated
    public WikiProvisioningJob createWikiFromTemplate(String newWikiId, String newWikiAlias, String templateId,
        String ownerId, boolean failOnExist) throws WikiTemplateManagerException
    {
        try {
            // Create the new wiki
            wikiManager.create(newWikiId, newWikiAlias, ownerId, failOnExist);

            // Apply the template to the new wiki
            return applyTemplate(newWikiId, templateId);
        } catch (WikiManagerException e) {
            throw new WikiTemplateManagerException(e.getMessage(), e);
        }
    }

    @Override
    @Deprecated
    public WikiProvisioningJob applyTemplate(String wikiId, String templateId) throws WikiTemplateManagerException
    {
        try {
            return wikiProvisionerExecutor.createAndExecuteJob(wikiId, TemplateWikiProvisioningJob.JOBTYPE, templateId);
        } catch (WikiProvisioningJobException e) {
            throw new WikiTemplateManagerException(e.getMessage(), e);
        }
    }

    @Override
    @Deprecated
    public WikiProvisioningJob getWikiProvisioningJob(List<String> jobId) throws WikiTemplateManagerException
    {
        try {
            return wikiProvisionerExecutor.getJob(jobId);
        } catch (WikiProvisioningJobException e) {
            throw new WikiTemplateManagerException(e.getMessage(), e);
        }
    }
}
