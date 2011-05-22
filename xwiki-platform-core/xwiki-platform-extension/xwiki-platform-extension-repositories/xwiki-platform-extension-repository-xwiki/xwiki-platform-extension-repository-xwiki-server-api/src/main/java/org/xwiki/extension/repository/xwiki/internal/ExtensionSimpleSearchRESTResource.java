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

package org.xwiki.extension.repository.xwiki.internal;

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.xwiki.model.jaxb.Extension;
import org.xwiki.extension.repository.xwiki.model.jaxb.Extensions;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.rest.Utils;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 * @since 3.1M2
 */
@Component("org.xwiki.extension.repository.xwiki.internal.ExtensionSimpleSearchRESTResource")
@Path("/extensions/search/simple/{pattern}")
public class ExtensionSimpleSearchRESTResource extends AbstractExtensionRESTResource
{
    @GET
    public Extensions search(@PathParam("pattern") String pattern,
        @QueryParam("offset") @DefaultValue("0") Integer offset, @QueryParam("number") @DefaultValue("-1") int number)
        throws XWikiException, QueryException
    {
        String queryStr =
            "from doc.object(XWiki.ExtensionClass) as extension where extension.id like :pattern"
                + " or extension.name like :pattern" + " or extension.description like :pattern";

        Query query = this.queryManager.createQuery(queryStr, Query.XWQL);

        query.bindValue("pattern", '%' + pattern + '%');
        query.setOffset(offset);
        if (number > 0) {
            query.setLimit(number);
        }

        List<String> documentNames = query.execute();

        Extensions extensions = this.objectFactory.createExtensions();
        List<Extension> extensionsList = extensions.getExtensions();
        for (String documentName : documentNames) {
            extensionsList
                .add(createExtension(Utils.getXWikiApi(this.componentManager).getDocument(documentName), null));
        }

        return extensions;
    }
}
