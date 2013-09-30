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
package org.xwiki.wikistream.instance.internal.output;

import java.io.InputStream;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.filter.FilterEventParameters;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.wikistream.WikiStreamException;
import org.xwiki.wikistream.instance.internal.XWikiDocumentFilter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 * @since 5.2
 */
@Component
@Named("documents")
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DocumentOutputInstanceWikiStream extends AbstractBeanOutputInstanceWikiStream<DocumentOutputProperties>
    implements XWikiDocumentFilter
{
    @Inject
    private DocumentReferenceResolver<EntityReference> entityResolver;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    private EntityReference currentEntityReference;

    private Locale currentLocale;

    private String currentVersion;

    private XWikiDocument currentDocument;

    @Override
    public void beginWiki(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentEntityReference = new EntityReference(name, EntityType.WIKI, this.currentEntityReference);
    }

    @Override
    public void endWiki(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentEntityReference = this.currentEntityReference.getParent();
    }

    @Override
    public void beginWikiSpace(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentEntityReference = new EntityReference(name, EntityType.SPACE, this.currentEntityReference);
    }

    @Override
    public void endWikiSpace(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentEntityReference = this.currentEntityReference.getParent();
    }

    @Override
    public void beginWikiDocument(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentEntityReference = new EntityReference(name, EntityType.DOCUMENT, this.currentEntityReference);

        this.currentDocument =
            new XWikiDocument(this.entityResolver.resolve(this.currentEntityReference,
                this.properties.getDefaultReference()));
    }

    @Override
    public void endWikiDocument(String name, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentEntityReference = this.currentEntityReference.getParent();
        this.currentDocument = null;
    }

    @Override
    public void beginWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentLocale = locale;
        this.currentDocument.setLocale(this.currentLocale);
    }

    @Override
    public void endWikiDocumentLocale(Locale locale, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentLocale = null;
    }

    @Override
    public void beginWikiDocumentRevision(String version, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentVersion = version;

        if (this.properties.isPreserveVersion()) {
            this.currentDocument.setVersion(this.currentVersion);
        }
    }

    @Override
    public void endWikiDocumentRevision(String version, FilterEventParameters parameters) throws WikiStreamException
    {
        this.currentVersion = null;

        // Save document
        XWikiContext xcontext = this.xcontextProvider.get();

        try {
            XWikiDocument document =
                xcontext.getWiki().getDocument(this.currentDocument.getDocumentReference(), xcontext);

            if (document.isNew()) {
                document = this.currentDocument;
            } else {
                if (!document.getLocale().equals(this.currentDocument.getLocale())) {
                    XWikiDocument localeDocument = document.getTranslatedDocument(this.currentLocale, xcontext);

                    if (localeDocument == document) {
                        document = this.currentDocument;
                    } else {
                        document = localeDocument;
                    }
                }
            }

            if (document != this.currentDocument) {
                document.apply(this.currentDocument);
                if (this.properties.isPreserveVersion()) {
                    document.setVersion(this.currentVersion);
                    document.setMetaDataDirty(false);
                }
            } else {
                if (!this.properties.isPreserveVersion()) {
                    this.currentDocument.setRCSVersion(null);
                } else {
                    document.setMetaDataDirty(false);
                }
            }

            saveDocument(document);
        } catch (Exception e) {
            throw new WikiStreamException("Failed to save document", e);
        }
    }

    private void saveDocument(XWikiDocument document) throws XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        if (this.properties.isAuthorSet()) {
            document.setAuthorReference(this.properties.getAuthor());
            document.setContentAuthorReference(this.properties.getAuthor());
        }

        xcontext.getWiki().saveDocument(document, this.properties.getSaveComment(), xcontext);
    }

    @Override
    public void onWikiAttachment(String name, InputStream content, Long size, FilterEventParameters parameters)
        throws WikiStreamException
    {

    }

    @Override
    public void beginWikiClass(FilterEventParameters parameters) throws WikiStreamException
    {

    }

    @Override
    public void endWikiClass(FilterEventParameters parameters) throws WikiStreamException
    {

    }

    @Override
    public void beginWikiClassProperty(String name, String type, FilterEventParameters parameters)
        throws WikiStreamException
    {

    }

    @Override
    public void endWikiClassProperty(String name, String type, FilterEventParameters parameters)
        throws WikiStreamException
    {

    }

    @Override
    public void onWikiClassPropertyField(String name, String value, FilterEventParameters parameters)
        throws WikiStreamException
    {

    }

    @Override
    public void beginWikiObject(String name, FilterEventParameters parameters) throws WikiStreamException
    {

    }

    @Override
    public void endWikiObject(String name, FilterEventParameters parameters) throws WikiStreamException
    {

    }

    @Override
    public void onWikiObjectProperty(String name, String value, FilterEventParameters parameters)
        throws WikiStreamException
    {

    }
}
