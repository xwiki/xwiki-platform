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
package org.xwiki.whatsnew;

import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Create a News Source instance. Examples of possible news sources from where to get news about XWiki:
 * <ul>
 *   <li>XWiki Blog</li>
 *   <li>Discourse Forum</li>
 * </ul>
 *
 * Example usage: {@code List<NewsSourceItem> items = sourceFactory.create(...).withRange(...).forCategories(...).build
 * ();}
 *
 * @version $Id$
 * @since 15.1RC1
 */
@Role
@Unstable
public interface NewsSourceFactory
{
    /**
     * @param parameters the source-dependent list of parameters to configure the source
     * @return the News source instance
     * @throws NewsException when there's a problem creating the news source (e.g. not specific RSS URL for XWiki Blog
     *         source type)
     */
    NewsSource create(Map<String, String> parameters) throws NewsException;
}
