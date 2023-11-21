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
package com.xpn.xwiki.redirection;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Allows to perform a redirection according to the context.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@Role
public interface RedirectionFilter
{
    /**
     * Possibly perform a redirection according to the context.
     *
     * @param context the XWiki context
     * @return {@code true} if a redirection has been performed, {@code false} otherwise
     * @throws XWikiException in case of error during the analysis of the context
     */
    boolean redirect(XWikiContext context) throws XWikiException;
}
