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
 *
 */
package com.xpn.xwiki.internal.observation.remote.converter;

import java.io.Serializable;
import java.util.Map;

import org.xwiki.bridge.DocumentName;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.observation.remote.converter.AbstractEventConverter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.LazyXWikiDocument;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Provide some serialization tools for old apis like {@link XWikiDocument} and {@link XWikiContext}.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
public abstract class AbstractXWikiEventConverter extends AbstractEventConverter
{
    private static final String CONTEXT_WIKI = "contextwiki";

    private static final String CONTEXT_USER = "contextuser";

    private static final String DOC_NAME = "docname";

    private static final String DOC_VERSION = "docversion";

    private static final String DOC_LANGUAGE = "doclanguage";

    private static final String ORIGDOC_VERSION = "origdocversion";

    private static final String ORIGDOC_LANGUAGE = "origdoclanguage";

    /**
     * Used to set some proper context informations.
     */
    @Requirement
    private Execution execution;

    protected void serializeXWikiContext(XWikiContext context, Map<String, Serializable> remoteData)
    {
        remoteData.put(CONTEXT_WIKI, context.getDatabase());
        remoteData.put(CONTEXT_USER, context.getUser());
    }

    protected XWikiContext unserializeXWikiContext(Map<String, Serializable> remoteData)
    {
        XWikiContext context = (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
        context.setDatabase((String) remoteData.get(CONTEXT_WIKI));
        context.setUser((String) remoteData.get(CONTEXT_USER));

        return context;
    }

    protected void serializeXWikiDocument(XWikiDocument document, Map<String, Serializable> remoteData)
    {
        remoteData.put(DOC_NAME, new DocumentName(document.getWikiName(), document.getSpaceName(),
            document.getPageName()));

        if (!document.isNew()) {
            remoteData.put(DOC_VERSION, document.getVersion());
            remoteData.put(DOC_LANGUAGE, document.getLanguage());
        }

        XWikiDocument originalDocument = document.getOriginalDocument();

        if (!originalDocument.isNew()) {
            remoteData.put(ORIGDOC_VERSION, originalDocument.getVersion());
            remoteData.put(ORIGDOC_LANGUAGE, originalDocument.getLanguage());
        }
    }

    protected XWikiDocument unserializeDocument(Map<String, Serializable> remoteData)
    {
        DocumentName docName = (DocumentName) remoteData.get(DOC_NAME);

        XWikiDocument doc;
        if (remoteData.get(DOC_VERSION) == null) {
            doc = new XWikiDocument(docName.getWiki(), docName.getSpace(), docName.getPage());
        } else {
            doc = new LazyXWikiDocument();
            doc.setDatabase(docName.getWiki());
            doc.setSpace(docName.getSpace());
            doc.setName(docName.getPage());
            doc.setLanguage((String) remoteData.get(DOC_LANGUAGE));
            doc.setVersion((String) remoteData.get(DOC_VERSION));
        }

        XWikiDocument origDoc;
        if (remoteData.get(ORIGDOC_VERSION) == null) {
            origDoc = new XWikiDocument(docName.getWiki(), docName.getSpace(), docName.getPage());
        } else {
            origDoc = new LazyXWikiDocument();
            origDoc.setDatabase(docName.getWiki());
            origDoc.setSpace(docName.getSpace());
            origDoc.setName(docName.getPage());
            origDoc.setLanguage((String) remoteData.get(ORIGDOC_LANGUAGE));
            origDoc.setVersion((String) remoteData.get(ORIGDOC_VERSION));
        }

        doc.setOriginalDocument(origDoc);

        return doc;
    }
}
