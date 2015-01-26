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

import org.xwiki.wiki.properties.WikiPropertyGroup;

/**
 * Property groups attached to wiki descriptors that store values about the wiki template feature.
 *
 * @version $Id $
 * @since 5.3M2
 */
public class WikiTemplatePropertyGroup extends WikiPropertyGroup
{
    /**
     * Name if the "is template" property.
     */
    public static final String IS_TEMPLATE_PROPERTY = "isTemplate";

    /**
     * Constructor.
     *
     * @param id Unique identifier of the group
     */
    public WikiTemplatePropertyGroup(String id)
    {
        super(id);
        // Default value
        setTemplate(false);
    }

    /**
     * Helper to know if the wiki that hold this property group is a template or not.
     * @return whether or not the wiki is a template
     */
    public boolean isTemplate()
    {
        return (Boolean) this.get(IS_TEMPLATE_PROPERTY);
    }

    /**
     * Helper to set if the wiki that hold this property group is a template or not.
     * @param template whether or not the wiki should be a template
     */
    public void setTemplate(boolean template)
    {
        this.set(IS_TEMPLATE_PROPERTY, template);
    }

}
