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

import java.util.HashMap;
import java.util.Map;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;

/**
 * API for {@link SyndEntrySource}
 */
public class SyndEntrySourceApi extends Api
{
    public static final String SYND_ENTRY_SOURCE_EXCEPTION = "SyndEntrySourceException";

    private SyndEntrySource source;

    public SyndEntrySourceApi(SyndEntrySource source, XWikiContext context)
    {
        super(context);
        this.source = source;
    }

    protected SyndEntrySource getSyndEntrySource()
    {
        return this.source;
    }

    /**
     * @see SyndEntrySource#source(SyndEntry, Object, java.util.Map, XWikiContext)
     */
    public boolean source(SyndEntry entry, Object obj, Map<String, Object> params)
    {
        getXWikiContext().remove(SYND_ENTRY_SOURCE_EXCEPTION);
        try {
            this.source.source(entry, obj, params, getXWikiContext());
            return true;
        } catch (XWikiException e) {
            getXWikiContext().put(SYND_ENTRY_SOURCE_EXCEPTION, e);
            return false;
        }
    }

    /**
     * @see SyndEntrySource#source(SyndEntry, Object, java.util.Map, XWikiContext)
     */
    public boolean source(SyndEntry entry, Object obj)
    {
        return this.source(entry, obj, new HashMap<String, Object>());
    }

    /**
     * @see SyndEntrySource#source(SyndEntry, Object, java.util.Map, XWikiContext)
     */
    public SyndEntry source(Object obj, Map<String, Object> params)
    {
        getXWikiContext().remove(SYND_ENTRY_SOURCE_EXCEPTION);
        try {
            SyndEntry entry = new SyndEntryImpl();
            this.source.source(entry, obj, params, getXWikiContext());
            return entry;
        } catch (XWikiException e) {
            getXWikiContext().put(SYND_ENTRY_SOURCE_EXCEPTION, e);
            return null;
        }
    }

    /**
     * @see SyndEntrySource#source(SyndEntry, Object, java.util.Map, XWikiContext)
     */
    public SyndEntry source(Object obj)
    {
        return this.source(obj, new HashMap<String, Object>());
    }
}
