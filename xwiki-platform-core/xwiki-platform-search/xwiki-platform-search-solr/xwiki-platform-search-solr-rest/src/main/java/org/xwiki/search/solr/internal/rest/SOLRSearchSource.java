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
package org.xwiki.search.solr.internal.rest;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.rest.Relations;
import org.xwiki.rest.internal.Utils;
import org.xwiki.rest.internal.resources.search.AbstractSearchSource;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.SearchResult;
import org.xwiki.rest.model.jaxb.SearchResults;
import org.xwiki.rest.resources.attachments.AttachmentResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.resources.pages.PageTranslationResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.XWiki;

/**
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Named("solr")
@Singleton
public class SOLRSearchSource extends AbstractSearchSource
{
    @Inject
    protected Provider<XWikiContext> xcontextProvider;

    @Override
    public List<SearchResult> search(String query, String defaultWikiName, String wikis, boolean hasProgrammingRights,
        String orderField, String order, boolean distinct, int number, int start, Boolean withPrettyNames,
        String className, UriInfo uriInfo) throws Exception
    {
        XWikiContext xwikiContext = this.xcontextProvider.get();
        XWiki xwikiApi = new XWiki(xwikiContext.getWiki(), xwikiContext);

        List<SearchResult> result = new ArrayList<SearchResult>();

        if (query == null) {
            return result;
        }

        /*
         * One of the two must be non-null. If default wiki name is non-null and wikis is null, then it's a local search
         * in a specific wiki. If wiki name is null and wikis is non-null it's a global query on different wikis. If
         * both of them are non-null then the wikis parameter takes the precedence.
         */
        if (defaultWikiName == null && wikis == null) {
            return result;
        }

        if (!hasProgrammingRights) {
            query += " AND NOT space:XWiki AND NOT space:Admin AND NOT space:Panels AND NOT name:WebPreferences";
        }

        try {
            // TODO: search on SOLR
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Error performing lucene search", e);
        }

        return result;
    }
}
