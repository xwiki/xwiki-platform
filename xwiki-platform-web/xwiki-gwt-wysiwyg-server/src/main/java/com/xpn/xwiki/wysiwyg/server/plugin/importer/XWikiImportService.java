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
package com.xpn.xwiki.wysiwyg.server.plugin.importer;

import java.io.StringReader;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.gwt.wysiwyg.client.plugin.importer.ImportService;
import org.xwiki.gwt.wysiwyg.client.wiki.Attachment;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.officeimporter.OfficeImporter;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;
import org.xwiki.xml.html.HTMLUtils;


/**
 * XWiki specific implementation of {@link ImportService}.
 * 
 * @version $Id$
 */
public class XWikiImportService implements ImportService
{
    /**
     * Default XWiki logger to report errors correctly.
     */
    private static final Log LOG = LogFactory.getLog(XWikiImportService.class);

    /**
     * The component used to import office documents.
     */
    @Requirement
    private OfficeImporter officeImporter;

    /**
     * The component used to serialize {@link org.xwiki.model.reference.DocumentReference} instances. This component is
     * needed only because OfficeImporter component uses String instead of
     * {@link org.xwiki.model.reference.DocumentReference}.
     */
    @Requirement
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * The component used to parse attachment references.
     */
    private AttachmentReferenceResolver<String> attachmentReferenceResolver;

    /**
     * The component manager. We need it because we have to access some components dynamically.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     * 
     * @see ImportService#cleanOfficeHTML(String, String, Map)
     */
    public String cleanOfficeHTML(String htmlPaste, String cleanerHint, Map<String, String> cleaningParams)
    {
        try {
            HTMLCleaner cleaner = this.componentManager.lookup(HTMLCleaner.class, cleanerHint);
            HTMLCleanerConfiguration configuration = cleaner.getDefaultConfiguration();
            configuration.setParameters(cleaningParams);
            Document cleanedDocument = cleaner.clean(new StringReader(htmlPaste), configuration);
            HTMLUtils.stripHTMLEnvelope(cleanedDocument);
            return HTMLUtils.toString(cleanedDocument, true, true);
        } catch (Exception e) {
            LOG.error("Exception while cleaning office HTML content.", e);
            throw new RuntimeException(e.getLocalizedMessage());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ImportService#officeToXHTML(Attachment, Map)
     */
    public String officeToXHTML(Attachment attachment, Map<String, String> cleaningParams)
    {
        try {
            AttachmentReference attachmentReference =
                this.attachmentReferenceResolver.resolve(attachment.getReference());
            // OfficeImporter should be improved to use DocumentName instead of String. This will remove the need for a
            // DocumentNameSerializer.
            return officeImporter.importAttachment(this.entityReferenceSerializer.serialize(attachmentReference
                .getDocumentReference()), attachmentReference.getName(), cleaningParams);
        } catch (Exception e) {
            LOG.error("Exception while importing office document.", e);
            throw new RuntimeException(e.getLocalizedMessage());
        }
    }
}
