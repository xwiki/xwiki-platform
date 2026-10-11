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
package org.xwiki.rest.resources.wikis;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Wikis;

/**
 * @version $Id$
 */
@Path("/wikis")
public interface WikisResource
{
    /**
     * Returns the wikis of the current instance that are visible to the current user.
     *
     * @return the wikis the current user may access, that is those they have view rights on, plus those that accept
     *  global users without requiring an invitation, plus those they hold a pending invitation to; the result also
     *  carries a {@code query} link pointing to the cross-wiki query resource
     * @throws XWikiRestException if the list of wikis cannot be retrieved
     */
    @GET Wikis getWikis() throws XWikiRestException;
}
