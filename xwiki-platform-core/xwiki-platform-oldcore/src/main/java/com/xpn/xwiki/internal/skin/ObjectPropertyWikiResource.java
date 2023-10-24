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
package com.xpn.xwiki.internal.skin;

import java.net.URL;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.inject.Provider;

import org.xwiki.filter.input.StringInputSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.skin.ResourceRepository;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiURLFactory;

/**
 * @version $Id$
 * @since 6.4M1
 */
public class ObjectPropertyWikiResource extends AbstractWikiResource<ObjectPropertyReference, StringInputSource>
{
    private final String content;

    private final Instant instant;

    public ObjectPropertyWikiResource(String path, ResourceRepository repository, ObjectPropertyReference reference,
        DocumentReference authorReference, Provider<XWikiContext> xcontextProvider, String content, Instant instant)
    {
        super(path, path, reference.getName(), repository, reference, authorReference, xcontextProvider);

        this.content = content;
        this.instant = instant;
    }

    @Override
    public StringInputSource getInputSource() throws Exception
    {
        return new StringInputSource(this.content);
    }

    @Override
    public Instant getInstant() throws Exception
    {
        return this.instant;
    }

    @Override
    public String getURL(XWikiDocument document) throws Exception
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put(XWiki.CACHE_VERSION, document.getVersion());

        XWikiURLFactory urlf = xcontext.getURLFactory();

        URL url = urlf.createSkinURL(this.reference.getName(), document.getSpace(), document.getName(),
            document.getDatabase(), xcontext, parameters);
        return urlf.getURL(url, xcontext);
    }
}
