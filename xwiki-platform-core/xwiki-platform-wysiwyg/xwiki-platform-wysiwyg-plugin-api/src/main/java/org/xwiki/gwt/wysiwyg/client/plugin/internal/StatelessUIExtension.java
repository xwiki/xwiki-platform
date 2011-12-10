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
package org.xwiki.gwt.wysiwyg.client.plugin.internal;

/**
 * An user interface extension that cannot be enabled or disabled.
 * 
 * @version $Id$
 */
public class StatelessUIExtension extends AbstractUIExtension
{
    /**
     * Creates a new state less UI extension.
     * 
     * @param role the name of the extension point where the newly created UI extension fits
     */
    public StatelessUIExtension(String role)
    {
        super(role);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractUIExtension#isEnabled(String)
     */
    public boolean isEnabled(String feature)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractUIExtension#setEnabled(String, boolean)
     */
    public void setEnabled(String feature, boolean enabled)
    {
        // ignore
    }
}
