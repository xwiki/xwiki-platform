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
package org.xwiki.rest.internal.resources.wikis;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.filter.input.DefaultInputStreamInputSource;
import org.xwiki.filter.input.InputSource;
import org.xwiki.internal.filter.Importer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.model.jaxb.Wiki;
import org.xwiki.rest.resources.wikis.WikiResource;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;

/**
 * Resource for interacting with a specific wiki.
 *
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.wikis.WikiResourceImpl")
public class WikiResourceImpl extends XWikiResource implements WikiResource
{
    @Inject
    private Importer importer;

    @Inject
    private WikiDescriptorManager wikis;

    @Override
    public Wiki get(String wikiName) throws XWikiRestException
    {
        try {
            if (this.wikis.exists(wikiName)) {
                return DomainObjectFactory.createWiki(this.objectFactory, this.uriInfo.getBaseUri(), wikiName);
            }

            throw new WebApplicationException(Response.Status.NOT_FOUND);
        } catch (WikiManagerException e) {
            throw new XWikiRestException(e);
        }
    }

    @Override
    public Wiki importXAR(String wikiName, Boolean backup, String historyStrategy, InputStream is)
        throws XWikiRestException
    {
        try {
            if (!this.wikis.exists(wikiName)) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            /* Use the package plugin for importing pages */
            XWikiContext xwikiContext = getXWikiContext();

            WikiReference currentWiki = xwikiContext.getWikiReference();

            try {
                xwikiContext.setWikiId(wikiName);

                // Define the data source to import
                InputSource source = new DefaultInputStreamInputSource(is, false);

                // Execute the import
                this.importer.importXAR(source, null, historyStrategy, backup == Boolean.TRUE, getXWikiContext());
            } catch (Exception e) {
                throw new WebApplicationException("Failed to import the XAR package", e);
            } finally {
                xwikiContext.setWikiReference(currentWiki);
            }

            return DomainObjectFactory.createWiki(this.objectFactory, this.uriInfo.getBaseUri(), wikiName);
        } catch (WikiManagerException e) {
            throw new XWikiRestException(e);
        }
    }
}
