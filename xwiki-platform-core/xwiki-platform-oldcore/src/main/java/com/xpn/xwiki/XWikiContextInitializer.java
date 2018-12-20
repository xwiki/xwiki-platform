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
package com.xpn.xwiki;

import org.xwiki.component.annotation.Role;
import org.xwiki.context.ExecutionContext;

/**
 * Initialize a {@link XWikiContext} with various properties.
 * 
 * @version $Id$
 * @since 9.11RC1
 */
@Role
public interface XWikiContextInitializer
{
    /**
     * Indicate that the current user should be authenticated.
     * 
     * @return this
     */
    XWikiContextInitializer authenticate();

    /**
     * Fallback on a stub XWikiContext if there is any issue.
     * 
     * @return this
     */
    XWikiContextInitializer fallbackOnStub();
    
    /**
     * @param econtext the {@link ExecutionContext} to inject the {@link XWikiContext} in, or null
     * @return an initialized {@link XWikiContext}
     * @throws XWikiException when failing to initialize the XWiki context
     */
    XWikiContext initialize(ExecutionContext econtext) throws XWikiException;
}
