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
package com.xpn.xwiki.internal.observation.remote.converter;

import java.io.Serializable;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.observation.remote.converter.AbstractEventConverter;
import org.xwiki.observation.remote.converter.DocumentRemoteEventConverter;
import org.xwiki.observation.remote.converter.RemoteEventConverterException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Provide some serialization tools for old apis like {@link XWikiDocument} and {@link XWikiContext}.
 *
 * @version $Id$
 * @since 2.0M4
 */
public abstract class AbstractXWikiEventConverter extends AbstractEventConverter
{
    /**
     * The logger to log.
     */
    @Inject
    protected Logger logger;

    @Inject
    private DocumentRemoteEventConverter documentRemoteEventConverter;

    /**
     * @param context the XWiki context to serialize
     * @return the serialized version of the context
     */
    protected Serializable serializeXWikiContext(XWikiContext context)
    {
        return this.documentRemoteEventConverter.serializeXWikiContext(context);
    }

    /**
     * @param remoteData the serialized version of the context
     * @return the XWiki context
     */
    protected XWikiContext unserializeXWikiContext(Serializable remoteData)
    {
        try {
            return this.documentRemoteEventConverter.unserializeXWikiContext(remoteData);
        } catch (RemoteEventConverterException e) {
            this.logger.warn(e.getMessage(), e);
            return null;
        }
    }

    /**
     * @param document the document to serialize
     * @return the serialized version of the document
     */
    protected Serializable serializeXWikiDocument(XWikiDocument document)
    {
        return this.documentRemoteEventConverter.serializeDocument(document);
    }

    /**
     * @param remoteData the serialized version of the document
     * @return the document
     * @throws XWikiException when failing to unserialize document
     */
    protected XWikiDocument unserializeDocument(Serializable remoteData) throws XWikiException
    {
        try {
            return (XWikiDocument) this.documentRemoteEventConverter.unserializeDocument(remoteData);
        } catch (RemoteEventConverterException e) {
            if (e.getCause() instanceof XWikiException) {
                throw (XWikiException) e.getCause();
            } else  {
                throw new XWikiException(String.format("Error to unserialize document from [%s]", remoteData), e);
            }
        }
    }

    protected XWikiDocument unserializeDeletedDocument(Serializable remoteData, XWikiContext context)
    {
        try {
            return (XWikiDocument) this.documentRemoteEventConverter.unserializeDeletedDocument(remoteData, context);
        } catch (RemoteEventConverterException e) {
            this.logger.error(e.getMessage(), e);
            return null;
        }
    }
}
