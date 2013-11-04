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
package org.xwiki.wiki.provisioning;

import org.xwiki.job.AbstractRequest;

/**
 * Base class for {@link org.xwiki.job.Request} implementations used by wiki provisioners.
 *
 * @since 5.3M2
 * @version $Id$
 */
public class WikiProvisionerRequest extends AbstractRequest
{
    /**
     * Name of the property that stores the od of the wiki to provision.
     */
    public static final String PROPERTY_WIKI_ID = "wikiprovisioner.wikiId";

    /**
     * Constructor.
     * @param wikiId id of the wiki to provision
     */
    public WikiProvisionerRequest(String wikiId)
    {
        super();
        setWikiId(wikiId);
    }

    /**
     * @param wikiId if of the wiki to provision
     */
    public void setWikiId(String wikiId)
    {
        setProperty(PROPERTY_WIKI_ID, wikiId);
    }

    /**
     * @return the id of the wiki to provision
     */
    public String getWikiId()
    {
        return getProperty(PROPERTY_WIKI_ID);
    }

}
