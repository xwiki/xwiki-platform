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
package org.xwiki.whatsnew.internal;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.xwiki.whatsnew.NewsCategory;
import org.xwiki.whatsnew.NewsContent;
import org.xwiki.whatsnew.NewsSourceItem;

/**
 * Default implementation of a News source item, for all sources.
 *
 * @version $Id$
 * @since 15.1RC1
 */
public class DefaultNewsSourceItem implements NewsSourceItem
{
    private Optional<String> title;

    private Optional<NewsContent> description;

    private Set<NewsCategory> categories = new HashSet<>();

    private Optional<Date> publishedDate;

    private Optional<String> author;

    private Optional<String> originURL;

    private Optional<String> imageURL;

    @Override
    public Optional<String> getTitle()
    {
        return this.title;
    }

    /**
     * @param title see {@link #getTitle()}
     */
    public void setTitle(Optional<String> title)
    {
        this.title = title;
    }

    @Override
    public Optional<NewsContent> getDescription()
    {
        return this.description;
    }

    /**
     * @param description see {@link #getDescription()}
     */
    public void setDescription(Optional<NewsContent> description)
    {
        this.description = description;
    }

    @Override
    public Set<NewsCategory> getCategories()
    {
        return this.categories;
    }

    /**
     * @param categories see {@link #getCategories()}
     */
    public void setCategories(Set<NewsCategory> categories)
    {
        this.categories = Collections.unmodifiableSet(categories);
    }

    @Override
    public Optional<Date> getPublishedDate()
    {
        return this.publishedDate;
    }

    /**
     * @param publishedDate see {@link #getPublishedDate()}
     */
    public void setPublishedDate(Optional<Date> publishedDate)
    {
        this.publishedDate = publishedDate;
    }

    @Override
    public Optional<String> getAuthor()
    {
        return this.author;
    }

    /**
     * @param author see {@link #getAuthor()}
     */
    public void setAuthor(Optional<String> author)
    {
        this.author = author;
    }

    @Override
    public Optional<String> getOriginURL()
    {
        return this.originURL;
    }

    /**
     * @param originURL see {@link #getOriginURL()}
     */
    public void setOriginURL(Optional<String> originURL)
    {
        this.originURL = originURL;
    }

    @Override
    public Optional<String> getImageURL()
    {
        return this.imageURL;
    }

    /**
     * @param imageURL see {@link #getImageURL()} ()}
     */
    public void setImageURL(Optional<String> imageURL)
    {
        this.imageURL = imageURL;
    }

    @Override
    public int compareTo(NewsSourceItem other)
    {
        // We sort descending, i.e. the first item is the newest of the two.
        return other.getPublishedDate().orElse(new Date()).compareTo(getPublishedDate().orElse(new Date()));
    }
}
