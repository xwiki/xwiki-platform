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
package org.xwiki.rest.resources.pages;

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Translations;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/translations")
public interface PageTranslationsResource
{
    /**
     * Returns the list of translations available for a page.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page whose translations are listed, for example {@code WebHome}
     * @return the default locale of the page together with the list of locales into which it has been translated
     * @throws XWikiRestException if the current user is not allowed to view the page or if the translations cannot be
     *  retrieved
     */
    @GET Translations getTranslations(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName
    ) throws XWikiRestException;
}
