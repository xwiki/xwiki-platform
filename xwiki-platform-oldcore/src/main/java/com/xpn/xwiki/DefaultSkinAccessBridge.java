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
 *
 */

package com.xpn.xwiki;

import org.xwiki.bridge.SkinAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;

/**
 * {@inheritDoc}
 * @see org.xwiki.bridge.SkinAccessBridge
 * @version $Id$ 
 * @since 1.7
 */
@Component
public class DefaultSkinAccessBridge implements SkinAccessBridge
{
    /** Execution context handler, needed for accessing the XWikiContext. */
    @Requirement
    private Execution execution;

    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }
    
    /**
     * {@inheritDoc}
     * @see org.xwiki.bridge.SkinAccessBridge#getSkinFile(String fileName)
     */
    public String getSkinFile(String fileName)
    {
        XWikiContext xcontext = getContext();
        XWiki xwiki = xcontext.getWiki();
        return xwiki.getSkinFile(fileName, xcontext);
    }

    /**
     * {@inheritDoc}
     * @see org.xwiki.bridge.SkinAccessBridge#getIconURL(String)
     * @since 2.6M1
     */
    public String getIconURL(String iconName)
    {
        XWikiContext xcontext = getContext();
        XWiki xwiki = xcontext.getWiki();
        return xwiki.getIconURL(iconName, xcontext);
    }
}
