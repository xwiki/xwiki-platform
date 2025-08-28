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
package com.xpn.xwiki.doc;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.dom4j.Document;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.Utils;

/**
 * Read only lazy loading document.
 * <p>
 * The following informations are taken into account:
 * <ul>
 * <li>document reference: the absolute reference of the document</li>
 * <li>document language: if provided the proper language version of the document is loaded. If not the default one is
 * loaded.</li>
 * <li>document version: if provided the proper version of the document is loaded. Also make extra sure to bypass cache
 * storage for remote observation use case.</li>
 * </ul>
 * <p>
 * originalDocument remain the property of {@link LazyXWikiDocument} object and is not taken from the lazy loaded
 * document since it depends on how this {@link XWikiDocument} object is used (its technical meaning is its state in the
 * database before any modification but in the case of observation it's used as the previous version of the document).
 * TODO: we should probably think about a separation of theses two notions in something more clear, something for the
 * new model.
 *
 * @version $Id$
 * @since 2.0M4
 */
public class LazyXWikiDocument extends XWikiDocument
{
    /**
     * The real document.
     */
    private XWikiDocument document;

    /**
     * @deprecated use {@link #LazyXWikiDocument(DocumentReference)} instead
     */
    @Deprecated
    public LazyXWikiDocument()
    {

    }

    public LazyXWikiDocument(DocumentReference documentReference)
    {
        super(documentReference);
    }

    /**
     * Load and return the document in the database.
     *
     * @return the real document
     */
    private XWikiDocument getDocument()
    {
        if (this.document == null) {
            // get context
            XWikiContext context =
                (XWikiContext) Utils.getComponent(Execution.class).getContext().getProperty("xwikicontext");

            XWikiDocument doc = new XWikiDocument(getDocumentReference(), getLocale());

            String currentWiki = context.getWikiId();
            try {
                // Put context in document wiki
                context.setWikiId(getDocumentReference().getWikiReference().getName());

                if (this.version == null) {
                    this.document = context.getWiki().getDocument(doc, context);
                } else {
                    // Force bypassing the cache to make extra sure we get the last version of the document. This is
                    // safer for example when LazyXWikiDocument is used in the context of remote events. This is for
                    // properly emulate events, XWikiCacheStore is taking care itself of invalidating itself.
                    doc = context.getWiki().getNotCacheStore().loadXWikiDoc(doc, context);
                    if (doc.getRCSVersion().equals(this.version)) {
                        // It's the last version of the document
                        this.document = doc;
                    } else {
                        // It's not the last version of the document, ask versioning store.
                        try {
                            this.document =
                                context.getWiki().getVersioningStore()
                                    .loadXWikiDoc(doc, this.version.toString(), context);
                        } catch (XWikiException e) {
                            // If the proper can't be found, return the last version of the document
                            this.document = doc;
                        }
                    }
                }
            } catch (XWikiException e) {
                throw new RuntimeException("Failed to get document [" + this + "]", e);
            } finally {
                context.setWikiId(currentWiki);
            }
        }

        return this.document;
    }

    @Override
    public Version getRCSVersion()
    {
        if (this.version == null) {
            return getDocument().getRCSVersion();
        } else {
            return this.version;
        }
    }

    @Override
    public String getContent()
    {
        return getDocument().getContent();
    }

    @Override
    public Map<DocumentReference, List<BaseObject>> getXObjects()
    {
        return getDocument().getXObjects();
    }

    @Override
    public BaseClass getXClass()
    {
        return getDocument().getXClass();
    }

    @Override
    public String getXClassXML()
    {
        return getDocument().getXClassXML();
    }

    @Override
    public DocumentReference getAuthorReference()
    {
        return getDocument().getAuthorReference();
    }

    @Override
    public DocumentReference getContentAuthorReference()
    {
        return getDocument().getContentAuthorReference();
    }

    @Override
    public DocumentReference getCreatorReference()
    {
        return super.getCreatorReference();
    }

    @Override
    public Date getDate()
    {
        return getDocument().getDate();
    }

    @Override
    public Date getCreationDate()
    {
        return getDocument().getCreationDate();
    }

    @Override
    public Date getContentUpdateDate()
    {
        return getDocument().getContentUpdateDate();
    }

    @Override
    public String getMeta()
    {
        return getDocument().getMeta();
    }

    @Override
    public String getTitle()
    {
        return getDocument().getTitle();
    }

    @Override
    public String getFormat()
    {
        return getDocument().getFormat();
    }

    @Override
    public Locale getDefaultLocale()
    {
        return getDocument().getDefaultLocale();
    }

    @Override
    public int getTranslation()
    {
        return getDocument().getTranslation();
    }

    @Override
    public String getCustomClass()
    {
        return getDocument().getCustomClass();
    }

    @Override
    public EntityReference getRelativeParentReference()
    {
        return getDocument().getRelativeParentReference();
    }

    @Override
    public DocumentReference getParentReference()
    {
        return getDocument().getParentReference();
    }

    @Override
    public int getElements()
    {
        return getDocument().getElements();
    }

    @Override
    public String getDefaultTemplate()
    {
        return getDocument().getDefaultTemplate();
    }

    @Override
    public String getValidationScript()
    {
        return getDocument().getValidationScript();
    }

    @Override
    public String getComment()
    {
        return getDocument().getComment();
    }

    @Override
    public Syntax getSyntax()
    {
        return getDocument().getSyntax();
    }

    @Override
    public Boolean isHidden()
    {
        return getDocument().isHidden();
    }

    @Override
    public boolean isEnforceRequiredRights()
    {
        return getDocument().isEnforceRequiredRights();
    }

    @Override
    public XWikiDocumentArchive getDocumentArchive()
    {
        return getDocument().getDocumentArchive();
    }

    @Override
    public void loadArchive(XWikiContext context) throws XWikiException
    {
        getDocument().loadArchive(context);
    }

    @Override
    public XWikiDocumentArchive getDocumentArchive(XWikiContext context) throws XWikiException
    {
        return getDocument().getDocumentArchive(context);
    }

    @Override
    public XWikiStoreInterface getStore()
    {
        return getDocument().getStore();
    }

    @Override
    public long getId()
    {
        return getDocument().getId();
    }

    @Override
    public XWikiStoreInterface getStore(XWikiContext context)
    {
        return getDocument().getStore(context);
    }

    @Override
    public XDOM getXDOM()
    {
        return getDocument().getXDOM();
    }

    @Override
    public XDOM getPreparedXDOM()
    {
        return getDocument().getPreparedXDOM();
    }

    @Override
    public String getTags(XWikiContext context)
    {
        return getDocument().getTags(context);
    }

    @Override
    public List<String> getTagsPossibleValues(XWikiContext context)
    {
        return getDocument().getTagsPossibleValues(context);
    }

    @Override
    public boolean isFromCache()
    {
        return getDocument().isFromCache();
    }

    @Override
    public boolean isMostRecent()
    {
        return getDocument().isMostRecent();
    }

    @Override
    public Document toXMLDocument(boolean bWithObjects, boolean bWithRendering, boolean bWithAttachmentContent,
        boolean bWithVersions, XWikiContext context) throws XWikiException
    {
        return getDocument()
            .toXMLDocument(bWithObjects, bWithRendering, bWithAttachmentContent, bWithVersions, context);
    }

    @Override
    public Object getWikiNode()
    {
        return getDocument().getWikiNode();
    }
}
