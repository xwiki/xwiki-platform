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

import java.nio.file.Path;
import java.util.Properties;

import org.apache.solr.common.SolrException;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.NodeConfig;
import org.apache.solr.core.SolrXmlConfig;

/**
 * Extends {@link CoreContainer} to workaround some limitations.
 * <ul>
 * <li>https://issues.apache.org/jira/browse/SOLR-14561?focusedCommentId=17280845#comment-17280845</li>
 * </ul>
 * 
 * @version $Id$
 * @since 13.1RC1
 */
public class XWikiCoreContainer extends CoreContainer
{
    /**
     * Create a new CoreContainer using the given SolrResourceLoader, configuration and CoresLocator. The container's
     * cores are not loaded.
     *
     * @param config a ConfigSolr representation of this container's configuration
     * @see #load()
     */
    public XWikiCoreContainer(NodeConfig config)
    {
        super(config);
    }

    /**
     * Create a new CoreContainer and load its cores.
     *
     * @param solrHome the solr home directory
     * @return a loaded CoreContainer
     */
    public static CoreContainer createAndLoad(Path solrHome)
    {
        return createAndLoad(solrHome, solrHome.resolve(SolrXmlConfig.SOLR_XML_FILE));
    }

    /**
     * Create a new CoreContainer and load its cores.
     *
     * @param solrHome the solr home directory
     * @param configFile the file containing this container's configuration
     * @return a loaded CoreContainer
     */
    public static CoreContainer createAndLoad(Path solrHome, Path configFile)
    {
        CoreContainer cc = new XWikiCoreContainer(SolrXmlConfig.fromFile(solrHome, configFile, new Properties()));
        try {
            cc.load();
        } catch (Exception e) {
            cc.shutdown();
            throw e;
        }

        return cc;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Disable path protection because we need {@code ../} prefixed paths.
     * 
     * @see org.apache.solr.core.CoreContainer#assertPathAllowed(java.nio.file.Path)
     */
    @Override
    public void assertPathAllowed(Path pathToAssert) throws SolrException
    {
        // Disable path protection because we need {@code ../} prefixed paths.
        // See https://issues.apache.org/jira/browse/SOLR-14561?focusedCommentId=17280845#comment-17280845
    }
}
