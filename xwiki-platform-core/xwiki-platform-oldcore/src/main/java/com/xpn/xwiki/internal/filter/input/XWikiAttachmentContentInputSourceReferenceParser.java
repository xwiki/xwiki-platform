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
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Add support for {@link InputSource} reference parser with prefix "attach:".
 * 
 * @version $Id$
 * @since 13.4RC1
 */
@Component
@Singleton
@Named("attach")
public class XWikiAttachmentContentInputSourceReferenceParser implements InputSourceReferenceParser
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("current")
    private AttachmentReferenceResolver<String> resolver;

    @Override
    public InputSource parse(String reference) throws FilterException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        if (xcontext != null && xcontext.getWiki() != null) {
            AttachmentReference attachmentReference = this.resolver.resolve(reference);

            XWikiDocument document;
            try {
                document = xcontext.getWiki().getDocument(attachmentReference.getDocumentReference(), xcontext);
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
