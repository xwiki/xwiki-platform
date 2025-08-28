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
package com.xpn.xwiki.plugin.feed;

import java.util.Map;

import com.rometools.rome.feed.synd.SyndEntry;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Abstracts a strategy for computing the field values of a feed entry from a generic source.
 */
public interface SyndEntrySource
{
    /**
     * Overwrites the current values of the given feed entry with new ones computed from the specified source object.
     * 
     * @param entry the feed entry whose fields are going to be overwritten
     * @param obj the source for the new values to be set on the fields of the feed entry
     * @param params parameters to adjust the computation. Each concrete strategy may define its own (key, value) pairs
     * @param context the XWiki context
     * @throws XWikiException
     */
    void source(SyndEntry entry, Object obj, Map<String, Object> params, XWikiContext context) throws XWikiException;
}
