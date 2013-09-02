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
package com.xpn.xwiki.internal.query;

import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.query.internal.AbstractWhereQueryFilter;

import com.xpn.xwiki.XWikiContext;

/**
 * Query filter returning only documents in the current language.
 *
 * @version $Id$
 * @since 5.1M2
 */
@Component
@Named("currentlanguage")
@Singleton
public class CurrentLanguageQueryFilter extends AbstractWhereQueryFilter
{
    /**
     * Used to get the current language.
     */
    @Inject
    private Execution execution;

    @Override
    public String filterStatement(String statement, String language)
    {
        // Note that we cannot have SQL injection here since getCurrentLanguage() will always return a valid Locale
        return insertWhereClause(String.format("(doc.language is null or doc.language = '' or doc.language = '%s')",
            getCurrentLanguage().toString()), statement, language);
    }

    @Override
    public List filterResults(List results)
    {
        return results;
    }

    /**
     * @return the current language
     */
    private Locale getCurrentLanguage()
    {
        XWikiContext context = getXWikiContext();
        return context.getWiki().getDefaultLocale(context);
    }

    /**
     * @return the XWiki Context object from which to extract the current language
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);
    }
}
