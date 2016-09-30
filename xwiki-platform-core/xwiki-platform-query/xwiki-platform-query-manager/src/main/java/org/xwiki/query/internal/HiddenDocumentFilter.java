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

import org.xwiki.component.annotation.Component;

/**
 * Query filter excluding 'hidden' documents from a {@link org.xwiki.query.Query}. Hidden documents should not be
 * returned in public search results or appear in the User Interface in general.
 * <p>
 * The filter assume the <code>XWikiDocument</code> table has a <code>space</code> alias.
 * <p>
 * Starting with 7.2M2, the 'hidden' hint is deprecated in favor of the 'hidden/document' one.
 *
 * @version $Id$
 * @since 4.0RC1
 */
@Component(hints = {"hidden", HiddenDocumentFilter.HINT})
public class HiddenDocumentFilter extends AbstractHiddenFilter
{
    /**
     * The role hint of that component.
     */
    public static final String HINT = "hidden/document";

    @Override
    protected String filterHidden(String statement, String language)
    {
        return insertWhereClause("(doc.hidden <> true or doc.hidden is null)", statement, language);
    }
}
