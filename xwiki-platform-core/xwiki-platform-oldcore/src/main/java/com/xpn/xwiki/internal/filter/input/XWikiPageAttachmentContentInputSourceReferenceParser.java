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
package com.xpn.xwiki.internal.filter.input;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.InputSource;
import org.xwiki.filter.input.InputSourceReferenceParser;
import org.xwiki.model.reference.PageAttachmentReference;
import org.xwiki.model.reference.PageAttachmentReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Add support for {@link InputSource} reference parser with prefix "pageAttach:".
 * 
 * @version $Id$
 * @since 13.10.5
 * @since 14.3RC1
 */
@Component
@Singleton
@Named("pageAttach")
public class XWikiPageAttachmentContentInputSourceReferenceParser implements InputSourceReferenceParser
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("current")
    private PageAttachmentReferenceResolver<String> resolver;

    @Override
    public InputSource parse(String reference) throws FilterException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext != null && xcontext.getWiki() != null) {
            PageAttachmentReference attachmentReference = this.resolver.resolve(reference);

            XWikiDocument document;
            try {
                document = xcontext.getWiki().getDocument(attachmentReference.getPageReference(), xcontext);
            } catch (XWikiException e) {
                throw new FilterException(
                    "Failed to get attachment document for input source reference [" + reference + ']', e);
            }

            XWikiAttachment attachment = document.getAttachment(attachmentReference.getName());

            try {
                return new XWikiAttachmentContentInputSource(attachment.getAttachmentContent(xcontext));
            } catch (XWikiException e) {
                throw new FilterException(
                    "Failed to get attachment content for input source reference [" + reference + ']', e);
            }
        }

        throw new FilterException("Failed to get attachment for input source reference [" + reference + ']');
    }
}
