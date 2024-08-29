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
package com.xpn.xwiki.internal.localization;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Provider;

import org.xwiki.localization.internal.DefaultLocalizationManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Extends {@link DefaultLocalizationManager} with oldcore specific logic.
 * 
 * @version $Id$
 * @since 14.1CR1
 * @since 13.10.3
 */
public class XWikiLocalizationManager extends DefaultLocalizationManager
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public Locale getDefaultLocale()
    {
        Locale defaultLocale = super.getDefaultLocale();

        XWikiContext xcontext = this.xcontextProvider.get();
        if (xcontext != null) {
            XWiki xwiki = xcontext.getWiki();
            if (xwiki != null) {
                defaultLocale = xwiki.getDefaultLocale(xcontext);
            }
        }

        return defaultLocale;
    }
}
