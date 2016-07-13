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

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.resources.BaseAttachmentsResource;
import org.xwiki.rest.model.jaxb.Attachments;
import org.xwiki.rest.resources.wikis.WikiAttachmentsResource;

/**
 * @version $Id$
 */
@Component
@Named("org.xwiki.rest.internal.resources.wikis.WikiAttachmentsResourceImpl")
public class WikiAttachmentsResourceImpl extends BaseAttachmentsResource implements WikiAttachmentsResource
{
    @Override
    public Attachments getAttachments(String wikiName, String name, String page, String space, String author,
            String types, Integer start, Integer number, Boolean withPrettyNames) throws XWikiRestException
    {
        return super.getAttachments(wikiName, name, page, space, author, types, start, number, withPrettyNames);
    }
}
