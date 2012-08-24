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
package com.xpn.xwiki.util;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiContext;

/**
 * Tool to make easier to generate stub XWikiContext. It's supposed to be initialized once with the first request and it
 * can be called to get a stub context generated from this initial XWikiContext.
 * <p>
 * The reason to initialize it based on first request is to get some informations we could not know otherwise like a
 * default scheme/host/port.
 * 
 * @version $Id$
 */
@Role
public interface XWikiStubContextProvider
{
    /**
     * Initialize a stub context from a real context.
     * <p>
     * We create initial stub context from a real XWikiContext to have a stub as complete as possible. Like getting the
     * proper host/port/scheme, the engine context etc.
     * 
     * @param context a real XWikiContext
     */
    void initialize(XWikiContext context);

    /**
     * @return a usable XWikiContext
     */
    XWikiContext createStubContext();
}
