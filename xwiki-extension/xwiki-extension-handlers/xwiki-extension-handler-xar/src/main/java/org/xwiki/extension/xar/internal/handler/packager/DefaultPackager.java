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
package org.xwiki.extension.xar.internal.handler.packager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.io.input.CloseShieldInputStream;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.extension.xar.internal.handler.packager.xml.DocumentImporterHandler;
import org.xwiki.extension.xar.internal.handler.packager.xml.DocumentReferenceHandler;
import org.xwiki.extension.xar.internal.handler.packager.xml.RootHandler;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

@Component
public class DefaultPackager extends AbstractLogEnabled implements Packager, Initializable
{
    @Requirement
    private ComponentManager componentManager;

    @Requirement
    private Execution execution;

    @Requirement("explicit/reference")
    private DocumentReferenceResolver<EntityReference> resolver;

    private SAXParserFactory parserFactory;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.parserFactory = SAXParserFactory.newInstance();
    }

    public void importXAR(File xarFile, String wiki) throws IOException
    {
        FileInputStream fis = new FileInputStream(xarFile);
        ZipInputStream zis = new ZipInputStream(fis);

        try {
            for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                try {
                    SAXParser saxParser = this.parserFactory.newSAXParser();
                    XMLReader xmlReader = saxParser.getXMLReader();

                    RootHandler handler = new RootHandler(this.componentManager);
                    DocumentImporterHandler documentHandler = new DocumentImporterHandler(this.componentManager);
                    documentHandler.setWiki(wiki);
                    handler.setHandler("xwikidoc", documentHandler);

                    xmlReader.setContentHandler(handler);

                    xmlReader.parse(new InputSource(new CloseShieldInputStream(zis)));
                } catch (Exception e) {
                    getLogger().error("Failed to parse document [" + entry.getName() + "]", e);
                }
            }
        } finally {
            zis.close();
        }
    }

    public void unimportXAR(File xarFile, String wiki) throws IOException
    {
        WikiReference wikiReference = new WikiReference(wiki);

        XWikiContext xcontext = getXWikiContext();
        for (EntityReference entityReference : getDocumentReferences(xarFile)) {
            DocumentReference documentReference = this.resolver.resolve(entityReference, wikiReference);
            try {
                XWikiDocument document = getXWikiContext().getWiki().getDocument(documentReference, xcontext);

                if (!document.isNew()) {
                    getXWikiContext().getWiki().deleteDocument(document, xcontext);
                }
            } catch (XWikiException e) {
                getLogger().error("Failed to delete document [" + documentReference + "]", e);
            }
        }
    }

    public List<EntityReference> getDocumentReferences(File xarFile) throws IOException
    {
        List<EntityReference> documents = null;

        FileInputStream fis = new FileInputStream(xarFile);
        ZipInputStream zis = new ZipInputStream(fis);

        try {
            for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                try {
                    SAXParser saxParser = this.parserFactory.newSAXParser();
                    XMLReader xmlReader = saxParser.getXMLReader();

                    RootHandler handler = new RootHandler(this.componentManager);
                    DocumentReferenceHandler documentHandler = new DocumentReferenceHandler(this.componentManager);
                    handler.setHandler("xwikidoc", documentHandler);
                    xmlReader.setContentHandler(handler);

                    xmlReader.parse(new InputSource(new CloseShieldInputStream(zis)));

                    if (documents == null) {
                        documents = new ArrayList<EntityReference>();
                    }

                    documents.add(documentHandler.getDocumentReference());
                } catch (Exception e) {
                    getLogger().error("Failed to parse document [" + entry.getName() + "]", e);
                }
            }
        } finally {
            zis.close();
        }

        return documents != null ? documents : Collections.<EntityReference> emptyList();
    }

    private ExecutionContext getExecutionContext()
    {
        return this.execution.getContext();
    }

    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) getExecutionContext().getProperty("xwikicontext");
    }
}
