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
package org.xwiki.search.solr.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.search.solr.Solr;
import org.xwiki.search.solr.SolrException;

/**
 * A wrapper around the new {@link Solr} API for the search core.
 * 
 * @version $Id$
 * @since 12.2
 */
@Component
@Singleton
public class SolrClientInstance extends AbstractSolrInstance
{
    /**
     * The name of the core containing the XWiki search index.
     */
    public static final String CORE_NAME = "search";

    @Inject
    private Solr solr;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.server = this.solr.getCore(CORE_NAME);
        } catch (SolrException e) {
            throw new InitializationException("Failed to create the solr client for core [search]", e);
        }

        if (this.server == null) {
            throw new InitializationException("No core with name [" + CORE_NAME + "] could be found");
        }
    }
}
