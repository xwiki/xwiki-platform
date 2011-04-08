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
package org.xwiki.gwt.wysiwyg.client.plugin.separator;

import org.xwiki.gwt.wysiwyg.client.plugin.UIExtension;

/**
 * Abstract separator.
 * 
 * @version $Id$
 */
public abstract class AbstractSeparator implements UIExtension
{
    /**
     * The user interface extension point.
     */
    private final String role;

    /**
     * Creates a new separator for the given extension point.
     * 
     * @param role the name of the extension point for which the new separator is created.
     */
    public AbstractSeparator(String role)
    {
        this.role = role;
    }

    /**
     * {@inheritDoc}
     * 
     * @see UIExtension#getRole()
     */
    public String getRole()
    {
        return role;
    }

    /**
     * {@inheritDoc}
     * 
     * @see UIExtension#isEnabled(String)
     */
    public boolean isEnabled(String feature)
    {
        // always disabled
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see UIExtension#setEnabled(String, boolean)
     */
    public void setEnabled(String feature, boolean enabled)
    {
        // ignore
    }
}
