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
package org.xwiki.extension.xar.internal.doc;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.internal.converter.ExtensionIdConverter;
import org.xwiki.extension.xar.internal.handler.packager.Packager;
import org.xwiki.filter.FilterException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.xar.XarException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.doc.AbstractDocumentRevisionProvider;

/**
 * Get document revision from an installed XAR extension.
 * 
 * @version $Id$
 * @since 9.3RC1
 */
@Component
@Named("xar")
@Singleton
public class XarDocumentRevisionProvider extends AbstractDocumentRevisionProvider
{
    @Inject
    private Packager packager;

    @Override
    public XWikiDocument getRevision(DocumentReference reference, String revision) throws XWikiException
    {
        ExtensionId extensionId = ExtensionIdConverter.toExtensionId(revision, null);

        try {
            return this.packager.getXWikiDocument(reference, extensionId);
        } catch (FilterException | IOException | ComponentLookupException | XarException e) {
            throw new XWikiException("Failed to load extension document [" + reference + "] in [" + revision + "]", e);
        }
    }
}
