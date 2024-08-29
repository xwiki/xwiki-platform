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
package org.xwiki.whatsnew.internal.xwikiblog;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.whatsnew.NewsException;
import org.xwiki.whatsnew.NewsSource;
import org.xwiki.whatsnew.NewsSourceFactory;

/**
 * The factory that returns a XWiki Blog source.
 *
 * @version $Id$
 * @since 15.1RC1
 */
@Component
@Named("xwikiblog")
@Singleton
public class XWikiBlogNewsSourceFactory implements NewsSourceFactory
{
    @Inject
    private RSSContentCleaner rssContentCleaner;

    @Override
    public NewsSource create(Map<String, String> parameters) throws NewsException
    {
        String rssURL = parameters.get("rssURL");
        if (rssURL == null) {
            throw new NewsException("Failed to create a XWiki Blog news source. A 'rssURL' parameter must be passed");
        }
        return new XWikiBlogNewsSource(rssURL, this.rssContentCleaner);
    }
}
