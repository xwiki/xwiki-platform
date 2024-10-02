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
package com.xpn.xwiki.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.suigeneris.jrcs.diff.delta.Delta;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.internal.document.DocumentRequiredRightsReader;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.internal.document.SafeDocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRight;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRightsManager;
import org.xwiki.stability.Unstable;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.criteria.impl.Period;
import com.xpn.xwiki.criteria.impl.PeriodFactory;
import com.xpn.xwiki.criteria.impl.Range;
import com.xpn.xwiki.criteria.impl.RangeFactory;
import com.xpn.xwiki.criteria.impl.RevisionCriteria;
import com.xpn.xwiki.criteria.impl.Scope;
import com.xpn.xwiki.criteria.impl.ScopeFactory;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.MetaDataDiff;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;
import com.xpn.xwiki.stats.api.XWikiStatsService;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.stats.impl.RefererStats;
import com.xpn.xwiki.util.TOCGenerator;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

/**
 * This class represents a document or page in the wiki. This is a security and usability wrapper which wraps
 * {@link com.xpn.xwiki.doc.XWikiDocument} In scripting, an object representing the document in which the script resides
 * will be bound to a variable called doc.
 *
 * @version $Id$
 */
public class Document extends Api
{
    /** Logging helper object. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Document.class);

    /**
     * The XWikiDocument object wrapped by this API.
     */
    protected XWikiDocument initialDoc;

    /**
     * The XWikiDocument object wrapped by this API.
     */
    protected XWikiDocument doc;

    /**
     * Convenience object used by object related methods.
     */
    protected Object currentObj;

    /**
     * Used to resolve a string into a proper Document Reference using the current document's reference to fill the
     * blanks, except for the page name for which the default page name is used instead and for the wiki name for which
     * the current wiki is used instead of the current document reference's wiki.
     */
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    /**
     * Used to convert a proper Document Reference to string (standard form).
     */
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    /**
     * Used to convert a proper Document Reference to a string but without the wiki name.
     */
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    private DocumentRevisionProvider documentRevisionProvider;

    private ConfigurationSource configuration;

    private DocumentRequiredRightsManager documentRequiredRightsManager;

    private DocumentAuthorizationManager documentAuthorizationManager;

    private DocumentReferenceResolver<String> getCurrentMixedDocumentReferenceResolver()
    {
        if (this.currentMixedDocumentReferenceResolver == null) {
            this.currentMixedDocumentReferenceResolver =
                Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
        }

        return this.currentMixedDocumentReferenceResolver;
    }

    private EntityReferenceSerializer<String> getDefaultEntityReferenceSerializer()
    {
        if (this.defaultEntityReferenceSerializer == null) {
            this.defaultEntityReferenceSerializer = Utils.getComponent(EntityReferenceSerializer.TYPE_STRING);
        }

        return this.defaultEntityReferenceSerializer;
    }

    private EntityReferenceSerializer<String> getLocalEntityReferenceSerializer()
    {
        if (this.localEntityReferenceSerializer == null) {
            this.localEntityReferenceSerializer = Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        }

        return this.localEntityReferenceSerializer;
    }

    private DocumentRevisionProvider getDocumentRevisionProvider()
    {
        if (this.documentRevisionProvider == null) {
            this.documentRevisionProvider = Utils.getComponent(DocumentRevisionProvider.class);
        }

        return this.documentRevisionProvider;
    }

    private ConfigurationSource getConfiguration()
    {
        if (this.configuration == null) {
            this.configuration = Utils.getComponent(ConfigurationSource.class);
        }

        return this.configuration;
    }

    private DocumentRequiredRightsManager getDocumentRequiredRightsManager()
    {
        if (this.documentRequiredRightsManager == null) {
            this.documentRequiredRightsManager = Utils.getComponent(DocumentRequiredRightsManager.class);
        }

        return this.documentRequiredRightsManager;
    }

    private DocumentAuthorizationManager getDocumentAuthorizationManager()
    {
        if (this.documentAuthorizationManager == null) {
            this.documentAuthorizationManager = Utils.getComponent(DocumentAuthorizationManager.class);
        }

        return this.documentAuthorizationManager;
    }

    /**
     * Document constructor.
     *
     * @param doc The XWikiDocument object to wrap.
     * @param context The current request context.
     */
    public Document(XWikiDocument doc, XWikiContext context)
    {
        super(context);

        this.initialDoc = doc;
        this.doc = this.initialDoc;
    }

    /**
     * Get the XWikiDocument wrapped by this API. This function is accessible only if you have the programming rights
     * give access to the priviledged API of the Document.
     *
     * @return The XWikiDocument wrapped by this API.
     */
    public XWikiDocument getDocument()
    {
        if (hasProgrammingRights()) {
            return this.doc;
        } else {
            return null;
        }
    }

    /**
     * Get a clone of the XWikiDocument wrapped by this API.
     *
     * @return A clone of the XWikiDocument wrapped by this API.
     */
    protected XWikiDocument getDoc()
    {
        if (this.initialDoc == this.doc) {
            this.doc = this.initialDoc.clone();
        }

        return this.doc;
    }

    /**
     * return the ID of the document. this ID is unique across the wiki.
     *
     * @return the id of the document.
     */
    public long getId()
    {
        return this.doc.getId();
    }

    /**
     * returns the DocumentReference for the current document
     *
     * @return the DocumentReference of the current document
     * @since 2.3M1
     */
    public DocumentReference getDocumentReference()
    {
        return this.doc.getDocumentReference();
    }

    /**
     * @return the {@link DocumentReference} of the document also containing the document {@link Locale}
     * @since 9.3RC1
     */
    public DocumentReference getDocumentReferenceWithLocale()
    {
        return this.doc.getDocumentReferenceWithLocale();
    }

    /**
     * @return the reference of the document as {@link PageReference} without the {@link Locale}
     * @since 10.6RC1
     */
    public PageReference getPageReference()
    {
        return this.doc.getPageReference();
    }

    /**
     * @return the reference of the document as {@link PageReference} including the {@link Locale}
     * @since 10.6RC1
     */
    public PageReference getPageReferenceWithLocale()
    {
        return this.doc.getPageReferenceWithLocale();
    }

    /**
     * @return the last part of the document's reference. For example if the reference of a document is
     *         {@code MySpace.Mydoc}, the returned name is {@code MyDoc}. For a nested document, the last part of the
     *         reference is always {@code WebHome} and thus the returned name is {@code Webhome}. It's better to use
     *         {@link #getPageReference()} or {@link #getDocumentReference()}, e.g. with
     *         {@code getPageReference().getName()} or {@code getDocumentReference().getName()}. To get the space name
     *         of the nested document you can use {@code getPageReference().getName()} or
     *         {@code getDocumentReference().getParent().getName()}.
     * @see #getPageReference()
     * @see #getDocumentReference()
     * @deprecated since 11.0, use {@link #getDocumentReference()} instead
     */
    @Deprecated
    public String getName()
{
    return this.doc.getDocumentReference().getName();
}

    /**
     * Return the full local space reference of the Document. For example a document located in sub-space
     * <code>space11</code> of space <code>space1</code> will return <code>space1.space11</code>.
     *
     * @return the name of the spaces of the document
     */
    public String getSpace()
    {
        return this.doc.getSpace();
    }

    /**
     * Get the name wiki where the document is stored.
     *
     * @return The name of the wiki where this document is stored.
     * @since 1.1.2
     * @since 1.2M2
     */
    public String getWiki()
    {
        return this.doc.getDocumentReference().getWikiReference().getName();
    }

    /**
     * Get the fullName of the document. If a document is named "MyDoc" in space "MySpace", the fullname is
     * "MySpace.MyDoc". In a wiki, all the documents have a different fullName.
     *
     * @return fullName of the document.
     */
    public String getFullName()
    {
        return getLocalEntityReferenceSerializer().serialize(this.doc.getDocumentReference());
    }

    /**
     * Get the complete fullName of the document. The real full name of the document containing the name of the wiki
     * where the document is stored. For a document stored in the wiki "xwiki", in space "MySpace", named "MyDoc", its
     * complete full name is "xwiki:MySpace.MyDoc".
     *
     * @return The complete fullName of the document.
     * @since 1.1.2
     * @since 1.2M2
     */
    public String getPrefixedFullName()
    {
        return getDefaultEntityReferenceSerializer().serialize(this.doc.getDocumentReference());
    }

    /**
     * Get a Version object representing the current version of the document.
     *
     * @return A Version object representing the current version of the document
     */
    public Version getRCSVersion()
    {
        return this.doc.getRCSVersion();
    }

    /**
     * Get a string representing the current version of the document.
     *
     * @return A string representing the current version of the document.
     */
    public String getVersion()
    {
        return this.doc.getVersion();
    }

    /**
     * Get a string representing the previous version of the document.
     *
     * @return A string representing the previous version of the document. If this is the first version then it returns
     *         null.
     */
    public String getPreviousVersion()
    {
        return this.doc.getPreviousVersion();
    }

    /**
     * Get the value of the title field of the document.
     *
     * @return The value of the title field of the document.
     */
    public String getTitle()
    {
        return this.doc.getTitle();
    }

    /**
     * Get document title. If a title has not been provided through the title field, it looks for a section title in the
     * document's content and if not found return the page name. The returned title is also interpreted which means it's
     * allowed to use Velocity, Groovy, etc syntax within a title.
     *
     * @return The document title as XHTML
     */
    public String getDisplayTitle()
    {
        return this.doc.getRenderedTitle(getXWikiContext());
    }

    /**
     * Returns the document title as plain text
     *
     * @return the document title as plain text (all markup removed)
     * @since 3.0M1
     */
    public String getPlainTitle()
    {
        return this.doc.getRenderedTitle(Syntax.PLAIN_1_0, getXWikiContext());
    }

    /**
     * Returns the title of the document rendered through wiki syntax and velocity
     *
     * @return the title rendered through wiki syntax and velocity
     * @see XWikiDocument#getRenderedTitle(Syntax, XWikiContext)
     */
    public String getRenderedTitle(String syntaxId) throws XWikiException
    {
        try {
            return this.doc.getRenderedTitle(Syntax.valueOf(syntaxId), getXWikiContext());
        } catch (ParseException e) {
            LOGGER.error("Failed to parse provided syntax identifier [" + syntaxId + "]", e);

            throw new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to parse syntax identifier [" + syntaxId + "]", e);
        } catch (Exception e) {
            LOGGER.error("Failed to render document [" + getPrefixedFullName() + "] title content", e);

            throw new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to render document [" + getPrefixedFullName() + "] content title", e);
        }
    }

    /**
     * TODO document this or mark it deprecated
     */
    public String getFormat()
    {
        return this.doc.getFormat();
    }

    /**
     * Get fullName of the profile document of the author of the current version of the document. Example: XWiki.Admin.
     *
     * @return The fullName of the profile document of the effective metadata author of the current version of the
     *         document.
     */
    public String getAuthor()
    {
        return this.doc.getAuthor();
    }

    /**
     * @return the document author reference
     * @since 6.4RC1
     */
    public DocumentReference getAuthorReference()
    {
        return this.doc.getAuthorReference();
    }

    /**
     * Get fullName of the profile document of the author of the content modification of this document version. Example:
     * XWiki.Admin.
     *
     * @return The fullName of the profile document of the author of the content modification in this document version.
     */
    public String getContentAuthor()
    {
        return this.doc.getContentAuthor();
    }

    /**
     * @return the document content author reference
     * @since 6.4RC1
     */
    public DocumentReference getContentAuthorReference()
    {
        return this.doc.getContentAuthorReference();
    }

    /**
     * @return The date when this document version has been modified.
     */
    public Date getDate()
    {
        return this.doc.getDate();
    }

    /**
     * Get the date when the content modification has been done on this document version. A content update excludes
     * modifications to meta data fields or comments of the document.
     *
     * @return The date where the content modification has been done on this document version.
     */
    public Date getContentUpdateDate()
    {
        return this.doc.getContentUpdateDate();
    }

    /**
     * @return the original creation date of the document.
     */
    public Date getCreationDate()
    {
        return this.doc.getCreationDate();
    }

    /**
     * Get the name of the parent of this document.
     *
     * @return The name of the parent of this document.
     */
    public String getParent()
    {
        return this.doc.getParent();
    }

    /**
     * @return the parent reference or null if the parent is not set
     * @since 7.3M1
     */
    public DocumentReference getParentReference()
    {
        return this.doc.getParentReference();
    }

    /**
     * Get fullName of the profile document of the document creator.
     *
     * @return The fullName of the profile document of the document creator.
     */
    public String getCreator()
    {
        return this.doc.getCreator();
    }

    /**
     * @return the document creator reference
     * @since 6.4RC1
     */
    public DocumentReference getCreatorReference()
    {
        return this.doc.getCreatorReference();
    }

    /**
     * Get raw content of the document, i.e. the content that is visible through the wiki editor.
     *
     * @return The raw content of the document.
     */
    public String getContent()
    {
        return this.doc.getContent();
    }

    /**
     * NOTE: This method caches the XDOM and returns a clone that can be safely modified.
     *
     * @return the XDOM corresponding to the document's string content
     * @since 7.0RC1
     */
    public XDOM getXDOM()
    {
        return this.doc.getXDOM();
    }

    /**
     * @return The syntax representing the syntax used for the document's content
     * @since 2.3M1
     */
    public Syntax getSyntax()
    {
        return this.doc.getSyntax();
    }

    /**
     * Get the Syntax id representing the syntax used for the document. For example "xwiki/1.0" represents the first
     * version XWiki syntax while "xwiki/2.0" represents version 2.0 of the XWiki Syntax.
     *
     * @return The syntax id representing the syntax used for the document.
     * @deprecated since 2.3M1 use {@link #getSyntax()} instead
     */
    @Deprecated
    public String getSyntaxId()
    {
        return this.doc.getSyntax().toIdString();
    }

    /**
     * Same as {@link #getLocale()} but as String.
     *
     * @return the locale of the document.
     * @deprecated since 5.4M1 use {@link #getLocale()} instead
     */
    @Deprecated
    public String getLanguage()
    {
        return this.doc.getLanguage();
    }

    /**
     * Get the locale of the document. If the document is a translation it returns the locale set for it, otherwise, it
     * returns the root locale.
     *
     * @return the locale of the document
     * @since 5.4M1
     */
    public Locale getLocale()
    {
        return this.doc.getLocale();
    }

    /**
     * TODO document this or mark it deprecated
     */
    public String getTemplate()
    {
        String templateReferenceAsString = "";
        DocumentReference templateDocumentReference = this.doc.getTemplateDocumentReference();
        if (templateDocumentReference != null) {
            templateReferenceAsString = getLocalEntityReferenceSerializer().serialize(templateDocumentReference);
        }
        return templateReferenceAsString;
    }

    /**
     * Same as {@link #getRealLocale()} but as String.
     *
     * @return the real locale
     * @deprecated since 8.0M1, use {@link #getRealLocale()} instead
     */
    @Deprecated
    public String getRealLanguage() throws XWikiException
    {
        return this.doc.getRealLanguage(getXWikiContext());
    }

    /**
     * Gets the real locale of the document. The real locale is either the default locale field when the locale field is
     * empty (when the document is the default document) or the locale field otherwise when the document is a
     * translation document
     * 
     * @return the actual locale of the document
     * @since 8.0M1
     */
    public Locale getRealLocale()
    {
        return this.doc.getRealLocale();
    }

    /**
     * Same as {@link #getDefaultLocale()} but as String.
     * 
     * @return the locale of the default document
     * @deprecated since 8.0M1, use {@link #getDefaultLocale()} instead
     */
    @Deprecated
    public String getDefaultLanguage()
    {
        return this.doc.getDefaultLanguage();
    }

    /**
     * @return the Locale of the default version of the document (usually {@link Locale#ROOT} or {@link Locale#ENGLISH})
     * @since 8.0M1
     */
    public Locale getDefaultLocale()
    {
        return this.doc.getDefaultLocale();
    }

    /**
     * @param defaultLocale the locale content in the default document version
     * @since 11.9RC1
     */
    public void setDefaultLocale(Locale defaultLocale)
    {
        this.doc.setDefaultLocale(defaultLocale);
    }

    /**
     * TODO document this or mark it deprecated
     */
    public String getDefaultTemplate()
    {
        return this.doc.getDefaultTemplate();
    }

    /**
     * @return the comment of of the document version
     */
    public String getComment()
    {
        return this.doc.getComment();
    }

    /**
     * @return true if the this document version was a minor edit.
     */
    public boolean isMinorEdit()
    {
        return this.doc.isMinorEdit();
    }

    /**
     * @return the list of locales for which this document has a translation; the original (default) locale is not
     *         included
     * @throws XWikiException if retrieving the document translations from the database fails
     * @since 12.4RC1
     */
    public List<Locale> getTranslationLocales() throws XWikiException
    {
        return this.doc.getTranslationLocales(getXWikiContext());
    }

    /**
     * @return the translated document's content if the wiki is multilingual, the locale is first checked in the URL,
     *         the cookie, the user profile and finally the wiki configuration if not, the locale is the one on the wiki
     *         configuration.
     */
    public String getTranslatedContent() throws XWikiException
    {
        return this.doc.getTranslatedContent(getXWikiContext());
    }

    /**
     * @return the translated content in the given locale
     */
    public String getTranslatedContent(String locale) throws XWikiException
    {
        return this.doc.getTranslatedContent(locale, getXWikiContext());
    }

    /**
     * @return the translated document in the given locale
     */
    public Document getTranslatedDocument(String locale) throws XWikiException
    {
        return this.doc.getTranslatedDocument(locale, getXWikiContext()).newDocument(getXWikiContext());
    }

    /**
     * @return the tranlated Document if the wiki is multilingual, the locale is first checked in the URL, the cookie,
     *         the user profile and finally the wiki configuration if not, the locale is the one on the wiki
     *         configuration.
     */
    public Document getTranslatedDocument() throws XWikiException
    {
        return this.doc.getTranslatedDocument(getXWikiContext()).newDocument(getXWikiContext());
    }

    /**
     * @param targetSyntax the syntax in which to render the document content
     * @return the content of the current document rendered.
     * @since 11.3RC1
     */
    public String displayDocument(Syntax targetSyntax) throws XWikiException
    {
        return this.doc.displayDocument(targetSyntax, getXWikiContext());
    }

    /**
     * @param targetSyntax the syntax in which to render the document content
     * @param restricted see {@link DocumentDisplayerParameters#isTransformationContextRestricted}.
     * @return the content of the current document rendered.
     * @since 11.5RC1
     */
    public String displayDocument(Syntax targetSyntax, boolean restricted) throws XWikiException
    {
        return this.doc.displayDocument(targetSyntax, restricted, getXWikiContext());
    }

    /**
     * @return the content of the current document rendered.
     * @since 11.3RC1
     */
    public String displayDocument() throws XWikiException
    {
        return this.doc.displayDocument(getXWikiContext());
    }

    /**
     * @return the content of the current document rendered.
     * @param restricted see {@link DocumentDisplayerParameters#isTransformationContextRestricted}.
     * @since 11.5RC1
     */
    public String displayDocument(boolean restricted) throws XWikiException
    {
        return this.doc.displayDocument(restricted, getXWikiContext());
    }

    /**
     * @return the content of the document or its translations rendered.
     */
    public String getRenderedContent() throws XWikiException
    {
        return this.doc.getRenderedContent(getXWikiContext());
    }

    /**
     * Execute and render the document in the current context.
     * <p>
     * The code is executed with right of this document content author.
     * 
     * @param transformationContextIsolated see {@link DocumentDisplayerParameters#isTransformationContextIsolated()}
     * @return the result
     * @throws XWikiException when failing to display the document
     * @since 8.4RC1
     */
    public String getRenderedContent(boolean transformationContextIsolated) throws XWikiException
    {
        return this.doc.getRenderedContent(transformationContextIsolated, getXWikiContext());
    }

    /**
     * @param text the text to render
     * @return the given text rendered in the context of this document
     * @deprecated since 1.6M1 use {@link #getRenderedContent(String, String)}
     */
    @Deprecated
    public String getRenderedContent(String text) throws XWikiException
    {
        return getRenderedContent(text, Syntax.XWIKI_1_0.toIdString());
    }

    /**
     * @param text the text to render
     * @param syntaxId the id of the Syntax used by the passed text (for example: "xwiki/1.0")
     * @return the given text rendered in the context of this document using the passed Syntax
     * @since 1.6M1
     */
    public String getRenderedContent(String text, String syntaxId) throws XWikiException
    {
        return getRenderedContent(text, syntaxId, false);
    }

    /**
     * Render a text in a restricted mode, where script macros are completely disabled.
     *
     * @param text the text to render
     * @param syntaxId the id of the Syntax used by the passed text (for example: "xwiki/1.0")
     * @return the given text rendered in the context of this document using the passed Syntax
     * @since 4.2M1
     */
    public String getRenderedContentRestricted(String text, String syntaxId) throws XWikiException
    {
        return getRenderedContent(text, syntaxId, true);
    }

    /**
     * Render a text in a restricted mode, where script macros are completely disabled.
     *
     * @param text the text to render
     * @param syntaxId the id of the Syntax used by the passed text (for example: "xwiki/1.0")
     * @param restricted see {@link DocumentDisplayerParameters#isTransformationContextRestricted}.
     * @return the given text rendered in the context of this document using the passed Syntax
     */
    private String getRenderedContent(String text, String syntaxId, boolean restricted) throws XWikiException
    {
        // Make sure we keep using current author as passed content author
        return this.doc.getRenderedContent(text, syntaxId, restricted, getCallerDocument(getXWikiContext()),
            getXWikiContext());
    }

    /**
     * @param text the text to render
     * @param sourceSyntaxId the id of the Syntax used by the passed text (for example: "xwiki/1.0")
     * @param targetSyntaxId the id of the syntax in which to render the document content
     * @return the given text rendered in the context of this document using the passed Syntax
     * @since 2.0M3
     */
    public String getRenderedContent(String text, String sourceSyntaxId, String targetSyntaxId) throws XWikiException
    {
        // Make sure we keep using current author as passed content author
        return this.doc.getRenderedContent(text, sourceSyntaxId, targetSyntaxId, false,
            getCallerDocument(getXWikiContext()), getXWikiContext());
    }

    private XWikiDocument getCallerDocument(XWikiContext xcontext)
    {
        XWikiDocument sdoc = (XWikiDocument) xcontext.get("sdoc");
        if (sdoc == null) {
            sdoc = xcontext.getDoc();
        }

        return sdoc;
    }

    /**
     * @param targetSyntax the syntax in which to render the document content
     * @return the rendered content
     * @throws XWikiException error when rendering content
     */
    public String getRenderedContent(Syntax targetSyntax) throws XWikiException
    {
        return this.doc.getRenderedContent(targetSyntax, getXWikiContext());
    }

    /**
     * Get the document's content XML-escaped.
     *
     * @return an XML-escaped version of the content of this document.
     */
    public String getEscapedContent() throws XWikiException
    {
        return this.doc.getEscapedContent(getXWikiContext());
    }

    /**
     * @return the archive of this document's history in string format
     */
    public String getArchive() throws XWikiException
    {
        return this.doc.getDocumentArchive(getXWikiContext()).getArchive(getXWikiContext());
    }

    /**
     * Get the archive of this document's history. This function is accessible only if you have the programming rights.
     *
     * @return the archive of this document's history as an {@link XWikiDocumentArchive}.
     */
    public XWikiDocumentArchive getDocumentArchive() throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.doc.getDocumentArchive(getXWikiContext());
        }
        return null;
    }

    /**
     * @return true if the document is a new one (ie it has never been saved) or false otherwise
     */
    public boolean isNew()
    {
        return this.doc.isNew();
    }

    /**
     * Return the relative URL of download for the the given attachment name.
     *
     * @param filename the name of the attachment
     * @return A String with the URL or null if the file name is empty
     */
    public String getAttachmentURL(String filename)
    {
        return this.doc.getAttachmentURL(filename, getXWikiContext());
    }

    /**
     * Get the relative URL of the given action for the the given attachment name.
     *
     * @param filename the name of the attachment.
     * @param action what to do to the file for example "delattachment", "download" or "downloadrev".
     * @return a string representation of a URL to do the given operation or null if the file name is empty
     */
    public String getAttachmentURL(String filename, String action)
    {
        return this.doc.getAttachmentURL(filename, action, getXWikiContext());
    }

    /**
     * Get the relative URL of an action on an attachment. the given action for the the given attachment name with
     * "queryString" parameters
     *
     * @param filename the name of the attachment.
     * @param action what to do to the file for example "delattachment", "download" or "downloadrev"
     * @param queryString parameters added to the URL, the "rev" parameter is used to specify a revision if using the
     *            "downloadrev" action. The query string must not begin with an ? character.
     * @return a string representation of a URL to do the given operation or null if the file name is empty
     */
    public String getAttachmentURL(String filename, String action, String queryString)
    {
        return this.doc.getAttachmentURL(filename, action, queryString, getXWikiContext());
    }

    /**
     * Get an old revision of an attachment.
     *
     * @param filename the name of the attachment.
     * @param version a revision number such as "1.1" or "1.2".
     * @return the URL for accessing to the archive of the attachment "filename" at the version "version" or null if the
     *         file name is empty
     */
    public String getAttachmentRevisionURL(String filename, String version)
    {
        return this.doc.getAttachmentRevisionURL(filename, version, getXWikiContext());
    }

    /**
     * Get an old revision of an attachment.
     *
     * @param filename the name of the attachment.
     * @param version a revision number such as "1.1" or "1.2".
     * @param queryString additional query parameters to pass in the request.
     * @return the URL for accessing to the archive of the attachment "filename" at the version "version" with the given
     *         queryString parameters or null if the file name is empty
     */
    public String getAttachmentRevisionURL(String filename, String version, String queryString)
    {
        return this.doc.getAttachmentRevisionURL(filename, version, queryString, getXWikiContext());
    }

    /**
     * Get the URL of this document.
     *
     * @return the URL to view this document, this will be a relative URL for example: /xwiki/bin/view/Main/WebHome
     * @see #getExternalURL() for an absolute URL which can used outside of the site.
     */
    public String getURL()
    {
        return this.doc.getURL("view", getXWikiContext());
    }

    /**
     * Get the URL to do a given action on this document.
     *
     * @param action what to do to the document for example "view", "edit" or "inline".
     * @return the URL of this document with the given action.
     * @see #getExternalURL(String) for an absolute URL which can used outside of the site.
     */
    public String getURL(String action)
    {
        return this.doc.getURL(action, getXWikiContext());
    }

    /**
     * Get the URL to do a given action on this document.
     *
     * @param action what to do to the document for example "view", "edit" or "inline".
     * @param queryString parameters to pass in the request eg: {@code paramA=value1&paramB=value2}
     * @return the URL of this document with the given action and queryString as parameters.
     * @see #getExternalURL(String, String) for an absolute URL which can used outside of the site.
     */
    public String getURL(String action, String queryString)
    {
        return this.doc.getURL(action, queryString, getXWikiContext());
    }

    /**
     * Get the external URL to do a given action on this document.
     *
     * @return the full URL of the document, sutable for use at external websites for example:
     *         http://www.xwiki.org/xwiki/bin/view/Main/WebHome
     * @see #getURL() for a relative URL which can only be used inside of the site.
     */
    public String getExternalURL()
    {
        return this.doc.getExternalURL("view", getXWikiContext());
    }

    /**
     * Get the external URL to do a given action on this document.
     *
     * @param action what to do to the document for example "view", "edit" or "inline".
     * @return the URL of this document with the given action.
     * @see #getURL() for a relative URL which can only be used inside of the site.
     */
    public String getExternalURL(String action)
    {
        return this.doc.getExternalURL(action, getXWikiContext());
    }

    /**
     * Get the URL to do a given action on this document.
     *
     * @param action what to do to the document for example "view", "edit" or "inline".
     * @param queryString parameters to pass in the request eg: {@code paramA=value1&paramB=value2}
     * @return the URL of this document with the given action and queryString as parameters.
     * @see #getURL() for a relative URL which can only be used inside of the site.
     */
    public String getExternalURL(String action, String queryString)
    {
        return this.doc.getExternalURL(action, queryString, getXWikiContext());
    }

    /**
     * @return the relative URL of the parent document of this document
     */
    public String getParentURL() throws XWikiException
    {
        return this.doc.getParentURL(getXWikiContext());
    }

    /**
     * @return the XClass associated to this document when the document represents an XWiki Class
     */
    public Class getxWikiClass()
    {
        BaseClass bclass = this.getDoc().getXClass();
        if (bclass == null) {
            return null;
        } else {
            return new Class(bclass, getXWikiContext());
        }
    }

    /**
     * @return the array of XClasses representing the objects of this document
     */
    public Class[] getxWikiClasses()
    {
        List<BaseClass> list = this.getDoc().getXClasses(getXWikiContext());
        if (list == null) {
            return null;
        }
        Class[] result = new Class[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = new Class(list.get(i), getXWikiContext());
        }
        return result;
    }

    /**
     * Creates a New XWiki Object of the given classname
     *
     * @param classname the classname used
     * @return the number of the object created
     */
    public int createNewObject(String classname) throws XWikiException
    {
        int index = getDoc().createNewObject(classname, getXWikiContext());

        updateAuthor();

        return index;
    }

    /**
     * Creates a New XWiki Object of the given classname
     *
     * @param classname the classname used
     * @return the object created
     */
    public Object newObject(String classname) throws XWikiException
    {
        int nb = createNewObject(classname);
        return getObject(classname, nb);
    }

    /**
     * @return true of the document has been loaded from cache
     */
    public boolean isFromCache()
    {
        return this.doc.isFromCache();
    }

    /**
     * @param classname the classname used
     * @return the number of objects available for a given classname
     */
    public int getObjectNumbers(String classname)
    {
        return this.doc.getObjectNumbers(classname);
    }

    /**
     * Get the list of all objects available in this document organized in a Map by classname
     *
     * @return the map of objects
     */
    public Map<String, Vector<Object>> getxWikiObjects()
    {
        Map<DocumentReference, List<BaseObject>> map = this.getDoc().getXObjects();
        Map<String, Vector<Object>> resultmap = new HashMap<String, Vector<Object>>();
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : map.entrySet()) {
            List<BaseObject> objects = entry.getValue();
            if (objects != null) {
                resultmap.put(getLocalEntityReferenceSerializer().serialize(entry.getKey()), getXObjects(objects));
            }
        }
        return resultmap;
    }

    protected Vector<Object> getXObjects(List<BaseObject> objects)
    {
        if (objects == null) {
            return new Vector<Object>(0);
        }
        Vector<Object> result = new Vector<Object>(objects.size());
        for (BaseObject bobj : objects) {
            if (bobj != null) {
                result.add(newObjectApi(bobj, getXWikiContext()));
            }
        }
        return result;
    }

    /**
     * Get the list of objects for a given classname classname
     *
     * @return the vector of objects
     */
    public Vector<Object> getObjects(String className)
    {
        List<BaseObject> objects = this.getDoc().getXObjects(this.doc.resolveClassReference(className));
        return getXObjects(objects);
    }

    /**
     * Get the first object that contains the given fieldname
     *
     * @param fieldname name of the field to find in the object
     * @return the XWiki Object
     */
    public Object getFirstObject(String fieldname)
    {
        try {
            BaseObject obj = this.getDoc().getFirstObject(fieldname, getXWikiContext());
            if (obj == null) {
                return null;
            } else {
                return newObjectApi(obj, getXWikiContext());
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the first object of a given classname that has a field name matching the given value
     *
     * @param classname name of the class of the object to look for
     * @param key name of the field to find in the object
     * @param value value of the field to find in the object
     * @param failover true if the first object will be given when none found
     * @return the XWiki Object
     */
    public Object getObject(String classname, String key, String value, boolean failover)
    {
        try {
            BaseObject obj = this.getDoc().getObject(classname, key, value, failover);
            if (obj == null) {
                return null;
            } else {
                return newObjectApi(obj, getXWikiContext());
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Select a subset of objects from a given class, filtered on a "key = value" criteria.
     *
     * @param classname The type of objects to return.
     * @param key The name of the property used for filtering.
     * @param value The required value.
     * @return A Vector of {@link Object objects} matching the criteria. If no objects are found, or if the key is an
     *         empty String, then an empty vector is returned.
     */
    public Vector<Object> getObjects(String classname, String key, String value)
    {
        Vector<Object> result = new Vector<Object>();
        if (StringUtils.isBlank(key) || value == null) {
            return getObjects(classname);
        }
        try {
            Vector<BaseObject> allObjects = this.getDoc().getObjects(classname);
            if (allObjects == null || allObjects.size() == 0) {
                return result;
            } else {
                for (BaseObject obj : allObjects) {
                    if (obj != null) {
                        BaseProperty prop = (BaseProperty) obj.get(key);
                        if (prop == null || prop.getValue() == null) {
                            continue;
                        }
                        if (value.equals(prop.getValue().toString())) {
                            result.add(newObjectApi(obj, getXWikiContext()));
                        }
                    }
                }
            }
        } catch (Exception e) {
        }
        return result;
    }

    /**
     * Get the first object of a given classname that has a field name matching the given value When none found this
     * method will return null
     *
     * @param classname name of the class of the object to look for
     * @param key name of the field to find in the object
     * @param value value of the field to find in the object
     * @return the XWiki Object
     */
    public Object getObject(String classname, String key, String value)
    {
        try {
            BaseObject obj = this.getDoc().getObject(classname, key, value);
            if (obj == null) {
                return null;
            } else {
                return newObjectApi(obj, getXWikiContext());
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the first object matching the given classname
     *
     * @param classname name of the class of the object to look for
     * @return the XWiki Object
     */
    public Object getObject(String classname)
    {
        return getObject(classname, false);
    }

    /**
     * get the object of the given className. If there is no object of this className and the create parameter at true,
     * the object is created.
     *
     * @param classname name of the class of the object to look for
     * @param create true of the object should be created when it does not exist
     * @return the XWiki Object
     */
    public Object getObject(String classname, boolean create)
    {
        try {
            BaseObject obj = getDoc().getObject(classname, create, getXWikiContext());

            if (obj == null) {
                return null;
            } else {
                return newObjectApi(obj, getXWikiContext());
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the object matching the given classname and given object number
     *
     * @param classname name of the class of the object
     * @param nb number of the object
     * @return the XWiki Object
     */
    public Object getObject(String classname, int nb)
    {
        try {
            BaseObject obj = this.getDoc().getObject(classname, nb);
            if (obj == null) {
                return null;
            } else {
                return newObjectApi(obj, getXWikiContext());
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param objectReference the object reference
     * @return the XWiki object from this document that matches the specified object reference
     * @since 12.3RC1
     */
    public Object getObject(ObjectReference objectReference)
    {
        try {
            BaseObject obj = this.getDoc().getXObject(objectReference);
            return obj == null ? null : newObjectApi(obj, getXWikiContext());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @param objectReference the object reference
     * @param create if true, the object will be created when missing
     * @return the XWiki object from this document that matches the specified object reference
     * @since 14.0RC1
     */
    public Object getObject(ObjectReference objectReference, boolean create)
    {
        try {
            BaseObject obj = this.getDoc().getXObject(objectReference, create, getXWikiContext());
            return obj == null ? null : newObjectApi(obj, getXWikiContext());
        } catch (Exception e) {
            return null;
        }
    }

    private Object newObjectApi(BaseObject obj, XWikiContext context)
    {
        return obj.newObjectApi(obj, context);
    }

    public String getXMLContent() throws XWikiException
    {
        String xml = this.doc.getXMLContent(getXWikiContext());
        return getXWikiContext().getUtil().substitute("s/<email>.*?<\\/email>/<email>********<\\/email>/goi",
            getXWikiContext().getUtil().substitute("s/<password>.*?<\\/password>/<password>********<\\/password>/goi",
                xml));
    }

    public String toXML() throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.doc.toXML(getXWikiContext());
        } else {
            return "";
        }
    }

    public org.dom4j.Document toXMLDocument() throws XWikiException
    {
        if (hasProgrammingRights()) {
            return this.doc.toXMLDocument(getXWikiContext());
        } else {
            return null;
        }
    }

    public Version[] getRevisions() throws XWikiException
    {
        return this.doc.getRevisions(getXWikiContext());
    }

    /**
     * Counts the number of document versions matching criteria like author, minimum creation date, etc.
     *
     * @param criteria criteria used to match versions
     * @return the number of matching versions
     * @since 15.10.8
     * @since 16.2.0RC1
     */
    @Unstable
    public long getRevisionsCount(RevisionCriteria criteria) throws XWikiException
    {
        return this.doc.getRevisionsCount(criteria, getXWikiContext());
    }

    public String[] getRecentRevisions() throws XWikiException
    {
        return this.doc.getRecentRevisions(5, getXWikiContext());
    }

    public String[] getRecentRevisions(int nb) throws XWikiException
    {
        return this.doc.getRecentRevisions(nb, getXWikiContext());
    }

    /**
     * Gets document versions matching criteria like author, minimum creation date, etc.
     *
     * @param criteria criteria used to match versions
     * @return a list of matching versions
     */
    public List<String> getRevisions(RevisionCriteria criteria) throws XWikiException
    {
        return this.doc.getRevisions(criteria, this.context);
    }

    /**
     * Get information about a document version : author, date, etc.
     *
     * @param version the version you want to get information about
     * @return a new RevisionInfo object
     */
    public RevisionInfo getRevisionInfo(String version) throws XWikiException
    {
        return new RevisionInfo(this.doc.getRevisionInfo(version, getXWikiContext()), getXWikiContext());
    }

    public List<Attachment> getAttachmentList()
    {
        List<Attachment> apis = new ArrayList<Attachment>();
        for (XWikiAttachment attachment : this.getDoc().getAttachmentList()) {
            apis.add(new Attachment(this, attachment, getXWikiContext()));
        }
        return apis;
    }

    public Vector<Object> getComments()
    {
        return getComments(true);
    }

    public Vector<Object> getComments(boolean asc)
    {
        return getXObjects(this.getDoc().getComments(asc));
    }

    /**
     * Setting the current object to the given object. Following calls to display() will use this object as the
     * reference for finding properties.
     *
     * @param object Object to use as a reference for further display calls
     */
    public void use(Object object)
    {
        this.currentObj = object;
    }

    /**
     * Setting the current object to the first object of the given class name. Following calls to display() will use
     * this object as the reference for finding properties.
     *
     * @param className class used to find the first object to use as the reference for display calls
     */
    public void use(String className)
    {
        this.currentObj = getObject(className);
    }

    /**
     * Setting the current object to the object of the given class name and the given number. Following calls to
     * display() will use this object as the reference for finding properties.
     *
     * @param className class used to find the object to use as the reference for display calls
     * @param nb number of the object to use as the reference for display calls
     */
    public void use(String className, int nb)
    {
        this.currentObj = getObject(className, nb);
    }

    /**
     * @return the className of the current active object use for display calls.
     */
    public String getActiveClass()
    {
        if (this.currentObj == null) {
            return null;
        } else {
            return this.currentObj.getName();
        }
    }

    /**
     * Displays the pretty name of the given field. This function uses the active object or will find the first object
     * that has the given field.
     *
     * @param fieldname fieldname to display the pretty name of
     * @return the pretty name display of the field.
     */
    public String displayPrettyName(String fieldname)
    {
        if (this.currentObj == null) {
            return this.doc.displayPrettyName(fieldname, getXWikiContext());
        } else {
            return this.doc.displayPrettyName(fieldname, this.currentObj.getBaseObject(), getXWikiContext());
        }
    }

    /**
     * Displays the pretty name of the given field of the given object.
     *
     * @param fieldname fieldname to display the pretty name of
     * @param obj Object to find the class to display the pretty name of
     * @return the pretty name display of the field.
     */
    public String displayPrettyName(String fieldname, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return this.doc.displayPrettyName(fieldname, obj.getBaseObject(), getXWikiContext());
    }

    /**
     * Displays the pretty name of the given field. This function uses the active object or will find the first object
     * that has the given field. with the optional addition of a mandatory field.
     *
     * @param fieldname fieldname to display the pretty name of
     * @param showMandatory true to display a mandatory sign
     * @return the pretty name display of the field.
     */
    public String displayPrettyName(String fieldname, boolean showMandatory)
    {
        if (this.currentObj == null) {
            return this.doc.displayPrettyName(fieldname, showMandatory, getXWikiContext());
        } else {
            return this.doc.displayPrettyName(fieldname, showMandatory, this.currentObj.getBaseObject(),
                getXWikiContext());
        }
    }

    /**
     * Displays the pretty name of the given field of the given object. with the optional addition of a mandatory field.
     *
     * @param fieldname fieldname to display the pretty name of
     * @param obj Object to find the class to display the pretty name of
     * @param showMandatory true to display a mandatory sign
     * @return the pretty name display of the field.
     */
    public String displayPrettyName(String fieldname, boolean showMandatory, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return this.doc.displayPrettyName(fieldname, showMandatory, obj.getBaseObject(), getXWikiContext());
    }

    /**
     * Displays the pretty name of the given field. This function uses the active object or will find the first object
     * that has the given field. with the optional addition of a mandatory field before or after the field
     *
     * @param fieldname fieldname to display the pretty name of
     * @param showMandatory true to display a mandatory sign
     * @param before true if the mandatory sign should be before the field
     * @return the pretty name display of the field.
     */
    public String displayPrettyName(String fieldname, boolean showMandatory, boolean before)
    {
        if (this.currentObj == null) {
            return this.doc.displayPrettyName(fieldname, showMandatory, before, getXWikiContext());
        } else {
            return this.doc.displayPrettyName(fieldname, showMandatory, before, this.currentObj.getBaseObject(),
                getXWikiContext());
        }
    }

    /**
     * Displays the pretty name of the given field of the given object. with the optional addition of a mandatory field
     * before or after the field
     *
     * @param fieldname fieldname to display the pretty name of
     * @param showMandatory true to display a mandatory sign
     * @param before true if the mandatory sign should be before the field
     * @param obj Object to find the class to display the pretty name of
     * @return the pretty name display of the field.
     */
    public String displayPrettyName(String fieldname, boolean showMandatory, boolean before, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return this.doc.displayPrettyName(fieldname, showMandatory, before, obj.getBaseObject(), getXWikiContext());
    }

    /**
     * Displays the given field. The display mode will be decided depending on page context (edit or inline context will
     * display in edit, view context in view) This function uses the active object or will find the first object that
     * has the given field. This function can return html inside and html macro
     *
     * @param fieldname fieldname to display
     * @return the display of the field.
     */
    public String display(String fieldname)
    {
        if (this.currentObj == null) {
            return this.doc.display(fieldname, getXWikiContext());
        } else {
            return this.doc.display(fieldname, this.currentObj.getBaseObject(), getXWikiContext());
        }
    }

    /**
     * Displays the given field in the given mode. This function uses the active object or will find the first object
     * that has the given field. This function can return html inside and html macro
     *
     * @param fieldname fieldname to display
     * @param mode display mode to use (view, edit, hidden, search)
     * @return the display of the field.
     */
    public String display(String fieldname, String mode)
    {
        if (this.currentObj == null) {
            return this.doc.display(fieldname, mode, getXWikiContext());
        } else {
            return this.doc.display(fieldname, mode, this.currentObj.getBaseObject(), getXWikiContext());
        }
    }

    /**
     * Displays the given field in the given mode. This function uses the active object or will find the first object
     * that has the given field. This function can return html inside and html macro A given prefix is added to the
     * field names when these are forms.
     *
     * @param fieldname fieldname to display
     * @param mode display mode to use (view, edit, hidden, search)
     * @param prefix prefix to use for the form names
     * @return the display of the field.
     */
    public String display(String fieldname, String mode, String prefix)
    {
        if (this.currentObj == null) {
            return this.doc.display(fieldname, mode, prefix, getXWikiContext());
        } else {
            return this.doc.display(fieldname, mode, prefix, this.currentObj.getBaseObject(), getSyntaxId(),
                getXWikiContext());
        }
    }

    /**
     * Displays the given field of the given object The display mode will be decided depending on page context (edit or
     * inline context will display in edit, view context in view) This function can return html inside and html macro
     *
     * @param fieldname fieldname to display
     * @param obj object from which to take the field
     * @return the display of the field.
     */
    public String display(String fieldname, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return this.doc.display(fieldname, obj.getBaseObject(), getXWikiContext());
    }

    /**
     * Displays the given field of the given object using the given mode and syntax rendering The display mode will be
     * decided depending on page context (edit or inline context will display in edit, view context in view) This
     * function can return html inside and html macro
     *
     * @param fieldname the name of the field to display
     * @param type the type of the field to display
     * @param obj the object containing the field to display
     * @param wrappingSyntaxId the syntax of the content in which the result will be included. This to take care of some
     *            escaping depending of the syntax.
     * @return the rendered field
     */
    public String display(String fieldname, String type, Object obj, String wrappingSyntaxId)
    {
        if (obj == null) {
            return "";
        }
        return this.doc.display(fieldname, type, obj.getBaseObject(), wrappingSyntaxId, getXWikiContext());
    }

    /**
     * Displays the given field of the given object in the given mode. This function can return html inside and html
     * macro
     *
     * @param fieldname fieldname to display
     * @param mode display mode to use (view, edit, hidden, search)
     * @param obj the object containing the field to display
     * @return the display of the field.
     */
    public String display(String fieldname, String mode, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return this.doc.display(fieldname, mode, obj.getBaseObject(), getXWikiContext());
    }

    /**
     * Displays the given field of the given object in the given mode. This function can return html inside and html
     * macro A given prefix is added to the field names when these are forms.
     *
     * @param fieldname fieldname to display
     * @param mode display mode to use (view, edit, hidden, search)
     * @param prefix prefix to use for the form names
     * @param obj the object containing the field to display
     * @return the display of the field.
     */
    public String display(String fieldname, String mode, String prefix, Object obj)
    {
        if (obj == null) {
            return "";
        }
        return this.doc.display(fieldname, mode, prefix, obj.getBaseObject(), getSyntaxId(), getXWikiContext());
    }

    public String displayForm(String className, String header, String format)
    {
        return this.doc.displayForm(className, header, format, getXWikiContext());
    }

    public String displayForm(String className, String header, String format, boolean linebreak)
    {
        return this.doc.displayForm(className, header, format, linebreak, getXWikiContext());
    }

    public String displayForm(String className)
    {
        return this.doc.displayForm(className, getXWikiContext());
    }

    public String displayRendered(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object)
        throws XWikiException
    {
        if ((pclass == null) || (object == null)) {
            return "";
        }
        return this.doc.displayRendered(pclass.getBasePropertyClass(), prefix, object.getCollection(),
            getXWikiContext());
    }

    public String displayView(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object)
    {
        if ((pclass == null) || (object == null)) {
            return "";
        }
        return this.doc.displayView(pclass.getBasePropertyClass(), prefix, object.getCollection(), getXWikiContext());
    }

    public String displayEdit(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object)
    {
        if ((pclass == null) || (object == null)) {
            return "";
        }
        return this.doc.displayEdit(pclass.getBasePropertyClass(), prefix, object.getCollection(), getXWikiContext());
    }

    public String displayHidden(com.xpn.xwiki.api.PropertyClass pclass, String prefix, Collection object)
    {
        if ((pclass == null) || (object == null)) {
            return "";
        }
        return this.doc.displayHidden(pclass.getBasePropertyClass(), prefix, object.getCollection(), getXWikiContext());
    }

    public List<String> getIncludedPages()
    {
        return this.doc.getIncludedPages(getXWikiContext());
    }

    public List<String> getIncludedMacros()
    {
        return this.doc.getIncludedMacros(getXWikiContext());
    }

    public List<String> getLinkedPages()
    {
        return new ArrayList<String>(this.doc.getUniqueLinkedPages(getXWikiContext()));
    }

    public Attachment getAttachment(String filename)
    {
        XWikiAttachment attach = this.getDoc().getAttachment(filename);
        if (attach == null) {
            return null;
        } else {
            return new Attachment(this, attach, getXWikiContext());
        }
    }

    public List<Delta> getContentDiff(Document origdoc, Document newdoc)
        throws XWikiException, DifferentiationFailedException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return Collections.emptyList();
            }
            if (origdoc == null) {
                return this.doc.getContentDiff(new XWikiDocument(newdoc.getDocumentReference()), newdoc.doc,
                    getXWikiContext());
            }
            if (newdoc == null) {
                return this.doc.getContentDiff(origdoc.doc, new XWikiDocument(origdoc.getDocumentReference()),
                    getXWikiContext());
            }

            return this.doc.getContentDiff(origdoc.doc, newdoc.doc, getXWikiContext());
        } catch (Exception e) {
            java.lang.Object[] args = { origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion() };
            List list = new ArrayList();
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_CONTENT_ERROR,
                    "Error while making content diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getXWikiContext());
            list.add(errormsg);
            return list;
        }
    }

    public List<Delta> getXMLDiff(Document origdoc, Document newdoc)
        throws XWikiException, DifferentiationFailedException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return Collections.emptyList();
            }
            if (origdoc == null) {
                return this.doc.getXMLDiff(new XWikiDocument(newdoc.getDocumentReference()), newdoc.doc,
                    getXWikiContext());
            }
            if (newdoc == null) {
                return this.doc.getXMLDiff(origdoc.doc, new XWikiDocument(origdoc.getDocumentReference()),
                    getXWikiContext());
            }

            return this.doc.getXMLDiff(origdoc.doc, newdoc.doc, getXWikiContext());
        } catch (Exception e) {
            java.lang.Object[] args = { origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion() };
            List list = new ArrayList();
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_XML_ERROR,
                    "Error while making xml diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getXWikiContext());
            list.add(errormsg);
            return list;
        }
    }

    public List<Delta> getRenderedContentDiff(Document origdoc, Document newdoc)
        throws XWikiException, DifferentiationFailedException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return Collections.emptyList();
            }
            if (origdoc == null) {
                return this.doc.getRenderedContentDiff(new XWikiDocument(newdoc.getDocumentReference()), newdoc.doc,
                    getXWikiContext());
            }
            if (newdoc == null) {
                return this.doc.getRenderedContentDiff(origdoc.doc, new XWikiDocument(origdoc.getDocumentReference()),
                    getXWikiContext());
            }

            return this.doc.getRenderedContentDiff(origdoc.doc, newdoc.doc, getXWikiContext());
        } catch (Exception e) {
            java.lang.Object[] args = { origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion() };
            List list = new ArrayList();
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_RENDERED_ERROR,
                    "Error while making rendered diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getXWikiContext());
            list.add(errormsg);
            return list;
        }
    }

    public List<MetaDataDiff> getMetaDataDiff(Document origdoc, Document newdoc) throws XWikiException
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return Collections.emptyList();
            }
            if (origdoc == null) {
                return this.doc.getMetaDataDiff(new XWikiDocument(newdoc.getDocumentReference()), newdoc.doc,
                    getXWikiContext());
            }
            if (newdoc == null) {
                return this.doc.getMetaDataDiff(origdoc.doc, new XWikiDocument(origdoc.getDocumentReference()),
                    getXWikiContext());
            }

            return this.doc.getMetaDataDiff(origdoc.doc, newdoc.doc, getXWikiContext());
        } catch (Exception e) {
            java.lang.Object[] args;
            if (origdoc != null) {
                args = new java.lang.Object[] { origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion() };
            } else {
                args = new java.lang.Object[] { doc.getFullName(), null, newdoc.getVersion() };
            }
            List list = new ArrayList();
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_METADATA_ERROR,
                    "Error while making meta data diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getXWikiContext());
            list.add(errormsg);
            return list;
        }
    }

    public List<List<ObjectDiff>> getObjectDiff(Document origdoc, Document newdoc)
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return Collections.emptyList();
            }
            if (origdoc == null) {
                return this.doc.getObjectDiff(new XWikiDocument(newdoc.getDocumentReference()), newdoc.doc,
                    getXWikiContext());
            }
            if (newdoc == null) {
                return this.doc.getObjectDiff(origdoc.doc, new XWikiDocument(origdoc.getDocumentReference()),
                    getXWikiContext());
            }

            return this.doc.getObjectDiff(origdoc.doc, newdoc.doc, getXWikiContext());
        } catch (Exception e) {
            java.lang.Object[] args = { origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion() };
            List list = new ArrayList();
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_OBJECT_ERROR,
                    "Error while making meta object diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getXWikiContext());
            list.add(errormsg);
            return list;
        }
    }

    public List<List<ObjectDiff>> getClassDiff(Document origdoc, Document newdoc)
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return Collections.emptyList();
            }
            if (origdoc == null) {
                return this.doc.getClassDiff(new XWikiDocument(newdoc.getDocumentReference()), newdoc.doc,
                    getXWikiContext());
            }
            if (newdoc == null) {
                return this.doc.getClassDiff(origdoc.doc, new XWikiDocument(origdoc.getDocumentReference()),
                    getXWikiContext());
            }

            return this.doc.getClassDiff(origdoc.doc, newdoc.doc, getXWikiContext());
        } catch (Exception e) {
            java.lang.Object[] args = { origdoc.getFullName(), origdoc.getVersion(), newdoc.getVersion() };
            List list = new ArrayList();
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_CLASS_ERROR,
                    "Error while making class diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getXWikiContext());
            list.add(errormsg);
            return list;
        }
    }

    public List<AttachmentDiff> getAttachmentDiff(Document origdoc, Document newdoc)
    {
        try {
            if ((origdoc == null) && (newdoc == null)) {
                return Collections.emptyList();
            }
            if (origdoc == null) {
                return wrapAttachmentDiff(this.doc.getAttachmentDiff(new XWikiDocument(newdoc.getDocumentReference()),
                    newdoc.doc, getXWikiContext()));
            }
            if (newdoc == null) {
                return wrapAttachmentDiff(this.doc.getAttachmentDiff(origdoc.doc,
                    new XWikiDocument(origdoc.getDocumentReference()), getXWikiContext()));
            }

            return wrapAttachmentDiff(this.doc.getAttachmentDiff(origdoc.doc, newdoc.doc, getXWikiContext()));
        } catch (Exception e) {
            java.lang.Object[] args = { (origdoc != null) ? origdoc.getFullName() : null,
            (origdoc != null) ? origdoc.getVersion() : null, (newdoc != null) ? newdoc.getVersion() : null };
            List list = new ArrayList();
            XWikiException xe =
                new XWikiException(XWikiException.MODULE_XWIKI_DIFF, XWikiException.ERROR_XWIKI_DIFF_ATTACHMENT_ERROR,
                    "Error while making attachment diff of {0} between version {1} and version {2}", e, args);
            String errormsg = Util.getHTMLExceptionMessage(xe, getXWikiContext());
            list.add(errormsg);
            return list;
        }
    }

    private List<AttachmentDiff> wrapAttachmentDiff(List<com.xpn.xwiki.doc.AttachmentDiff> diffs)
    {
        List<AttachmentDiff> safeAttachmentDiffs = new ArrayList<>();
        for (com.xpn.xwiki.doc.AttachmentDiff diff : diffs) {
            safeAttachmentDiffs.add(new AttachmentDiff(diff, getXWikiContext()));
        }
        return safeAttachmentDiffs;
    }

    public List<Delta> getLastChanges() throws XWikiException, DifferentiationFailedException
    {
        return this.doc.getLastChanges(getXWikiContext());
    }

    /**
     * Get statistics about the number of request for the current page during the current month.
     *
     * @param action the type of request for which to retrieve statistics: view, edit...
     * @return the statistics object holding information for this document and the current month
     */
    public DocumentStats getCurrentMonthPageStats(String action)
    {
        Scope scope = ScopeFactory.createPageScope(this.getFullName());
        Range range = RangeFactory.ALL;
        Period period = PeriodFactory.getCurrentMonth();
        XWikiStatsService statisticsService = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        List<DocumentStats> stats = statisticsService.getDocumentStatistics(action, scope, period, range, this.context);
        if (stats.size() > 0) {
            return stats.get(0);
        }
        return new DocumentStats();
    }

    /**
     * Get statistics about the number of request for the current space during the current month.
     *
     * @param action the type of request for which to retrieve statistics: view, edit...
     * @return the statistics object holding information for the document's space and the current month
     */
    public DocumentStats getCurrentMonthSpaceStats(String action)
    {
        Scope scope = ScopeFactory.createSpaceScope(this.doc.getSpace(), false);
        Range range = RangeFactory.ALL;
        Period period = PeriodFactory.getCurrentMonth();
        XWikiStatsService statisticsService = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        List<DocumentStats> stats = statisticsService.getDocumentStatistics(action, scope, period, range, this.context);
        if (stats.size() > 0) {
            return stats.get(0);
        }
        return new DocumentStats();
    }

    /**
     * Get referer statistics for the current document during the current month.
     *
     * @return a list of referer statistics for the document's space
     */
    public List<RefererStats> getCurrentMonthRefStats()
    {
        Scope scope = ScopeFactory.createPageScope(this.getFullName());
        Range range = RangeFactory.ALL;
        Period period = PeriodFactory.getCurrentMonth();
        XWikiStatsService statisticsService = getXWikiContext().getWiki().getStatsService(getXWikiContext());
        List<RefererStats> stats = statisticsService.getRefererStatistics("", scope, period, range, this.context);
        return stats;
    }

    public boolean checkAccess(String right)
    {
        try {
            return getXWikiContext().getWiki().checkAccess(right, this.doc, getXWikiContext());
        } catch (XWikiException e) {
            return false;
        }
    }

    public boolean hasAccessLevel(String level)
    {
        try {
            return getXWikiContext().getWiki().getRightService().hasAccessLevel(level, getXWikiContext().getUser(),
                this.getPrefixedFullName(), getXWikiContext());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasAccessLevel(String level, String user)
    {
        try {
            return getXWikiContext().getWiki().getRightService().hasAccessLevel(level, user, this.getPrefixedFullName(),
                getXWikiContext());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Verifies if the user identified by {@code userReference} has the access identified by {@code right} on this
     * document.
     * Note that this method does not override {@link Api#hasAccess(Right, DocumentReference)}: they share same
     * signature but on the {@code Api} one the {@link DocumentReference} parameter is about the entity where to check
     * the right, while here it's about the user to check right for.
     * 
     * @param right the right to check
     * @param userReference the user to check the right for
     * @return {@code true} if the user has the specified right on this document, {@code false} otherwise
     * @since 10.6RC1
     */
    @Override
    public boolean hasAccess(Right right, DocumentReference userReference)
    {
        return getAuthorizationManager().hasAccess(right, userReference, getDocumentReference());
    }

    /**
     * Verifies if the context user has the access identified by {@code right} on the current context document.
     * @param right the right to check
     * @return {@code true} if the user has the specified right on this document, {@code false} otherwise
     * @since 14.10
     * @since 14.4.7
     */
    public boolean hasAccess(Right right)
    {
        return hasAccess(right, getXWikiContext().getUserReference());
    }

    public boolean getLocked()
    {
        try {
            XWikiLock lock = this.doc.getLock(getXWikiContext());
            if (lock != null && !getXWikiContext().getUser().equals(lock.getUserName())) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public String getLockingUser()
    {
        try {
            XWikiLock lock = this.doc.getLock(getXWikiContext());
            if (lock != null && !getXWikiContext().getUser().equals(lock.getUserName())) {
                return lock.getUserName();
            } else {
                return "";
            }
        } catch (XWikiException e) {
            return "";
        }
    }

    public Date getLockingDate()
    {
        try {
            XWikiLock lock = this.doc.getLock(getXWikiContext());
            if (lock != null && !getXWikiContext().getUser().equals(lock.getUserName())) {
                return lock.getDate();
            } else {
                return null;
            }
        } catch (XWikiException e) {
            return null;
        }
    }

    /**
     * Renders the passed xproperty as HTML. Note that if you need the raw value, you should call 
     * {@link #getValue(String)} instead. 
     *
     * @param classOrFieldName the xproperty (aka field) name to render or an xclass reference
     * @return the rendered xproperty as HTML if an xobject exists with that xproperty. Otherwise considers that the
     *         passed parameter is an xclass reference and return the xobject for it or null if none exist
     * @see #getValue(String) 
     * @see #getValue(String, Object) 
     */
    public java.lang.Object get(String classOrFieldName)
    {
        if (this.currentObj != null) {
            return this.doc.display(classOrFieldName, this.currentObj.getBaseObject(), getXWikiContext());
        }
        BaseObject object = this.doc.getFirstObject(classOrFieldName, getXWikiContext());
        if (object != null) {
            return this.doc.display(classOrFieldName, object, getXWikiContext());
        }
        return this.getDoc().getObject(classOrFieldName);
    }

    /**
     * @param fieldName the xproperty (aka field) name for which to get the value
     * @return the raw value of the passed xproperty found in the current xobject or in the first xobject containing
     *         such a field
     * @see #getValue(String, Object) 
     */
    public java.lang.Object getValue(String fieldName)
    {
        Object object;
        if (this.currentObj == null) {
            object = new Object(this.getDoc().getFirstObject(fieldName, getXWikiContext()), getXWikiContext());
        } else {
            object = this.currentObj;
        }
        return getValue(fieldName, object);
    }

    /**
     * @param fieldName the xproperty (aka field) name for which to get the value
     * @param object the specific xobject from which to get the xproperty value
     * @return the raw value of the passed xproperty
     * @see #getValue(String)
     */
    public java.lang.Object getValue(String fieldName, Object object)
    {
        if (object != null) {
            return object.getValue(fieldName);
        }

        return null;
    }

    public String getTextArea()
    {
        return com.xpn.xwiki.XWiki.getTextArea(this.doc.getContent(), getXWikiContext());
    }

    /**
     * Returns data needed for a generation of Table of Content for this document.
     *
     * @param init an intial level where the TOC generation should start at
     * @param max maximum level TOC is generated for
     * @param numbered if should generate numbering for headings
     * @return a map where an heading (title) ID is the key and value is another map with two keys: text, level and
     *         numbering
     */
    public Map<String, Map<String, java.lang.Object>> getTOC(int init, int max, boolean numbered)
    {
        getXWikiContext().put("tocNumbered", new Boolean(numbered));
        return TOCGenerator.generateTOC(getContent(), init, max, numbered, getXWikiContext());
    }

    public String getTags()
    {
        return this.doc.getTags(getXWikiContext());
    }

    public List<String> getTagList()
    {
        return this.getDoc().getTagsList(getXWikiContext());
    }

    public List<String> getTagsPossibleValues()
    {
        return this.doc.getTagsPossibleValues(getXWikiContext());
    }

    public void insertText(String text, String marker) throws XWikiException
    {
        if (hasAccessLevel("edit")) {
            getDoc().insertText(text, marker, getXWikiContext());

            updateAuthor();
            updateContentAuthor();
        }
    }

    @Override
    public boolean equals(java.lang.Object arg0)
    {
        if (!(arg0 instanceof Document)) {
            return false;
        }
        Document d = (Document) arg0;
        return d.getXWikiContext().equals(getXWikiContext()) && this.doc.equals(d.doc);
    }

    /**
     * Check if the passed one is the one wrapped by this {@link Document}.
     * 
     * @param document the document to compare
     * @return true if passed document is the wrapped one
     * @since 8.3M1
     */
    public boolean same(XWikiDocument document)
    {
        return document == this.doc || document == this.initialDoc;
    }

    public List<String> getBacklinks() throws XWikiException
    {
        return this.doc.getBackLinkedPages(getXWikiContext());
    }

    /**
     * Retrieve the references of the page containing a link to the current page.
     *
     * @return a list of references of the page containing a link to the current page.
     * @throws XWikiException in case of problem to perform the query.
     * @since 12.5RC1
     */
    public List<DocumentReference> getBackLinkedReferences() throws XWikiException
    {
        return this.doc.getBackLinkedReferences(getXWikiContext());
    }

    public List<XWikiLink> getLinks() throws XWikiException
    {
        return new ArrayList<XWikiLink>(this.doc.getUniqueWikiLinkedPages(getXWikiContext()));
    }

    /**
     * Get the top sections contained in the document.
     * <p>
     * The section are filtered by xwiki.section.depth property on the maximum depth of the sections to return. This
     * method is usually used to get "editable" sections.
     *
     * @return the sections in the current document
     */
    public List<DocumentSection> getSections() throws XWikiException
    {
        return this.doc.getSections();
    }

    /**
     * Get document children. Children are documents with the current document as parent.
     *
     * @return The list of children for the current document.
     * @since 1.8 Milestone 2
     */
    public List<String> getChildren() throws XWikiException
    {
        return this.doc.getChildren(getXWikiContext());
    }

    /**
     * Get document children references. Children are documents with the current document as parent.
     * @return The list of children for the current document.
     * @throws XWikiException in case of problem to query the children.
     * @since 12.5RC1
     */
    public List<DocumentReference> getChildrenReferences() throws XWikiException
    {
        return this.doc.getChildrenReferences(getXWikiContext());
    }

    /**
     * Get document children. Children are documents with the current document as parent. Where a document has a large
     * number of children, one may desire to return a certain number of children (nb) and skip some number (start) of
     * the first results.
     *
     * @param nb The number of results to return.
     * @param start The number of results to skip before we begin returning results.
     * @return The list of children for the current document.
     */
    public List<String> getChildren(int nb, int start) throws XWikiException
    {
        return this.doc.getChildren(nb, start, getXWikiContext());
    }

    /**
     * Gets the default edit mode for this document. An edit mode (other than the default "edit") can be enforced by
     * creating an {@code XWiki.EditModeClass} object in the current document, with the appropriate value for the
     * defaultEditMode property, or by adding this object in a sheet included by the document. This function also falls
     * back on the old {@code SheetClass}, deprecated since 3.1M2, which can be attached to included documents to
     * specify that the current document should be edited inline.
     *
     * @return the default edit mode for this document ("edit" or "inline" usually)
     * @throws XWikiException since XWiki 6.3M1 it's not used anymore and "edit" is returned in case of error, with an
     *             error log
     */
    public String getDefaultEditMode() throws XWikiException
    {
        return this.doc.getDefaultEditMode(getXWikiContext());
    }

    public String getDefaultEditURL() throws XWikiException
    {
        return this.doc.getDefaultEditURL(getXWikiContext());
    }

    public String getEditURL(String action, String mode) throws XWikiException
    {
        return this.doc.getEditURL(action, mode, getXWikiContext());
    }

    public String getEditURL(String action, String mode, String locale)
    {
        return this.doc.getEditURL(action, mode, locale, getXWikiContext());
    }

    public boolean isCurrentUserCreator()
    {
        return this.doc.isCurrentUserCreator(getXWikiContext());
    }

    public boolean isCurrentUserPage()
    {
        return this.doc.isCurrentUserPage(getXWikiContext());
    }

    public boolean isCurrentLocalUserPage()
    {
        return this.doc.isCurrentLocalUserPage(getXWikiContext());
    }

    public boolean isCreator(String username)
    {
        return this.doc.isCreator(username);
    }

    public void set(String fieldname, java.lang.Object value)
    {
        Object obj;
        if (this.currentObj != null) {
            obj = this.currentObj;
        } else {
            obj = getFirstObject(fieldname);
        }
        set(fieldname, value, obj);
    }

    public void set(String fieldname, java.lang.Object value, Object obj)
    {
        if (obj == null) {
            return;
        }
        obj.set(fieldname, value);
    }

    public void setTitle(String title)
    {
        getDoc().setTitle(title);

        updateAuthor();
        updateContentAuthor();
    }

    public void setCustomClass(String customClass)
    {
        getDoc().setCustomClass(customClass);

        updateAuthor();
    }

    public void setParent(String parent)
    {
        getDoc().setParent(parent);

        updateAuthor();
    }

    private void updateContentAuthor()
    {
        // Temporary set as content author of the document the current script author (until the document is saved)
        XWikiContext xcontext = getXWikiContext();
        getDoc().setContentAuthorReference(xcontext.getAuthorReference());
    }

    private void updateAuthor()
    {
        updateAuthor(getDoc(), getXWikiContext());
    }

    protected static void updateAuthor(XWikiDocument document, XWikiContext xcontext)
    {
        // Temporary set as author of the document the current script author (until the document is saved)
        document.setAuthorReference(xcontext.getAuthorReference());

        XWikiDocument secureDocument = xcontext.getSecureDocument();
        // If there is a secure document that has required rights enforced, we need to be careful.
        if (secureDocument != null) {
            DocumentRequiredRightsManager requiredRightsManager =
                Utils.getComponent(DocumentRequiredRightsManager.class);
            DocumentReference secureDocumentReference = secureDocument.getDocumentReference();
            try {
                DocumentRequiredRights secureRequiredRights =
                    requiredRightsManager.getRequiredRights(secureDocumentReference)
                        .orElse(DocumentRequiredRights.EMPTY);
                DocumentRequiredRights requiredRights =
                    requiredRightsManager.getRequiredRights(document.getDocumentReference())
                        .orElse(DocumentRequiredRights.EMPTY);

                DocumentAuthorizationManager authorizationManager =
                    Utils.getComponent(DocumentAuthorizationManager.class);
                if (secureRequiredRights.enforce()
                    // If the secure document has programming right, everything is fine.
                    && !authorizationManager.hasRequiredRight(Right.PROGRAM, null, secureDocumentReference)
                    // If this document doesn't have required rights enforced or has more rights than the secure
                    // document, we need to restrict this document to be safe.
                    && (!requiredRights.enforce() || !hasAllRequiredRights(requiredRights, secureDocumentReference)))
                {
                    document.setRestricted(true);
                }
            } catch (AuthorizationException e) {
                document.setRestricted(true);

                LOGGER.error("Failed to load or check required rights in update of document [{}]",
                    document.getDocumentReference(), e);
            }
        }
    }

    private static boolean hasAllRequiredRights(DocumentRequiredRights requiredRights,
        DocumentReference secureDocumentReference)
    {
        DocumentAuthorizationManager authorizationManager = Utils.getComponent(DocumentAuthorizationManager.class);
        return !requiredRights.rights().stream().allMatch(requiredRight ->
        {
            try {
                return authorizationManager.hasRequiredRight(requiredRight.right(),
                    requiredRight.scope(), secureDocumentReference);
            } catch (AuthorizationException e) {
                LOGGER.error(
                    "Failed to check required rights for secure document[{}] in document update",
                    secureDocumentReference, e);
                return false;
            }
        });
    }

    public void setContent(String content)
    {
        getDoc().setContent(content);

        updateAuthor();
        updateContentAuthor();
    }

    /**
     * @param content the content as XDOM
     * @throws XWikiException when failing to convert the XDOM to String content
     * @since 7.0RC1
     */
    public void setContent(XDOM content) throws XWikiException
    {
        getDoc().setContent(content);

        updateAuthor();
        updateContentAuthor();
    }

    /**
     * @param syntax the Syntax representing the syntax used for the current document's content.
     * @since 2.3M1
     */
    public void setSyntax(Syntax syntax)
    {
        getDoc().setSyntax(syntax);

        updateAuthor();
        updateContentAuthor();
    }

    /**
     * @param syntaxId the Syntax id representing the syntax used for the current document. For example "xwiki/1.0"
     *            represents the first version XWiki syntax while "xwiki/2.0" represents version 2.0 of the XWiki
     *            Syntax.
     */
    public void setSyntaxId(String syntaxId)
    {
        getDoc().setSyntaxId(syntaxId);

        updateAuthor();
        updateContentAuthor();
    }

    public void setDefaultTemplate(String dtemplate)
    {
        getDoc().setDefaultTemplate(dtemplate);

        updateAuthor();
    }

    public void setComment(String comment)
    {
        getDoc().setComment(comment);
    }

    public void setMinorEdit(boolean isMinor)
    {
        getDoc().setMinorEdit(isMinor);
    }

    public void save() throws XWikiException
    {
        save("", false);
    }

    public void save(String comment) throws XWikiException
    {
        save(comment, false);
    }

    private UserReferenceResolver<CurrentUserReference> getCurrentUserReferenceResolver()
    {
        return Utils.getComponent(new DefaultParameterizedType(null, UserReferenceResolver.class,
                CurrentUserReference.class));
    }

    public void save(String comment, boolean minorEdit) throws XWikiException
    {
        if (hasAccessLevel("edit")) {

            DocumentAuthors authors = getDoc().getAuthors();
            authors.setOriginalMetadataAuthor(getCurrentUserReferenceResolver().resolve(CurrentUserReference.INSTANCE));
            // If the current author does not have PR don't let it set current user as author of the saved document
            // since it can lead to right escalation
            if (hasProgrammingRights() || !getConfiguration().getProperty("security.script.save.checkAuthor", true)) {
                saveDocument(comment, minorEdit);
            } else {
                saveAsAuthor(comment, minorEdit);
            }
        } else {
            java.lang.Object[] args = {getDefaultEntityReferenceSerializer().serialize(getDocumentReference())};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied in edit mode on document {0}", null, args);
        }
    }

    public void saveWithProgrammingRights() throws XWikiException
    {
        saveWithProgrammingRights("", false);
    }

    public void saveWithProgrammingRights(String comment) throws XWikiException
    {
        saveWithProgrammingRights(comment, false);
    }

    public void saveWithProgrammingRights(String comment, boolean minorEdit) throws XWikiException
    {
        if (hasProgrammingRights()) {
            // The rights check above is generic, but the current method is a save operation, thus it should not be
            // performed if the document's wiki is in read only mode.
            XWikiContext context = getXWikiContext();
            String currentWikiId = context.getWikiId();
            try {
                // Make sure we check the current document's wiki and not the current context's wiki.
                context.setWikiId(getWiki());

                if (!context.getWiki().isReadOnly()) {
                    saveDocument(comment, minorEdit, false);
                } else {
                    java.lang.Object[] args =
                        { getDefaultEntityReferenceSerializer().serialize(getDocumentReference()), getWiki() };
                    throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                        XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                        "Access denied in edit mode on document [{0}]. The wiki [{1}] is in read only mode.", null,
                        args);
                }
            } finally {
                // Restore the context wiki.
                context.setWikiId(currentWikiId);
            }
        } else {
            java.lang.Object[] args = { this.getFullName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied with no programming rights document {0}", null, args);
        }
    }

    /**
     * Save the document if the current author of the script calling this method has permission to do so. The author of
     * this document is also set to the said author.
     *
     * @throws XWikiException if script author is not allowed to save the document or if save operation fails.
     * @since 2.3M2
     */
    public void saveAsAuthor() throws XWikiException
    {
        saveAsAuthor("", false);
    }

    /**
     * Save the document if the current author of the script calling this method has permission to do so. The author of
     * this document is also set to the said author.
     *
     * @param comment The comment to display in document history (what did you change in the document)
     * @throws XWikiException if script author is not allowed to save the document or if save operation fails.
     * @since 2.3M2
     */
    public void saveAsAuthor(String comment) throws XWikiException
    {
        saveAsAuthor(comment, false);
    }

    /**
     * Save the document if the current author of the script calling this method has permission to do so. The author of
     * this document is also set to the said author.
     *
     * @param comment The comment to display in document history (what did you change in the document)
     * @param minorEdit Set true to advance the document version number by 0.1 or false to advance version to the next
     *            integer + 0.1 eg: 25.1
     * @throws XWikiException if script author is not allowed to save the document or if save operation fails.
     * @since 2.3M2
     */
    public void saveAsAuthor(String comment, boolean minorEdit) throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();

        getDoc().getAuthors()
            .setOriginalMetadataAuthor(getCurrentUserReferenceResolver().resolve(CurrentUserReference.INSTANCE));
        DocumentReference author = getEffectiveAuthorReference();
        if (hasAccess(Right.EDIT, author)) {
            DocumentReference currentUser = xcontext.getUserReference();
            try {
                xcontext.setUserReference(author);

                saveDocument(comment, minorEdit);
            } finally {
                xcontext.setUserReference(currentUser);
            }
        } else {
            java.lang.Object[] args = { author, xcontext.getDoc(), getFullName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied; user {0}, acting through script in document {1} cannot save document {2}", null, args);
        }
    }

    protected void saveDocument(String comment, boolean minorEdit) throws XWikiException
    {
        saveDocument(comment, minorEdit, true);
    }

    private void saveDocument(String comment, boolean minorEdit, boolean checkSaving) throws XWikiException
    {
        XWikiDocument doc = getDoc();

        UserReference currentUserReference = getCurrentUserReferenceResolver().resolve(CurrentUserReference.INSTANCE);
        doc.getAuthors().setEffectiveMetadataAuthor(currentUserReference);

        if (doc.isNew()) {
            doc.getAuthors().setCreator(currentUserReference);
        }

        XWikiContext xWikiContext = getXWikiContext();
        if (checkSaving) {
            DocumentReference author = doc.getAuthorReference();

            XWikiDocument secureDocument = xWikiContext.getSecureDocument();
            if (secureDocument != null) {
                checkRequiredRightsForSaving(secureDocument, doc, author);
            }

            // Make sure the user is allowed to make this modification
            xWikiContext.getWiki().checkSavingDocument(author, doc, comment, minorEdit,
                xWikiContext);
        }

        xWikiContext.getWiki().saveDocument(doc, comment, minorEdit, xWikiContext);
        this.initialDoc = this.doc;
    }

    private void checkRequiredRightsForSaving(XWikiDocument secureDocument, XWikiDocument doc, DocumentReference author)
        throws XWikiException
    {
        DocumentRequiredRights documentRequiredRights;
        try {
            documentRequiredRights =
                getDocumentRequiredRightsManager().getRequiredRights(secureDocument.getDocumentReference())
                    .orElse(DocumentRequiredRights.EMPTY);
        } catch (AuthorizationException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "The required rights for document [%s] couldn't be loaded"
                    .formatted(secureDocument.getDocumentReference()), e);
        }

        try {
            // No programming right? Enforce required rights!
            if (documentRequiredRights.enforce()
                && !getDocumentAuthorizationManager().hasRequiredRight(
                Right.PROGRAM, null, secureDocument.getDocumentReference()))
            {
                if (!doc.isEnforceRequiredRights()) {
                    doc.setEnforceRequiredRights(true);
                }

                DocumentRequiredRightsReader rightsReader =
                    Utils.getComponent(DocumentRequiredRightsReader.class);
                DocumentRequiredRights newRequiredRights = rightsReader.readRequiredRights(doc);

                for (DocumentRequiredRight requiredRight : newRequiredRights.rights()) {
                    getDocumentAuthorizationManager()
                        .checkAccess(requiredRight.right(), requiredRight.scope(), author,
                            doc.getDocumentReference());
                }
            }
        } catch (AuthorizationException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                ("The document cannot be saved because rights on the secure document [%s] are restricted using "
                    + "required rights and the document to save [%s] has more rights than the secure document.")
                    .formatted(secureDocument.getDocumentReference(), doc.getDocumentReference()), e);
        }
    }

    public com.xpn.xwiki.api.Object addObjectFromRequest() throws XWikiException
    {
        // Call to getDoc() ensures that we are working on a clone()
        com.xpn.xwiki.api.Object obj =
            new com.xpn.xwiki.api.Object(getDoc().addXObjectFromRequest(getXWikiContext()), getXWikiContext());

        updateAuthor();

        return obj;
    }

    public com.xpn.xwiki.api.Object addObjectFromRequest(String className) throws XWikiException
    {
        com.xpn.xwiki.api.Object obj = new com.xpn.xwiki.api.Object(
            getDoc().addObjectFromRequest(className, getXWikiContext()), getXWikiContext());

        updateAuthor();

        return obj;
    }

    public List<Object> addObjectsFromRequest(String className) throws XWikiException
    {
        return addObjectsFromRequest(className, "");
    }

    public com.xpn.xwiki.api.Object addObjectFromRequest(String className, String prefix) throws XWikiException
    {
        com.xpn.xwiki.api.Object obj = new com.xpn.xwiki.api.Object(
            getDoc().addObjectFromRequest(className, prefix, getXWikiContext()), getXWikiContext());

        updateAuthor();

        return obj;
    }

    public List<Object> addObjectsFromRequest(String className, String prefix) throws XWikiException
    {
        List<BaseObject> objs = getDoc().addObjectsFromRequest(className, prefix, getXWikiContext());
        List<Object> wrapped = new ArrayList<Object>();
        for (BaseObject object : objs) {
            wrapped.add(new com.xpn.xwiki.api.Object(object, getXWikiContext()));
        }

        updateAuthor();

        return wrapped;
    }

    public com.xpn.xwiki.api.Object updateObjectFromRequest(String className) throws XWikiException
    {
        com.xpn.xwiki.api.Object obj = new com.xpn.xwiki.api.Object(
            getDoc().updateObjectFromRequest(className, getXWikiContext()), getXWikiContext());

        updateAuthor();

        return obj;
    }

    public List<Object> updateObjectsFromRequest(String className) throws XWikiException
    {
        return updateObjectsFromRequest(className, "");
    }

    public com.xpn.xwiki.api.Object updateObjectFromRequest(String className, String prefix) throws XWikiException
    {
        com.xpn.xwiki.api.Object obj = new com.xpn.xwiki.api.Object(
            getDoc().updateObjectFromRequest(className, prefix, getXWikiContext()), getXWikiContext());

        updateAuthor();

        return obj;
    }

    public List<Object> updateObjectsFromRequest(String className, String prefix) throws XWikiException
    {
        List<BaseObject> objs = getDoc().updateObjectsFromRequest(className, prefix, getXWikiContext());
        List<Object> wrapped = new ArrayList<Object>();
        for (BaseObject object : objs) {
            wrapped.add(new com.xpn.xwiki.api.Object(object, getXWikiContext()));
        }

        updateAuthor();

        return wrapped;
    }

    public boolean isAdvancedContent()
    {
        return this.doc.isAdvancedContent();
    }

    public boolean isProgrammaticContent()
    {
        return this.doc.isProgrammaticContent();
    }

    /**
     * Remove an XObject from the document. The changes are not persisted until the document is saved.
     *
     * @param object the object to remove
     * @return {@code true} if the object was successfully removed, {@code false} if the object was not found in the
     *         current document.
     */
    public boolean removeObject(Object object)
    {
        return getDoc().removeObject(object.getBaseObject());
    }

    /**
     * Remove all the objects of a given type (XClass) from the document. The object counter is left unchanged, so that
     * future objects will have new (different) numbers. However, on some storage engines the counter will be reset if
     * the document is removed from the cache and reloaded from the persistent storage.
     *
     * @param className The class name of the objects to be removed.
     * @return {@code true} if the objects were successfully removed, {@code false} if no object from the target class
     *         was in the current document.
     */
    public boolean removeObjects(String className)
    {
        return getDoc().removeObjects(className);
    }

    /**
     * Remove document from the wiki. Reinit <code>cloned</code>.
     *
     * @throws XWikiException
     */
    protected void deleteDocument() throws XWikiException
    {
        getXWikiContext().getWiki().deleteDocument(this.doc, getXWikiContext());
        this.initialDoc = this.doc;
    }

    public void delete() throws XWikiException
    {
        if (hasAccessLevel("delete")) {
            deleteDocument();
        } else {
            java.lang.Object[] args = { this.getFullName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied in edit mode on document {0}", null, args);
        }
    }

    /**
     * Delete the document if the {@link #getContentAuthor content author} of the script calling this method has
     * permission to do so. The deleter is also set to the said content author.
     *
     * @throws XWikiException if script author is not allowed to delete the document or if save operation fails.
     * @since 2.3M2
     */
    public void deleteAsAuthor() throws XWikiException
    {
        XWikiContext xcontext = getXWikiContext();

        DocumentReference author = getEffectiveAuthorReference();
        if (hasAccess(Right.DELETE, author)) {
            DocumentReference currentUser = xcontext.getUserReference();
            try {
                xcontext.setUserReference(author);

                deleteDocument();
            } finally {
                xcontext.setUserReference(currentUser);
            }
        } else {
            java.lang.Object[] args = { author, xcontext.getDoc(), this.getFullName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied; user {0}, acting through script in document {1} cannot delete document {2}", null,
                args);
        }
    }

    public void deleteWithProgrammingRights() throws XWikiException
    {
        if (hasProgrammingRights()) {
            deleteDocument();
        } else {
            java.lang.Object[] args = { this.getFullName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied with no programming rights document {0}", null, args);
        }
    }

    public String getVersionHashCode()
    {
        return this.doc.getVersionHashCode(getXWikiContext());
    }

    public int addAttachments() throws XWikiException
    {
        return addAttachments(null);
    }

    public int addAttachments(String fieldName) throws XWikiException
    {
        if (!hasAccessLevel("edit")) {
            java.lang.Object[] args = { this.getFullName() };
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access denied in edit mode on document {0}", null, args);
        }
        XWiki xwiki = getXWikiContext().getWiki();
        FileUploadPlugin fileupload = (FileUploadPlugin) xwiki.getPlugin("fileupload", getXWikiContext());
        List<FileItem> fileuploadlist = fileupload.getFileItems(getXWikiContext());
        List<XWikiAttachment> attachments = new ArrayList<XWikiAttachment>();
        // adding attachment list to context so we find the names
        this.context.put("addedAttachments", attachments);
        int nb = 0;

        if (fileuploadlist == null) {
            return 0;
        }

        for (FileItem item : fileuploadlist) {
            String name = item.getFieldName();
            if (fieldName != null && !fieldName.equals(name)) {
                continue;
            }
            if (item.isFormField()) {
                continue;
            }
            byte[] data = fileupload.getFileItemData(name, getXWikiContext());
            String filename;
            String fname = fileupload.getFileName(name, getXWikiContext());
            int i = fname.lastIndexOf("\\");
            if (i == -1) {
                i = fname.lastIndexOf("/");
            }
            filename = fname.substring(i + 1);
            filename = filename.replaceAll("\\+", " ");

            if ((data != null) && (data.length > 0)) {
                XWikiAttachment attachment = this.getDoc().addAttachment(filename, data, getXWikiContext());
                getDoc().saveAttachmentContent(attachment, getXWikiContext());
                // commenting because this was already done by addAttachment
                // getDoc().getAttachmentList().add(attachment);
                attachments.add(attachment);
                nb++;
            }
        }

        if (nb > 0) {
            getXWikiContext().getWiki().saveDocument(getDoc(), getXWikiContext());
            this.initialDoc = this.doc;
        }

        return nb;
    }

    public Attachment addAttachment(String fileName, InputStream iStream)
    {
        try {
            return new Attachment(this, this.getDoc().setAttachment(fileName, iStream, getXWikiContext()),
                getXWikiContext());
        } catch (IOException e) {
            // TODO Log the error and let the user know about it
        } finally {
            updateAuthor();
        }

        return null;
    }

    public Attachment addAttachment(String fileName, byte[] data)
    {
        try {
            return new Attachment(this, this.getDoc().addAttachment(fileName, data, getXWikiContext()),
                getXWikiContext());
        } catch (XWikiException e) {
            // TODO Log the error and let the user know about it
        } finally {
            updateAuthor();
        }

        return null;
    }

    public boolean validate() throws XWikiException
    {
        return this.doc.validate(getXWikiContext());
    }

    public boolean validate(String[] classNames) throws XWikiException
    {
        return this.doc.validate(classNames, getXWikiContext());
    }

    /**
     * Retrieves the validation script associated with this document, a Velocity script that is executed when validating
     * the document data.
     *
     * @return A <code>String</code> representation of the validation script, or an empty string if there is no such
     *         script.
     */
    public String getValidationScript()
    {
        return getDoc().getValidationScript();
    }

    /**
     * Sets a new validation script for this document, a Velocity script that is executed when validating the document
     * data.
     *
     * @param validationScript The new validation script, which can be an empty string or <code>null</code> if the
     *            script should be removed.
     */
    public void setValidationScript(String validationScript)
    {
        getDoc().setValidationScript(validationScript);

        updateAuthor();
    }

    /**
     * Rename the current document and all the backlinks leading to it. Will also change parent field in all documents
     * which list the document we are renaming as their parent. See
     * {@link #rename(String, java.util.List, java.util.List)} for more details.
     *
     * @param newReference the reference to the new document
     * @throws XWikiException in case of an error
     * @since 2.3M2
     */
    public void rename(DocumentReference newReference) throws XWikiException
    {
        XWiki xWiki = this.context.getWiki();
        if (hasAccessLevel("delete") && xWiki.checkAccess("edit",
            xWiki.getDocument(newReference, this.context), this.context)) {
            List<DocumentReference> backLinkedReferences = getDocument().getBackLinkedReferences(this.context);
            List<DocumentReference> childrenReferences = getDocument().getChildrenReferences(this.context);
            xWiki.renameDocument(getDocumentReference(), newReference, true, backLinkedReferences, childrenReferences,
                    this.context);
        }
    }

    /**
     * Rename the current document and all the links pointing to it in the list of passed backlink documents. The
     * renaming algorithm takes into account the fact that there are several ways to write a link to a given page and
     * all those forms need to be renamed. For example the following links all point to the same page:
     * <ul>
     * <li>[Page]</li>
     * <li>[Page?param=1]</li>
     * <li>[currentwiki:Page]</li>
     * <li>[currentwiki:CurrentSpace.Page]</li>
     * </ul>
     * <p>
     * Note: links without a space are renamed with the space added and all documents which have the document being
     * renamed as parent have their parent field set to "currentwiki:CurrentSpace.Page".
     * </p>
     *
     * @param newDocumentName the new document name. If the space is not specified then defaults to the current space.
     * @param backlinkDocumentNames the list of documents to parse and for which links will be modified to point to the
     *            new renamed document.
     * @throws XWikiException in case of an error
     */
    public void rename(String newDocumentName, List<String> backlinkDocumentNames) throws XWikiException
    {
        rename(newDocumentName, backlinkDocumentNames, Collections.emptyList());
    }

    /**
     * Same as {@link #rename(String, List)} but the list of documents having the current document as their parent is
     * passed in parameter.
     *
     * @param newDocumentName the new document name. If the space is not specified then defaults to the current space.
     * @param backlinkDocumentNames the list of documents to parse and for which links will be modified to point to the
     *            new renamed document.
     * @param childDocumentNames the list of documents whose parent field will be set to the new document name.
     * @throws XWikiException in case of an error
     */
    public void rename(String newDocumentName, List<String> backlinkDocumentNames, List<String> childDocumentNames)
        throws XWikiException
    {
        List<DocumentReference> backlinkDocumentReferences = new ArrayList<DocumentReference>();
        for (String backlinkDocumentName : backlinkDocumentNames) {
            backlinkDocumentReferences.add(getCurrentMixedDocumentReferenceResolver().resolve(backlinkDocumentName));
        }

        List<DocumentReference> childDocumentReferences = new ArrayList<DocumentReference>();
        for (String childDocumentName : childDocumentNames) {
            childDocumentReferences.add(getCurrentMixedDocumentReferenceResolver().resolve(childDocumentName));
        }

        rename(getCurrentMixedDocumentReferenceResolver().resolve(newDocumentName), backlinkDocumentReferences,
            childDocumentReferences);
    }

    /**
     * Same as {@link #rename(String, List)} but the list of documents having the current document as their parent is
     * passed in parameter.
     *
     * @param newReference the reference to the new document
     * @param backlinkDocumentNames the list of reference to documents to parse and for which links will be modified to
     *            point to the new renamed document
     * @param childDocumentNames the list of references to documents whose parent field will be set to the new document
     *            reference
     * @throws XWikiException in case of an error
     * @since 2.3M2
     */
    public void rename(DocumentReference newReference, List<DocumentReference> backlinkDocumentNames,
        List<DocumentReference> childDocumentNames) throws XWikiException
    {
        XWiki xWiki = this.context.getWiki();
        if (hasAccessLevel("delete") && xWiki.checkAccess("edit",
            xWiki.getDocument(newReference, this.context), this.context)) {

            // Every page given in childDocumentNames has it's parent changed whether it needs it or not.
            // Let's make sure the user has edit permission on any page given which is not actually a child.
            // Otherwise it would be embarrassing if a user called:
            // $doc.rename("mynewpage",$doc.getBacklinks(),$xwiki.searchDocuments("true"))
            int counter = childDocumentNames.size();
            List<String> actuallyChildren = getChildren();
            while (counter > 0) {
                counter--;
                if (!actuallyChildren.contains(childDocumentNames.get(counter))
                    && !xWiki.checkAccess("edit",
                        xWiki.getDocument(childDocumentNames.get(counter), this.context),
                        this.context)) {
                    return;
                }
            }
            xWiki.renameDocument(getDocumentReference(), newReference, true, backlinkDocumentNames, childDocumentNames,
                    this.context);
        }
    }

    /**
     * Allow to easily access any revision of a document
     *
     * @param revision the version to access
     * @return the document corresponding to the requested revision or {@code null} if the revision does not exist or
     * access is denied.
     */
    public Document getDocumentRevision(String revision)
    {
        try {
            DocumentRevisionProvider revisionProvider = getDocumentRevisionProvider();
            revisionProvider.checkAccess(Right.VIEW, CurrentUserReference.INSTANCE, getDocumentReference(), revision);
            XWikiDocument documentRevision = revisionProvider.getRevision(this.doc, revision);

            return documentRevision != null ? new Document(documentRevision, this.context) : null;
        } catch (AuthorizationException e) {
            LOGGER.info("Access denied for loading revision [{}] of document [{}]: [{}]", revision,
                getDocumentReferenceWithLocale(), ExceptionUtils.getRootCauseMessage(e));
        } catch (Exception e) {
            LOGGER.error("Failed to load revision [{}] of document [{}]", revision, getDocumentReferenceWithLocale(),
                e);
        }

        return null;
    }

    /**
     * Allow to easily access the previous revision of a document
     *
     * @return Document
     * @throws XWikiException
     */
    public Document getPreviousDocument() throws XWikiException
    {
        return getDocumentRevision(getPreviousVersion());
    }

    /**
     * @return is document most recent. false if and only if there are older versions of this document.
     */
    public boolean isMostRecent()
    {
        return this.doc.isMostRecent();
    }

    /**
     * @return if rendering transformations shall be executed in restricted mode and the title not be executed
     * @since 14.10.7
     * @since 15.2RC1
     */
    @Unstable
    public boolean isRestricted()
    {
        return this.doc.isRestricted();
    }

    @Override
    public String toString()
    {
        return this.doc.toString();
    }

    /**
     * Convert the current document content from its current syntax to the new syntax passed as parameter.
     *
     * @param targetSyntaxId the syntax to convert to (eg "xwiki/2.0", "xhtml/1.0", etc)
     * @throws XWikiException if an exception occurred during the conversion process
     */
    public boolean convertSyntax(String targetSyntaxId) throws XWikiException
    {
        try {
            getDoc().convertSyntax(targetSyntaxId, this.context);
        } catch (Exception ex) {
            LOGGER.error(
                "Failed to convert document [" + getPrefixedFullName() + "] to syntax [" + targetSyntaxId + "]", ex);

            return false;
        } finally {
            updateAuthor();
            updateContentAuthor();
        }

        return true;
    }

    /**
     * Indicates whether the document is 'hidden' or not, meaning that it should not be returned in public search
     * results or appear in the User Interface in general.
     *
     * @return <code>true</code> if the document is hidden, <code>false</code> otherwise.
     */
    public boolean isHidden()
    {
        return this.doc.isHidden();
    }

    /**
     * Indicates whether the document should be 'hidden' or not, meaning that it should not be returned in public search
     * results or appear in the User Interface in general.
     *
     * @param hidden <code>true</code> if the document should be 'hidden', <code>false</code> otherwise.
     */
    public void setHidden(boolean hidden)
    {
        this.doc.setHidden(hidden);
    }

    /**
     * @return {@code true} if required rights defined in a {@code XWiki.RequiredRightClass} object shall be
     * enforced, meaning that editing will be limited to users with these rights and content of this document can't
     * use more rights than defined in the object, {@code false} otherwise
     * @since 16.6.0RC1
     */
    @Unstable
    public boolean isEnforceRequiredRights()
    {
        return this.doc.isEnforceRequiredRights();
    }

    /**
     * @param enforceRequiredRights if required rights defined in a {@code XWiki.RequiredRightClass} object shall be
     * enforced, meaning that editing will be limited to users with these rights and content of this document can't use
     * more rights than defined in the object
     * @since 16.6.0RC1
     */
    @Unstable
    public void setEnforceRequiredRights(boolean enforceRequiredRights)
    {
        getDoc().setEnforceRequiredRights(enforceRequiredRights);

        updateAuthor();

        updateContentAuthor();
    }

    /**
     * Drop permissions for the remainder of the rendering cycle. After this is called:
     * <ul>
     * <li>1. {@link com.xpn.xwiki.api.Api#hasProgrammingRights()} will always return false.</li>
     * <li>2. {@link com.xpn.xwiki.api.XWiki#getDocumentAsAuthor(org.xwiki.model.reference.DocumentReference)},
     * {@link com.xpn.xwiki.api.XWiki#getDocumentAsAuthor(String)}, {@link com.xpn.xwiki.api.Document#saveAsAuthor()},
     * {@link com.xpn.xwiki.api.Document#saveAsAuthor(String)},
     * {@link com.xpn.xwiki.api.Document#saveAsAuthor(String, boolean)}, and
     * {@link com.xpn.xwiki.api.Document#deleteAsAuthor()} will perform all of their actions as if the document's
     * content author was the guest user (XWiki.XWikiGuest).</li>
     * </ul>
     * <p>
     * This sandboxing will expire at the end of the rendering cycle and can be suspended by beginning a new rendering
     * cycle. A rendering cycle can be begin by calling {@link #getRenderedContent(String)}, {@link #display(String)}
     * (or variations thereof) or by invoking the include macro or using
     * {@link com.xpn.xwiki.api.XWiki#includeTopic(String)}
     * <p>
     * NOTE: Even if you include the same document, permissions will be regained. What this does is sandbox the
     * remainder of the code on the page because although it can temporarily suspend the permissions drop, it cannot get
     * itself to be executed with permissions because if it calls itself, it will hit the drop function first.
     * <p>
     * If you are interested in a more secure sandboxing method where code is guaranteed not to have permissions for the
     * remainder of the request, you should consider {@link com.xpn.xwiki.api.Context#dropPermissions()}.
     * <p>
     *
     * @since 3.2M2
     */
    public void dropPermissions()
    {
        // Set the droppedPermissions key to the context so if the context is cloned and
        // pushed, it will return false until it is popped again.
        final ExecutionContext context = Utils.getComponent(Execution.class).getContext();
        context.setProperty(XWikiConstant.DROPPED_PERMISSIONS, System.identityHashCode(context));
    }

    /**
     * @return true if this document is a translation of the main document (i.e. returned by
     *         {@link #getTranslatedDocument(String)}); false if this is actually the main document (i.e. returned by
     *         {@link com.xpn.xwiki.api.XWiki#getDocument(DocumentReference)}.
     * @since 6.2M2
     */
    public boolean isTranslation()
    {
        return 1 == this.getDoc().getTranslation();
    }

    /**
     * @return the maximum authorized length for a document full name (see {@link #getFullName()}).
     * @since 11.4RC1
     */
    public int getLocalReferenceMaxLength()
    {
        return this.doc.getLocalReferenceMaxLength();
    }

    /**
     * @return the authors of the document.
     * @since 14.0RC1
     */
    public DocumentAuthors getAuthors()
    {
        if (this.hasAccess(Right.PROGRAM)) {
            // We're using getDoc here to ensure to have a cloned doc
            return getDoc().getAuthors();
        } else {
            // in this case we don't care if the doc is cloned or not since it's readonly
            return new SafeDocumentAuthors(this.doc.getAuthors());
        }
    }

    /**
     * You need to have programming right to use this API.
     * <p>
     * Update the author of the document. It's the recommended way to update it if you don't fully understand the
     * various types of authors exposed by {@link #getAuthor()}.
     * <p>
     * What will happen in practice is the following:
     * <ul>
     * <li>the effective and original metadata authors are set to the passed reference</li>
     * <li>when saving the document, if the content is modified (the content dirty flag is true) then the content author
     * will also be updated to the passed reference</li>
     * </ul>
     * 
     * @param userReference the reference of the new author of the document
     * @throws AccessDeniedException when the current author is not allowed to use this API
     * @since 16.1.0RC1
     */
    @Unstable
    public void setAuthor(UserReference userReference) throws AccessDeniedException
    {
        getContextualAuthorizationManager().checkAccess(Right.PROGRAM);

        getDoc().setAuthor(userReference);
    }
}
