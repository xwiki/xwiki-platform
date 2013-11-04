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

import org.xwiki.wiki.provisioning.WikiProvisionerRequest;

/**
 * Request for the template provisioner.
 *
 * @since 5.3M2
 * @version $Id$
 */
public class TemplateWikiProvisionerRequest extends WikiProvisionerRequest
{
    /**
     * Name of the property that stores the Id of the template to use.
     */
    public static final String PROPERTY_TEMPLATE_ID = "wikiprovisioner.templateId";

    /**
     * Constructor.
     * @param wikiId id of the wiki to provide
     * @param templateId id of the template to use
     */
    public TemplateWikiProvisionerRequest(String wikiId, String templateId)
    {
        super(wikiId);
    }

    /**
     * @param templateId the id of the template to use
     */
    public void setTemplateId(String templateId)
    {
        setProperty(PROPERTY_TEMPLATE_ID, templateId);
    }

    /**
     * @return the id of the template to use
     */
    public String getTemplateId()
    {
        return getProperty(PROPERTY_TEMPLATE_ID);
    }
}
