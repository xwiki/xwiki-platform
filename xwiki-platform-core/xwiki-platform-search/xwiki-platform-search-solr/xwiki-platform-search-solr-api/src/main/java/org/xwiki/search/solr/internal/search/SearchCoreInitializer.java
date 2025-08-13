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
package org.xwiki.search.solr.internal.search;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.search.solr.SolrCoreInitializer;
import org.xwiki.search.solr.SolrException;
import org.xwiki.search.solr.XWikiSolrCore;

/**
 * Take care of the initialization of the search core which can be done through the SolrJ API.
 * 
 * @version $Id$
 * @since 17.8.0RC1
 */
@Component
@Named(SearchCoreInitializer.CORE_NAME)
@Singleton
public class SearchCoreInitializer implements SolrCoreInitializer
{
    /**
     * The name of the core containing the XWiki search index.
     */
    public static final String CORE_NAME = "search";

    @Inject
    private SearchCoreMigrationManager migrations;

    @Override
    public String getCoreName()
    {
        return CORE_NAME;
    }

    @Override
    public void initialize(XWikiSolrCore core) throws SolrException
    {
        this.migrations.update(core);
    }
}
