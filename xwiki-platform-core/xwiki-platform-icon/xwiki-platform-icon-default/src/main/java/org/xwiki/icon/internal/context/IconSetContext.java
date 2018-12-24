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
package org.xwiki.icon.internal.context;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.icon.IconSet;

/**
 * Control context icon set.
 * 
 * @version $Id$
 * @since 10.11RC1
 */
@Component(roles = IconSetContext.class)
@Singleton
public class IconSetContext
{
    /**
     * Name of the entry containing the icon set.
     */
    private static final String EPROPERTY_ICON = "icon.set";

    @Inject
    private Execution execution;

    /**
     * @return the icon set stored in the context
     */
    public IconSet getIconSet()
    {
        ExecutionContext econtext = this.execution.getContext();

        if (econtext != null) {
            return (IconSet) econtext.getProperty(EPROPERTY_ICON);
        }

        return null;
    }

    /**
     * @param isonSet the icon set stored in the context
     */
    public void setIconSet(IconSet isonSet)
    {
        ExecutionContext econtext = this.execution.getContext();

        if (econtext != null) {
            if (isonSet != null) {
                econtext.setProperty(EPROPERTY_ICON, isonSet);
            } else {
                econtext.removeProperty(EPROPERTY_ICON);
            }
        }
    }
}
