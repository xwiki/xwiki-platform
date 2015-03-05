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
package org.xwiki.wiki.template;

import java.util.Collection;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.provisioning.WikiProvisioningJob;

/**
 * Component in charge of managing wiki templates and creating new wiki from these templates.
 *
 * @version $Id$
 * @since 5.3M2
 */
@Role
public interface WikiTemplateManager
{
    /**
     * Get the list of all wiki templates.
     *
     * @return list of wiki templates
     * @throws WikiTemplateManagerException if problems occur
     */
    Collection<WikiDescriptor> getTemplates() throws WikiTemplateManagerException;

    /**
     * Set if the specified wiki is a template or not.
     *
     * @param wikiId the ID of the wiki to specify
     * @param value whether or not the wiki is a template
     * @throws WikiTemplateManagerException if problems occur
     */
    void setTemplate(String wikiId, boolean value) throws WikiTemplateManagerException;

    /**
     * @param wikiId The id of the wiki to test
     * @return if the wiki is a template or not
     * @throws WikiTemplateManagerException if problems occur
     */
    boolean isTemplate(String wikiId) throws WikiTemplateManagerException;

    /**
     * Create a new wiki from the specified template.
     *
     * @param newWikiId ID of the wiki to create
     * @param newWikiAlias Default alias of the wiki to create
     * @param templateId Id of the template to use
     * @param ownerId Id of the wiki owner
     * @param failOnExist fail if the wiki id is not available
     * @return the job that provisions the new wiki with the template content
     * @throws WikiTemplateManagerException if problems occur
     */
    WikiProvisioningJob createWikiFromTemplate(String newWikiId, String newWikiAlias, String templateId, String ownerId,
        boolean failOnExist) throws WikiTemplateManagerException;

    /**
     * Apply a template to an existing wiki.
     *
     * @param wikiId if of the wiki on which the template will be installed
     * @param templateId id of the template to use
     * @return the job that provisions the wiki with the template content
     * @throws WikiTemplateManagerException if problems occur
     *
     * @since 5.4RC1
     */
    WikiProvisioningJob applyTemplate(String wikiId, String templateId) throws WikiTemplateManagerException;

    /**
     * Get the wiki provisioning job.
     *
     * @param jobId id of wiki provisioning job
     * @return the job
     * @throws WikiTemplateManagerException if problem occurs
     */
    WikiProvisioningJob getWikiProvisioningJob(List<String> jobId) throws WikiTemplateManagerException;
}
