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
package com.xpn.xwiki.api;

import com.xpn.xwiki.api.Context;
import com.xpn.xwiki.util.Util;

/**
 * Add a backward compatibility layer to the {@link Context} class.
 * 
 * @version $Id$
 */
public privileged aspect ContextCompatibilityAspect
{
    /**
     * @return true it's main wiki's context, false otherwise.
     * @deprecated replaced by {@link Context#isMainWiki()} since 1.4M1.
     */
    @Deprecated
    public boolean Context.isVirtual()
    {
        return !this.isMainWiki();
    }

    /**
     * Returns an instance of the {@link com.xpn.xwiki.util.Util} class.
     * 
     * @return an instance of the {@link com.xpn.xwiki.util.Util} class
     * @see Util
     * @deprecated since 2.6M1 the functions provided by Util are internal, please do not use them.
     */
    @Deprecated
    public Util Context.getUtil()
    {
        return this.context.getUtil();
    }

    /**
     * Return the current uix object. This method is defined to allow to access this object without programming rights.
     *
     * @return the current uix object
     *
     * @since 14.1RC1
     * @since 13.10.3
     * @deprecated since 14.1RC1, use the "uix" key from the velocity context instead in the UIX templates
     */
    @Deprecated
    public java.lang.Object Context.getUix()
    {
        return getXWikiContext().get("uix");
    }
}
