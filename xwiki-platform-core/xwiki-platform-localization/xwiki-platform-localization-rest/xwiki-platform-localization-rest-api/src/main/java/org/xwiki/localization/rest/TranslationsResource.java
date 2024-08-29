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
package org.xwiki.localization.rest;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.localization.rest.model.jaxb.Translations;

/**
 * Exposes the wiki translations through REST.
 *
 * @version $Id$
 * @since 13.3RC1
 */
@Path("/wikis/{wikiName}/localization/translations")
public interface TranslationsResource
{
    /**
     * Returns the raw source of the requested translation keys. In other words, the translation values without the
     * parameters resolved.
     * <p>
     * The payload of a successful response is a {@link Translations} object containing a collection of {@link
     * org.xwiki.localization.rest.model.jaxb.Translation}. Each translation is composed of a translation key (with the
     * prefix concatenated), and the resolved translation source for the requested locale. If the translation key is not
     * found, the raw source is {@code null}.
     *
     * @param wikiName the name of the wiki holding the translation keys
     * @param locale the locale of the translations, when {@code null} the current locale of the user is used
     * @param prefix a common prefix, concatenated to each translation key before resolving their sources (can be
     *     {@code null} in which case nothing is concatenated)
     * @param keys the translation keys to resolve. If no key is passed, and empty object is returned
     * @return the list of resolved translations
     */
    @GET
    Translations getTranslations(@PathParam("wikiName") String wikiName, @QueryParam("locale") String locale,
        @QueryParam("prefix") String prefix, @QueryParam("key") List<String> keys);
}
