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
package org.xwiki.platform.wiki.creationjob.internal.steps;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.platform.wiki.creationjob.WikiCreationRequest;
import org.xwiki.platform.wiki.creationjob.WikiCreationStep;
import org.xwiki.platform.wiki.creationjob.WikiCreationException;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.template.WikiTemplateManager;
import org.xwiki.wiki.template.WikiTemplateManagerException;
import org.xwiki.wiki.user.WikiUserManager;
import org.xwiki.wiki.user.WikiUserManagerException;

/**
 * Component that save the metadata of the wiki (pretty name, description, etc...) as well as the configuration (user
 * scope, membership type, etc...).
 *
 * @version $Id$
 * @since 7.0M2
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named("metadata")
public class SaveWikiMetaDataStep implements WikiCreationStep
{
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private WikiTemplateManager wikiTemplateManager;

    @Inject
    private WikiUserManager wikiUserManager;

    @Override
    public void execute(WikiCreationRequest request) throws WikiCreationException
    {
        try {
            String wikiId = request.getWikiId();
            // Meta data about the wiki
            WikiDescriptor descriptor = wikiDescriptorManager.getById(wikiId);
            descriptor.setDescription(request.getDescription());
            descriptor.setPrettyName(request.getPrettyName());
            descriptor.setOwnerId(request.getOwnerId());
            wikiDescriptorManager.saveDescriptor(descriptor);

            // Meta data about the templates
            wikiTemplateManager.setTemplate(wikiId, request.isTemplate());

            // Meta data about the users
            wikiUserManager.setUserScope(wikiId, request.getUserScope());
            wikiUserManager.setMembershipType(wikiId, request.getMembershipType());

        } catch (WikiManagerException | WikiTemplateManagerException | WikiUserManagerException e) {
            throw new WikiCreationException(
                String.format("Failed to set metadata to the wiki [%s].", request.getWikiId()), e);
        }
    }

    @Override
    public int getOrder()
    {
        return 2000;
    }
}
