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
package org.xwiki.store.merge;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.logging.LogLevel;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;

/**
 * Script service allowing to perform merge operations on documents.
 *
 * @version $Id$
 * @since 11.5RC1
 */
@Component
@Named("merge")
@Singleton
@Unstable
public class MergeScriptService implements ScriptService
{
    /**
     * Used to lookup parsers and renderers to discover available syntaxes.
     */
    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Perform a merge 3 points between the currentDocument and the newDocument, the previousDocument being the common
     * ancestor of both documents. This operation doesn't save anything.
     *
     * @param previousDocument the previous version of the document to merge.
     * @param currentDocument the current version of the document to merge.
     * @param newDocument the new version of the document to merge.
     * @return a {@link MergeDocumentResult} object containing the merged document.
     */
    public MergeDocumentResult mergeDocument(Document previousDocument, Document currentDocument,
        Document newDocument)
    {
        MergeConfiguration mergeConfiguration = new MergeConfiguration();
        mergeConfiguration.setProvidedVersionsModifiables(false);

        XWikiDocument previousDoc = previousDocument.getDocument();
        XWikiDocument currentDoc = currentDocument.getDocument();
        XWikiDocument newDoc = newDocument.getDocument();

        XWikiDocument mergeDoc = currentDoc.clone();
        MergeResult mergeResult = mergeDoc.merge(previousDoc, newDoc, mergeConfiguration, contextProvider.get());

        return new MergeDocumentResult(currentDoc, previousDoc, newDoc, mergeDoc,
            !mergeResult.getLog().getLogs(LogLevel.ERROR).isEmpty());
    }
}
