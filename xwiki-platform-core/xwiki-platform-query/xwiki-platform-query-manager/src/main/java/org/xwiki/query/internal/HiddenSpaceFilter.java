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
package org.xwiki.query.internal;

import javax.inject.Named;

import org.xwiki.component.annotation.Component;

/**
 * Query filter excluding 'hidden' spaces from a {@link org.xwiki.query.Query}. Hidden spaces should not be
 * returned in public search results or appear in the User Interface in general.
 * <p>
 * The filter assume the <code>XWikiSpace</code> table has a <code>space</code> alias.
 *
 * @version $Id$
 * @since 7.2M2
 */
@Component
@Named("hidden/space")
public class HiddenSpaceFilter extends AbstractHiddenFilter
{
    @Override
    protected boolean isFilterable(String statement)
    {
        // This could be replaced by the following regex: "xwikispace(\\s)+(as)?(\\s)+space"
        return statement.indexOf("xwikispace as space") > -1 || statement.indexOf("xwikispace space") > -1;
    }

    @Override
    protected String filterHidden(String statement, String language)
    {
        return insertWhereClause("space.hidden <> true", statement, language);
    }
}
