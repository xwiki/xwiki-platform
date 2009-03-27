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
package org.xwiki.rendering.macro.descriptor;

/**
 * The default implementation of {@link ContentDescriptor}.
 * 
 * @version $Id$
 * @since 1.9M1
 */
public class DefaultContentDescriptor implements ContentDescriptor
{
    /**
     * The description of the content.
     */
    private String description;

    /**
     * Indicate if the content is mandatory.
     */
    private boolean mandatory = true;

    /**
     * Default constructor.
     */
    public DefaultContentDescriptor()
    {
    }

    /**
     * @param description the description of the content.
     */
    public DefaultContentDescriptor(String description)
    {
        this.description = description;
    }

    /**
     * @param mandatory indicate if the content is mandatory.
     */
    public DefaultContentDescriptor(boolean mandatory)
    {
        this.mandatory = mandatory;
    }

    /**
     * @param description the description of the content.
     * @param mandatory indicate if the content is mandatory.
     */
    public DefaultContentDescriptor(String description, boolean mandatory)
    {
        this.description = description;
        this.mandatory = mandatory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.ContentDescriptor#getDescription()
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.ContentDescriptor#isMandatory()
     */
    public boolean isMandatory()
    {
        return this.mandatory;
    }
}
