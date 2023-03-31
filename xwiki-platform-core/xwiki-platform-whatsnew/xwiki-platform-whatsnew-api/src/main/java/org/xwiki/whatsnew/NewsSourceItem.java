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

import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.xwiki.stability.Unstable;

/**
 * Data for a source item.
 *
 * @version $Id$
 * @since 15.1RC1
 */
@Unstable
public interface NewsSourceItem extends Comparable<NewsSourceItem>
{
    /**
     * @return the news item title
     */
    Optional<String> getTitle();

    /**
     * @return the news item content (cleaned and safe to be rendered)
     */
    Optional<NewsContent> getDescription();

    /**
     * @return the news item categories
     */
    Set<NewsCategory> getCategories();

    /**
     * @return the news item publication date
     */
    Optional<Date> getPublishedDate();

    /**
     * @return the news item author
     */
    Optional<String> getAuthor();

    /**
     * @return the news item origin URL
     */
    Optional<String> getOriginURL();

    /**
     * @return the URL to an image associated with the news item
     * @since 15.3RC1
     */
    Optional<String> getImageURL();
}
