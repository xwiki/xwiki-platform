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
package com.xpn.xwiki.web;

import javax.inject.Inject;
import javax.inject.Named;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.redirection.RedirectionFilter;

/**
 * Add a backward compatibility layer to {@link XWikiAction}.
 * 
 * @version $Id$
 */
public privileged aspect XWikiActionCompatibilityAspect
{
    @Inject
    @Named("XWiki.RedirectClass")
    private RedirectionFilter XWikiAction.redirectionClassFilter;
    
    // handleRedirectObject and handleRedirectObject while previously {@code protected} are now {@code public} due of 
    // technical limitations of AspectJ.
    // See https://doanduyhai.wordpress.com/2011/12/12/advanced-aspectj-part-ii-inter-type-declaration/
    // "If their is one thing to remember from access modifier, it’s that their semantic applies with respect to the
    // declaring aspect, and not to the target."
    // They must still be used as if {@code protected}.
    /**
     * Indicate if the XWiki.RedirectClass is handled by the action (see handleRedirectObject()).
     * 
     * @deprecated since 14.0RC1, see {@link XWikiAction#supportRedirections()}
     */
    @Deprecated
    public boolean XWikiAction.handleRedirectObject = false;

    /**
     * Redirect the user to an other location if the document holds an XWiki.RedirectClass instance (used when a
     * document is moved).
     *
     * @param context the XWiki context
     * @return either or not a redirection have been sent
     * @throws XWikiException if error occurs
     * @since 8.0RC1
     * @since 7.4.2
     * 
     * @deprecated since 14.0RC1, not used anymore
     */
    @Deprecated
    public boolean XWikiAction.handleRedirectObject(XWikiContext context) throws XWikiException
    {
       return this.redirectionClassFilter.redirect(context);
    }
}
