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

import org.xwiki.component.annotation.Role;
import org.xwiki.wiki.descriptor.WikiDescriptor;

/**
 * Component in charge of managing wiki templates and creating new wiki from these templates.
 *
 * @since 5.3M1
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
     * @param wikiDescriptor the wiki to specify
     * @param value whether or not the wiki is a template
     * @throws WikiTemplateManagerException if problems occur
     */
    void setTemplate(WikiDescriptor wikiDescriptor, boolean value) throws WikiTemplateManagerException;

    /**
     * Create a new wiki from the specified template.
     *
     * @param newWikiId ID of the wiki to create
     * @param newWikiAlias Default alias of the wiki to create
     * @param templateDescriptor Descriptor of the template to use
     * @return The descriptor of the new wiki
     * @throws WikiTemplateManagerException if problems occur
     */
    WikiDescriptor createWikiFromTemplate(String newWikiId, String newWikiAlias, WikiDescriptor templateDescriptor)
            throws WikiTemplateManagerException;
}
