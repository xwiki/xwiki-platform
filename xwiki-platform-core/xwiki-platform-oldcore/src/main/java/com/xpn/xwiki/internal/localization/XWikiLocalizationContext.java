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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.localization.LocalizationContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;

/**
 * Default implementation of {@link LocalizationContext}.
 *
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Singleton
public class XWikiLocalizationContext implements LocalizationContext
{
    /**
     * Used to access the configured locale.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public Locale getCurrentLocale()
    {
        Locale currentLocale = Locale.getDefault();

        XWikiContext xcontext = this.xcontextProvider.get();
        if (xcontext != null) {
            XWiki xwiki = xcontext.getWiki();
            if (xwiki != null) {
                String locale = xwiki.getLanguagePreference(xcontext);
                if (locale != null) {
                    currentLocale = LocaleUtils.toLocale(locale);
                }
            }
        }

        return currentLocale;
    }
}
