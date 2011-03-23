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
package org.xwiki.wysiwyg.server.internal.plugin.sync;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.gwt.wysiwyg.client.converter.HTMLConverter;
import org.xwiki.gwt.wysiwyg.client.diff.Revision;
import org.xwiki.gwt.wysiwyg.client.plugin.sync.SyncResult;
import org.xwiki.gwt.wysiwyg.client.plugin.sync.SyncService;
import org.xwiki.gwt.wysiwyg.client.plugin.sync.SyncStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.wysiwyg.server.plugin.sync.SyncEngine;



/**
 * XWiki specific implementation of {@link SyncService}.
 * 
 * @version $Id$
 */
public class XWikiSyncService implements SyncService
{
    /**
     * Default XWiki logger to report errors correctly.
     */
    private static final Log LOG = LogFactory.getLog(XWikiSyncService.class);

    /**
     * The object used to synchronize the content edited by multiple users when the real time feature of the editor is
     * activated.
     */
    @Requirement
    private SyncEngine syncEngine;

    /**
     * The component used to push the edited document on the context before converting its content to XHTML.
     */
    @Requirement
    private DocumentAccessBridge docAccessBridge;

    /**
     * The component used to parse document references.
     */
    @Requirement
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * The component used to convert the content of the edited page from source syntax to XHTML.
     */
    @Requirement
    private HTMLConverter htmlConverter;

    /**
     * {@inheritDoc}
     * 
     * @see SyncService#syncEditorContent(Revision, String, int, boolean)
     */
    public synchronized SyncResult syncEditorContent(Revision revision, String pageName, int version, boolean syncReset)
    {
        try {
            SyncStatus syncStatus = syncEngine.getSyncStatus(pageName);
            if (syncStatus == null || syncReset) {
                DocumentReference docRef = documentReferenceResolver.resolve(pageName);
                DocumentModelBridge docModelBridge = docAccessBridge.getDocument(docRef);
                syncStatus = new SyncStatus(pageName, docModelBridge.getVersion(), getRenderedContent(docModelBridge));
                syncEngine.setSyncStatus(pageName, syncStatus);
            } else {
                // TODO: We need to check the version versus the one that was initially loaded. If the version is
                // different then we should handle this.
            }
            return syncEngine.sync(syncStatus, revision, version);
        } catch (Exception e) {
            LOG.error("Exception while synchronizing edited content.", e);
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * @param docModelBridge the document whose content should be rendered
     * @return the rendered content of the specified document
     * @throws Exception if the conversion from source syntax to XHTML fails
     */
    private String getRenderedContent(DocumentModelBridge docModelBridge) throws Exception
    {
        // Push the document on the context before rendering its content.
        Map<String, Object> backupObjects = new HashMap<String, Object>();
        try {
            docAccessBridge.pushDocumentInContext(backupObjects, docModelBridge.getDocumentReference());
            return htmlConverter.toHTML(docModelBridge.getContent(), docModelBridge.getSyntaxId());
        } finally {
            // Restore the context after the conversion.
            docAccessBridge.popDocumentFromContext(backupObjects);
        }
    }
}
