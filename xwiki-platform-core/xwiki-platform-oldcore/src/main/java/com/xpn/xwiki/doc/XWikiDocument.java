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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.lang.ref.SoftReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.suigeneris.jrcs.diff.Diff;
import org.suigeneris.jrcs.diff.DifferentiationFailedException;
import org.suigeneris.jrcs.diff.Revision;
import org.suigeneris.jrcs.diff.delta.Delta;
import org.suigeneris.jrcs.rcs.Version;
import org.suigeneris.jrcs.util.ToString;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.Block.Axes;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.LinkBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.SectionBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.MacroBlockMatcher;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.listener.MetaData;
import org.xwiki.rendering.listener.reference.DocumentResourceReference;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;
import org.xwiki.rendering.transformation.TransformationManager;
import org.xwiki.rendering.util.ParserUtils;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.CoreConfiguration;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConstant;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.DocumentSection;
import com.xpn.xwiki.content.Link;
import com.xpn.xwiki.content.parsers.DocumentParser;
import com.xpn.xwiki.content.parsers.RenamePageReplaceLinkHandler;
import com.xpn.xwiki.content.parsers.ReplacementResultCollection;
import com.xpn.xwiki.criteria.impl.RevisionCriteria;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xpn.xwiki.internal.cache.rendering.RenderingCache;
import com.xpn.xwiki.internal.xml.DOMXMLWriter;
import com.xpn.xwiki.internal.xml.XMLWriter;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.ObjectDiff;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.TextAreaClass;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.validation.XWikiValidationInterface;
import com.xpn.xwiki.validation.XWikiValidationStatus;
import com.xpn.xwiki.web.EditForm;
import com.xpn.xwiki.web.ObjectAddForm;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiMessageTool;
import com.xpn.xwiki.web.XWikiRequest;

public class XWikiDocument implements DocumentModelBridge
{
    private static final Logger LOGGER = LoggerFactory.getLogger(XWikiDocument.class);

    /**
     * Regex Pattern to recognize if there's HTML code in a XWiki page.
     */
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile(
        "</?+(html|img|a|i|br?|embed|script|form|input|textarea|object|font|li|[dou]l|table|center|hr|p) ?([^>]*+)>");

    /** Regex for finding the first level 1 or 2 heading in the document title, to be used as the document title. */
    private static final Pattern HEADING_PATTERN_10 = Pattern.compile("^\\s*+1(?:\\.1)?\\s++(.++)$", Pattern.MULTILINE);

    private String title;

    /**
     * Reference to this document's parent.
     * <p>
     * Note that we're saving the parent reference as a relative reference instead of an absolute one because We want
     * the ability (for example) to create a parent reference relative to the current space or wiki so that a copy of
     * this XWikiDocument object would retain that relativity. This is for example useful when copying a Wiki into
     * another Wiki so that the copied XWikiDcoument's parent reference points to the new wiki.
     */
    private EntityReference parentReference;

    /**
     * Cache the parent reference resolved as an absolute reference for improved performance (so that we don't have to
     * resolve the relative reference every time getParentReference() is called.
     */
    private DocumentReference parentReferenceCache;

    private DocumentReference documentReference;

    private String content;

    private String meta;

    private String format;

    /**
     * First author of the document.
     */
    private DocumentReference creatorReference;

    /**
     * The Author is changed when any part of the document changes (content, objects, attachments).
     */
    private DocumentReference authorReference;

    /**
     * The last user that has changed the document's content (ie not object, attachments). The Content author is only
     * changed when the document content changes. Note that Content Author is used to check programming rights on a
     * document and this is the reason we need to know the last author who's modified the content since programming
     * rights depend on this.
     */
    private DocumentReference contentAuthorReference;

    private String customClass;

    private Date contentUpdateDate;

    private Date updateDate;

    private Date creationDate;

    protected Version version;

    private long id = 0;

    private boolean mostRecent = true;

    private boolean isNew = true;

    /**
     * The reference to the document that is the template for the current document.
     * 
     * @todo this field is not used yet since it's not currently saved in the database.
     */
    private DocumentReference templateDocumentReference;

    protected String language;

    private String defaultLanguage;

    private int translation;

    /**
     * Indicates whether the document is 'hidden', meaning that it should not be returned in public search results.
     * WARNING: this is a temporary hack until the new data model is designed and implemented. No code should rely on or
     * use this property, since it will be replaced with a generic metadata.
     */
    private boolean hidden = false;

    /**
     * Comment on the latest modification.
     */
    private String comment;

    /**
     * Wiki syntax supported by this document. This is used to support different syntaxes inside the same wiki. For
     * example a page can use the Confluence 2.0 syntax while another one uses the XWiki 1.0 syntax. In practice our
     * first need is to support the new rendering component. To use the old rendering implementation specify a
     * "xwiki/1.0" syntaxId and use a "xwiki/2.0" syntaxId for using the new rendering component.
     */
    private Syntax syntax;

    /**
     * Is latest modification a minor edit.
     */
    private boolean isMinorEdit = false;

    /**
     * Used to make sure the MetaData String is regenerated.
     */
    private boolean isContentDirty = true;

    /**
     * Used to make sure the MetaData String is regenerated.
     */
    private boolean isMetaDataDirty = true;

    public static final int HAS_ATTACHMENTS = 1;

    public static final int HAS_OBJECTS = 2;

    public static final int HAS_CLASS = 4;

    private int elements = HAS_OBJECTS | HAS_ATTACHMENTS;

    /**
     * Separator string between database name and space name.
     */
    public static final String DB_SPACE_SEP = ":";

    /**
     * Separator string between space name and page name.
     */
    public static final String SPACE_NAME_SEP = ".";

    // Meta Data
    private BaseClass xClass;

    private String xClassXML;

    /**
     * Map holding document objects indexed by XClass references (i.e. Document References since a XClass reference
     * points to a document). The map is not synchronized, and uses a TreeMap implementation to preserve index ordering
     * (consistent sorted order for output to XML, rendering in velocity, etc.)
     */
    private Map<DocumentReference, List<BaseObject>> xObjects = new TreeMap<DocumentReference, List<BaseObject>>();

    private List<XWikiAttachment> attachmentList;

    // Caching
    private boolean fromCache = false;

    private List<BaseObject> xObjectsToRemove = new ArrayList<BaseObject>();

    /**
     * The view template (vm file) to use. When not set the default view template is used.
     * 
     * @see com.xpn.xwiki.web.ViewAction#render(XWikiContext)
     */
    private String defaultTemplate;

    private String validationScript;

    private Object wikiNode;

    /**
     * We are using a SoftReference which will allow the archive to be discarded by the Garbage collector as long as the
     * context is closed (usually during the request)
     */
    private SoftReference<XWikiDocumentArchive> archive;

    private XWikiStoreInterface store;

    /**
     * @see #getOriginalDocument()
     */
    private XWikiDocument originalDocument;

    /**
     * The document structure expressed as a tree of Block objects. We store it for performance reasons since parsing is
     * a costly operation that we don't want to repeat whenever some code ask for the XDOM information.
     */
    private XDOM xdom;

    /**
     * Used to resolve a string into a proper Document Reference using the current document's reference to fill the
     * blanks.
     */
    @SuppressWarnings("unchecked")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.class, "current");

    /**
     * Used to resolve a string into a proper Document Reference.
     */
    @SuppressWarnings("unchecked")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.class, "explicit");

    @SuppressWarnings("unchecked")
    private EntityReferenceResolver<String> xClassEntityReferenceResolver = Utils.getComponent(
        EntityReferenceResolver.class, "xclass");

    /**
     * Used to resolve a string into a proper Document Reference using the current document's reference to fill the
     * blanks, except for the page name for which the default page name is used instead and for the wiki name for which
     * the current wiki is used instead of the current document reference's wiki.
     */
    @SuppressWarnings("unchecked")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.class, "currentmixed");

    /**
     * Used to normalize references.
     */
    @SuppressWarnings("unchecked")
    private DocumentReferenceResolver<EntityReference> currentReferenceDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.class, "current/reference");

    /**
     * Used to normalize references.
     */
    @SuppressWarnings("unchecked")
    private DocumentReferenceResolver<EntityReference> explicitReferenceDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.class, "explicit/reference");

    /**
     * Used to resolve parent references in the way they are stored externally (database, xml, etc), ie relative or
     * absolute.
     */
    @SuppressWarnings("unchecked")
    private EntityReferenceResolver<String> relativeEntityReferenceResolver = Utils.getComponent(
        EntityReferenceResolver.class, "relative");

    /**
     * Used to convert a proper Document Reference to string (compact form).
     */
    @SuppressWarnings("unchecked")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer = Utils.getComponent(
        EntityReferenceSerializer.class, "compact");

    /**
     * Used to convert a proper Document Reference to string (standard form).
     */
    @SuppressWarnings("unchecked")
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer = Utils
        .getComponent(EntityReferenceSerializer.class);

    /**
     * Used to convert a Document Reference to string (compact form without the wiki part if it matches the current
     * wiki).
     */
    @SuppressWarnings("unchecked")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer = Utils.getComponent(
        EntityReferenceSerializer.class, "compactwiki");

    /**
     * Used to convert a proper Document Reference to a string but without the wiki name.
     */
    @SuppressWarnings("unchecked")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer = Utils.getComponent(
        EntityReferenceSerializer.class, "local");

    /**
     * Used to emulate an inline parsing.
     */
    private ParserUtils parserUtils = new ParserUtils();

    /**
     * Used to create proper {@link Syntax} objects.
     */
    private SyntaxFactory syntaxFactory = Utils.getComponent(SyntaxFactory.class);

    private RenderingCache renderingCache = Utils.getComponent(RenderingCache.class);

    /**
     * @since 2.2M1
     */
    public XWikiDocument(DocumentReference reference)
    {
        init(reference);
    }

    /**
     * @deprecated since 2.2M1 use {@link #XWikiDocument(org.xwiki.model.reference.DocumentReference)} instead
     */
    @Deprecated
    public XWikiDocument()
    {
        this(null);
    }

    /**
     * Constructor that specifies the local document identifier: space name, document name. {@link #setDatabase(String)}
     * must be called afterwards to specify the wiki name.
     * 
     * @param space the space this document belongs to
     * @param name the name of the document
     * @deprecated since 2.2M1 use {@link #XWikiDocument(org.xwiki.model.reference.DocumentReference)} instead
     */
    @Deprecated
    public XWikiDocument(String space, String name)
    {
        this(null, space, name);
    }

    /**
     * Constructor that specifies the full document identifier: wiki name, space name, document name.
     * 
     * @param wiki The wiki this document belongs to.
     * @param space The space this document belongs to.
     * @param name The name of the document (can contain either the page name or the space and page name)
     * @deprecated since 2.2M1 use {@link #XWikiDocument(org.xwiki.model.reference.DocumentReference)} instead
     */
    @Deprecated
    public XWikiDocument(String wiki, String space, String name)
    {
        // We allow to specify the space in the name (eg name = "space.page"). In this case the passed space is
        // ignored.

        // Build an entity reference that will serve as a current context reference against which to resolve if the
        // passed name doesn't contain a space.
        EntityReference contextReference = null;
        if (!StringUtils.isEmpty(space)) {
            contextReference = new EntityReference(space, EntityType.SPACE);
            if (!StringUtils.isEmpty(wiki)) {
                contextReference.setParent(new WikiReference(wiki));
            }
        } else if (!StringUtils.isEmpty(wiki)) {
            contextReference = new WikiReference(wiki);
        }

        DocumentReference reference;
        if (contextReference != null) {
            reference = this.currentDocumentReferenceResolver.resolve(name, contextReference);
            // Replace the resolved wiki by the passed wiki if not empty/null
            if (!StringUtils.isEmpty(wiki)) {
                reference.setWikiReference(new WikiReference(wiki));
            }
        } else {
            // Both the wiki and space params are empty/null, thus don't use a context reference.
            reference = this.currentDocumentReferenceResolver.resolve(name);
        }

        init(reference);
    }

    public XWikiStoreInterface getStore(XWikiContext context)
    {
        return context.getWiki().getStore();
    }

    public XWikiAttachmentStoreInterface getAttachmentStore(XWikiContext context)
    {
        return context.getWiki().getAttachmentStore();
    }

    public XWikiVersioningStoreInterface getVersioningStore(XWikiContext context)
    {
        return context.getWiki().getVersioningStore();
    }

    public XWikiStoreInterface getStore()
    {
        return this.store;
    }

    public void setStore(XWikiStoreInterface store)
    {
        this.store = store;
    }

    /**
     * @return the unique id used to represent the document, as a number. This id is technical and is equivalent to the
     *         Document Reference + the language of the Document. This technical id should only be used for the storage
     *         layer and all user APIs should instead use Document Reference and language as they are model-related
     *         while the id isn't (it's purely technical).
     */
    public long getId()
    {
        // TODO: The implemented below doesn't guarantee a unique id since it uses the hashCode() method which doesn't
        // guarantee unicity. From the JDK's javadoc: "It is not required that if two objects are unequal according to
        // the equals(java.lang.Object) method, then calling the hashCode method on each of the two objects must
        // produce distinct integer results.". This needs to be fixed to produce a real unique id since otherwise we
        // can have clashes in the database.

        // Note: We don't use the wiki name in the document id's computation. The main historical reason is so
        // that all things saved in a given wiki's database are always stored relative to that wiki so that
        // changing that wiki's name is simpler.
        if ((this.language == null) || this.language.trim().equals("")) {
            this.id = this.localEntityReferenceSerializer.serialize(getDocumentReference()).hashCode();
        } else {
            this.id = (this.localEntityReferenceSerializer.serialize(getDocumentReference()) + ":" + this.language)
                .hashCode();
        }

        return this.id;
    }

    /**
     * @see #getId()
     */
    public void setId(long id)
    {
        this.id = id;
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for saving a XWikiDocument.
     * 
     * @return the name of the space of the document
     * @deprecated since 2.2M1 used {@link #getDocumentReference()} instead
     */
    @Deprecated
    public String getSpace()
    {
        return getDocumentReference().getLastSpaceReference().getName();
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for loading a XWikiDocument.
     * 
     * @deprecated since 2.2M1 used {@link #setDocumentReference(DocumentReference)} instead
     */
    @Deprecated
    public void setSpace(String space)
    {
        if (space != null) {
            getDocumentReference().getLastSpaceReference().setName(space);

            // Clean the absolute parent reference cache to rebuild it next time getParentReference is called.
            this.parentReferenceCache = null;
        }
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for saving a XWikiDocument.
     * 
     * @return the name of the space of the document
     * @deprecated use {@link #getDocumentReference()} instead
     */
    @Deprecated
    public String getWeb()
    {
        return getSpace();
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for loading a XWikiDocument.
     * 
     * @deprecated use {@link #setDocumentReference(DocumentReference)} instead
     */
    @Deprecated
    public void setWeb(String space)
    {
        setSpace(space);
    }

    public String getVersion()
    {
        return getRCSVersion().toString();
    }

    public void setVersion(String version)
    {
        if (!StringUtils.isEmpty(version)) {
            this.version = new Version(version);
        }
    }

    public Version getRCSVersion()
    {
        if (this.version == null) {
            return new Version("1.1");
        }
        return this.version;
    }

    public void setRCSVersion(Version version)
    {
        this.version = version;
    }

    /**
     * @return the copy of this XWikiDocument instance before any modification was made to it. It is reset to the actual
     *         values when the document is saved in the database. This copy is used for finding out differences made to
     *         this document (useful for example to send the correct notifications to document change listeners).
     */
    public XWikiDocument getOriginalDocument()
    {
        return this.originalDocument;
    }

    /**
     * @param originalDocument the original document representing this document instance before any change was made to
     *            it, prior to the last time it was saved
     * @see #getOriginalDocument()
     */
    public void setOriginalDocument(XWikiDocument originalDocument)
    {
        this.originalDocument = originalDocument;
    }

    /**
     * @return the parent reference or null if the parent is not set
     * @since 2.2M1
     */
    public DocumentReference getParentReference()
    {
        // Ensure we always return absolute document references for the parent since we always want well-constructed
        // references and since we store the parent reference as relative internally.
        if (this.parentReferenceCache == null && getRelativeParentReference() != null) {
            this.parentReferenceCache = this.explicitReferenceDocumentReferenceResolver.resolve(
                getRelativeParentReference(), getDocumentReference());
        }

        return this.parentReferenceCache;
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for saving a XWikiDocument.
     * 
     * @return the parent reference stored in the database, which is relative to this document, or an empty string ("")
     *         if the parent is not set
     * @see #getParentReference()
     * @deprecated since 2.2M1 use {@link #getParentReference()} instead
     */
    @Deprecated
    public String getParent()
    {
        String parentReferenceAsString;
        if (getParentReference() != null) {
            parentReferenceAsString = this.defaultEntityReferenceSerializer.serialize(getRelativeParentReference());
        } else {
            parentReferenceAsString = "";
        }
        return parentReferenceAsString;
    }

    /**
     * @deprecated since 2.2M1 use {@link #getParentReference()} instead
     */
    @Deprecated
    public XWikiDocument getParentDoc()
    {
        return new XWikiDocument(getParentReference());
    }

    /**
     * @since 2.2.3
     */
    public void setParentReference(EntityReference parentReference)
    {
        if ((parentReference == null && getRelativeParentReference() != null)
            || (parentReference != null && !parentReference.equals(getRelativeParentReference()))) {
            this.parentReference = parentReference;

            // Clean the absolute parent reference cache to rebuild it next time getParentReference is called.
            this.parentReferenceCache = null;

            setMetaDataDirty(true);
        }
    }

    /**
     * Convert a full document reference into the proper relative document reference (wiki part is removed if it's the
     * same as document wiki) to store as parent.
     * 
     * @deprecated since 2.2.3 use {@link #setParentReference(org.xwiki.model.reference.EntityReference)} instead
     */
    @Deprecated
    public void setParentReference(DocumentReference parentReference)
    {
        if (parentReference != null) {
            setParent(serializeReference(parentReference, this.compactWikiEntityReferenceSerializer,
                getDocumentReference()));
        } else {
            setParentReference((EntityReference) null);
        }
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for loading a XWikiDocument.
     * 
     * @param parent the reference of the parent relative to the document
     * @deprecated since 2.2M1 used {@link #setParentReference(DocumentReference)} instead
     */
    @Deprecated
    public void setParent(String parent)
    {
        // If the passed parent is an empty string we also need to set the reference to null. The reason is that
        // in the database we store "" when the parent is empty and thus when Hibernate loads this class it'll call
        // setParent with "" if the parent had not been set when saved.
        if (StringUtils.isEmpty(parent)) {
            setParentReference((EntityReference) null);
        } else {
            setParentReference(this.relativeEntityReferenceResolver.resolve(parent, EntityType.DOCUMENT));
        }
    }

    public String getContent()
    {
        return this.content;
    }

    public void setContent(String content)
    {
        if (content == null) {
            content = "";
        }
        if (!content.equals(this.content)) {
            setContentDirty(true);
            setWikiNode(null);
        }
        this.content = content;

        // invalidate parsed xdom
        this.xdom = null;
    }

    public void setContent(XDOM content) throws XWikiException
    {
        setContent(renderXDOM(content, getSyntax()));
    }

    public String getRenderedContent(Syntax targetSyntax, XWikiContext context) throws XWikiException
    {
        return getRenderedContent(targetSyntax, true, context);
    }

    public String getRenderedContent(Syntax targetSyntax, boolean isolateVelocityMacros, XWikiContext context)
        throws XWikiException
    {
        // Note: We are currently duplicating code from the other getRendered signature because some calling
        // code is expecting that the rendering will happen in the calling document's context and not in this
        // document's context. For example this is true for the Admin page, see
        // http://jira.xwiki.org/jira/browse/XWIKI-4274 for more details.

        String content = getTranslatedContent(context);

        String renderedContent = this.renderingCache.getRenderedContent(getDocumentReference(), content, context);

        String documentName = this.defaultEntityReferenceSerializer.serialize(
            isolateVelocityMacros ? getDocumentReference() : context.getDoc().getDocumentReference());

        if (renderedContent == null) {
            Object isInRenderingEngine = context.get("isInRenderingEngine");

            // Mark that we're starting to use the current document as a macro namespace
            if (isolateVelocityMacros && (isInRenderingEngine == null || isInRenderingEngine == Boolean.FALSE)) {
                try {
                    Utils.getComponent(VelocityManager.class).getVelocityEngine()
                        .startedUsingMacroNamespace(documentName);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Started using velocity macro namespace [" + documentName + "]");
                    }
                } catch (XWikiVelocityException e) {
                    // Failed to get the Velocity Engine and this to clear Velocity Macro cache. Log this as a warning
                    // but continue since it's not absolutely critical.
                    LOGGER.warn("Failed to notify Velocity Macro cache for the [" + documentName
                        + "] namespace. Reason = [" + e.getMessage() + "]");
                }
            }

            try {
                // This tells display() methods that we are inside the rendering engine and thus
                // that they can return wiki syntax and not HTML syntax (which is needed when
                // outside the rendering engine, i.e. when we're inside templates using only
                // Velocity for example).
                context.put("isInRenderingEngine", true);

                // If the Syntax id is "xwiki/1.0" then use the old rendering subsystem. Otherwise use the new one.
                if (is10Syntax()) {
                    renderedContent = context.getWiki().getRenderingEngine().renderDocument(this, context);
                } else {
                    TransformationContext txContext = new TransformationContext();
                    txContext.setSyntax(getSyntax());
                    txContext.setId(documentName);
                    renderedContent = performSyntaxConversion(content, documentName, targetSyntax, txContext);
                }

                this.renderingCache.setRenderedContent(getDocumentReference(), content, renderedContent, context);
            } finally {
                if (isInRenderingEngine != null) {
                    context.put("isInRenderingEngine", isInRenderingEngine);
                } else {
                    context.remove("isInRenderingEngine");
                }

                // Since we configure Velocity to have local macros (i.e. macros visible only to the local context),
                // since Velocity caches the velocity macros in a local cache (we use key which is the absolute
                // document reference) and since documents can include other documents or panels, we need to make sure
                // we empty the local Velocity macro cache at the end of the rendering for the document as otherwise the
                // local Velocity macro caches will keep growing as users create new pages.
                //
                // Note that we check if we are in the rendering engine as this cleanup must be done only once after the
                // document has been rendered but this method can be called recursively. We know it's the initial entry
                // point when isInRenderingEngine is false...
                if (isolateVelocityMacros && (isInRenderingEngine == null || isInRenderingEngine == Boolean.FALSE)) {
                    try {
                        Utils.getComponent(VelocityManager.class).getVelocityEngine()
                            .stoppedUsingMacroNamespace(documentName);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Stopped using velocity macro namespace [" + documentName + "]");
                        }
                    } catch (XWikiVelocityException e) {
                        // Failed to get the Velocity Engine and this to clear Velocity Macro cache. Log this as a
                        // warning
                        // but continue since it's not absolutely critical.
                        LOGGER.warn("Failed to notify Velocity Macro cache for the [" + documentName
                            + "] namespace. Reason = [" + e.getMessage() + "]");
                    }
                }
            }
        }

        return renderedContent;
    }

    public String getRenderedContent(XWikiContext context) throws XWikiException
    {
        return getRenderedContent(Syntax.XHTML_1_0, context);
    }

    /**
     * @param text the text to render
     * @param syntaxId the id of the Syntax used by the passed text (for example: "xwiki/1.0")
     * @param context the XWiki Context object
     * @return the given text rendered in the context of this document using the passed Syntax
     * @since 1.6M1
     */
    public String getRenderedContent(String text, String syntaxId, XWikiContext context)
    {
        return getRenderedContent(text, syntaxId, Syntax.XHTML_1_0.toIdString(), context);
    }

    /**
     * @param text the text to render
     * @param sourceSyntaxId the id of the Syntax used by the passed text (for example: "xwiki/1.0")
     * @param targetSyntaxId the id of the syntax in which to render the document content
     * @return the given text rendered in the context of this document using the passed Syntax
     * @since 2.0M3
     */
    public String getRenderedContent(String text, String sourceSyntaxId, String targetSyntaxId, XWikiContext context)
    {
        String result = this.renderingCache.getRenderedContent(getDocumentReference(), text, context);

        String documentName = this.defaultEntityReferenceSerializer.serialize(getDocumentReference());

        if (result == null) {
            Map<String, Object> backup = new HashMap<String, Object>();
            Object isInRenderingEngine = context.get("isInRenderingEngine");
            try {
                backupContext(backup, context);
                setAsContextDoc(context);

                // Mark that we're starting to use the current document as a macro namespace
                if (isInRenderingEngine == null || isInRenderingEngine == Boolean.FALSE) {
                    try {
                        Utils.getComponent(VelocityManager.class).getVelocityEngine()
                            .startedUsingMacroNamespace(documentName);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Started using velocity macro namespace [" + documentName + "]");
                        }
                    } catch (XWikiVelocityException e) {
                        // Failed to get the Velocity Engine and this to clear Velocity Macro cache. Log this as a
                        // warning
                        // but continue since it's not absolutely critical.
                        LOGGER.warn("Failed to notify Velocity Macro cache for the [" + documentName
                            + "] namespace. Reason = [" + e.getMessage() + "]");
                    }
                }

                // This tells display() methods that we are inside the rendering engine and thus
                // that they can return wiki syntax and not HTML syntax (which is needed when
                // outside the rendering engine, i.e. when we're inside templates using only
                // Velocity for example).
                context.put("isInRenderingEngine", true);

                // If the Syntax id is "xwiki/1.0" then use the old rendering subsystem. Otherwise use the new one.
                if (is10Syntax(sourceSyntaxId)) {
                    result = context.getWiki().getRenderingEngine().renderText(text, this, context);
                } else {
                    SyntaxFactory syntaxFactory = Utils.getComponent(SyntaxFactory.class);
                    TransformationContext txContext = new TransformationContext();
                    txContext.setSyntax(syntaxFactory.createSyntaxFromIdString(sourceSyntaxId));
                    txContext.setId(documentName);
                    result = performSyntaxConversion(text, documentName,
                        syntaxFactory.createSyntaxFromIdString(targetSyntaxId), txContext);
                }

                this.renderingCache.setRenderedContent(getDocumentReference(), text, result, context);
            } catch (Exception e) {
                // Failed to render for some reason. This method should normally throw an exception but this
                // requires changing the signature of calling methods too.
                LOGGER.warn("Failed to render content [" + text + "]", e);
                result = "";
            } finally {
                restoreContext(backup, context);
                if (isInRenderingEngine != null) {
                    context.put("isInRenderingEngine", isInRenderingEngine);
                } else {
                    context.remove("isInRenderingEngine");
                }

                // Since we configure Velocity to have local macros (i.e. macros visible only to the local context),
                // since Velocity caches the velocity macros in a local cache (we use key which is the absolute
                // document reference) and since documents can include other documents or panels, we need to make sure
                // we empty the local Velocity macro cache at the end of the rendering for the document as otherwise the
                // local Velocity macro caches will keep growing as users create new pages.
                //
                // Note that we check if we are in the rendering engine as this cleanup must be done only once after the
                // document has been rendered but this method can be called recursively. We know it's the initial entry
                // point when isInRenderingEngine is false...
                if (isInRenderingEngine == null || isInRenderingEngine == Boolean.FALSE) {
                    try {
                        Utils.getComponent(VelocityManager.class).getVelocityEngine()
                            .stoppedUsingMacroNamespace(documentName);
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Stopped using velocity macro namespace [" + documentName + "]");
                        }
                    } catch (XWikiVelocityException e) {
                        // Failed to get the Velocity Engine and this to clear Velocity Macro cache. Log this as a
                        // warning
                        // but continue since it's not absolutely critical.
                        LOGGER.warn("Failed to notify Velocity Macro cache for the [" + documentName
                            + "] namespace. Reason = [" + e.getMessage() + "]");
                    }
                }
            }
        }

        return result;
    }

    public String getEscapedContent(XWikiContext context) throws XWikiException
    {
        return XMLUtils.escape(getTranslatedContent(context));
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for saving a XWikiDocument.
     * 
     * @deprecated since 2.2M1 used {@link #getDocumentReference()} instead
     */
    @Deprecated
    public String getName()
    {
        return getDocumentReference().getName();
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for loading a XWikiDocument.
     * 
     * @deprecated since 2.2M1 used {@link #setDocumentReference(DocumentReference)} instead
     */
    @Deprecated
    public void setName(String name)
    {
        if (name != null) {
            getDocumentReference().setName(name);

            // Clean the absolute parent reference cache to rebuild it next time getParentReference is called.
            this.parentReferenceCache = null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentModelBridge#getDocumentReference()
     * @since 2.2M1
     */
    public DocumentReference getDocumentReference()
    {
        return this.documentReference;
    }

    /**
     * @return the document's space + page name (eg "space.page")
     * @deprecated since 2.2M1 use {@link #getDocumentReference()} instead
     */
    @Deprecated
    public String getFullName()
    {
        return this.localEntityReferenceSerializer.serialize(getDocumentReference());
    }

    /**
     * @return the docoument's wiki + space + page name (eg "wiki:space.page")
     * @deprecated since 2.2M1 use {@link #getDocumentReference()} instead
     */
    @Deprecated
    public String getPrefixedFullName()
    {
        return this.defaultEntityReferenceSerializer.serialize(getDocumentReference());
    }

    /**
     * @since 2.2M1
     * @deprecated since 2.2.3 don't change the reference of a document once it's been constructed. Instead you can
     *             clone the doc, rename it or copy it.
     */
    @Deprecated
    public void setDocumentReference(DocumentReference reference)
    {
        // Don't allow setting a null reference for now, ie. don't do anything to preserve backward compatibility
        // with previous behavior (i.e. {@link #setFullName}.
        if (reference != null) {
            if (!reference.equals(getDocumentReference())) {
                this.documentReference = reference;
                setMetaDataDirty(true);

                // Clean the absolute parent reference cache to rebuild it next time getParentReference is called.
                this.parentReferenceCache = null;
            }
        }
    }

    /**
     * @deprecated since 2.2M1 use {@link #setDocumentReference(org.xwiki.model.reference.DocumentReference)} instead
     */
    @Deprecated
    public void setFullName(String name)
    {
        setFullName(name, null);
    }

    /**
     * @deprecated since 2.2M1 use {@link #setDocumentReference(org.xwiki.model.reference.DocumentReference)} instead
     */
    @Deprecated
    public void setFullName(String fullName, XWikiContext context)
    {
        // We ignore the passed full name if it's null to be backward compatible with previous behaviors and to be
        // consistent with {@link #setName} and {@link #setSpace}.
        if (fullName != null) {
            // Note: We use the CurrentMixed Resolver since we want to use the default page name if the page isn't
            // specified in the passed string, rather than use the current document's page name.
            setDocumentReference(this.currentMixedDocumentReferenceResolver.resolve(fullName));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.bridge.DocumentModelBridge#getDocumentName()
     * @deprecated replaced by {@link #getDocumentReference()} since 2.2M1
     */
    @Deprecated
    public org.xwiki.bridge.DocumentName getDocumentName()
    {
        return new org.xwiki.bridge.DocumentName(getWikiName(), getSpaceName(), getPageName());
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentModelBridge#getWikiName()
     * @deprecated since 2.2M1 use {@link #getDocumentReference()} instead
     */
    @Deprecated
    public String getWikiName()
    {
        return getDatabase();
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentModelBridge#getSpaceName()
     * @deprecated since 2.2M1 use {@link #getDocumentReference()} instead
     */
    @Deprecated
    public String getSpaceName()
    {
        return this.getSpace();
    }

    /**
     * {@inheritDoc}
     * 
     * @see DocumentModelBridge#getSpaceName()
     * @deprecated since 2.2M1 use {@link #getDocumentReference()} instead
     */
    @Deprecated
    public String getPageName()
    {
        return this.getName();
    }

    public String getTitle()
    {
        return (this.title != null) ? this.title : "";
    }

    /**
     * @param context the XWiki context used to get access to the com.xpn.xwiki.render.XWikiRenderingEngine object
     * @return the document title. If a title has not been provided, look for a section title in the document's content
     *         and if not found return the page name. The returned title is also interpreted which means it's allowed to
     *         use Velocity, Groovy, etc syntax within a title.
     * @deprecated use {@link #getRenderedTitle(Syntax, XWikiContext)} instead
     */
    @Deprecated
    public String getDisplayTitle(XWikiContext context)
    {
        return getRenderedTitle(Syntax.XHTML_1_0, context);
    }

    /**
     * The first found first or second level header content is rendered with
     * {@link com.xpn.xwiki.render.XWikiRenderingEngine#interpretText(String, XWikiDocument, XWikiContext)}.
     * 
     * @param context the XWiki context
     * @return the rendered version of the found header content. Empty string if not can be found.
     */
    private String getRenderedContentTitle10(XWikiContext context)
    {
        // 1) Check if the user has provided a title
        String title = extractTitle10();

        // 3) Last if a title has been found renders it as it can contain macros, velocity code,
        // groovy, etc.
        if (title.length() > 0) {
            // Only needed for xwiki 1.0 syntax, for other syntaxes it's already rendered in #extractTitle
            // This will not completely work for scripting code in title referencing variables
            // defined elsewhere. In that case it'll only work if those variables have been
            // parsed and put in the corresponding scripting context. This will not work for
            // breadcrumbs for example.
            title = context.getWiki().getRenderingEngine().interpretText(title, this, context);
        }

        return title;
    }

    /**
     * Get the rendered version of the first or second level first found header content in the document content.
     * <ul>
     * <li>xwiki/1.0: the first found first or second level header content is rendered with
     * {@link com.xpn.xwiki.render.XWikiRenderingEngine#interpretText(String, XWikiDocument, XWikiContext)}</li>
     * <li>xwiki/2.0: the first found first or second level content is executed and rendered with renderer for the
     * provided syntax</li>
     * </ul>
     * 
     * @param outputSyntax the syntax to render to. This is not taken into account for xwiki/1.0 syntax.
     * @param context the XWiki context
     * @return the rendered version of the title. null or empty (when xwiki/1.0 syntax) string if none can be found
     * @throws XWikiException failed to render content
     */
    private String getRenderedContentTitle(Syntax outputSyntax, XWikiContext context) throws XWikiException
    {
        String title = null;

        // Protect against cycles. For example that cold happen with a call to getRenderedTitle on current document from
        // a script in the first heading block title
        @SuppressWarnings("unchecked")
        Stack<DocumentReference> stackTrace =
            (Stack<DocumentReference>) context.get("internal.getRenderedContentTitleStackTrace");
        if (stackTrace == null) {
            stackTrace = new Stack<DocumentReference>();
            context.put("internal.getRenderedContentTitleStackTrace", stackTrace);
        } else if (stackTrace.contains(getDocumentReference())) {
            // TODO: generate an error message instead ?
            return null;
        }
        stackTrace.push(getDocumentReference());

        try {
            // Extract and render the document title

            if (is10Syntax()) {
                title = getRenderedContentTitle10(context);
            } else {
                List<HeaderBlock> blocks = getXDOM().getChildrenByType(HeaderBlock.class, true);
                if (blocks.size() > 0) {
                    HeaderBlock header = blocks.get(0);
                    // Check the header depth after which we should return null if no header was found.
                    int titleHeaderDepth = (int) context.getWiki().ParamAsLong("xwiki.title.headerdepth", 2);
                    if (header.getLevel().getAsInt() <= titleHeaderDepth) {
                        XDOM headerXDOM = new XDOM(Collections.<Block> singletonList(header));

                        // Transform
                        try {
                            TransformationContext txContext = new TransformationContext(headerXDOM, getSyntax());
                            Utils.getComponent(TransformationManager.class).performTransformations(headerXDOM,
                                txContext);
                        } catch (TransformationException e) {
                            // An error happened during one of the transformations. Since the error has been logged
                            // continue
                            // TODO: We should have a visual clue for the user in the future to let him know something
                            // didn't work as expected.
                        }

                        // Render
                        Block headerBlock = headerXDOM.getChildren().get(0);
                        if (headerBlock instanceof HeaderBlock) {
                            title = renderXDOM(new XDOM(headerBlock.getChildren()), outputSyntax);
                        }
                    }
                }
            }
        } finally {
            stackTrace.pop();
        }

        return title;
    }

    /**
     * Get the rendered version of the title of the document.
     * <ul>
     * <li>if document <code>title</code> field is not empty: it's returned after a call to
     * {@link com.xpn.xwiki.render.XWikiRenderingEngine#interpretText(String, XWikiDocument, XWikiContext)}</li>
     * <li>if document <code>title</code> field is empty: see {@link #getRenderedContentTitle(Syntax, XWikiContext)}</li>
     * <li>if after the two first step the title is still empty, the page name is returned</li>
     * </ul>
     * 
     * @param outputSyntax the syntax to render to. This is not taken into account for xwiki/1.0 syntax.
     * @param context the XWiki context
     * @return the rendered version of the title
     */
    public String getRenderedTitle(Syntax outputSyntax, XWikiContext context)
    {
        // 1) Check if the user has provided a title
        String title = getTitle();

        try {
            if (!StringUtils.isEmpty(title)) {
                title = context.getWiki().getRenderingEngine().interpretText(title, this, context);

                // If there's been an error during the Velocity evaluation then consider that the title is empty as a
                // fallback.
                // TODO: Since interpretText() never throws an exception it's hard to know if there's been an error.
                // Right now interpretText() returns some HTML when there's an error, so we need to check the returned
                // result for some marker to decide if an error has occurred... Fix this by refactoring the whole
                // system used for Velocity evaluation.
                if (title.indexOf("<div id=\"xwikierror") == -1) {
                    if (!outputSyntax.equals(Syntax.HTML_4_01) && !outputSyntax.equals(Syntax.XHTML_1_0)) {
                        XDOM xdom = parseContent(Syntax.HTML_4_01.toIdString(), title);
                        this.parserUtils.removeTopLevelParagraph(xdom.getChildren());
                        title = renderXDOM(xdom, outputSyntax);
                    }

                    return title;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to interpret title of document ["
                + this.defaultEntityReferenceSerializer.serialize(getDocumentReference()) + "]", e);
        }

        try {
            // 2) If not, then try to extract the title from the first document section title
            title = getRenderedContentTitle(outputSyntax, context);
        } catch (Exception e) {
            LOGGER.warn("Failed to extract title from content of document ["
                + this.defaultEntityReferenceSerializer.serialize(getDocumentReference()) + "]", e);
        }

        // 3) No title has been found, return the page name as the title
        if (StringUtils.isEmpty(title)) {
            title = getDocumentReference().getName();
        }

        return title;
    }

    public String extractTitle()
    {
        String title = "";

        try {
            if (is10Syntax()) {
                title = extractTitle10();
            } else {
                List<HeaderBlock> blocks = getXDOM().getChildrenByType(HeaderBlock.class, true);
                if (blocks.size() > 0) {
                    HeaderBlock header = blocks.get(0);
                    if (header.getLevel().compareTo(HeaderLevel.LEVEL2) <= 0) {
                        XDOM headerXDOM = new XDOM(Collections.<Block> singletonList(header));

                        // transform
                        TransformationContext context = new TransformationContext(headerXDOM, getSyntax());
                        Utils.getComponent(TransformationManager.class).performTransformations(headerXDOM, context);

                        // render
                        Block headerBlock = headerXDOM.getChildren().get(0);
                        if (headerBlock instanceof HeaderBlock) {
                            title = renderXDOM(new XDOM(headerBlock.getChildren()), Syntax.XHTML_1_0);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Don't stop when there's a problem rendering the title.
        }

        return title;
    }

    /**
     * @return the first level 1 or level 1.1 title text in the document's content or "" if none are found
     * @todo this method has nothing to do in this class and should be moved elsewhere
     */
    private String extractTitle10()
    {
        String content = getContent();
        Matcher m = HEADING_PATTERN_10.matcher(content);
        if (m.find()) {
            return m.group(1).trim();
        }

        return "";
    }

    public void setTitle(String title)
    {
        if (title != null && !title.equals(this.title)) {
            setContentDirty(true);
        }
        this.title = title;
    }

    public String getFormat()
    {
        return this.format != null ? this.format : "";
    }

    public void setFormat(String format)
    {
        this.format = format;
        if (!format.equals(this.format)) {
            setMetaDataDirty(true);
        }
    }

    /**
     * @since 3.0M3
     */
    public DocumentReference getAuthorReference()
    {
        return this.authorReference;
    }

    /**
     * @since 3.0M3
     */
    public void setAuthorReference(DocumentReference authorReference)
    {
        this.authorReference = authorReference;
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for saving a XWikiDocument.
     * 
     * @deprecated since 3.0M3 use {@link #getAuthorReference()} instead
     */
    @Deprecated
    public String getAuthor()
    {
        String author;
        DocumentReference authorReference = getAuthorReference();
        if (authorReference == null) {
            author = "";
        } else {
            author = this.compactWikiEntityReferenceSerializer.serialize(authorReference, getDocumentReference());
        }

        return author;
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for loading a XWikiDocument.
     * 
     * @deprecated since 3.0M3 use {@link #setAuthorReference} instead
     */
    @Deprecated
    public void setAuthor(String author)
    {
        // Note: Consider "" or null as the same, i.e. the author not being set
        DocumentReference authorReference = null;
        if (author != null && author.length() > 0) {
            authorReference = this.explicitReferenceDocumentReferenceResolver.resolve(
                this.xClassEntityReferenceResolver.resolve(author, EntityType.DOCUMENT), getDocumentReference());
        }

        if ((getAuthorReference() == null && authorReference != null)
            || (getAuthorReference() != null && !getAuthorReference().equals(authorReference))) {
            setMetaDataDirty(true);
        }

        setAuthorReference(authorReference);
    }

    /**
     * @since 3.0M3
     */
    public DocumentReference getContentAuthorReference()
    {
        return this.contentAuthorReference;
    }

    /**
     * @since 3.0M3
     */
    public void setContentAuthorReference(DocumentReference contentAuthorReference)
    {
        this.contentAuthorReference = contentAuthorReference;
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for saving a XWikiDocument.
     * 
     * @deprecated since 3.0M3 use {@link #getContentAuthorReference()} instead
     */
    @Deprecated
    public String getContentAuthor()
    {
        String contentAuthor;
        DocumentReference contentAuthorReference = getContentAuthorReference();
        if (contentAuthorReference == null) {
            contentAuthor = "";
        } else {
            contentAuthor =
                this.compactWikiEntityReferenceSerializer.serialize(contentAuthorReference, getDocumentReference());
        }

        return contentAuthor;
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for loading a XWikiDocument.
     * 
     * @deprecated since 3.0M3 use {@link #setContentAuthorReference} instead
     */
    @Deprecated
    public void setContentAuthor(String contentAuthor)
    {
        // Note: Consider "" or null as the same, i.e. the content author not being set
        DocumentReference contentAuthorReference = null;
        if (contentAuthor != null && contentAuthor.length() > 0) {
            contentAuthorReference = this.explicitReferenceDocumentReferenceResolver.resolve(
                this.xClassEntityReferenceResolver.resolve(contentAuthor, EntityType.DOCUMENT), getDocumentReference());
        }

        if ((getContentAuthorReference() == null && contentAuthorReference != null)
            || (getContentAuthorReference() != null && !getContentAuthorReference().equals(contentAuthorReference))) {
            setMetaDataDirty(true);
        }

        setContentAuthorReference(contentAuthorReference);
    }

    /**
     * @since 3.0M3
     */
    public DocumentReference getCreatorReference()
    {
        return this.creatorReference;
    }

    /**
     * @since 3.0M3
     */
    public void setCreatorReference(DocumentReference creatorReference)
    {
        this.creatorReference = creatorReference;
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for saving a XWikiDocument.
     * 
     * @deprecated since 3.0M2 use {@link #getCreatorReference()} instead
     */
    @Deprecated
    public String getCreator()
    {
        String creator;
        DocumentReference creatorReference = getCreatorReference();
        if (creatorReference == null) {
            creator = "";
        } else {
            creator = this.compactWikiEntityReferenceSerializer.serialize(creatorReference, getDocumentReference());
        }

        return creator;
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for loading a XWikiDocument.
     * 
     * @deprecated since 3.0M2 use {@link #setCreatorReference} instead
     */
    @Deprecated
    public void setCreator(String creator)
    {
        // Note: Consider "" or null as the same, i.e. the creator not being set
        DocumentReference creatorReference = null;
        if (creator != null && creator.length() > 0) {
            creatorReference = this.explicitReferenceDocumentReferenceResolver.resolve(
                this.xClassEntityReferenceResolver.resolve(creator, EntityType.DOCUMENT), getDocumentReference());
        }

        if ((getCreatorReference() == null && creatorReference != null)
            || (getCreatorReference() != null && !getCreatorReference().equals(creatorReference))) {
            setMetaDataDirty(true);
        }

        setCreatorReference(creatorReference);
    }

    public Date getDate()
    {
        if (this.updateDate == null) {
            return new Date();
        } else {
            return this.updateDate;
        }
    }

    public void setDate(Date date)
    {
        if ((date != null) && (!date.equals(this.updateDate))) {
            setMetaDataDirty(true);
        }
        // Make sure we drop milliseconds for consistency with the database
        if (date != null) {
            date.setTime((date.getTime() / 1000) * 1000);
        }
        this.updateDate = date;
    }

    public Date getCreationDate()
    {
        if (this.creationDate == null) {
            return new Date();
        } else {
            return this.creationDate;
        }
    }

    public void setCreationDate(Date date)
    {
        if ((date != null) && (!date.equals(this.creationDate))) {
            setMetaDataDirty(true);
        }

        // Make sure we drop milliseconds for consistency with the database
        if (date != null) {
            date.setTime((date.getTime() / 1000) * 1000);
        }
        this.creationDate = date;
    }

    public Date getContentUpdateDate()
    {
        if (this.contentUpdateDate == null) {
            return new Date();
        } else {
            return this.contentUpdateDate;
        }
    }

    public void setContentUpdateDate(Date date)
    {
        if ((date != null) && (!date.equals(this.contentUpdateDate))) {
            setMetaDataDirty(true);
        }

        // Make sure we drop milliseconds for consistency with the database
        if (date != null) {
            date.setTime((date.getTime() / 1000) * 1000);
        }
        this.contentUpdateDate = date;
    }

    public String getMeta()
    {
        return this.meta;
    }

    public void setMeta(String meta)
    {
        if (meta == null) {
            if (this.meta != null) {
                setMetaDataDirty(true);
            }
        } else if (!meta.equals(this.meta)) {
            setMetaDataDirty(true);
        }
        this.meta = meta;
    }

    public void appendMeta(String meta)
    {
        StringBuffer buf = new StringBuffer(this.meta);
        buf.append(meta);
        buf.append("\n");
        this.meta = buf.toString();
        setMetaDataDirty(true);
    }

    public boolean isContentDirty()
    {
        return this.isContentDirty;
    }

    public void incrementVersion()
    {
        if (this.version == null) {
            this.version = new Version("1.1");
        } else {
            if (isMinorEdit()) {
                this.version = this.version.next();
            } else {
                this.version = this.version.getBranchPoint().next().newBranch(1);
            }
        }
    }

    public void setContentDirty(boolean contentDirty)
    {
        this.isContentDirty = contentDirty;
    }

    public boolean isMetaDataDirty()
    {
        return this.isMetaDataDirty;
    }

    public void setMetaDataDirty(boolean metaDataDirty)
    {
        this.isMetaDataDirty = metaDataDirty;
    }

    public String getAttachmentURL(String filename, XWikiContext context)
    {
        return getAttachmentURL(filename, "download", context);
    }

    public String getAttachmentURL(String filename, String action, XWikiContext context)
    {
        URL url = context.getURLFactory().createAttachmentURL(filename, getSpace(), getName(), action, null,
            getDatabase(), context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getExternalAttachmentURL(String filename, String action, XWikiContext context)
    {
        URL url = context.getURLFactory().createAttachmentURL(filename, getSpace(), getName(), action, null,
            getDatabase(), context);
        return url.toString();
    }

    public String getAttachmentURL(String filename, String action, String querystring, XWikiContext context)
    {
        URL url = context.getURLFactory().createAttachmentURL(filename, getSpace(), getName(), action, querystring,
            getDatabase(), context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getAttachmentRevisionURL(String filename, String revision, XWikiContext context)
    {
        URL url = context.getURLFactory().createAttachmentRevisionURL(filename, getSpace(), getName(), revision, null,
            getDatabase(), context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getAttachmentRevisionURL(String filename, String revision, String querystring, XWikiContext context)
    {
        URL url = context.getURLFactory().createAttachmentRevisionURL(filename, getSpace(), getName(), revision,
            querystring, getDatabase(), context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getURL(String action, String params, boolean redirect, XWikiContext context)
    {
        URL url =
            context.getURLFactory().createURL(getSpace(), getName(), action, params, null, getDatabase(), context);
        if (redirect) {
            if (url == null) {
                return null;
            } else {
                return url.toString();
            }
        } else {
            return context.getURLFactory().getURL(url, context);
        }
    }

    public String getURL(String action, boolean redirect, XWikiContext context)
    {
        return getURL(action, null, redirect, context);
    }

    public String getURL(String action, XWikiContext context)
    {
        return getURL(action, false, context);
    }

    public String getURL(String action, String querystring, XWikiContext context)
    {
        URL url = context.getURLFactory().createURL(getSpace(), getName(), action, querystring, null, getDatabase(),
            context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getURL(String action, String querystring, String anchor, XWikiContext context)
    {
        URL url = context.getURLFactory().createURL(getSpace(), getName(), action, querystring, anchor, getDatabase(),
            context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getExternalURL(String action, XWikiContext context)
    {
        URL url = context.getURLFactory().createExternalURL(getSpace(), getName(), action, null, null, getDatabase(),
            context);
        return url.toString();
    }

    public String getExternalURL(String action, String querystring, XWikiContext context)
    {
        URL url = context.getURLFactory().createExternalURL(getSpace(), getName(), action, querystring, null,
            getDatabase(), context);
        return url.toString();
    }

    public String getParentURL(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(getParentReference());
        URL url = context.getURLFactory().createURL(doc.getSpace(), doc.getName(), "view", null, null, getDatabase(),
            context);
        return context.getURLFactory().getURL(url, context);
    }

    public XWikiDocumentArchive getDocumentArchive(XWikiContext context) throws XWikiException
    {
        loadArchive(context);
        return getDocumentArchive();
    }

    /**
     * Create a new protected {@link com.xpn.xwiki.api.Document} public API to access page information and actions from
     * scripting.
     * 
     * @param customClassName the name of the custom {@link com.xpn.xwiki.api.Document} class of the object to create.
     * @param context the XWiki context.
     * @return a wrapped version of an XWikiDocument. Prefer this function instead of new Document(XWikiDocument,
     *         XWikiContext)
     */
    public com.xpn.xwiki.api.Document newDocument(String customClassName, XWikiContext context)
    {
        if (!((customClassName == null) || (customClassName.equals("")))) {
            try {
                return newDocument(Class.forName(customClassName), context);
            } catch (ClassNotFoundException e) {
                LOGGER.error("Failed to get java Class object from class name", e);
            }
        }

        return new com.xpn.xwiki.api.Document(this, context);
    }

    /**
     * Create a new protected {@link com.xpn.xwiki.api.Document} public API to access page information and actions from
     * scripting.
     * 
     * @param customClass the custom {@link com.xpn.xwiki.api.Document} class the object to create.
     * @param context the XWiki context.
     * @return a wrapped version of an XWikiDocument. Prefer this function instead of new Document(XWikiDocument,
     *         XWikiContext)
     */
    public com.xpn.xwiki.api.Document newDocument(Class< ? > customClass, XWikiContext context)
    {
        if (customClass != null) {
            try {
                Class< ? >[] classes = new Class[] {XWikiDocument.class, XWikiContext.class};
                Object[] args = new Object[] {this, context};

                return (com.xpn.xwiki.api.Document) customClass.getConstructor(classes).newInstance(args);
            } catch (Exception e) {
                LOGGER.error("Failed to create a custom Document object", e);
            }
        }

        return new com.xpn.xwiki.api.Document(this, context);
    }

    public com.xpn.xwiki.api.Document newDocument(XWikiContext context)
    {
        String customClass = getCustomClass();
        return newDocument(customClass, context);
    }

    public void loadArchive(XWikiContext context) throws XWikiException
    {
        if (this.archive == null || this.archive.get() == null) {
            XWikiDocumentArchive arch = getVersioningStore(context).getXWikiDocumentArchive(this, context);
            // We are using a SoftReference which will allow the archive to be
            // discarded by the Garbage collector as long as the context is closed (usually during
            // the request)
            this.archive = new SoftReference<XWikiDocumentArchive>(arch);
        }
    }

    /**
     * @return the {@link XWikiDocumentArchive} for this document. If it is not stored in the document, null is
     *         returned.
     */
    public XWikiDocumentArchive getDocumentArchive()
    {
        // If there is a soft reference, return it.
        if (this.archive != null) {
            return this.archive.get();
        }
        return null;
    }

    /**
     * @return the {@link XWikiDocumentArchive} for this document. If it is not stored in the document, we get it using
     *         the current context. If there is an exception, null is returned.
     */
    public XWikiDocumentArchive loadDocumentArchive()
    {
        XWikiDocumentArchive arch = getDocumentArchive();
        if (arch != null) {
            return arch;
        }

        XWikiContext xcontext = getXWikiContext();

        try {
            arch = getVersioningStore(xcontext).getXWikiDocumentArchive(this, xcontext);

            // Put a copy of the archive in the soft reference for later use if needed.
            setDocumentArchive(arch);

            return arch;
        } catch (Exception e) {
            // VersioningStore.getXWikiDocumentArchive may throw an XWikiException, and xcontext or VersioningStore
            // may be null (tests)
            // To maintain the behavior of this method we can't throw an exception.
            // Formerly, null was returned if there was no SoftReference.
            LOGGER.warn("Could not get document archive", e);
            return null;
        }
    }

    public void setDocumentArchive(XWikiDocumentArchive arch)
    {
        // We are using a SoftReference which will allow the archive to be
        // discarded by the Garbage collector as long as the context is closed (usually during the
        // request)
        if (arch != null) {
            this.archive = new SoftReference<XWikiDocumentArchive>(arch);
        }
    }

    public void setDocumentArchive(String sarch) throws XWikiException
    {
        XWikiDocumentArchive xda = new XWikiDocumentArchive(getId());
        xda.setArchive(sarch);
        setDocumentArchive(xda);
    }

    public Version[] getRevisions(XWikiContext context) throws XWikiException
    {
        return getVersioningStore(context).getXWikiDocVersions(this, context);
    }

    public String[] getRecentRevisions(int nb, XWikiContext context) throws XWikiException
    {
        try {
            Version[] revisions = getVersioningStore(context).getXWikiDocVersions(this, context);
            int length = nb;
            // 0 means all revisions
            if (nb == 0) {
                length = revisions.length;
            }

            if (revisions.length < length) {
                length = revisions.length;
            }

            String[] recentrevs = new String[length];
            for (int i = 1; i <= length; i++) {
                recentrevs[i - 1] = revisions[revisions.length - i].toString();
            }
            return recentrevs;
        } catch (Exception e) {
            return new String[0];
        }
    }

    /**
     * Get document versions matching criterias like author, minimum creation date, etc.
     * 
     * @param criteria criteria used to match versions
     * @return a list of matching versions
     */
    public List<String> getRevisions(RevisionCriteria criteria, XWikiContext context) throws XWikiException
    {
        List<String> results = new ArrayList<String>();

        Version[] revisions = getRevisions(context);

        XWikiRCSNodeInfo nextNodeinfo = null;
        XWikiRCSNodeInfo nodeinfo;
        for (int i = 0; i < revisions.length; i++) {
            nodeinfo = nextNodeinfo;
            nextNodeinfo = getRevisionInfo(revisions[i].toString(), context);

            if (nodeinfo == null) {
                continue;
            }

            // Minor/Major version matching
            if (criteria.getIncludeMinorVersions() || !nextNodeinfo.isMinorEdit()) {
                // Author matching
                if (criteria.getAuthor().equals("") || criteria.getAuthor().equals(nodeinfo.getAuthor())) {
                    // Date range matching
                    Date versionDate = nodeinfo.getDate();
                    if (versionDate.after(criteria.getMinDate()) && versionDate.before(criteria.getMaxDate())) {
                        results.add(nodeinfo.getVersion().toString());
                    }
                }
            }
        }

        nodeinfo = nextNodeinfo;
        if (nodeinfo != null) {
            if (criteria.getAuthor().equals("") || criteria.getAuthor().equals(nodeinfo.getAuthor())) {
                // Date range matching
                Date versionDate = nodeinfo.getDate();
                if (versionDate.after(criteria.getMinDate()) && versionDate.before(criteria.getMaxDate())) {
                    results.add(nodeinfo.getVersion().toString());
                }
            }
        }

        return criteria.getRange().subList(results);
    }

    public XWikiRCSNodeInfo getRevisionInfo(String version, XWikiContext context) throws XWikiException
    {
        return getDocumentArchive(context).getNode(new Version(version));
    }

    /**
     * @return Is this version the most recent one. False if and only if there are newer versions of this document in
     *         the database.
     */
    public boolean isMostRecent()
    {
        return this.mostRecent;
    }

    /**
     * must not be used unless in store system.
     * 
     * @param mostRecent - mark document as most recent.
     */
    public void setMostRecent(boolean mostRecent)
    {
        this.mostRecent = mostRecent;
    }

    /**
     * @since 2.2M1
     */
    public BaseClass getXClass()
    {
        if (this.xClass == null) {
            this.xClass = new BaseClass();
            this.xClass.setDocumentReference(getDocumentReference());
        }
        return this.xClass;
    }

    /**
     * @deprecated since 2.2M1, use {@link #getXClass()} instead
     */
    @Deprecated
    public BaseClass getxWikiClass()
    {
        return getXClass();
    }

    /**
     * @since 2.2M1
     */
    public void setXClass(BaseClass xwikiClass)
    {
        xwikiClass.setDocumentReference(getDocumentReference());

        this.xClass = xwikiClass;
    }

    /**
     * @deprecated since 2.2M1, use {@link #setXClass(BaseClass)} instead
     */
    @Deprecated
    public void setxWikiClass(BaseClass xwikiClass)
    {
        setXClass(xwikiClass);
    }

    /**
     * @since 2.2M1
     */
    public Map<DocumentReference, List<BaseObject>> getXObjects()
    {
        return this.xObjects;
    }

    /**
     * @deprecated since 2.2M1 use {@link #getXObjects()} instead. Warning: if you used to modify the returned Map note
     *             that since 2.2M1 this will no longer work and you'll need to call the setXObject methods instead (or
     *             setxWikiObjects()). Obviously the best is to move to the new API.
     */
    @Deprecated
    public Map<String, Vector<BaseObject>> getxWikiObjects()
    {
        // Use a liked hash map to ensure we keep the order stored from the internal objects map.
        Map<String, Vector<BaseObject>> objects = new LinkedHashMap<String, Vector<BaseObject>>();

        for (Map.Entry<DocumentReference, List<BaseObject>> entry : getXObjects().entrySet()) {
            objects.put(this.compactWikiEntityReferenceSerializer.serialize(entry.getKey()), new Vector<BaseObject>(
                entry.getValue()));
        }

        return objects;
    }

    /**
     * @since 2.2M1
     */
    public void setXObjects(Map<DocumentReference, List<BaseObject>> objects)
    {
        this.xObjects = objects;
    }

    /**
     * @deprecated since 2.2M1 use {@link #setXObjects(Map)} instead
     */
    @Deprecated
    public void setxWikiObjects(Map<String, Vector<BaseObject>> objects)
    {
        // Use a liked hash map to ensure we keep the order stored from the internal objects map.
        Map<DocumentReference, List<BaseObject>> newObjects = new LinkedHashMap<DocumentReference, List<BaseObject>>();

        for (Map.Entry<String, Vector<BaseObject>> entry : objects.entrySet()) {
            newObjects.put(resolveClassReference(entry.getKey()), new ArrayList<BaseObject>(entry.getValue()));
        }

        setXObjects(newObjects);
    }

    /**
     * @since 2.2M1
     */
    public BaseObject getXObject()
    {
        return getXObject(getDocumentReference());
    }

    /**
     * @deprecated since 2.2M1 use {@link #getXObject()} instead
     */
    @Deprecated
    public BaseObject getxWikiObject()
    {
        return getXObject(getDocumentReference());
    }

    /**
     * @since 2.2M1
     */
    public List<BaseClass> getXClasses(XWikiContext context)
    {
        List<BaseClass> list = new ArrayList<BaseClass>();

        // getXObjects() is a TreeMap, with elements sorted by className reference
        for (DocumentReference classReference : getXObjects().keySet()) {
            BaseClass bclass = null;
            List<BaseObject> objects = getXObjects(classReference);
            for (BaseObject obj : objects) {
                if (obj != null) {
                    bclass = obj.getXClass(context);
                    if (bclass != null) {
                        break;
                    }
                }
            }
            if (bclass != null) {
                list.add(bclass);
            }
        }
        return list;
    }

    /**
     * @deprecated since 2.2M1 use {@link #getXClasses(XWikiContext)} instead
     */
    @Deprecated
    public List<BaseClass> getxWikiClasses(XWikiContext context)
    {
        return getXClasses(context);
    }

    /**
     * @since 2.2.3
     */
    public int createXObject(EntityReference classReference, XWikiContext context) throws XWikiException
    {
        DocumentReference absoluteClassReference = resolveClassReference(classReference);
        BaseObject object = BaseClass.newCustomClassInstance(absoluteClassReference, context);
        object.setDocumentReference(getDocumentReference());
        object.setXClassReference(classReference);
        List<BaseObject> objects = getXObjects(absoluteClassReference);
        if (objects == null) {
            objects = new ArrayList<BaseObject>();
            setXObjects(absoluteClassReference, objects);
        }
        objects.add(object);
        int nb = objects.size() - 1;
        object.setNumber(nb);
        setContentDirty(true);
        return nb;
    }

    /**
     * @deprecated since 2.2M1 use {@link #createXObject(EntityReference, XWikiContext)} instead
     */
    @Deprecated
    public int createNewObject(String className, XWikiContext context) throws XWikiException
    {
        return createXObject(
            this.xClassEntityReferenceResolver.resolve(className, EntityType.DOCUMENT, getDocumentReference()), context);
    }

    /**
     * @since 2.2M1
     */
    public int getXObjectSize(DocumentReference classReference)
    {
        try {
            return getXObjects().get(classReference).size();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * @deprecated since 2.2M1 use {@link #getXObjectSize(DocumentReference)} instead
     */
    @Deprecated
    public int getObjectNumbers(String className)
    {
        return getXObjectSize(resolveClassReference(className));
    }

    /**
     * @since 2.2M1
     */
    public List<BaseObject> getXObjects(DocumentReference classReference)
    {
        if (classReference == null) {
            return new ArrayList<BaseObject>();
        }
        return getXObjects().get(classReference);
    }

    /**
     * @deprecated since 2.2M1 use {@link #getXObjects(DocumentReference)} instead
     */
    @Deprecated
    public Vector<BaseObject> getObjects(String className)
    {
        List<BaseObject> result = getXObjects(resolveClassReference(className));
        return result == null ? null : new Vector<BaseObject>(result);
    }

    /**
     * @since 2.2M1
     */
    public void setXObjects(DocumentReference classReference, List<BaseObject> objects)
    {
        if (objects.isEmpty()) {
            getXObjects().put(classReference, objects);
        } else {
            for (BaseObject baseObject : objects) {
                addXObject(classReference, baseObject);
            }
        }
    }

    /**
     * @deprecated since 2.2M1 use {@link #setXObjects(DocumentReference, List)} instead
     */
    @Deprecated
    public void setObjects(String className, Vector<BaseObject> objects)
    {
        setXObjects(resolveClassReference(className), new ArrayList<BaseObject>(objects));
    }

    /**
     * @since 2.2M1
     */
    public BaseObject getXObject(DocumentReference classReference)
    {
        BaseObject result = null;
        List<BaseObject> objects = getXObjects().get(classReference);
        if (objects != null) {
            for (BaseObject object : objects) {
                if (object != null) {
                    result = object;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * @deprecated since 2.2M1 use {@link #getXObject(DocumentReference)} instead
     */
    @Deprecated
    public BaseObject getObject(String className)
    {
        return getXObject(resolveClassReference(className));
    }

    /**
     * @since 2.2M1
     */
    public BaseObject getXObject(DocumentReference classReference, int nb)
    {
        try {
            return getXObjects().get(classReference).get(nb);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @deprecated since 2.2M1 use {@link #getXObject(DocumentReference, int)} instead
     */
    @Deprecated
    public BaseObject getObject(String className, int nb)
    {
        return getXObject(resolveClassReference(className), nb);
    }

    /**
     * @since 2.2M1
     */
    public BaseObject getXObject(DocumentReference classReference, String key, String value)
    {
        return getXObject(classReference, key, value, false);
    }

    /**
     * @deprecated since 2.2M1 use {@link #getXObject(DocumentReference, String, String)} instead
     */
    @Deprecated
    public BaseObject getObject(String className, String key, String value)
    {
        return getObject(className, key, value, false);
    }

    /**
     * @since 2.2M1
     */
    public BaseObject getXObject(DocumentReference classReference, String key, String value, boolean failover)
    {
        try {
            if (value == null) {
                if (failover) {
                    return getXObject(classReference);
                } else {
                    return null;
                }
            }

            List<BaseObject> objects = getXObjects().get(classReference);
            if ((objects == null) || (objects.size() == 0)) {
                return null;
            }
            for (int i = 0; i < objects.size(); i++) {
                BaseObject obj = objects.get(i);
                if (obj != null) {
                    if (value.equals(obj.getStringValue(key))) {
                        return obj;
                    }
                }
            }

            if (failover) {
                return getXObject(classReference);
            } else {
                return null;
            }
        } catch (Exception e) {
            if (failover) {
                return getXObject(classReference);
            }

            e.printStackTrace();
            return null;
        }
    }

    /**
     * @deprecated since 2.2M1 use {@link #getXObject(DocumentReference, String, String, boolean)} instead
     */
    @Deprecated
    public BaseObject getObject(String className, String key, String value, boolean failover)
    {
        return getXObject(resolveClassReference(className), key, value, failover);
    }

    /**
     * @since 2.2M1
     * @deprecated use {@link #addXObject(BaseObject)} instead
     */
    @Deprecated
    public void addXObject(DocumentReference classReference, BaseObject object)
    {
        List<BaseObject> vobj = getXObjects(classReference);
        if (vobj == null) {
            setXObject(classReference, 0, object);
        } else {
            setXObject(classReference, vobj.size(), object);
        }
    }

    /**
     * Add the object to the document.
     * 
     * @param object the xobject to add
     * @throws NullPointerException if the specified object is null because we need the get the class reference from the
     *             object
     * @since 2.2.3
     */
    public void addXObject(BaseObject object)
    {
        object.setDocumentReference(getDocumentReference());

        List<BaseObject> vobj = getXObjects(object.getXClassReference());
        if (vobj == null) {
            setXObject(0, object);
        } else {
            setXObject(vobj.size(), object);
        }
    }

    /**
     * @deprecated since 2.2M1 use {@link #addXObject(BaseObject)} instead
     */
    @Deprecated
    public void addObject(String className, BaseObject object)
    {
        addXObject(resolveClassReference(className), object);
    }

    /**
     * @since 2.2M1
     * @deprecated use {@link #setXObject(int, BaseObject)} instead
     */
    @Deprecated
    public void setXObject(DocumentReference classReference, int nb, BaseObject object)
    {
        if (object != null) {
            object.setDocumentReference(getDocumentReference());
            object.setNumber(nb);
        }
        List<BaseObject> objects = getXObjects(classReference);
        if (objects == null) {
            objects = new ArrayList<BaseObject>();
            setXObjects(classReference, objects);
        }
        while (nb >= objects.size()) {
            objects.add(null);
        }
        objects.set(nb, object);
        setContentDirty(true);
    }

    /**
     * Replaces the object at the specified position and for the specified object xclass.
     * 
     * @param nb index of the element to replace
     * @param object the xobject to insert
     * @throws NullPointerException if the specified object is null because we need the get the class reference from the
     *             object
     * @since 2.2.3
     */
    public void setXObject(int nb, BaseObject object)
    {
        object.setDocumentReference(getDocumentReference());
        object.setNumber(nb);

        List<BaseObject> objects = getXObjects(object.getXClassReference());
        if (objects == null) {
            objects = new ArrayList<BaseObject>();
            setXObjects(object.getXClassReference(), objects);
        }
        while (nb >= objects.size()) {
            objects.add(null);
        }
        objects.set(nb, object);
        setContentDirty(true);
    }

    /**
     * @deprecated since 2.2M1 use {@link #setXObject(DocumentReference, int, BaseObject)} instead
     */
    @Deprecated
    public void setObject(String className, int nb, BaseObject object)
    {
        setXObject(resolveClassReference(className), nb, object);
    }

    /**
     * @return true if the document is a new one (i.e. it has never been saved) or false otherwise
     */
    public boolean isNew()
    {
        return this.isNew;
    }

    public void setNew(boolean aNew)
    {
        this.isNew = aNew;
    }

    /**
     * @since 2.2M1
     */
    public void mergeXClass(XWikiDocument templatedoc)
    {
        BaseClass bclass = getXClass();
        BaseClass tbclass = templatedoc.getXClass();
        if (tbclass != null) {
            if (bclass == null) {
                setXClass((BaseClass) tbclass.clone());
            } else {
                getXClass().merge((BaseClass) tbclass.clone());
            }
        }
        setContentDirty(true);
    }

    /**
     * @deprecated since 2.2M1 use {@link #mergeXClass(XWikiDocument)} instead
     */
    @Deprecated
    public void mergexWikiClass(XWikiDocument templatedoc)
    {
        mergeXClass(templatedoc);
    }

    /**
     * @since 2.2M1
     */
    public void mergeXObjects(XWikiDocument templatedoc)
    {
        // TODO: look for each object if it already exist and add it if it doesn't
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : templatedoc.getXObjects().entrySet()) {
            List<BaseObject> myObjects = getXObjects().get(entry.getKey());

            if (myObjects == null) {
                myObjects = new ArrayList<BaseObject>();
            }

            if (!entry.getValue().isEmpty()) {
                DocumentReference newXClassReference = null;
                for (BaseObject otherObject : entry.getValue()) {
                    if (otherObject != null) {
                        BaseObject myObject = otherObject.duplicate(getDocumentReference());
                        myObjects.add(myObject);
                        myObject.setNumber(myObjects.size() - 1);
                        newXClassReference = myObject.getXClassReference();
                    }
                }
                setXObjects(newXClassReference, myObjects);
            }
        }
        setContentDirty(true);
    }

    /**
     * @deprecated since 2.2M1 use {@link #mergeXObjects(XWikiDocument)} instead
     */
    @Deprecated
    public void mergexWikiObjects(XWikiDocument templatedoc)
    {
        mergeXObjects(templatedoc);
    }

    /**
     * @since 2.2M1
     */
    public void cloneXObjects(XWikiDocument templatedoc)
    {
        cloneXObjects(templatedoc, true);
    }

    /**
     * @since 2.2.3
     */
    public void duplicateXObjects(XWikiDocument templatedoc)
    {
        cloneXObjects(templatedoc, false);
    }

    /**
     * Copy specified document objects into current document.
     * 
     * @param templatedoc the document to copy
     * @param keepsIdentity if true it does an exact java copy, otherwise it duplicate objects with the new document
     *            name (and new class names)
     */
    private void cloneXObjects(XWikiDocument templatedoc, boolean keepsIdentity)
    {
        // clean map
        this.xObjects.clear();

        // fill map
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : templatedoc.getXObjects().entrySet()) {
            List<BaseObject> tobjects = entry.getValue();

            // clone and insert xobjects
            for (BaseObject otherObject : tobjects) {
                if (otherObject != null) {
                    if (keepsIdentity) {
                        addXObject((BaseObject) otherObject.clone());
                    } else {
                        BaseObject newObject = otherObject.duplicate(getDocumentReference());
                        setXObject(newObject.getNumber(), newObject);
                    }
                } else if (keepsIdentity) {
                    // set null object to make sure to have exactly the same thing when cloning a document
                    addXObject(entry.getKey(), null);
                }
            }
        }
    }

    /**
     * @deprecated since 2.2M1 use {@link #cloneXObjects(XWikiDocument)} instead
     */
    @Deprecated
    public void clonexWikiObjects(XWikiDocument templatedoc)
    {
        cloneXObjects(templatedoc);
    }

    /**
     * @since 2.2M1
     */
    public DocumentReference getTemplateDocumentReference()
    {
        return this.templateDocumentReference;
    }

    /**
     * @deprecated since 2.2M1 use {@link #getTemplateDocumentReference()} instead
     */
    @Deprecated
    public String getTemplate()
    {
        String templateReferenceAsString;
        DocumentReference templateDocumentReference = getTemplateDocumentReference();
        if (templateDocumentReference != null) {
            templateReferenceAsString = this.localEntityReferenceSerializer.serialize(templateDocumentReference);
        } else {
            templateReferenceAsString = "";
        }
        return templateReferenceAsString;
    }

    /**
     * @since 2.2M1
     */
    public void setTemplateDocumentReference(DocumentReference templateDocumentReference)
    {
        if ((templateDocumentReference == null && getTemplateDocumentReference() != null)
            || (templateDocumentReference != null && !templateDocumentReference.equals(getTemplateDocumentReference()))) {
            this.templateDocumentReference = templateDocumentReference;
            setMetaDataDirty(true);
        }
    }

    /**
     * @deprecated since 2.2M1 use {@link #setTemplateDocumentReference(DocumentReference)} instead
     */
    @Deprecated
    public void setTemplate(String template)
    {
        DocumentReference templateReference = null;
        if (!StringUtils.isEmpty(template)) {
            templateReference = this.currentMixedDocumentReferenceResolver.resolve(template);
        }
        setTemplateDocumentReference(templateReference);
    }

    public String displayPrettyName(String fieldname, XWikiContext context)
    {
        return displayPrettyName(fieldname, false, true, context);
    }

    public String displayPrettyName(String fieldname, boolean showMandatory, XWikiContext context)
    {
        return displayPrettyName(fieldname, showMandatory, true, context);
    }

    public String displayPrettyName(String fieldname, boolean showMandatory, boolean before, XWikiContext context)
    {
        try {
            BaseObject object = getXObject();
            if (object == null) {
                object = getFirstObject(fieldname, context);
            }
            return displayPrettyName(fieldname, showMandatory, before, object, context);
        } catch (Exception e) {
            return "";
        }
    }

    public String displayPrettyName(String fieldname, BaseObject obj, XWikiContext context)
    {
        return displayPrettyName(fieldname, false, true, obj, context);
    }

    public String displayPrettyName(String fieldname, boolean showMandatory, BaseObject obj, XWikiContext context)
    {
        return displayPrettyName(fieldname, showMandatory, true, obj, context);
    }

    public String displayPrettyName(String fieldname, boolean showMandatory, boolean before, BaseObject obj,
        XWikiContext context)
    {
        try {
            PropertyClass pclass = (PropertyClass) obj.getXClass(context).get(fieldname);
            String dprettyName = "";
            if (showMandatory) {
                dprettyName = context.getWiki().addMandatory(context);
            }
            if (before) {
                return dprettyName + pclass.getPrettyName(context);
            } else {
                return pclass.getPrettyName(context) + dprettyName;
            }
        } catch (Exception e) {
            return "";
        }
    }

    public String displayTooltip(String fieldname, XWikiContext context)
    {
        try {
            BaseObject object = getXObject();
            if (object == null) {
                object = getFirstObject(fieldname, context);
            }
            return displayTooltip(fieldname, object, context);
        } catch (Exception e) {
            return "";
        }
    }

    public String displayTooltip(String fieldname, BaseObject obj, XWikiContext context)
    {
        String result = "";

        try {
            PropertyClass pclass = (PropertyClass) obj.getXClass(context).get(fieldname);
            String tooltip = pclass.getTooltip(context);
            if ((tooltip != null) && (!tooltip.trim().equals(""))) {
                String img = "<img src=\"" + context.getWiki().getSkinFile("info.gif", context)
                    + "\" class=\"tooltip_image\" align=\"middle\" />";
                result = context.getWiki().addTooltip(img, tooltip, context);
            }
        } catch (Exception e) {

        }

        return result;
    }

    /**
     * @param fieldname the name of the field to display
     * @param context the XWiki context
     * @return the rendered field
     */
    public String display(String fieldname, XWikiContext context)
    {
        String result = "";

        try {
            BaseObject object = getXObject();
            if (object == null) {
                object = getFirstObject(fieldname, context);
            }

            result = display(fieldname, object, context);
        } catch (Exception e) {
            LOGGER.error("Failed to display field [" + fieldname + "] of document ["
                + this.defaultEntityReferenceSerializer.serialize(getDocumentReference()) + "]", e);
        }

        return result;
    }

    /**
     * @param fieldname the name of the field to display
     * @param obj the object containing the field to display
     * @param context the XWiki context
     * @return the rendered field
     */
    public String display(String fieldname, BaseObject obj, XWikiContext context)
    {
        String type = null;
        try {
            type = (String) context.get("display");
        } catch (Exception e) {
        }

        if (type == null) {
            type = "view";
        }

        return display(fieldname, type, obj, context);
    }

    /**
     * @param fieldname the name of the field to display
     * @param mode the mode to use ("view", "edit", ...)
     * @param context the XWiki context
     * @return the rendered field
     */
    public String display(String fieldname, String mode, XWikiContext context)
    {
        return display(fieldname, mode, "", context);
    }

    /**
     * @param fieldname the name of the field to display
     * @param type the type of the field to display
     * @param obj the object containing the field to display
     * @param context the XWiki context
     * @return the rendered field
     */
    public String display(String fieldname, String type, BaseObject obj, XWikiContext context)
    {
        return display(fieldname, type, "", obj, context);
    }

    /**
     * @param fieldname the name of the field to display
     * @param mode the mode to use ("view", "edit", ...)
     * @param prefix the prefix to add in the field identifier in edit display for example
     * @param context the XWiki context
     * @return the rendered field
     */
    public String display(String fieldname, String mode, String prefix, XWikiContext context)
    {
        try {
            BaseObject object = getXObject();
            if (object == null) {
                object = getFirstObject(fieldname, context);
            }
            if (object == null) {
                return "";
            } else {
                return display(fieldname, mode, prefix, object, context);
            }
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * @param fieldname the name of the field to display
     * @param type the type of the field to display
     * @param obj the object containing the field to display
     * @param wrappingSyntaxId the syntax of the content in which the result will be included. This to take care of some
     *            escaping depending of the syntax.
     * @param context the XWiki context
     * @return the rendered field
     */
    public String display(String fieldname, String type, BaseObject obj, String wrappingSyntaxId, XWikiContext context)
    {
        return display(fieldname, type, "", obj, wrappingSyntaxId, context);
    }

    /**
     * @param fieldname the name of the field to display
     * @param type the type of the field to display
     * @param pref the prefix to add in the field identifier in edit display for example
     * @param obj the object containing the field to display
     * @param context the XWiki context
     * @return the rendered field
     */
    public String display(String fieldname, String type, String pref, BaseObject obj, XWikiContext context)
    {
        return display(fieldname, type, pref, obj, context.getWiki().getCurrentContentSyntaxId(getSyntaxId(), context),
            context);
    }

    /**
     * @param fieldname the name of the field to display
     * @param type the type of the field to display
     * @param pref the prefix to add in the field identifier in edit display for example
     * @param obj the object containing the field to display
     * @param wrappingSyntaxId the syntax of the content in which the result will be included. This to take care of some
     *            escaping depending of the syntax.
     * @param context the XWiki context
     * @return the rendered field
     */
    public String display(String fieldname, String type, String pref, BaseObject obj, String wrappingSyntaxId,
        XWikiContext context)
    {
        if (obj == null) {
            return "";
        }

        boolean isInRenderingEngine = BooleanUtils.toBoolean((Boolean) context.get("isInRenderingEngine"));
        HashMap<String, Object> backup = new HashMap<String, Object>();
        try {
            backupContext(backup, context);
            setAsContextDoc(context);

            type = type.toLowerCase();
            StringBuffer result = new StringBuffer();
            PropertyClass pclass = (PropertyClass) obj.getXClass(context).get(fieldname);
            String prefix = pref +
                this.localEntityReferenceSerializer.serialize(obj.getXClass(context).getDocumentReference()) + "_"
                + obj.getNumber() + "_";

            if (pclass == null) {
                return "";
            } else if (pclass.isCustomDisplayed(context)) {
                pclass.displayCustom(result, fieldname, prefix, type, obj, context);
            } else if (type.equals("view")) {
                pclass.displayView(result, fieldname, prefix, obj, context);
            } else if (type.equals("rendered")) {
                String fcontent = pclass.displayView(fieldname, prefix, obj, context);
                // This mode is deprecated for the new rendering and should also be removed for the old rendering
                // since the way to implement this now is to choose the type of rendering to do in the class itself.
                // Thus for the new rendering we simply make this mode work like the "view" mode.
                if (is10Syntax(wrappingSyntaxId)) {
                    result.append(getRenderedContent(fcontent, getSyntaxId(), context));
                } else {
                    result.append(fcontent);
                }
            } else if (type.equals("edit")) {
                context.addDisplayedField(fieldname);
                // If the Syntax id is "xwiki/1.0" then use the old rendering subsystem and prevent wiki syntax
                // rendering using the pre macro. In the new rendering system it's the XWiki Class itself that does the
                // escaping. For example for a textarea check the TextAreaClass class.
                if (is10Syntax(wrappingSyntaxId)) {
                    // Don't use pre when not in the rendernig engine since for template we don't evaluate wiki syntax.
                    if (isInRenderingEngine) {
                        result.append("{pre}");
                    }
                }
                pclass.displayEdit(result, fieldname, prefix, obj, context);
                if (is10Syntax(wrappingSyntaxId)) {
                    if (isInRenderingEngine) {
                        result.append("{/pre}");
                    }
                }
            } else if (type.equals("hidden")) {
                // If the Syntax id is "xwiki/1.0" then use the old rendering subsystem and prevent wiki syntax
                // rendering using the pre macro. In the new rendering system it's the XWiki Class itself that does the
                // escaping. For example for a textarea check the TextAreaClass class.
                if (is10Syntax(wrappingSyntaxId) && isInRenderingEngine) {
                    result.append("{pre}");
                }
                pclass.displayHidden(result, fieldname, prefix, obj, context);
                if (is10Syntax(wrappingSyntaxId) && isInRenderingEngine) {
                    result.append("{/pre}");
                }
            } else if (type.equals("search")) {
                // If the Syntax id is "xwiki/1.0" then use the old rendering subsystem and prevent wiki syntax
                // rendering using the pre macro. In the new rendering system it's the XWiki Class itself that does the
                // escaping. For example for a textarea check the TextAreaClass class.
                if (is10Syntax(wrappingSyntaxId) && isInRenderingEngine) {
                    result.append("{pre}");
                }
                prefix = this.localEntityReferenceSerializer.serialize(obj.getXClass(context).getDocumentReference())
                    + "_";
                pclass.displaySearch(result, fieldname, prefix, (XWikiCriteria) context.get("query"), context);
                if (is10Syntax(wrappingSyntaxId) && isInRenderingEngine) {
                    result.append("{/pre}");
                }
            } else {
                pclass.displayView(result, fieldname, prefix, obj, context);
            }

            // If we're in new rendering engine we want to wrap the HTML returned by displayView() in
            // a {{html/}} macro so that the user doesn't have to do it.
            // We test if we're inside the rendering engine since it's also possible that this display() method is
            // called
            // directly from a template and in this case we only want HTML as a result and not wiki syntax.
            // TODO: find a more generic way to handle html macro because this works only for XWiki 1.0 and XWiki 2.0
            // Add the {{html}}{{/html}} only when result really contains html since it's not needed for pure text
            if (isInRenderingEngine && !is10Syntax(wrappingSyntaxId)
                && (result.indexOf("<") != -1 || result.indexOf(">") != -1)) {
                result.insert(0, "{{html clean=\"false\" wiki=\"false\"}}");
                result.append("{{/html}}");
            }

            return result.toString();
        } catch (Exception ex) {
            // TODO: It would better to check if the field exists rather than catching an exception
            // raised by a NPE as this is currently the case here...
            LOGGER.warn("Failed to display field [" + fieldname + "] in [" + type + "] mode for Object of Class ["
                + this.defaultEntityReferenceSerializer.serialize(obj.getDocumentReference()) + "]", ex);
            return "";
        } finally {
            restoreContext(backup, context);
        }
    }

    /**
     * @since 2.2M1
     */
    public String displayForm(DocumentReference classReference, String header, String format, XWikiContext context)
    {
        return displayForm(classReference, header, format, true, context);
    }

    /**
     * @deprecated since 2.2M1, use {@link #displayForm(DocumentReference, String, String, XWikiContext)} instead
     */
    @Deprecated
    public String displayForm(String className, String header, String format, XWikiContext context)
    {
        return displayForm(className, header, format, true, context);
    }

    /**
     * @since 2.2M1
     */
    public String displayForm(DocumentReference classReference, String header, String format, boolean linebreak,
        XWikiContext context)
    {
        List<BaseObject> objects = getXObjects(classReference);
        if (format.endsWith("\\n")) {
            linebreak = true;
        }

        BaseObject firstobject = null;
        Iterator<BaseObject> foit = objects.iterator();
        while ((firstobject == null) && foit.hasNext()) {
            firstobject = foit.next();
        }

        if (firstobject == null) {
            return "";
        }

        BaseClass bclass = firstobject.getXClass(context);
        if (bclass.getPropertyList().size() == 0) {
            return "";
        }

        StringBuffer result = new StringBuffer();
        VelocityContext vcontext = new VelocityContext();
        for (String propertyName : bclass.getPropertyList()) {
            PropertyClass pclass = (PropertyClass) bclass.getField(propertyName);
            vcontext.put(pclass.getName(), pclass.getPrettyName());
        }
        result
            .append(XWikiVelocityRenderer.evaluate(header, context.getDoc().getPrefixedFullName(), vcontext, context));
        if (linebreak) {
            result.append("\n");
        }

        // display each line
        for (int i = 0; i < objects.size(); i++) {
            vcontext.put("id", Integer.valueOf(i + 1));
            BaseObject object = objects.get(i);
            if (object != null) {
                for (String name : bclass.getPropertyList()) {
                    vcontext.put(name, display(name, object, context));
                }
                result.append(XWikiVelocityRenderer.evaluate(format, context.getDoc().getPrefixedFullName(), vcontext,
                    context));
                if (linebreak) {
                    result.append("\n");
                }
            }
        }
        return result.toString();
    }

    /**
     * @deprecated since 2.2M1, use {@link #displayForm(DocumentReference, String, String, boolean, XWikiContext)}
     *             instead
     */
    @Deprecated
    public String displayForm(String className, String header, String format, boolean linebreak, XWikiContext context)
    {
        return displayForm(resolveClassReference(className), header, format, linebreak, context);
    }

    /**
     * @since 2.2M1
     */
    public String displayForm(DocumentReference classReference, XWikiContext context)
    {
        List<BaseObject> objects = getXObjects(classReference);
        if (objects == null) {
            return "";
        }

        BaseObject firstobject = null;
        Iterator<BaseObject> foit = objects.iterator();
        while ((firstobject == null) && foit.hasNext()) {
            firstobject = foit.next();
        }

        if (firstobject == null) {
            return "";
        }

        BaseClass bclass = firstobject.getXClass(context);
        if (bclass.getPropertyList().size() == 0) {
            return "";
        }

        StringBuffer result = new StringBuffer();
        result.append("{table}\n");
        boolean first = true;
        for (String propertyName : bclass.getPropertyList()) {
            if (first == true) {
                first = false;
            } else {
                result.append("|");
            }
            PropertyClass pclass = (PropertyClass) bclass.getField(propertyName);
            result.append(pclass.getPrettyName());
        }
        result.append("\n");
        for (int i = 0; i < objects.size(); i++) {
            BaseObject object = objects.get(i);
            if (object != null) {
                first = true;
                for (String propertyName : bclass.getPropertyList()) {
                    if (first == true) {
                        first = false;
                    } else {
                        result.append("|");
                    }
                    String data = display(propertyName, object, context);
                    data = data.trim();
                    data = data.replaceAll("\n", " ");
                    if (data.length() == 0) {
                        result.append("&nbsp;");
                    } else {
                        result.append(data);
                    }
                }
                result.append("\n");
            }
        }
        result.append("{table}\n");
        return result.toString();
    }

    /**
     * @deprecated since 2.2M1, use {@link #displayForm(DocumentReference, XWikiContext)} instead
     */
    @Deprecated
    public String displayForm(String className, XWikiContext context)
    {
        return displayForm(resolveClassReference(className), context);
    }

    public boolean isFromCache()
    {
        return this.fromCache;
    }

    public void setFromCache(boolean fromCache)
    {
        this.fromCache = fromCache;
    }

    public void readDocMetaFromForm(EditForm eform, XWikiContext context) throws XWikiException
    {
        String defaultLanguage = eform.getDefaultLanguage();
        if (defaultLanguage != null) {
            setDefaultLanguage(defaultLanguage);
        }

        String defaultTemplate = eform.getDefaultTemplate();
        if (defaultTemplate != null) {
            setDefaultTemplate(defaultTemplate);
        }

        String creator = eform.getCreator();
        if ((creator != null) && (!creator.equals(getCreator()))) {
            if ((getCreatorReference().equals(context.getUserReference()))
                || (context.getWiki().getRightService().hasAdminRights(context))) {
                setCreator(creator);
            }
        }

        String parent = eform.getParent();
        if (parent != null) {
            setParent(parent);
        }

        // Read the comment from the form
        String comment = eform.getComment();
        if (comment != null) {
            setComment(comment);
        }

        // Read the minor edit checkbox from the form
        setMinorEdit(eform.isMinorEdit());

        String tags = eform.getTags();
        if (!StringUtils.isEmpty(tags)) {
            setTags(tags, context);
        }

        // Set the Syntax id if defined
        String syntaxId = eform.getSyntaxId();
        if (syntaxId != null) {
            setSyntaxId(syntaxId);
        }
    }

    /**
     * add tags to the document.
     */
    public void setTags(String tagsStr, XWikiContext context) throws XWikiException
    {
        BaseClass tagsClass = context.getWiki().getTagClass(context);

        StaticListClass tagProp = (StaticListClass) tagsClass.getField(XWikiConstant.TAG_CLASS_PROP_TAGS);

        BaseObject tags = getObject(XWikiConstant.TAG_CLASS, true, context);

        tags.safeput(XWikiConstant.TAG_CLASS_PROP_TAGS, tagProp.fromString(tagsStr));

        setMetaDataDirty(true);
    }

    public String getTags(XWikiContext context)
    {
        ListProperty prop = (ListProperty) getTagProperty(context);

        return prop != null ? prop.getTextValue() : "";
    }

    public List<String> getTagsList(XWikiContext context)
    {
        List<String> tagList = null;

        BaseProperty prop = getTagProperty(context);
        if (prop != null) {
            tagList = (List<String>) prop.getValue();
        }

        return tagList;
    }

    private BaseProperty getTagProperty(XWikiContext context)
    {
        BaseObject tags = getObject(XWikiConstant.TAG_CLASS);

        return tags != null ? ((BaseProperty) tags.safeget(XWikiConstant.TAG_CLASS_PROP_TAGS)) : null;
    }

    public List<String> getTagsPossibleValues(XWikiContext context)
    {
        List<String> list;

        try {
            BaseClass tagsClass = context.getWiki().getTagClass(context);

            String possibleValues =
                ((StaticListClass) tagsClass.getField(XWikiConstant.TAG_CLASS_PROP_TAGS)).getValues();

            return ListClass.getListFromString(possibleValues);
        } catch (XWikiException e) {
            LOGGER.error("Failed to get tag class", e);

            list = Collections.emptyList();
        }

        return list;
    }

    public void readTranslationMetaFromForm(EditForm eform, XWikiContext context) throws XWikiException
    {
        String content = eform.getContent();
        if (content != null) {
            // Cleanup in case we use HTMLAREA
            // content = context.getUtil().substitute("s/<br class=\\\"htmlarea\\\"\\/>/\\r\\n/g",
            // content);
            content = context.getUtil().substitute("s/<br class=\"htmlarea\" \\/>/\r\n/g", content);
            setContent(content);
        }
        String title = eform.getTitle();
        if (title != null) {
            setTitle(title);
        }
    }

    public void readObjectsFromForm(EditForm eform, XWikiContext context) throws XWikiException
    {
        for (DocumentReference reference : getXObjects().keySet()) {
            List<BaseObject> oldObjects = getXObjects(reference);
            List<BaseObject> newObjects = new ArrayList<BaseObject>();
            while (newObjects.size() < oldObjects.size()) {
                newObjects.add(null);
            }
            for (int i = 0; i < oldObjects.size(); i++) {
                BaseObject oldobject = oldObjects.get(i);
                if (oldobject != null) {
                    BaseClass baseclass = oldobject.getXClass(context);
                    BaseObject newobject = (BaseObject) baseclass.fromMap(eform.getObject(
                        this.localEntityReferenceSerializer.serialize(baseclass.getDocumentReference()) + "_" + i),
                        oldobject);
                    newobject.setNumber(oldobject.getNumber());
                    newobject.setGuid(oldobject.getGuid());
                    newobject.setDocumentReference(getDocumentReference());
                    newObjects.set(newobject.getNumber(), newobject);
                }
            }
            getXObjects().put(reference, newObjects);
        }
        setContentDirty(true);
    }

    public void readFromForm(EditForm eform, XWikiContext context) throws XWikiException
    {
        readDocMetaFromForm(eform, context);
        readTranslationMetaFromForm(eform, context);
        readObjectsFromForm(eform, context);
    }

    public void readFromTemplate(EditForm eform, XWikiContext context) throws XWikiException
    {
        String template = eform.getTemplate();
        readFromTemplate(template, context);
    }

    /**
     * @since 2.2M1
     */
    public void readFromTemplate(DocumentReference templateDocumentReference, XWikiContext context)
        throws XWikiException
    {
        if (templateDocumentReference != null) {
            String content = getContent();
            if ((!content.equals("\n")) && (!content.equals("")) && !isNew()) {
                Object[] args = {this.defaultEntityReferenceSerializer.serialize(getDocumentReference())};
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY,
                    "Cannot add a template to document {0} because it already has content", null, args);
            } else {
                XWiki xwiki = context.getWiki();
                XWikiDocument templatedoc = xwiki.getDocument(templateDocumentReference, context);
                if (templatedoc.isNew()) {
                    Object[] args =
                        {this.defaultEntityReferenceSerializer.serialize(templateDocumentReference),
                        this.compactEntityReferenceSerializer.serialize(getDocumentReference())};
                    throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                        XWikiException.ERROR_XWIKI_APP_TEMPLATE_DOES_NOT_EXIST,
                        "Template document {0} does not exist when adding to document {1}", null, args);
                } else {
                    setTemplateDocumentReference(templateDocumentReference);
                    setContent(templatedoc.getContent());

                    // Set the new document syntax as the syntax of the template since the template content
                    // is copied into the new document
                    setSyntax(templatedoc.getSyntax());

                    // If the parent is not set in the current document set the template parent as the parent.
                    if (getParentReference() == null) {
                        setParentReference(templatedoc.getParentReference());
                    }

                    if (isNew()) {
                        // We might have received the object from the cache and the template objects might have been
                        // copied already we need to remove them
                        setXObjects(new TreeMap<DocumentReference, List<BaseObject>>());
                    }
                    // Merge the external objects.
                    // Currently the choice is not to merge the base class and object because it is not the prefered way
                    // of using external classes and objects.
                    mergeXObjects(templatedoc);
                }
            }
        }
        setContentDirty(true);
    }

    /**
     * @deprecated since 2.2M1 use {@link #readFromTemplate(DocumentReference, XWikiContext)} instead
     */
    @Deprecated
    public void readFromTemplate(String template, XWikiContext context) throws XWikiException
    {
        // Keep the same behavior for backward compatibility
        DocumentReference templateDocumentReference = null;
        if (StringUtils.isNotEmpty(template)) {
            templateDocumentReference = this.currentMixedDocumentReferenceResolver.resolve(template);
        }
        readFromTemplate(templateDocumentReference, context);
    }

    /**
     * Use the document passed as parameter as the new identity for the current document.
     * 
     * @param document the document containing the new identity
     * @throws XWikiException in case of error
     */
    private void clone(XWikiDocument document) throws XWikiException
    {
        setDocumentReference(document.getDocumentReference());
        setRCSVersion(document.getRCSVersion());
        setDocumentArchive(document.getDocumentArchive());
        setAuthor(document.getAuthor());
        setContentAuthor(document.getContentAuthor());
        setContent(document.getContent());
        setContentDirty(document.isContentDirty());
        setCreationDate(document.getCreationDate());
        setDate(document.getDate());
        setCustomClass(document.getCustomClass());
        setContentUpdateDate(document.getContentUpdateDate());
        setTitle(document.getTitle());
        setFormat(document.getFormat());
        setFromCache(document.isFromCache());
        setElements(document.getElements());
        setId(document.getId());
        setMeta(document.getMeta());
        setMetaDataDirty(document.isMetaDataDirty());
        setMostRecent(document.isMostRecent());
        setNew(document.isNew());
        setStore(document.getStore());
        setTemplateDocumentReference(document.getTemplateDocumentReference());
        setParent(document.getParent());
        setCreator(document.getCreator());
        setDefaultLanguage(document.getDefaultLanguage());
        setDefaultTemplate(document.getDefaultTemplate());
        setValidationScript(document.getValidationScript());
        setLanguage(document.getLanguage());
        setTranslation(document.getTranslation());
        setXClass((BaseClass) document.getXClass().clone());
        setXClassXML(document.getXClassXML());
        setComment(document.getComment());
        setMinorEdit(document.isMinorEdit());
        setSyntax(document.getSyntax());
        setHidden(document.isHidden());

        cloneXObjects(document);
        cloneAttachments(document);
        this.elements = document.elements;

        this.originalDocument = document.originalDocument;
    }

    @Override
    public XWikiDocument clone()
    {
        return cloneInternal(getDocumentReference(), true);
    }

    /**
     * Duplicate this document and give it a new name.
     * 
     * @since 2.2.3
     */
    public XWikiDocument duplicate(DocumentReference newDocumentReference)
    {
        return cloneInternal(newDocumentReference, false);
    }

    private XWikiDocument cloneInternal(DocumentReference newDocumentReference, boolean keepsIdentity)
    {
        XWikiDocument doc = null;
        try {
            Constructor< ? extends XWikiDocument> constructor = getClass().getConstructor(DocumentReference.class);
            doc = constructor.newInstance(newDocumentReference);

            // use version field instead of getRCSVersion because it returns "1.1" if version==null.
            doc.version = this.version;
            doc.setDocumentArchive(getDocumentArchive());
            doc.setAuthorReference(getAuthorReference());
            doc.setContentAuthorReference(getContentAuthorReference());
            doc.setContent(getContent());
            doc.setContentDirty(isContentDirty());
            doc.setCreationDate(getCreationDate());
            doc.setDate(getDate());
            doc.setCustomClass(getCustomClass());
            doc.setContentUpdateDate(getContentUpdateDate());
            doc.setTitle(getTitle());
            doc.setFormat(getFormat());
            doc.setFromCache(isFromCache());
            doc.setElements(getElements());
            doc.setId(getId());
            doc.setMeta(getMeta());
            doc.setMetaDataDirty(isMetaDataDirty());
            doc.setMostRecent(isMostRecent());
            doc.setNew(isNew());
            doc.setStore(getStore());
            doc.setTemplateDocumentReference(getTemplateDocumentReference());
            doc.setParentReference(getRelativeParentReference());
            doc.setCreatorReference(getCreatorReference());
            doc.setDefaultLanguage(getDefaultLanguage());
            doc.setDefaultTemplate(getDefaultTemplate());
            doc.setValidationScript(getValidationScript());
            doc.setLanguage(getLanguage());
            doc.setTranslation(getTranslation());
            doc.setXClass((BaseClass) getXClass().clone());
            doc.setXClassXML(getXClassXML());
            doc.setComment(getComment());
            doc.setMinorEdit(isMinorEdit());
            doc.setSyntax(getSyntax());
            doc.setHidden(isHidden());

            if (keepsIdentity) {
                doc.cloneXObjects(this);
                doc.cloneAttachments(this);
            } else {
                doc.duplicateXObjects(this);
                doc.copyAttachments(this);
            }

            doc.elements = this.elements;

            doc.originalDocument = this.originalDocument;
        } catch (Exception e) {
            // This should not happen
            LOGGER.error("Exception while cloning document", e);
        }
        return doc;
    }

    /**
     * Clone attachments from another document. This implementation expects that this document is the same as the other
     * document and thus attachments will be saved in the database in the same place as the ones which they are cloning.
     * 
     * @param sourceDocument an XWikiDocument to copy attachments from
     */
    private void cloneAttachments(final XWikiDocument sourceDocument)
    {
        this.getAttachmentList().clear();
        for (XWikiAttachment attach : sourceDocument.getAttachmentList()) {
            XWikiAttachment newAttach = (XWikiAttachment) attach.clone();

            // Document is set to this because if this document is renamed then the attachment will have a new id
            // and be saved somewhere different.
            newAttach.setDoc(this);

            this.getAttachmentList().add(newAttach);
        }
    }

    /**
     * Copy attachments from one document to another. This implementation expects that you are copying the attachment
     * from one document to another and thus it should be saved seperately from the original in the database.
     * 
     * @param sourceDocument an XWikiDocument to copy attachments from
     */
    public void copyAttachments(XWikiDocument sourceDocument)
    {
        getAttachmentList().clear();
        Iterator<XWikiAttachment> attit = sourceDocument.getAttachmentList().iterator();
        while (attit.hasNext()) {
            XWikiAttachment attachment = attit.next();
            XWikiAttachment newattachment = (XWikiAttachment) attachment.clone();
            newattachment.setDoc(this);

            // TODO: Why must attachment content must be set dirty --cjdelisle
            if (newattachment.getAttachment_content() != null) {
                newattachment.getAttachment_content().setContentDirty(true);
            }

            getAttachmentList().add(newattachment);
        }
        setContentDirty(true);
    }

    public void loadAttachments(XWikiContext context) throws XWikiException
    {
        for (XWikiAttachment attachment : getAttachmentList()) {
            attachment.loadContent(context);
            attachment.loadArchive(context);
        }
    }

    @Override
    public boolean equals(Object object)
    {
        // Same Java object, they sure are equal
        if (this == object) {
            return true;
        }

        XWikiDocument doc = (XWikiDocument) object;
        if (!getDocumentReference().equals(doc.getDocumentReference())) {
            return false;
        }

        if (!getAuthor().equals(doc.getAuthor())) {
            return false;
        }

        if (!getContentAuthor().equals(doc.getContentAuthor())) {
            return false;
        }

        if ((getParentReference() != null && !getParentReference().equals(doc.getParentReference()))
            || (getParentReference() == null && doc.getParentReference() != null)) {
            return false;
        }

        if (!getCreator().equals(doc.getCreator())) {
            return false;
        }

        if (!getDefaultLanguage().equals(doc.getDefaultLanguage())) {
            return false;
        }

        if (!getLanguage().equals(doc.getLanguage())) {
            return false;
        }

        if (getTranslation() != doc.getTranslation()) {
            return false;
        }

        if (getDate().getTime() != doc.getDate().getTime()) {
            return false;
        }

        if (getContentUpdateDate().getTime() != doc.getContentUpdateDate().getTime()) {
            return false;
        }

        if (getCreationDate().getTime() != doc.getCreationDate().getTime()) {
            return false;
        }

        if (!getFormat().equals(doc.getFormat())) {
            return false;
        }

        if (!getTitle().equals(doc.getTitle())) {
            return false;
        }

        if (!getContent().equals(doc.getContent())) {
            return false;
        }

        if (!getVersion().equals(doc.getVersion())) {
            return false;
        }

        if ((getTemplateDocumentReference() != null && !getTemplateDocumentReference().equals(
            doc.getTemplateDocumentReference()))
            || (getTemplateDocumentReference() == null && doc.getTemplateDocumentReference() != null)) {
            return false;
        }

        if (!getDefaultTemplate().equals(doc.getDefaultTemplate())) {
            return false;
        }

        if (!getValidationScript().equals(doc.getValidationScript())) {
            return false;
        }

        if (!getComment().equals(doc.getComment())) {
            return false;
        }

        if (isMinorEdit() != doc.isMinorEdit()) {
            return false;
        }

        if ((getSyntaxId() != null && !getSyntaxId().equals(doc.getSyntaxId()))
            || (getSyntaxId() == null && doc.getSyntaxId() != null)) {
            return false;
        }

        if (isHidden() != doc.isHidden()) {
            return false;
        }

        if (!getXClass().equals(doc.getXClass())) {
            return false;
        }

        Set<DocumentReference> myObjectClassReferences = getXObjects().keySet();
        Set<DocumentReference> otherObjectClassReferences = doc.getXObjects().keySet();
        if (!myObjectClassReferences.equals(otherObjectClassReferences)) {
            return false;
        }

        for (DocumentReference reference : myObjectClassReferences) {
            List<BaseObject> myObjects = getXObjects(reference);
            List<BaseObject> otherObjects = doc.getXObjects(reference);
            if (myObjects.size() != otherObjects.size()) {
                return false;
            }
            for (int i = 0; i < myObjects.size(); i++) {
                if ((myObjects.get(i) == null && otherObjects.get(i) != null)
                    || (myObjects.get(i) != null && otherObjects.get(i) == null)) {
                    return false;
                }
                if (myObjects.get(i) == null && otherObjects.get(i) == null) {
                    continue;
                }
                if (!myObjects.get(i).equals(otherObjects.get(i))) {
                    return false;
                }
            }
        }

        // We consider that 2 documents are still equal even when they have different original
        // documents (see getOriginalDocument() for more details as to what is an original
        // document).

        return true;
    }

    /**
     * Convert a {@link Document} into an XML string. You should prefer
     * {@link #toXML(OutputStream, boolean, boolean, boolean, boolean, XWikiContext)} or
     * {@link #toXML(com.xpn.xwiki.internal.xml.XMLWriter, boolean, boolean, boolean, boolean, XWikiContext)} when
     * possible to avoid memory load.
     * 
     * @param doc the {@link Document} to convert to a String
     * @param context current XWikiContext
     * @return an XML representation of the {@link Document}
     * @deprecated this method has nothing to do here and is apparently unused
     */
    @Deprecated
    public String toXML(Document doc, XWikiContext context)
    {
        String encoding = context.getWiki().getEncoding();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            XMLWriter wr = new XMLWriter(os, new OutputFormat("", true, encoding));
            wr.write(doc);
            return os.toString(encoding);
        } catch (IOException e) {
            LOGGER.error("Exception while doc.toXML", e);
            return "";
        }
    }

    /**
     * Retrieve the document in the current context language as an XML string. The rendrered document content and all
     * XObjects are included. Document attachments and archived versions are excluded. You should prefer
     * toXML(OutputStream, true, true, false, false, XWikiContext)} or toXML(com.xpn.xwiki.util.XMLWriter, true, true,
     * false, false, XWikiContext) on the translated document when possible to reduce memory load.
     * 
     * @param context current XWikiContext
     * @return a string containing an XML representation of the document in the current context language
     * @throws XWikiException when an error occurs during wiki operation
     */
    public String getXMLContent(XWikiContext context) throws XWikiException
    {
        XWikiDocument tdoc = getTranslatedDocument(context);
        return tdoc.toXML(true, true, false, false, context);
    }

    /**
     * Retrieve the document as an XML string. All XObject are included. Rendered content, attachments and archived
     * version are excluded. You should prefer toXML(OutputStream, true, false, false, false, XWikiContext)} or
     * toXML(com.xpn.xwiki.util.XMLWriter, true, false, false, false, XWikiContext) when possible to reduce memory load.
     * 
     * @param context current XWikiContext
     * @return a string containing an XML representation of the document
     * @throws XWikiException when an error occurs during wiki operation
     */
    public String toXML(XWikiContext context) throws XWikiException
    {
        return toXML(true, false, false, false, context);
    }

    /**
     * Retrieve the document as an XML string. All XObjects attachments and archived version are included. Rendered
     * content is excluded. You should prefer toXML(OutputStream, true, false, true, true, XWikiContext)} or
     * toXML(com.xpn.xwiki.util.XMLWriter, true, false, true, true, XWikiContext) when possible to reduce memory load.
     * 
     * @param context current XWikiContext
     * @return a string containing an XML representation of the document
     * @throws XWikiException when an error occurs during wiki operation
     */
    public String toFullXML(XWikiContext context) throws XWikiException
    {
        return toXML(true, false, true, true, context);
    }

    /**
     * Serialize the document into a new entry of an ZipOutputStream in XML format. All XObjects and attachments are
     * included. Rendered content is excluded.
     * 
     * @param zos the ZipOutputStream to write to
     * @param zipname the name of the new entry to create
     * @param withVersions if true, also include archived version of the document
     * @param context current XWikiContext
     * @throws XWikiException when an error occurs during xwiki operations
     * @throws IOException when an error occurs during streaming operations
     * @since 2.3M2
     */
    public void addToZip(ZipOutputStream zos, String zipname, boolean withVersions, XWikiContext context)
        throws XWikiException, IOException
    {
        ZipEntry zipentry = new ZipEntry(zipname);
        zos.putNextEntry(zipentry);
        toXML(zos, true, false, true, withVersions, context);
        zos.closeEntry();
    }

    /**
     * Serialize the document into a new entry of an ZipOutputStream in XML format. The new entry is named
     * 'LastSpaceName/DocumentName'. All XObjects and attachments are included. Rendered content is excluded.
     * 
     * @param zos the ZipOutputStream to write to
     * @param withVersions if true, also include archived version of the document
     * @param context current XWikiContext
     * @throws XWikiException when an error occurs during xwiki operations
     * @throws IOException when an error occurs during streaming operations
     * @since 2.3M2
     */
    public void addToZip(ZipOutputStream zos, boolean withVersions, XWikiContext context) throws XWikiException,
        IOException
    {
        String zipname =
            getDocumentReference().getLastSpaceReference().getName() + "/" + getDocumentReference().getName();
        String language = getLanguage();
        if (!StringUtils.isEmpty(language)) {
            zipname += "." + language;
        }
        addToZip(zos, zipname, withVersions, context);
    }

    /**
     * Serialize the document into a new entry of an ZipOutputStream in XML format. The new entry is named
     * 'LastSpaceName/DocumentName'. All XObjects, attachments and archived versions are included. Rendered content is
     * excluded.
     * 
     * @param zos the ZipOutputStream to write to
     * @param context current XWikiContext
     * @throws XWikiException when an error occurs during xwiki operations
     * @throws IOException when an error occurs during streaming operations
     * @since 2.3M2
     */
    public void addToZip(ZipOutputStream zos, XWikiContext context) throws XWikiException, IOException
    {
        addToZip(zos, true, context);
    }

    /**
     * Serialize the document to an XML string. You should prefer
     * {@link #toXML(OutputStream, boolean, boolean, boolean, boolean, XWikiContext)} or
     * {@link #toXML(com.xpn.xwiki.internal.xml.XMLWriter, boolean, boolean, boolean, boolean, XWikiContext)} when
     * possible to reduce memory load.
     * 
     * @param bWithObjects include XObjects
     * @param bWithRendering include the rendered content
     * @param bWithAttachmentContent include attachments content
     * @param bWithVersions include archived versions
     * @param context current XWikiContext
     * @return a string containing an XML representation of the document
     * @throws XWikiException when an errors occurs during wiki operations
     */
    public String toXML(boolean bWithObjects, boolean bWithRendering, boolean bWithAttachmentContent,
        boolean bWithVersions, XWikiContext context) throws XWikiException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            toXML(baos, bWithObjects, bWithRendering, bWithAttachmentContent, bWithVersions, context);
            return baos.toString(context.getWiki().getEncoding());
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * Serialize the document to an XML {@link DOMDocument}. All XObject are included. Rendered content, attachments and
     * archived version are excluded. You should prefer toXML(OutputStream, true, false, false, false, XWikiContext)} or
     * toXML(com.xpn.xwiki.util.XMLWriter, true, false, false, false, XWikiContext) when possible to reduce memory load.
     * 
     * @param context current XWikiContext
     * @return a {@link DOMDocument} containing the serialized document.
     * @throws XWikiException when an errors occurs during wiki operations
     */
    public Document toXMLDocument(XWikiContext context) throws XWikiException
    {
        return toXMLDocument(true, false, false, false, context);
    }

    /**
     * Serialize the document to an XML {@link DOMDocument}. You should prefer
     * {@link #toXML(OutputStream, boolean, boolean, boolean, boolean, XWikiContext)} or
     * {@link #toXML(com.xpn.xwiki.internal.xml.XMLWriter, boolean, boolean, boolean, boolean, XWikiContext)} when
     * possible to reduce memory load.
     * 
     * @param bWithObjects include XObjects
     * @param bWithRendering include the rendered content
     * @param bWithAttachmentContent include attachments content
     * @param bWithVersions include archived versions
     * @param context current XWikiContext
     * @return a {@link DOMDocument} containing the serialized document.
     * @throws XWikiException when an errors occurs during wiki operations
     */
    public Document toXMLDocument(boolean bWithObjects, boolean bWithRendering, boolean bWithAttachmentContent,
        boolean bWithVersions, XWikiContext context) throws XWikiException
    {
        Document doc = new DOMDocument();
        DOMXMLWriter wr = new DOMXMLWriter(doc, new OutputFormat("", true, context.getWiki().getEncoding()));

        try {
            toXML(wr, bWithObjects, bWithRendering, bWithAttachmentContent, bWithVersions, context);
            return doc;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Serialize the document to a {@link com.xpn.xwiki.internal.xml.XMLWriter}.
     * 
     * @param bWithObjects include XObjects
     * @param bWithRendering include the rendered content
     * @param bWithAttachmentContent include attachments content
     * @param bWithVersions include archived versions
     * @param context current XWikiContext
     * @throws XWikiException when an errors occurs during wiki operations
     * @throws IOException when an errors occurs during streaming operations
     * @since 2.3M2
     */
    public void toXML(XMLWriter wr, boolean bWithObjects, boolean bWithRendering, boolean bWithAttachmentContent,
        boolean bWithVersions, XWikiContext context) throws XWikiException, IOException
    {
        // IMPORTANT: we don't use SAX apis here because the specified XMLWriter could be a DOMXMLWriter for retro
        // compatibility reasons

        Element docel = new DOMElement("xwikidoc");
        wr.writeOpen(docel);

        Element el = new DOMElement("web");
        el.addText(getDocumentReference().getLastSpaceReference().getName());
        wr.write(el);

        el = new DOMElement("name");
        el.addText(getDocumentReference().getName());
        wr.write(el);

        el = new DOMElement("language");
        el.addText(getLanguage());
        wr.write(el);

        el = new DOMElement("defaultLanguage");
        el.addText(getDefaultLanguage());
        wr.write(el);

        el = new DOMElement("translation");
        el.addText("" + getTranslation());
        wr.write(el);

        el = new DOMElement("parent");
        if (getRelativeParentReference() == null) {
            // No parent have been specified
            el.addText("");
        } else {
            el.addText(this.defaultEntityReferenceSerializer.serialize(getRelativeParentReference()));
        }
        wr.write(el);

        el = new DOMElement("creator");
        el.addText(getCreator());
        wr.write(el);

        el = new DOMElement("author");
        el.addText(getAuthor());
        wr.write(el);

        el = new DOMElement("customClass");
        el.addText(getCustomClass());
        wr.write(el);

        el = new DOMElement("contentAuthor");
        el.addText(getContentAuthor());
        wr.write(el);

        long d = getCreationDate().getTime();
        el = new DOMElement("creationDate");
        el.addText("" + d);
        wr.write(el);

        d = getDate().getTime();
        el = new DOMElement("date");
        el.addText("" + d);
        wr.write(el);

        d = getContentUpdateDate().getTime();
        el = new DOMElement("contentUpdateDate");
        el.addText("" + d);
        wr.write(el);

        el = new DOMElement("version");
        el.addText(getVersion());
        wr.write(el);

        el = new DOMElement("title");
        el.addText(getTitle());
        wr.write(el);

        el = new DOMElement("template");
        if (getTemplateDocumentReference() == null) {
            // No template doc have been specified
            el.addText("");
        } else {
            el.addText(this.localEntityReferenceSerializer.serialize(getTemplateDocumentReference()));
        }
        wr.write(el);

        el = new DOMElement("defaultTemplate");
        el.addText(getDefaultTemplate());
        wr.write(el);

        el = new DOMElement("validationScript");
        el.addText(getValidationScript());
        wr.write(el);

        el = new DOMElement("comment");
        el.addText(getComment());
        wr.write(el);

        el = new DOMElement("minorEdit");
        el.addText(String.valueOf(isMinorEdit()));
        wr.write(el);

        el = new DOMElement("syntaxId");
        el.addText(getSyntaxId());
        wr.write(el);

        el = new DOMElement("hidden");
        el.addText(String.valueOf(isHidden()));
        wr.write(el);

        for (XWikiAttachment attach : getAttachmentList()) {
            attach.toXML(wr, bWithAttachmentContent, bWithVersions, context);
        }

        if (bWithObjects) {
            // Add Class
            BaseClass bclass = getXClass();
            if (bclass.getFieldList().size() > 0) {
                // If the class has fields, add class definition and field information to XML
                wr.write(bclass.toXML(null));
            }

            // Add Objects (THEIR ORDER IS MOLDED IN STONE!)
            for (List<BaseObject> objects : getXObjects().values()) {
                for (BaseObject obj : objects) {
                    if (obj != null) {
                        BaseClass objclass;
                        if (getDocumentReference() == obj.getXClassReference()) {
                            objclass = bclass;
                        } else {
                            objclass = obj.getXClass(context);
                        }
                        wr.write(obj.toXML(objclass));
                    }
                }
            }
        }

        // Add Content
        el = new DOMElement("content");

        // Filter filter = new CharacterFilter();
        // String newcontent = filter.process(getContent());
        // String newcontent = encodedXMLStringAsUTF8(getContent());
        String newcontent = this.content;
        el.addText(newcontent);
        wr.write(el);

        if (bWithRendering) {
            el = new DOMElement("renderedcontent");
            try {
                el.addText(getRenderedContent(context));
            } catch (XWikiException e) {
                el.addText("Exception with rendering content: " + e.getFullMessage());
            }
            wr.write(el);
        }

        if (bWithVersions) {
            el = new DOMElement("versions");
            try {
                el.addText(getDocumentArchive(context).getArchive(context));
                wr.write(el);
            } catch (XWikiException e) {
                LOGGER.error("Document [" + this.defaultEntityReferenceSerializer.serialize(getDocumentReference())
                    + "] has malformed history");
            }
        }
    }

    /**
     * Serialize the document to an OutputStream.
     * 
     * @param bWithObjects include XObjects
     * @param bWithRendering include the rendered content
     * @param bWithAttachmentContent include attachments content
     * @param bWithVersions include archived versions
     * @param context current XWikiContext
     * @throws XWikiException when an errors occurs during wiki operations
     * @throws IOException when an errors occurs during streaming operations
     * @since 2.3M2
     */
    public void toXML(OutputStream out, boolean bWithObjects, boolean bWithRendering, boolean bWithAttachmentContent,
        boolean bWithVersions, XWikiContext context) throws XWikiException, IOException
    {
        XMLWriter wr = new XMLWriter(out, new OutputFormat("", true, context.getWiki().getEncoding()));

        Document doc = new DOMDocument();
        wr.writeDocumentStart(doc);
        toXML(wr, bWithObjects, bWithRendering, bWithAttachmentContent, bWithVersions, context);
        wr.writeDocumentEnd(doc);
    }

    protected String encodedXMLStringAsUTF8(String xmlString)
    {
        if (xmlString == null) {
            return "";
        }

        int length = xmlString.length();
        char character;
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < length; i++) {
            character = xmlString.charAt(i);
            switch (character) {
                case '&':
                    result.append("&amp;");
                    break;
                case '"':
                    result.append("&quot;");
                    break;
                case '<':
                    result.append("&lt;");
                    break;
                case '>':
                    result.append("&gt;");
                    break;
                case '\n':
                    result.append("\n");
                    break;
                case '\r':
                    result.append("\r");
                    break;
                case '\t':
                    result.append("\t");
                    break;
                default:
                    if (character < 0x20) {
                    } else if (character > 0x7F) {
                        result.append("&#x");
                        result.append(Integer.toHexString(character).toUpperCase());
                        result.append(";");
                    } else {
                        result.append(character);
                    }
                    break;
            }
        }

        return result.toString();
    }

    protected String getElement(Element docel, String name)
    {
        Element el = docel.element(name);
        if (el == null) {
            return "";
        } else {
            return el.getText();
        }
    }

    public void fromXML(String xml) throws XWikiException
    {
        fromXML(xml, false);
    }

    public void fromXML(InputStream is) throws XWikiException
    {
        fromXML(is, false);
    }

    public void fromXML(String xml, boolean withArchive) throws XWikiException
    {
        SAXReader reader = new SAXReader();
        Document domdoc;

        try {
            StringReader in = new StringReader(xml);
            domdoc = reader.read(in);
        } catch (DocumentException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING,
                "Error parsing xml", e, null);
        }

        fromXML(domdoc, withArchive);
    }

    public void fromXML(InputStream in, boolean withArchive) throws XWikiException
    {
        SAXReader reader = new SAXReader();
        Document domdoc;

        try {
            domdoc = reader.read(in);
        } catch (DocumentException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING,
                "Error parsing xml", e, null);
        }

        fromXML(domdoc, withArchive);
    }

    public void fromXML(Document domdoc, boolean withArchive) throws XWikiException
    {
        Element docel = domdoc.getRootElement();

        // If, for some reason, the document name or space are not set in the XML input, we still ensure that the
        // constructed XWikiDocument object has a valid name or space (by using current document values if they are
        // missing). This is important since document name, space and wiki must always be set in a XWikiDocument
        // instance.
        String name = getElement(docel, "name");
        String space = getElement(docel, "web");

        if (StringUtils.isEmpty(name) || StringUtils.isEmpty(space)) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Invalid XML: \"name\" and \"web\" cannot be empty");
        }

        EntityReference reference =
            new EntityReference(name, EntityType.DOCUMENT, new EntityReference(space, EntityType.SPACE));
        reference = this.currentReferenceDocumentReferenceResolver.resolve(reference);
        setDocumentReference(new DocumentReference(reference));

        String parent = getElement(docel, "parent");
        if (StringUtils.isNotEmpty(parent)) {
            setParentReference(this.currentMixedDocumentReferenceResolver.resolve(parent));
        }

        setCreator(getElement(docel, "creator"));
        setAuthor(getElement(docel, "author"));
        setCustomClass(getElement(docel, "customClass"));
        setContentAuthor(getElement(docel, "contentAuthor"));
        if (docel.element("version") != null) {
            setVersion(getElement(docel, "version"));
        }
        setContent(getElement(docel, "content"));
        setLanguage(getElement(docel, "language"));
        setDefaultLanguage(getElement(docel, "defaultLanguage"));
        setTitle(getElement(docel, "title"));
        setDefaultTemplate(getElement(docel, "defaultTemplate"));
        setValidationScript(getElement(docel, "validationScript"));
        setComment(getElement(docel, "comment"));

        String minorEdit = getElement(docel, "minorEdit");
        setMinorEdit(Boolean.valueOf(minorEdit).booleanValue());

        String hidden = getElement(docel, "hidden");
        setHidden(Boolean.valueOf(hidden).booleanValue());

        String strans = getElement(docel, "translation");
        if ((strans == null) || strans.equals("")) {
            setTranslation(0);
        } else {
            setTranslation(Integer.parseInt(strans));
        }

        String archive = getElement(docel, "versions");
        if (withArchive && archive != null && archive.length() > 0) {
            setDocumentArchive(archive);
        }

        String sdate = getElement(docel, "date");
        if (!sdate.equals("")) {
            Date date = new Date(Long.parseLong(sdate));
            setDate(date);
        }

        String contentUpdateDateString = getElement(docel, "contentUpdateDate");
        if (!StringUtils.isEmpty(contentUpdateDateString)) {
            Date contentUpdateDate = new Date(Long.parseLong(contentUpdateDateString));
            setContentUpdateDate(contentUpdateDate);
        }

        String scdate = getElement(docel, "creationDate");
        if (!scdate.equals("")) {
            Date cdate = new Date(Long.parseLong(scdate));
            setCreationDate(cdate);
        }

        String syntaxId = getElement(docel, "syntaxId");
        if ((syntaxId == null) || (syntaxId.length() == 0)) {
            // Documents that don't have syntax ids are considered old documents and thus in
            // XWiki Syntax 1.0 since newer documents always have syntax ids.
            setSyntax(Syntax.XWIKI_1_0);
        } else {
            setSyntaxId(syntaxId);
        }

        List<Element> atels = docel.elements("attachment");
        for (Element atel : atels) {
            XWikiAttachment attach = new XWikiAttachment();
            attach.setDoc(this);
            attach.fromXML(atel);
            getAttachmentList().add(attach);
        }

        Element cel = docel.element("class");
        BaseClass bclass = new BaseClass();
        if (cel != null) {
            bclass.fromXML(cel);
            setXClass(bclass);
        }

        @SuppressWarnings("unchecked")
        List<Element> objels = docel.elements("object");
        for (Element objel : objels) {
            BaseObject bobject = new BaseObject();
            bobject.fromXML(objel);
            setXObject(bobject.getNumber(), bobject);
        }

        // We have been reading from XML so the document does not need a new version when saved
        setMetaDataDirty(false);
        setContentDirty(false);

        // Note: We don't set the original document as that is not stored in the XML, and it doesn't make much sense to
        // have an original document for a de-serialized object.
    }

    /**
     * Check if provided xml document is a wiki document.
     * 
     * @param domdoc the xml document.
     * @return true if provided xml document is a wiki document.
     */
    public static boolean containsXMLWikiDocument(Document domdoc)
    {
        return domdoc.getRootElement().getName().equals("xwikidoc");
    }

    public void setAttachmentList(List<XWikiAttachment> list)
    {
        this.attachmentList = list;
    }

    public List<XWikiAttachment> getAttachmentList()
    {
        return this.attachmentList;
    }

    public void saveAllAttachments(XWikiContext context) throws XWikiException
    {
        for (XWikiAttachment attachment : this.attachmentList) {
            saveAttachmentContent(attachment, context);
        }
    }

    public void saveAllAttachments(boolean updateParent, boolean transaction, XWikiContext context)
        throws XWikiException
    {
        for (XWikiAttachment attachment : this.attachmentList) {
            saveAttachmentContent(attachment, updateParent, transaction, context);
        }
    }

    public void saveAttachmentsContent(List<XWikiAttachment> attachments, XWikiContext context) throws XWikiException
    {
        String database = context.getDatabase();
        try {
            // We might need to switch database to get the translated content
            if (getDatabase() != null) {
                context.setDatabase(getDatabase());
            }

            context.getWiki().getAttachmentStore().saveAttachmentsContent(attachments, this, true, context, true);
        } catch (OutOfMemoryError e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_JAVA_HEAP_SPACE,
                "Out Of Memory Exception");
        } finally {
            if (database != null) {
                context.setDatabase(database);
            }
        }
    }

    public void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context) throws XWikiException
    {
        saveAttachmentContent(attachment, true, true, context);
    }

    public void saveAttachmentContent(XWikiAttachment attachment, boolean bParentUpdate, boolean bTransaction,
        XWikiContext context) throws XWikiException
    {
        String database = context.getDatabase();
        try {
            // We might need to switch database to
            // get the translated content
            if (getDatabase() != null) {
                context.setDatabase(getDatabase());
            }

            // We need to make sure there is a version upgrade
            setMetaDataDirty(true);

            context.getWiki().getAttachmentStore()
                .saveAttachmentContent(attachment, bParentUpdate, context, bTransaction);
        } catch (OutOfMemoryError e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_JAVA_HEAP_SPACE,
                "Out Of Memory Exception");
        } finally {
            if (database != null) {
                context.setDatabase(database);
            }
        }
    }

    public void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context) throws XWikiException
    {
        String database = context.getDatabase();
        try {
            // We might need to switch database to
            // get the translated content
            if (getDatabase() != null) {
                context.setDatabase(getDatabase());
            }

            context.getWiki().getAttachmentStore().loadAttachmentContent(attachment, context, true);
        } finally {
            if (database != null) {
                context.setDatabase(database);
            }
        }
    }

    public void deleteAttachment(XWikiAttachment attachment, XWikiContext context) throws XWikiException
    {
        deleteAttachment(attachment, true, context);
    }

    public void deleteAttachment(XWikiAttachment attachment, boolean toRecycleBin, XWikiContext context)
        throws XWikiException
    {
        String database = context.getDatabase();
        try {
            // We might need to switch database to
            // get the translated content
            if (getDatabase() != null) {
                context.setDatabase(getDatabase());
            }
            try {
                // We need to make sure there is a version upgrade
                setMetaDataDirty(true);
                if (toRecycleBin && context.getWiki().hasAttachmentRecycleBin(context)) {
                    context.getWiki().getAttachmentRecycleBinStore()
                        .saveToRecycleBin(attachment, context.getUser(), new Date(), context, true);
                }
                context.getWiki().getAttachmentStore().deleteXWikiAttachment(attachment, context, true);
            } catch (java.lang.OutOfMemoryError e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_JAVA_HEAP_SPACE, "Out Of Memory Exception");
            }
        } finally {
            if (database != null) {
                context.setDatabase(database);
            }
        }
    }

    /**
     * Get the wiki document references pointing to this document.
     * <p>
     * Theses links are stored to the database when documents are saved. You can use "backlinks" in XWikiPreferences or
     * "xwiki.backlinks" in xwiki.cfg file to enable links storage in the database.
     * 
     * @param context the XWiki context.
     * @return the found wiki document references
     * @throws XWikiException error when getting pages names from database.
     * @since 2.2M2
     */
    public List<DocumentReference> getBackLinkedReferences(XWikiContext context) throws XWikiException
    {
        return getStore(context).loadBacklinks(getDocumentReference(), true, context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #getBackLinkedReferences(XWikiContext)}
     */
    @Deprecated
    public List<String> getBackLinkedPages(XWikiContext context) throws XWikiException
    {
        return getStore(context).loadBacklinks(getFullName(), context, true);
    }

    /**
     * Get a list of unique links from this document to others documents.
     * <p>
     * <ul>
     * <li>1.0 content: get the unique links associated to document from database. This is stored when the document is
     * saved. You can use "backlinks" in XWikiPreferences or "xwiki.backlinks" in xwiki.cfg file to enable links storage
     * in the database.</li>
     * <li>Other content: call {@link #getUniqueLinkedPages(XWikiContext)} and generate the List</li>.
     * </ul>
     * 
     * @param context the XWiki context
     * @return the found wiki links.
     * @throws XWikiException error when getting links from database when 1.0 content.
     * @since 1.9M2
     */
    public Set<XWikiLink> getUniqueWikiLinkedPages(XWikiContext context) throws XWikiException
    {
        Set<XWikiLink> links;

        if (is10Syntax()) {
            links = new LinkedHashSet<XWikiLink>(getStore(context).loadLinks(getId(), context, true));
        } else {
            Set<String> linkedPages = getUniqueLinkedPages(context);
            links = new LinkedHashSet<XWikiLink>(linkedPages.size());
            for (String linkedPage : linkedPages) {
                XWikiLink wikiLink = new XWikiLink();

                wikiLink.setDocId(getId());
                wikiLink.setFullName(this.localEntityReferenceSerializer.serialize(getDocumentReference()));
                wikiLink.setLink(linkedPage);

                links.add(wikiLink);
            }
        }

        return links;
    }

    /**
     * Extract all the unique static (i.e. not generated by macros) wiki links (pointing to wiki page) from this 1.0
     * document's content to others documents.
     * 
     * @param context the XWiki context.
     * @return the document names for linked pages, if null an error append.
     * @since 1.9M2
     */
    private Set<String> getUniqueLinkedPages10(XWikiContext context)
    {
        Set<String> pageNames;

        try {
            List<String> list = context.getUtil().getUniqueMatches(getContent(), "\\[(.*?)\\]", 1);
            pageNames = new HashSet<String>(list.size());

            DocumentReference currentDocumentReference = getDocumentReference();
            for (String name : list) {
                int i1 = name.indexOf('>');
                if (i1 != -1) {
                    name = name.substring(i1 + 1);
                }
                i1 = name.indexOf("&gt;");
                if (i1 != -1) {
                    name = name.substring(i1 + 4);
                }
                i1 = name.indexOf('#');
                if (i1 != -1) {
                    name = name.substring(0, i1);
                }
                i1 = name.indexOf('?');
                if (i1 != -1) {
                    name = name.substring(0, i1);
                }

                // Let's get rid of anything that's not a real link
                if (name.trim().equals("") || (name.indexOf('$') != -1) || (name.indexOf("://") != -1)
                    || (name.indexOf('"') != -1) || (name.indexOf('\'') != -1) || (name.indexOf("..") != -1)
                    || (name.indexOf(':') != -1) || (name.indexOf('=') != -1)) {
                    continue;
                }

                // generate the link
                String newname = StringUtils.replace(Util.noaccents(name), " ", "");

                // If it is a local link let's add the space
                if (newname.indexOf('.') == -1) {
                    newname = getSpace() + "." + name;
                }
                if (context.getWiki().exists(newname, context)) {
                    name = newname;
                } else {
                    // If it is a local link let's add the space
                    if (name.indexOf('.') == -1) {
                        name = getSpace() + "." + name;
                    }
                }

                // If the reference is empty, the link is an autolink
                if (!StringUtils.isEmpty(name)) {
                    // The reference may not have the space or even document specified (in case of an empty
                    // string)
                    // Thus we need to find the fully qualified document name
                    DocumentReference documentReference = this.currentDocumentReferenceResolver.resolve(name);

                    // Verify that the link is not an autolink (i.e. a link to the current document)
                    if (!documentReference.equals(currentDocumentReference)) {
                        pageNames.add(this.compactEntityReferenceSerializer.serialize(documentReference));
                    }
                }
            }

            return pageNames;
        } catch (Exception e) {
            // This should never happen
            LOGGER.error("Failed to get linked documents", e);

            return null;
        }
    }

    /**
     * Extract all the unique static (i.e. not generated by macros) wiki links (pointing to wiki page) from this
     * document's content to others documents.
     * 
     * @param context the XWiki context.
     * @return the document names for linked pages, if null an error append.
     * @since 1.9M2
     */
    public Set<String> getUniqueLinkedPages(XWikiContext context)
    {
        Set<String> pageNames;

        XWikiDocument contextDoc = context.getDoc();
        String contextWiki = context.getDatabase();

        try {
            // Make sure the right document is used as context document
            context.setDoc(this);
            // Make sure the right wiki is used as context document
            context.setDatabase(getDatabase());

            if (is10Syntax()) {
                pageNames = getUniqueLinkedPages10(context);
            } else {
                XDOM dom = getXDOM();

                List<LinkBlock> linkBlocks = dom.getChildrenByType(LinkBlock.class, true);
                pageNames = new LinkedHashSet<String>(linkBlocks.size());

                DocumentReference currentDocumentReference = getDocumentReference();

                for (LinkBlock linkBlock : linkBlocks) {
                    ResourceReference reference = linkBlock.getReference();
                    if (reference.getType().equals(ResourceType.DOCUMENT)) {
                        // If the reference is empty, the link is an autolink
                        if (!StringUtils.isEmpty(reference.getReference())
                            || (StringUtils.isEmpty(reference.getParameter(DocumentResourceReference.ANCHOR)) && StringUtils
                                .isEmpty(reference.getParameter(DocumentResourceReference.QUERY_STRING)))) {
                            // The reference may not have the space or even document specified (in case of an empty
                            // string)
                            // Thus we need to find the fully qualified document name
                            DocumentReference documentReference =
                                this.currentDocumentReferenceResolver.resolve(reference.getReference());

                            // Verify that the link is not an autolink (i.e. a link to the current document)
                            if (!documentReference.equals(currentDocumentReference)) {
                                // Since this method is used for saving backlinks and since backlinks must be
                                // saved with the space and page name but without the wiki part, we remove the wiki
                                // part before serializing.
                                // This is a bit of a hack since the default serializer should theoretically fail
                                // if it's passed an invalid reference.
                                pageNames.add(this.compactWikiEntityReferenceSerializer.serialize(documentReference));
                            }
                        }
                    }
                }
            }
        } finally {
            context.setDoc(contextDoc);
            context.setDatabase(contextWiki);
        }

        return pageNames;
    }

    /**
     * Returns a list of references of all documents which list this document as their parent
     * {@link #getChildren(int, int, com.xpn.xwiki.XWikiContext)}
     * 
     * @since 2.2M2
     */
    public List<DocumentReference> getChildrenReferences(XWikiContext context) throws XWikiException
    {
        return getChildrenReferences(0, 0, context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #getChildrenReferences(XWikiContext)}
     */
    @Deprecated
    public List<String> getChildren(XWikiContext context) throws XWikiException
    {
        return getChildren(0, 0, context);
    }

    /**
     * Returns a list of references of all documents which list this document as their parent
     * 
     * @param nb The number of results to return.
     * @param start The number of results to skip before we begin returning results.
     * @param context The {@link com.xpn.xwiki.XWikiContext context}.
     * @return the list of document references
     * @throws XWikiException If there's an error querying the database.
     * @since 2.2M2
     */
    public List<DocumentReference> getChildrenReferences(int nb, int start, XWikiContext context) throws XWikiException
    {
        // Use cases:
        // - the parent document reference saved in the database matches the reference of this document, in its fully
        // serialized form (eg "wiki:space.page"). Note that this is normally not required since the wiki part
        // isn't saved in the database when it matches the current wiki.
        // - the parent document reference saved in the database matches the reference of this document, in its
        // serialized form without the wiki part (eg "space.page"). The reason we don't need to specify the wiki
        // part is because document parents saved in the database don't have the wiki part specified when it matches
        // the current wiki.
        // - the parent document reference saved in the database matches the page name part of this document's
        // reference (eg "page") and the parent document's space is the same as this document's space.
        List<String> params = Arrays.asList(this.defaultEntityReferenceSerializer.serialize(getDocumentReference()),
            this.localEntityReferenceSerializer.serialize(getDocumentReference()),
            getDocumentReference().getName(), getDocumentReference().getLastSpaceReference().getName());

        String whereStatement = "doc.parent=? or doc.parent=? or (doc.parent=? and doc.space=?)";
        return context.getWiki().getStore().searchDocumentReferences(whereStatement, nb, start, params, context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #getChildrenReferences(XWikiContext)}
     */
    @Deprecated
    public List<String> getChildren(int nb, int start, XWikiContext context) throws XWikiException
    {
        List<String> childrenNames = new ArrayList<String>();
        for (DocumentReference reference : getChildrenReferences(nb, start, context)) {
            childrenNames.add(this.localEntityReferenceSerializer.serialize(reference));
        }
        return childrenNames;
    }

    /**
     * @since 2.2M2
     */
    public void renameProperties(DocumentReference classReference, Map<String, String> fieldsToRename)
    {
        List<BaseObject> objects = getXObjects(classReference);
        if (objects == null) {
            return;
        }

        for (BaseObject bobject : objects) {
            if (bobject == null) {
                continue;
            }
            for (Map.Entry<String, String> entry : fieldsToRename.entrySet()) {
                String origname = entry.getKey();
                String newname = entry.getValue();
                BaseProperty origprop = (BaseProperty) bobject.safeget(origname);
                if (origprop != null) {
                    BaseProperty prop = (BaseProperty) origprop.clone();
                    bobject.removeField(origname);
                    prop.setName(newname);
                    bobject.addField(newname, prop);
                }
            }
        }

        setContentDirty(true);
    }

    /**
     * @deprecated since 2.2M2 use {@link #renameProperties(DocumentReference, Map)} instead
     */
    @Deprecated
    public void renameProperties(String className, Map<String, String> fieldsToRename)
    {
        renameProperties(resolveClassReference(className), fieldsToRename);
    }

    /**
     * @since 2.2M1
     */
    public void addXObjectToRemove(BaseObject object)
    {
        getXObjectsToRemove().add(object);
        setContentDirty(true);
    }

    /**
     * @deprecated since 2.2M2 use {@link #addXObjectToRemove(BaseObject)} )} instead
     */
    @Deprecated
    public void addObjectsToRemove(BaseObject object)
    {
        addXObjectToRemove(object);
    }

    /**
     * @since 2.2M2
     */
    public List<BaseObject> getXObjectsToRemove()
    {
        return this.xObjectsToRemove;
    }

    /**
     * @deprecated since 2.2M2 use {@link #getObjectsToRemove()} instead
     */
    @Deprecated
    public ArrayList<BaseObject> getObjectsToRemove()
    {
        return (ArrayList<BaseObject>) getXObjectsToRemove();
    }

    /**
     * @since 2.2M1
     */
    public void setXObjectsToRemove(List<BaseObject> objectsToRemove)
    {
        this.xObjectsToRemove = objectsToRemove;
        setContentDirty(true);
    }

    /**
     * @deprecated since 2.2M2 use {@link #setXObjectsToRemove(List)} instead
     */
    @Deprecated
    public void setObjectsToRemove(ArrayList<BaseObject> objectsToRemove)
    {
        setXObjectsToRemove(objectsToRemove);
    }

    public List<String> getIncludedPages(XWikiContext context)
    {
        if (is10Syntax()) {
            return getIncludedPagesForXWiki10Syntax(getContent(), context);
        } else {
            // Find all include macros listed on the page
            XDOM dom = getXDOM();

            List<String> result = new ArrayList<String>();
            for (MacroBlock macroBlock : dom.getChildrenByType(MacroBlock.class, true)) {

                // - Add each document pointed to by the include macro
                // - Also add all the included pages found in the velocity macro when using the deprecated #include*
                // macros
                // This should be removed when we fully drop support for the XWiki Syntax 1.0 but for now we want to
                // play
                // nice with people migrating from 1.0 to 2.0 syntax

                if (macroBlock.getId().equalsIgnoreCase("include")) {
                    String documentName = macroBlock.getParameters().get("document");
                    if (documentName.indexOf('.') == -1) {
                        documentName = getSpace() + "." + documentName;
                    }
                    result.add(documentName);
                } else if (macroBlock.getId().equalsIgnoreCase("velocity")
                    && !StringUtils.isEmpty(macroBlock.getContent())) {
                    // Try to find matching content inside each velocity macro
                    result.addAll(getIncludedPagesForXWiki10Syntax(macroBlock.getContent(), context));
                }
            }

            return result;
        }
    }

    private List<String> getIncludedPagesForXWiki10Syntax(String content, XWikiContext context)
    {
        try {
            String pattern = "#include(Topic|InContext|Form|Macros|parseGroovyFromPage)\\([\"'](.*?)[\"']\\)";
            List<String> list = context.getUtil().getUniqueMatches(content, pattern, 2);
            for (int i = 0; i < list.size(); i++) {
                String name = list.get(i);
                if (name.indexOf('.') == -1) {
                    list.set(i, getSpace() + "." + name);
                }
            }

            return list;
        } catch (Exception e) {
            LOGGER.error("Failed to extract include target from provided content [" + content + "]", e);

            return null;
        }
    }

    public List<String> getIncludedMacros(XWikiContext context)
    {
        return context.getWiki().getIncludedMacros(getSpace(), getContent(), context);
    }

    public String displayRendered(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context)
        throws XWikiException
    {
        String result = pclass.displayView(pclass.getName(), prefix, object, context);
        return getRenderedContent(result, Syntax.XWIKI_1_0.toIdString(), context);
    }

    public String displayView(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context)
    {
        return (pclass == null) ? "" : pclass.displayView(pclass.getName(), prefix, object, context);
    }

    public String displayEdit(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context)
    {
        return (pclass == null) ? "" : pclass.displayEdit(pclass.getName(), prefix, object, context);
    }

    public String displayHidden(PropertyClass pclass, String prefix, BaseCollection object, XWikiContext context)
    {
        return (pclass == null) ? "" : pclass.displayHidden(pclass.getName(), prefix, object, context);
    }

    public String displaySearch(PropertyClass pclass, String prefix, XWikiCriteria criteria, XWikiContext context)
    {
        return (pclass == null) ? "" : pclass.displaySearch(pclass.getName(), prefix, criteria, context);
    }

    public XWikiAttachment getAttachment(String filename)
    {
        for (XWikiAttachment attach : getAttachmentList()) {
            if (attach.getFilename().equals(filename)) {
                return attach;
            }
        }

        for (XWikiAttachment attach : getAttachmentList()) {
            if (attach.getFilename().startsWith(filename + ".")) {
                return attach;
            }
        }

        return null;
    }

    public XWikiAttachment addAttachment(String fileName, InputStream iStream, XWikiContext context)
        throws XWikiException, IOException
    {
        ByteArrayOutputStream bAOut = new ByteArrayOutputStream();
        IOUtils.copy(iStream, bAOut);

        return addAttachment(fileName, bAOut.toByteArray(), context);
    }

    public XWikiAttachment addAttachment(String fileName, byte[] data, XWikiContext context) throws XWikiException
    {
        int i = fileName.indexOf('\\');
        if (i == -1) {
            i = fileName.indexOf('/');
        }

        String filename = fileName.substring(i + 1);

        // TODO : avoid name clearing when encoding problems will be solved
        // JIRA : http://jira.xwiki.org/jira/browse/XWIKI-94
        filename = context.getWiki().clearName(filename, false, true, context);

        XWikiAttachment attachment = getAttachment(filename);
        if (attachment == null) {
            attachment = new XWikiAttachment();
            // TODO: Review this code and understand why it's needed.
            // Add the attachment in the current doc
            getAttachmentList().add(attachment);
        }

        attachment.setContent(data);
        attachment.setFilename(filename);
        attachment.setAuthor(context.getUser());
        // Add the attachment to the document
        attachment.setDoc(this);

        return attachment;
    }

    public BaseObject getFirstObject(String fieldname)
    {
        // Keeping this function with context null for compatibility reasons.
        // It should not be used, since it would miss properties which are only defined in the class
        // and not present in the object because the object was not updated
        return getFirstObject(fieldname, null);
    }

    public BaseObject getFirstObject(String fieldname, XWikiContext context)
    {
        Collection<List<BaseObject>> objectscoll = getXObjects().values();
        if (objectscoll == null) {
            return null;
        }

        for (List<BaseObject> objects : objectscoll) {
            for (BaseObject obj : objects) {
                if (obj != null) {
                    BaseClass bclass = obj.getXClass(context);
                    if (bclass != null) {
                        Set<String> set = bclass.getPropertyList();
                        if ((set != null) && set.contains(fieldname)) {
                            return obj;
                        }
                    }
                    Set<String> set = obj.getPropertyList();
                    if ((set != null) && set.contains(fieldname)) {
                        return obj;
                    }
                }
            }
        }

        return null;
    }

    /**
     * @since 2.2.3
     */
    public void setProperty(EntityReference classReference, String fieldName, BaseProperty value)
    {
        BaseObject bobject = prepareXObject(classReference);
        bobject.safeput(fieldName, value);
    }

    /**
     * @deprecated since 2.2M2 use {@link #setProperty(EntityReference, String, BaseProperty)} instead
     */
    @Deprecated
    public void setProperty(String className, String fieldName, BaseProperty value)
    {
        setProperty(this.xClassEntityReferenceResolver.resolve(className, EntityType.DOCUMENT, getDocumentReference()),
            fieldName, value);
    }

    /**
     * @since 2.2M2
     */
    public int getIntValue(DocumentReference classReference, String fieldName)
    {
        BaseObject obj = getXObject(classReference, 0);
        if (obj == null) {
            return 0;
        }

        return obj.getIntValue(fieldName);
    }

    /**
     * @deprecated since 2.2M2 use {@link #getIntValue(DocumentReference, String)} instead
     */
    @Deprecated
    public int getIntValue(String className, String fieldName)
    {
        return getIntValue(resolveClassReference(className), fieldName);
    }

    /**
     * @since 2.2M2
     */
    public long getLongValue(DocumentReference classReference, String fieldName)
    {
        BaseObject obj = getXObject(classReference, 0);
        if (obj == null) {
            return 0;
        }

        return obj.getLongValue(fieldName);
    }

    /**
     * @deprecated since 2.2M2 use {@link #getLongValue(DocumentReference, String)} instead
     */
    @Deprecated
    public long getLongValue(String className, String fieldName)
    {
        return getLongValue(resolveClassReference(className), fieldName);
    }

    /**
     * @since 2.2M2
     */
    public String getStringValue(DocumentReference classReference, String fieldName)
    {
        BaseObject obj = getXObject(classReference);
        if (obj == null) {
            return "";
        }

        String result = obj.getStringValue(fieldName);
        if (result.equals(" ")) {
            return "";
        } else {
            return result;
        }
    }

    /**
     * @deprecated since 2.2M2 use {@link #getStringValue(DocumentReference, String)} instead
     */
    @Deprecated
    public String getStringValue(String className, String fieldName)
    {
        return getStringValue(resolveClassReference(className), fieldName);
    }

    public int getIntValue(String fieldName)
    {
        BaseObject object = getFirstObject(fieldName, null);
        if (object == null) {
            return 0;
        } else {
            return object.getIntValue(fieldName);
        }
    }

    public long getLongValue(String fieldName)
    {
        BaseObject object = getFirstObject(fieldName, null);
        if (object == null) {
            return 0;
        } else {
            return object.getLongValue(fieldName);
        }
    }

    public String getStringValue(String fieldName)
    {
        BaseObject object = getFirstObject(fieldName, null);
        if (object == null) {
            return "";
        }

        String result = object.getStringValue(fieldName);
        if (result.equals(" ")) {
            return "";
        } else {
            return result;
        }
    }

    /**
     * @since 2.2.3
     */
    public void setStringValue(EntityReference classReference, String fieldName, String value)
    {
        BaseObject bobject = prepareXObject(classReference);
        bobject.setStringValue(fieldName, value);
    }

    /**
     * @deprecated since 2.2M2 use {@link #setStringValue(EntityReference, String, String)} instead
     */
    @Deprecated
    public void setStringValue(String className, String fieldName, String value)
    {
        setStringValue(
            this.xClassEntityReferenceResolver.resolve(className, EntityType.DOCUMENT, getDocumentReference()),
            fieldName, value);
    }

    /**
     * @since 2.2M2
     */
    public List getListValue(DocumentReference classReference, String fieldName)
    {
        BaseObject obj = getXObject(classReference);
        if (obj == null) {
            return new ArrayList();
        }

        return obj.getListValue(fieldName);
    }

    /**
     * @deprecated since 2.2M2 use {@link #getListValue(DocumentReference, String)} instead
     */
    @Deprecated
    public List getListValue(String className, String fieldName)
    {
        return getListValue(resolveClassReference(className), fieldName);
    }

    public List getListValue(String fieldName)
    {
        BaseObject object = getFirstObject(fieldName, null);
        if (object == null) {
            return new ArrayList();
        }

        return object.getListValue(fieldName);
    }

    /**
     * @since 2.2.3
     */
    public void setStringListValue(EntityReference classReference, String fieldName, List value)
    {
        BaseObject bobject = prepareXObject(classReference);
        bobject.setStringListValue(fieldName, value);
    }

    /**
     * @deprecated since 2.2M2 use {@link #setStringListValue(EntityReference, String, List)} instead
     */
    @Deprecated
    public void setStringListValue(String className, String fieldName, List value)
    {
        setStringListValue(
            this.xClassEntityReferenceResolver.resolve(className, EntityType.DOCUMENT, getDocumentReference()),
            fieldName, value);
    }

    /**
     * @since 2.2.3
     */
    public void setDBStringListValue(EntityReference classReference, String fieldName, List value)
    {
        BaseObject bobject = prepareXObject(classReference);
        bobject.setDBStringListValue(fieldName, value);
    }

    /**
     * @deprecated since 2.2M2 use {@link #setDBStringListValue(EntityReference, String, List)} instead
     */
    @Deprecated
    public void setDBStringListValue(String className, String fieldName, List value)
    {
        setDBStringListValue(
            this.xClassEntityReferenceResolver.resolve(className, EntityType.DOCUMENT, getDocumentReference()),
            fieldName, value);
    }

    /**
     * @since 2.2.3
     */
    public void setLargeStringValue(EntityReference classReference, String fieldName, String value)
    {
        BaseObject bobject = prepareXObject(classReference);
        bobject.setLargeStringValue(fieldName, value);
    }

    /**
     * @deprecated since 2.2M2 use {@link #setLargeStringValue(EntityReference, String, String)} instead
     */
    @Deprecated
    public void setLargeStringValue(String className, String fieldName, String value)
    {
        setLargeStringValue(
            this.xClassEntityReferenceResolver.resolve(className, EntityType.DOCUMENT, getDocumentReference()),
            fieldName, value);
    }

    /**
     * @since 2.2.3
     */
    public void setIntValue(EntityReference classReference, String fieldName, int value)
    {
        BaseObject bobject = prepareXObject(classReference);
        bobject.setIntValue(fieldName, value);
    }

    /**
     * @deprecated since 2.2M2 use {@link #setIntValue(EntityReference, String, int)} instead
     */
    @Deprecated
    public void setIntValue(String className, String fieldName, int value)
    {
        setIntValue(this.xClassEntityReferenceResolver.resolve(className, EntityType.DOCUMENT, getDocumentReference()),
            fieldName, value);
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for saving a XWikiDocument.
     * 
     * @deprecated since 2.2M1 use {@link #getDocumentReference()} instead
     */
    @Deprecated
    public String getDatabase()
    {
        return getDocumentReference().getWikiReference().getName();
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for loading a XWikiDocument.
     * 
     * @deprecated since 2.2M1 use {@link #setDocumentReference(DocumentReference)} instead
     */
    @Deprecated
    public void setDatabase(String database)
    {
        if (database != null) {
            getDocumentReference().getWikiReference().setName(database);

            // Clean the absolute parent reference cache to rebuild it next time getParentReference is called.
            this.parentReferenceCache = null;
        }
    }

    public String getLanguage()
    {
        if (this.language == null) {
            return "";
        } else {
            return this.language.trim();
        }
    }

    public void setLanguage(String language)
    {
        this.language = Util.normalizeLanguage(language);
    }

    public String getDefaultLanguage()
    {
        if (this.defaultLanguage == null) {
            return "";
        } else {
            return this.defaultLanguage.trim();
        }
    }

    public void setDefaultLanguage(String defaultLanguage)
    {
        this.defaultLanguage = defaultLanguage;

        setMetaDataDirty(true);
    }

    public int getTranslation()
    {
        return this.translation;
    }

    public void setTranslation(int translation)
    {
        this.translation = translation;

        setMetaDataDirty(true);
    }

    public String getTranslatedContent(XWikiContext context) throws XWikiException
    {
        String language = context.getWiki().getLanguagePreference(context);

        return getTranslatedContent(language, context);
    }

    public String getTranslatedContent(String language, XWikiContext context) throws XWikiException
    {
        XWikiDocument tdoc = getTranslatedDocument(language, context);
        return tdoc.getContent();
    }

    public XWikiDocument getTranslatedDocument(XWikiContext context) throws XWikiException
    {
        String language = context.getWiki().getLanguagePreference(context);
        return getTranslatedDocument(language, context);
    }

    public XWikiDocument getTranslatedDocument(String language, XWikiContext context) throws XWikiException
    {
        XWikiDocument tdoc = this;

        if (!((language == null) || (language.equals("")) || language.equals(getDefaultLanguage()))) {
            tdoc = new XWikiDocument(getDocumentReference());
            tdoc.setLanguage(language);
            String database = context.getDatabase();
            try {
                // We might need to switch database to
                // get the translated content
                if (getDatabase() != null) {
                    context.setDatabase(getDatabase());
                }

                tdoc = getStore(context).loadXWikiDoc(tdoc, context);

                if (tdoc.isNew()) {
                    tdoc = this;
                }
            } catch (Exception e) {
                tdoc = this;
            } finally {
                context.setDatabase(database);
            }
        }

        return tdoc;
    }

    public String getRealLanguage(XWikiContext context) throws XWikiException
    {
        return getRealLanguage();
    }

    public String getRealLanguage()
    {
        String lang = getLanguage();
        if ((lang.equals("") || lang.equals("default"))) {
            return getDefaultLanguage();
        } else {
            return lang;
        }
    }

    public List<String> getTranslationList(XWikiContext context) throws XWikiException
    {
        return getStore().getTranslationList(this, context);
    }

    public List<Delta> getXMLDiff(XWikiDocument fromDoc, XWikiDocument toDoc, XWikiContext context)
        throws XWikiException, DifferentiationFailedException
    {
        return getDeltas(Diff.diff(ToString.stringToArray(fromDoc.toXML(context)),
            ToString.stringToArray(toDoc.toXML(context))));
    }

    public List<Delta> getContentDiff(XWikiDocument fromDoc, XWikiDocument toDoc, XWikiContext context)
        throws XWikiException, DifferentiationFailedException
    {
        return getDeltas(Diff.diff(ToString.stringToArray(fromDoc.getContent()),
            ToString.stringToArray(toDoc.getContent())));
    }

    public List<Delta> getContentDiff(String fromRev, String toRev, XWikiContext context) throws XWikiException,
        DifferentiationFailedException
    {
        XWikiDocument fromDoc = context.getWiki().getDocument(this, fromRev, context);
        XWikiDocument toDoc = context.getWiki().getDocument(this, toRev, context);
        return getContentDiff(fromDoc, toDoc, context);
    }

    public List<Delta> getContentDiff(String fromRev, XWikiContext context) throws XWikiException,
        DifferentiationFailedException
    {
        XWikiDocument revdoc = context.getWiki().getDocument(this, fromRev, context);
        return getContentDiff(revdoc, this, context);
    }

    public List<Delta> getLastChanges(XWikiContext context) throws XWikiException, DifferentiationFailedException
    {
        Version version = getRCSVersion();
        try {
            String prev = getDocumentArchive(context).getPrevVersion(version).toString();
            XWikiDocument prevDoc = context.getWiki().getDocument(this, prev, context);

            return getDeltas(Diff.diff(ToString.stringToArray(prevDoc.getContent()),
                ToString.stringToArray(getContent())));
        } catch (Exception ex) {
            LOGGER.debug("Exception getting differences from previous version: " + ex.getMessage());
        }

        return new ArrayList<Delta>();
    }

    public List<Delta> getRenderedContentDiff(XWikiDocument fromDoc, XWikiDocument toDoc, XWikiContext context)
        throws XWikiException, DifferentiationFailedException
    {
        String originalContent, newContent;

        originalContent = context.getWiki().getRenderingEngine().renderText(fromDoc.getContent(), fromDoc, context);
        newContent = context.getWiki().getRenderingEngine().renderText(toDoc.getContent(), toDoc, context);

        return getDeltas(Diff.diff(ToString.stringToArray(originalContent), ToString.stringToArray(newContent)));
    }

    public List<Delta> getRenderedContentDiff(String fromRev, String toRev, XWikiContext context)
        throws XWikiException, DifferentiationFailedException
    {
        XWikiDocument fromDoc = context.getWiki().getDocument(this, fromRev, context);
        XWikiDocument toDoc = context.getWiki().getDocument(this, toRev, context);

        return getRenderedContentDiff(fromDoc, toDoc, context);
    }

    public List<Delta> getRenderedContentDiff(String fromRev, XWikiContext context) throws XWikiException,
        DifferentiationFailedException
    {
        XWikiDocument revdoc = context.getWiki().getDocument(this, fromRev, context);

        return getRenderedContentDiff(revdoc, this, context);
    }

    protected List<Delta> getDeltas(Revision rev)
    {
        List<Delta> list = new ArrayList<Delta>();
        for (int i = 0; i < rev.size(); i++) {
            list.add(rev.getDelta(i));
        }

        return list;
    }

    public List<MetaDataDiff> getMetaDataDiff(String fromRev, String toRev, XWikiContext context) throws XWikiException
    {
        XWikiDocument fromDoc = context.getWiki().getDocument(this, fromRev, context);
        XWikiDocument toDoc = context.getWiki().getDocument(this, toRev, context);

        return getMetaDataDiff(fromDoc, toDoc, context);
    }

    public List<MetaDataDiff> getMetaDataDiff(String fromRev, XWikiContext context) throws XWikiException
    {
        XWikiDocument revdoc = context.getWiki().getDocument(this, fromRev, context);

        return getMetaDataDiff(revdoc, this, context);
    }

    public List<MetaDataDiff> getMetaDataDiff(XWikiDocument fromDoc, XWikiDocument toDoc, XWikiContext context)
        throws XWikiException
    {
        List<MetaDataDiff> list = new ArrayList<MetaDataDiff>();

        if ((fromDoc == null) || (toDoc == null)) {
            return list;
        }

        if (!fromDoc.getTitle().equals(toDoc.getTitle())) {
            list.add(new MetaDataDiff("title", fromDoc.getTitle(), toDoc.getTitle()));
        }

        if (!fromDoc.getParent().equals(toDoc.getParent())) {
            list.add(new MetaDataDiff("parent", fromDoc.getParent(), toDoc.getParent()));
        }

        if (!fromDoc.getAuthor().equals(toDoc.getAuthor())) {
            list.add(new MetaDataDiff("author", fromDoc.getAuthor(), toDoc.getAuthor()));
        }

        if (!fromDoc.getSpace().equals(toDoc.getSpace())) {
            list.add(new MetaDataDiff("web", fromDoc.getSpace(), toDoc.getSpace()));
        }

        if (!fromDoc.getName().equals(toDoc.getName())) {
            list.add(new MetaDataDiff("name", fromDoc.getName(), toDoc.getName()));
        }

        if (!fromDoc.getLanguage().equals(toDoc.getLanguage())) {
            list.add(new MetaDataDiff("language", fromDoc.getLanguage(), toDoc.getLanguage()));
        }

        if (!fromDoc.getDefaultLanguage().equals(toDoc.getDefaultLanguage())) {
            list.add(new MetaDataDiff("defaultLanguage", fromDoc.getDefaultLanguage(), toDoc.getDefaultLanguage()));
        }

        return list;
    }

    public List<List<ObjectDiff>> getObjectDiff(String fromRev, String toRev, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument fromDoc = context.getWiki().getDocument(this, fromRev, context);
        XWikiDocument toDoc = context.getWiki().getDocument(this, toRev, context);

        return getObjectDiff(fromDoc, toDoc, context);
    }

    public List<List<ObjectDiff>> getObjectDiff(String fromRev, XWikiContext context) throws XWikiException
    {
        XWikiDocument revdoc = context.getWiki().getDocument(this, fromRev, context);

        return getObjectDiff(revdoc, this, context);
    }

    /**
     * Return the object differences between two document versions. There is no hard requirement on the order of the two
     * versions, but the results are semantically correct only if the two versions are given in the right order.
     * 
     * @param fromDoc The old ('before') version of the document.
     * @param toDoc The new ('after') version of the document.
     * @param context The {@link com.xpn.xwiki.XWikiContext context}.
     * @return The object differences. The returned list's elements are other lists, one for each changed object. The
     *         inner lists contain {@link ObjectDiff} elements, one object for each changed property of the object.
     *         Additionally, if the object was added or removed, then the first entry in the list will be an
     *         "object-added" or "object-removed" marker.
     * @throws XWikiException If there's an error computing the differences.
     */
    public List<List<ObjectDiff>> getObjectDiff(XWikiDocument fromDoc, XWikiDocument toDoc, XWikiContext context)
        throws XWikiException
    {
        ArrayList<List<ObjectDiff>> difflist = new ArrayList<List<ObjectDiff>>();

        // Since objects could have been deleted or added, we iterate on both the old and the new
        // object collections.
        // First, iterate over the old objects.
        for (List<BaseObject> objects : fromDoc.getXObjects().values()) {
            for (BaseObject originalObj : objects) {
                // This happens when objects are deleted, and the document is still in the cache
                // storage.
                if (originalObj != null) {
                    BaseObject newObj = toDoc.getXObject(originalObj.getXClassReference(), originalObj.getNumber());
                    List<ObjectDiff> dlist;
                    if (newObj == null) {
                        // The object was deleted.
                        dlist = new BaseObject().getDiff(originalObj, context);
                        ObjectDiff deleteMarker =
                            new ObjectDiff(originalObj.getXClassReference(), originalObj.getNumber(), originalObj.getGuid(),
                                ObjectDiff.ACTION_OBJECTREMOVED, "", "", "", "");
                        dlist.add(0, deleteMarker);
                    } else {
                        // The object exists in both versions, but might have been changed.
                        dlist = newObj.getDiff(originalObj, context);
                    }
                    if (dlist.size() > 0) {
                        difflist.add(dlist);
                    }
                }
            }
        }

        // Second, iterate over the objects which are only in the new version.
        for (List<BaseObject> objects : toDoc.getXObjects().values()) {
            for (BaseObject newObj : objects) {
                // This happens when objects are deleted, and the document is still in the cache
                // storage.
                if (newObj != null) {
                    BaseObject originalObj = fromDoc.getXObject(newObj.getXClassReference(), newObj.getNumber());
                    if (originalObj == null) {
                        // TODO: Refactor this so that getDiff() accepts null Object as input.
                        // Only consider added objects, the other case was treated above.
                        originalObj = new BaseObject();
                        originalObj.setXClassReference(newObj.getXClassReference());
                        originalObj.setNumber(newObj.getNumber());
                        originalObj.setGuid(newObj.getGuid());
                        List<ObjectDiff> dlist = newObj.getDiff(originalObj, context);
                        ObjectDiff addMarker =
                            new ObjectDiff(newObj.getXClassReference(), newObj.getNumber(), newObj.getGuid(),
                                ObjectDiff.ACTION_OBJECTADDED, "", "", "", "");
                        dlist.add(0, addMarker);
                        if (dlist.size() > 0) {
                            difflist.add(dlist);
                        }
                    }
                }
            }
        }

        return difflist;
    }

    public List<List<ObjectDiff>> getClassDiff(XWikiDocument fromDoc, XWikiDocument toDoc, XWikiContext context)
        throws XWikiException
    {
        ArrayList<List<ObjectDiff>> difflist = new ArrayList<List<ObjectDiff>>();
        BaseClass oldClass = fromDoc.getXClass();
        BaseClass newClass = toDoc.getXClass();

        if ((newClass == null) && (oldClass == null)) {
            return difflist;
        }

        List<ObjectDiff> dlist = newClass.getDiff(oldClass, context);
        if (dlist.size() > 0) {
            difflist.add(dlist);
        }

        return difflist;
    }

    /**
     * @param fromDoc
     * @param toDoc
     * @param context
     * @return
     * @throws XWikiException
     */
    public List<AttachmentDiff> getAttachmentDiff(XWikiDocument fromDoc, XWikiDocument toDoc, XWikiContext context)
        throws XWikiException
    {
        List<AttachmentDiff> difflist = new ArrayList<AttachmentDiff>();
        for (XWikiAttachment origAttach : fromDoc.getAttachmentList()) {
            String fileName = origAttach.getFilename();
            XWikiAttachment newAttach = toDoc.getAttachment(fileName);
            if (newAttach == null) {
                difflist.add(new AttachmentDiff(fileName, origAttach.getVersion(), null));
            } else {
                if (!origAttach.getVersion().equals(newAttach.getVersion())) {
                    difflist.add(new AttachmentDiff(fileName, origAttach.getVersion(), newAttach.getVersion()));
                }
            }
        }

        for (XWikiAttachment newAttach : toDoc.getAttachmentList()) {
            String fileName = newAttach.getFilename();
            XWikiAttachment origAttach = fromDoc.getAttachment(fileName);
            if (origAttach == null) {
                difflist.add(new AttachmentDiff(fileName, null, newAttach.getVersion()));
            }
        }

        return difflist;
    }

    /**
     * Rename the current document and all the backlinks leading to it. Will also change parent field in all documents
     * which list the document we are renaming as their parent.
     * <p>
     * See {@link #rename(String, java.util.List, com.xpn.xwiki.XWikiContext)} for more details.
     * 
     * @param newDocumentReference the new document reference
     * @param context the ubiquitous XWiki Context
     * @throws XWikiException in case of an error
     * @since 2.2M2
     */
    public void rename(DocumentReference newDocumentReference, XWikiContext context) throws XWikiException
    {
        rename(newDocumentReference, getBackLinkedReferences(context), context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #rename(DocumentReference, XWikiContext)}
     */
    @Deprecated
    public void rename(String newDocumentName, XWikiContext context) throws XWikiException
    {
        rename(newDocumentName, getBackLinkedPages(context), context);
    }

    /**
     * Rename the current document and all the links pointing to it in the list of passed backlink documents. The
     * renaming algorithm takes into account the fact that there are several ways to write a link to a given page and
     * all those forms need to be renamed. For example the following links all point to the same page:
     * <ul>
     * <li>[Page]</li>
     * <li>[Page?param=1]</li>
     * <li>[currentwiki:Page]</li>
     * <li>[CurrentSpace.Page]</li>
     * <li>[currentwiki:CurrentSpace.Page]</li>
     * </ul>
     * <p>
     * Note: links without a space are renamed with the space added and all documents which have the document being
     * renamed as parent have their parent field set to "currentwiki:CurrentSpace.Page".
     * </p>
     * 
     * @param newDocumentReference the new document reference
     * @param backlinkDocumentReferences the list of references of documents to parse and for which links will be
     *            modified to point to the new document reference
     * @param context the ubiquitous XWiki Context
     * @throws XWikiException in case of an error
     * @since 2.2M2
     */
    public void rename(DocumentReference newDocumentReference, List<DocumentReference> backlinkDocumentReferences,
        XWikiContext context) throws XWikiException
    {
        rename(newDocumentReference, backlinkDocumentReferences, getChildrenReferences(context), context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #rename(DocumentReference, java.util.List, com.xpn.xwiki.XWikiContext)}
     */
    @Deprecated
    public void rename(String newDocumentName, List<String> backlinkDocumentNames, XWikiContext context)
        throws XWikiException
    {
        rename(newDocumentName, backlinkDocumentNames, getChildren(context), context);
    }

    /**
     * Same as {@link #rename(String, List, XWikiContext)} but the list of documents having the current document as
     * their parent is passed in parameter.
     * 
     * @param newDocumentReference the new document reference
     * @param backlinkDocumentReferences the list of references of documents to parse and for which links will be
     *            modified to point to the new document reference
     * @param childDocumentReferences the list of references of document whose parent field will be set to the new
     *            document reference
     * @param context the ubiquitous XWiki Context
     * @throws XWikiException in case of an error
     * @since 2.2M2
     */
    public void rename(DocumentReference newDocumentReference, List<DocumentReference> backlinkDocumentReferences,
        List<DocumentReference> childDocumentReferences, XWikiContext context) throws XWikiException
    {
        // TODO: Do all this in a single DB transaction as otherwise the state will be unknown if
        // something fails in the middle...

        // TODO: Why do we verify if the document has just been created and not been saved.
        // If the user is trying to rename to the same name... In that case, simply exits for efficiency.
        if (isNew() || getDocumentReference().equals(newDocumentReference)) {
            return;
        }

        // Grab the xwiki object, it gets used a few times.
        XWiki xwiki = context.getWiki();

        // Step 1: Copy the document and all its translations under a new document with the new reference.
        xwiki.copyDocument(getDocumentReference(), newDocumentReference, false, context);

        // Step 2: For each child document, update its parent reference.
        if (childDocumentReferences != null) {
            for (DocumentReference childDocumentReference : childDocumentReferences) {
                XWikiDocument childDocument = xwiki.getDocument(childDocumentReference, context);
                childDocument.setParentReference(newDocumentReference);
                String saveMessage = context.getMessageTool().get("core.comment.renameParent",
                    Arrays.asList(this.compactEntityReferenceSerializer.serialize(newDocumentReference)));
                xwiki.saveDocument(childDocument, saveMessage, true, context);
            }
        }

        // Step 3: For each backlink to rename, parse the backlink document and replace the links with the new name.
        // Note: we ignore invalid links here. Invalid links should be shown to the user so
        // that they fix them but the rename feature ignores them.
        DocumentParser documentParser = new DocumentParser();

        // This link handler recognizes that 2 links are the same when they point to the same
        // document (regardless of query string, target or alias). It keeps the query string,
        // target and alias from the link being replaced.
        RenamePageReplaceLinkHandler linkHandler = new RenamePageReplaceLinkHandler();

        // Used for replacing links in XWiki Syntax 1.0
        Link oldLink = createLink(getDocumentReference());
        Link newLink = createLink(newDocumentReference);

        for (DocumentReference backlinkDocumentReference : backlinkDocumentReferences) {
            XWikiDocument backlinkDocument = xwiki.getDocument(backlinkDocumentReference, context);

            if (backlinkDocument.is10Syntax()) {
                // Note: Here we cannot do a simple search/replace as there are several ways to point
                // to the same document. For example [Page], [Page?param=1], [currentwiki:Page],
                // [CurrentSpace.Page] all point to the same document. Thus we have to parse the links
                // to recognize them and do the replace.
                ReplacementResultCollection result = documentParser.parseLinksAndReplace(backlinkDocument.getContent(),
                    oldLink, newLink, linkHandler, getDocumentReference().getLastSpaceReference().getName());

                backlinkDocument.setContent((String) result.getModifiedContent());
            } else if (Utils.getComponentManager().hasComponent(BlockRenderer.class,
                backlinkDocument.getSyntax().toIdString())) {
                backlinkDocument.refactorDocumentLinks(getDocumentReference(), newDocumentReference, context);
            }

            String saveMessage = context.getMessageTool().get("core.comment.renameLink",
                Arrays.asList(this.compactEntityReferenceSerializer.serialize(newDocumentReference)));
            xwiki.saveDocument(backlinkDocument, saveMessage, true, context);
        }

        // Get new document
        XWikiDocument newDocument = xwiki.getDocument(newDocumentReference, context);

        // Step 4: Refactor the links contained in the document
        if (Utils.getComponentManager().hasComponent(BlockRenderer.class, getSyntax().toIdString())) {
            // Only support syntax for which a renderer is provided
            XDOM newDocumentXDOM = newDocument.getXDOM();
            List<LinkBlock> linkBlockList = newDocumentXDOM.getChildrenByType(LinkBlock.class, true);

            boolean modified = false;
            for (LinkBlock linkBlock : linkBlockList) {
                ResourceReference linkReference = linkBlock.getReference();
                if (linkReference.getType().equals(ResourceType.DOCUMENT)) {
                    DocumentReference currentLinkReference = this.explicitDocumentReferenceResolver.resolve(
                        linkReference.getReference(), getDocumentReference());

                    DocumentReference newLinkReference = this.explicitDocumentReferenceResolver.resolve(
                        linkReference.getReference(), newDocument.getDocumentReference());

                    if (!newLinkReference.equals(currentLinkReference)) {
                        modified = true;
                        linkReference.setReference(this.compactWikiEntityReferenceSerializer.serialize(
                            currentLinkReference, newDocument.getDocumentReference()));
                    }
                }
            }
            // Set new content and save document if needed
            if (modified) {
                newDocument.setContent(newDocumentXDOM);
                xwiki.saveDocument(newDocument, context);
            }
        }

        // Step 5: Delete the old document
        xwiki.deleteDocument(this, context);

        // Step 6: The current document needs to point to the renamed document as otherwise it's pointing to an
        // invalid XWikiDocument object as it's been deleted...
        clone(newDocument);
    }

    /**
     * Generate a {@link Link} object from {@link DocumentReference} to be used in
     * {@link DocumentParser#parseLinksAndReplace(String, Link, Link, com.xpn.xwiki.content.parsers.ReplaceLinkHandler, String)}
     * 
     * @param documentReference the full document reference
     * @return a {@link Link}
     */
    private Link createLink(DocumentReference documentReference)
    {
        Link link = new Link();

        link.setVirtualWikiAlias(documentReference.getWikiReference().getName());
        link.setSpace(documentReference.getLastSpaceReference().getName());
        link.setPage(documentReference.getName());

        return link;
    }

    /**
     * @deprecated since 2.2M2 use {@link #rename(DocumentReference, List, List, com.xpn.xwiki.XWikiContext)}
     */
    @Deprecated
    public void rename(String newDocumentName, List<String> backlinkDocumentNames, List<String> childDocumentNames,
        XWikiContext context) throws XWikiException
    {
        List<DocumentReference> backlinkDocumentReferences = new ArrayList<DocumentReference>();
        for (String backlinkDocumentName : backlinkDocumentNames) {
            backlinkDocumentReferences.add(this.currentMixedDocumentReferenceResolver.resolve(backlinkDocumentName));
        }

        List<DocumentReference> childDocumentReferences = new ArrayList<DocumentReference>();
        for (String childDocumentName : childDocumentNames) {
            childDocumentReferences.add(this.currentMixedDocumentReferenceResolver.resolve(childDocumentName));
        }

        rename(this.currentMixedDocumentReferenceResolver.resolve(newDocumentName), backlinkDocumentReferences,
            childDocumentReferences, context);
    }

    /**
     * @since 2.2M1
     */
    private void refactorDocumentLinks(DocumentReference oldDocumentReference, DocumentReference newDocumentReference,
        XWikiContext context) throws XWikiException
    {
        XDOM xdom = getXDOM();

        List<LinkBlock> linkBlockList = xdom.getChildrenByType(LinkBlock.class, true);

        for (LinkBlock linkBlock : linkBlockList) {
            ResourceReference linkReference = linkBlock.getReference();
            if (linkReference.getType().equals(ResourceType.DOCUMENT)) {
                DocumentReference documentReference = this.explicitDocumentReferenceResolver.resolve(
                    linkReference.getReference(), getDocumentReference());

                if (documentReference.equals(oldDocumentReference)) {
                    linkReference.setReference(this.compactEntityReferenceSerializer.serialize(newDocumentReference,
                        getDocumentReference()));
                }
            }
        }

        setContent(xdom);
    }

    /**
     * @since 2.2M1
     */
    public XWikiDocument copyDocument(DocumentReference newDocumentReference, XWikiContext context)
        throws XWikiException
    {
        loadAttachments(context);
        loadArchive(context);

        XWikiDocument newdoc = duplicate(newDocumentReference);
        newdoc.setOriginalDocument(null);
        newdoc.setContentDirty(true);
        newdoc.getXClass().setDocumentReference(newDocumentReference);

        XWikiDocumentArchive archive = newdoc.getDocumentArchive();
        if (archive != null) {
            newdoc.setDocumentArchive(archive.clone(newdoc.getId(), context));
        }

        return newdoc;
    }

    /**
     * @deprecated since 2.2M1 use {@link #copyDocument(DocumentReference, XWikiContext)} instead
     */
    @Deprecated
    public XWikiDocument copyDocument(String newDocumentName, XWikiContext context) throws XWikiException
    {
        return copyDocument(this.currentMixedDocumentReferenceResolver.resolve(newDocumentName), context);
    }

    public XWikiLock getLock(XWikiContext context) throws XWikiException
    {
        XWikiLock theLock = getStore(context).loadLock(getId(), context, true);
        if (theLock != null) {
            int timeout = context.getWiki().getXWikiPreferenceAsInt("lock_Timeout", 30 * 60, context);
            if (theLock.getDate().getTime() + timeout * 1000 < new Date().getTime()) {
                getStore(context).deleteLock(theLock, context, true);
                theLock = null;
            }
        }

        return theLock;
    }

    public void setLock(String userName, XWikiContext context) throws XWikiException
    {
        XWikiLock lock = new XWikiLock(getId(), userName);
        getStore(context).saveLock(lock, context, true);
    }

    public void removeLock(XWikiContext context) throws XWikiException
    {
        XWikiLock lock = getStore(context).loadLock(getId(), context, true);
        if (lock != null) {
            getStore(context).deleteLock(lock, context, true);
        }
    }

    public void insertText(String text, String marker, XWikiContext context) throws XWikiException
    {
        setContent(StringUtils.replaceOnce(getContent(), marker, text + marker));
        context.getWiki().saveDocument(this, context);
    }

    public Object getWikiNode()
    {
        return this.wikiNode;
    }

    public void setWikiNode(Object wikiNode)
    {
        this.wikiNode = wikiNode;
    }

    /**
     * @since 2.2M1
     */
    public String getXClassXML()
    {
        return this.xClassXML;
    }

    /**
     * @deprecated since 2.2M1 use {@link #getXClassXML()} instead
     */
    @Deprecated
    public String getxWikiClassXML()
    {
        return getXClassXML();
    }

    /**
     * @since 2.2M1
     */
    public void setXClassXML(String xClassXML)
    {
        this.xClassXML = xClassXML;
    }

    /**
     * @deprecated since 2.2M1 use {@link #setXClassXML(String)} ()} instead
     */
    @Deprecated
    public void setxWikiClassXML(String xClassXML)
    {
        setXClassXML(xClassXML);
    }

    public int getElements()
    {
        return this.elements;
    }

    public void setElements(int elements)
    {
        this.elements = elements;
    }

    public void setElement(int element, boolean toggle)
    {
        if (toggle) {
            this.elements = this.elements | element;
        } else {
            this.elements = this.elements & (~element);
        }
    }

    public boolean hasElement(int element)
    {
        return ((this.elements & element) == element);
    }

    /**
     * Gets the default edit mode for this document. An edit mode (other than the default "edit") can be enforced by
     * creating an {@code XWiki.EditModeClass} object in the current document, with the appropriate value for the
     * defaultEditMode property, or by adding this object in a sheet included by the document. This function also falls
     * back on the old {@code SheetClass}, deprecated since 3.1M2, which can be attached to included documents to
     * specify that the current document should be edited inline.
     * 
     * @return the default edit mode for this document ("edit" or "inline" usually)
     * @param context the context of the request for this document
     * @throws XWikiException if an error happens when computing the edit mode
     */
    public String getDefaultEditMode(XWikiContext context) throws XWikiException
    {
        String editModeProperty = "defaultEditMode";
        DocumentReference editModeClass =
            currentReferenceDocumentReferenceResolver.resolve(XWikiConstant.EDIT_MODE_CLASS);
        // check if the current document has any edit mode class object attached to it, and read the edit mode from it
        BaseObject editModeObject = this.getXObject(editModeClass);
        if (editModeObject != null) {
            String defaultEditMode = editModeObject.getStringValue(editModeProperty);
            if (StringUtils.isEmpty(defaultEditMode)) {
                return "edit";
            } else {
                return defaultEditMode;
            }
        }
        // otherwise look for included documents
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        if (is10Syntax()) {
            if (getContent().indexOf("includeForm(") != -1) {
                return "inline";
            }
        } else {
            // Algorithm: look in all include macro and for all document included check if one of them
            // has an EditModeClass object attached to it, or a SheetClass object (deprecated since 3.1M2) attached to
            // it. If so then the edit mode is inline.

            // Find all include macros and extract the document names
            // TODO: Is there a good way not to hardcode the macro name? The macro itself shouldn't know
            // its own name since it's a deployment time concern.
            for (Block macroBlock : getXDOM().getBlocks(new MacroBlockMatcher("include"), Axes.CHILD)) {
                String documentName = macroBlock.getParameter("document");
                if (documentName != null) {
                    // Resolve the document name into a valid Reference
                    DocumentReference documentReference =
                        this.currentMixedDocumentReferenceResolver.resolve(documentName);
                    XWikiDocument includedDocument = xwiki.getDocument(documentReference, context);
                    if (!includedDocument.isNew()) {
                        // get the edit mode object, first the new class and then the deprecated class if new class
                        // is not found
                        editModeObject = includedDocument.getXObject(editModeClass);
                        if (editModeObject == null) {
                            editModeObject = includedDocument.getObject(XWikiConstant.SHEET_CLASS);
                        }
                        if (editModeObject != null) {
                            // Use the user-defined default edit mode if set.
                            String defaultEditMode = editModeObject.getStringValue(editModeProperty);
                            if (StringUtils.isBlank(defaultEditMode)) {
                                // TODO: maybe here the real value should be returned if the object is edit mode class,
                                // and inline only if the object is sheetclass
                                return "inline";
                            } else {
                                return defaultEditMode;
                            }
                        }
                    }
                }
            }
        }

        return "edit";
    }

    public String getDefaultEditURL(XWikiContext context) throws XWikiException
    {
        String editMode = getDefaultEditMode(context);

        if ("inline".equals(editMode)) {
            return getEditURL("inline", "", context);
        } else {
            com.xpn.xwiki.XWiki xwiki = context.getWiki();
            String editor = xwiki.getEditorPreference(context);
            return getEditURL("edit", editor, context);
        }
    }

    public String getEditURL(String action, String mode, XWikiContext context) throws XWikiException
    {
        com.xpn.xwiki.XWiki xwiki = context.getWiki();
        String language = "";
        XWikiDocument tdoc = (XWikiDocument) context.get("tdoc");
        String realLang = tdoc.getRealLanguage(context);
        if ((xwiki.isMultiLingual(context) == true) && (!realLang.equals(""))) {
            language = realLang;
        }

        return getEditURL(action, mode, language, context);
    }

    public String getEditURL(String action, String mode, String language, XWikiContext context)
    {
        StringBuffer editparams = new StringBuffer();
        if (!mode.equals("")) {
            editparams.append("xpage=");
            editparams.append(mode);
        }

        if (!language.equals("")) {
            if (!mode.equals("")) {
                editparams.append("&");
            }
            editparams.append("language=");
            editparams.append(language);
        }

        return getURL(action, editparams.toString(), context);
    }

    public String getDefaultTemplate()
    {
        if (this.defaultTemplate == null) {
            return "";
        } else {
            return this.defaultTemplate;
        }
    }

    public void setDefaultTemplate(String defaultTemplate)
    {
        this.defaultTemplate = defaultTemplate;

        setMetaDataDirty(true);
    }

    public Vector<BaseObject> getComments()
    {
        return getComments(true);
    }

    /**
     * @return the syntax of the document
     * @since 2.3M1
     */
    public Syntax getSyntax()
    {
        // Can't be initialized in the XWikiDocument constructor because #getDefaultDocumentSyntax() need to create a
        // XWikiDocument object to get preferences from wiki preferences pages and would thus generate an infinite loop
        if (isNew() && this.syntax == null) {
            this.syntax = getDefaultDocumentSyntax();
        }

        return this.syntax;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method cannot be removed for now since it's used by Hibernate for saving a XWikiDocument.
     * 
     * @see org.xwiki.bridge.DocumentModelBridge#getSyntaxId()
     * @deprecated since 2.3M1, use {link #getSyntax()} instead
     */
    @Deprecated
    public String getSyntaxId()
    {
        return getSyntax().toIdString();
    }

    /**
     * @param syntax the new syntax to set for this document
     * @see #getSyntax()
     * @since 2.3M1
     */
    public void setSyntax(Syntax syntax)
    {
        this.syntax = syntax;
    }

    /**
     * Note that this method cannot be removed for now since it's used by Hibernate for saving a XWikiDocument.
     * 
     * @param syntaxId the new syntax id to set (eg "xwiki/1.0", "xwiki/2.0", etc)
     * @see #getSyntaxId()
     * @deprecated since 2.3M1, use {link #setSyntax(Syntax)} instead
     */
    @Deprecated
    public void setSyntaxId(String syntaxId)
    {
        Syntax syntax;

        // In order to preserve backward-compatibility with previous versions of XWiki in which the notion of Syntax Id
        // did not exist, we check the passed syntaxId parameter. Since this parameter comes from the database (it's
        // called automatically by Hibernate) it can be NULL or empty. In this case we consider the document is in
        // syntax/1.0 syntax.
        if (StringUtils.isBlank(syntaxId)) {
            syntax = Syntax.XWIKI_1_0;
        } else {
            try {
                syntax = this.syntaxFactory.createSyntaxFromIdString(syntaxId);
            } catch (ParseException e) {
                syntax = getDefaultDocumentSyntax();
                LOGGER.warn("Failed to set syntax [" + syntaxId + "] for ["
                    + this.defaultEntityReferenceSerializer.serialize(getDocumentReference())
                    + "], setting syntax [" + syntax.toIdString() + "] instead.", e);
            }
        }

        setSyntax(syntax);
    }

    public Vector<BaseObject> getComments(boolean asc)
    {
        Vector<BaseObject> list = getObjects("XWiki.XWikiComments");
        if (asc) {
            return list;
        } else {
            if (list == null) {
                return list;
            }
            Vector<BaseObject> newlist = new Vector<BaseObject>();
            for (int i = list.size() - 1; i >= 0; i--) {
                newlist.add(list.get(i));
            }
            return newlist;
        }
    }

    public boolean isCurrentUserCreator(XWikiContext context)
    {
        return isCreator(context.getUser());
    }

    public boolean isCreator(String username)
    {
        if (username.equals(XWikiRightService.GUEST_USER_FULLNAME)) {
            return false;
        }

        return username.equals(getCreator());
    }

    public boolean isCurrentUserPage(XWikiContext context)
    {
        DocumentReference userReference = context.getUserReference();
        if (userReference == null) {
            return false;
        }

        return userReference.equals(getDocumentReference());
    }

    public boolean isCurrentLocalUserPage(XWikiContext context)
    {
        String username = context.getLocalUser();
        if (username.equals(XWikiRightService.GUEST_USER_FULLNAME)) {
            return false;
        }

        return context.getUser().equals(getFullName());
    }

    public void resetArchive(XWikiContext context) throws XWikiException
    {
        boolean hasVersioning = context.getWiki().hasVersioning(context);
        if (hasVersioning) {
            getVersioningStore(context).resetRCSArchive(this, true, context);
        }
    }

    /**
     * Adds an object from an new object creation form.
     * 
     * @since 2.2M2
     */
    public BaseObject addXObjectFromRequest(XWikiContext context) throws XWikiException
    {
        // Read info in object
        ObjectAddForm form = new ObjectAddForm();
        form.setRequest((HttpServletRequest) context.getRequest());
        form.readRequest();

        EntityReference classReference = this.xClassEntityReferenceResolver.resolve(form.getClassName(),
            EntityType.DOCUMENT, getDocumentReference());
        BaseObject object = newXObject(classReference, context);
        BaseClass baseclass = object.getXClass(context);
        baseclass.fromMap(
            form.getObject(this.localEntityReferenceSerializer.serialize(resolveClassReference(classReference))),
            object);

        return object;
    }

    /**
     * @deprecated since 2.2M2 use {@link #addXObjectFromRequest(XWikiContext)}
     */
    @Deprecated
    public BaseObject addObjectFromRequest(XWikiContext context) throws XWikiException
    {
        return addXObjectFromRequest(context);
    }

    /**
     * Adds an object from an new object creation form.
     * 
     * @since 2.2.3
     */
    public BaseObject addXObjectFromRequest(EntityReference classReference, XWikiContext context) throws XWikiException
    {
        return addXObjectFromRequest(classReference, "", 0, context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #addXObjectFromRequest(EntityReference, XWikiContext)}
     */
    @Deprecated
    public BaseObject addObjectFromRequest(String className, XWikiContext context) throws XWikiException
    {
        return addObjectFromRequest(className, "", 0, context);
    }

    /**
     * Adds an object from an new object creation form.
     * 
     * @since 2.2M2
     */
    public BaseObject addXObjectFromRequest(DocumentReference classReference, String prefix, XWikiContext context)
        throws XWikiException
    {
        return addXObjectFromRequest(classReference, prefix, 0, context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #addXObjectFromRequest(DocumentReference, String, XWikiContext)}
     */
    @Deprecated
    public BaseObject addObjectFromRequest(String className, String prefix, XWikiContext context) throws XWikiException
    {
        return addObjectFromRequest(className, prefix, 0, context);
    }

    /**
     * Adds multiple objects from an new objects creation form.
     * 
     * @since 2.2M2
     */
    public List<BaseObject> addXObjectsFromRequest(DocumentReference classReference, XWikiContext context)
        throws XWikiException
    {
        return addXObjectsFromRequest(classReference, "", context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #addXObjectsFromRequest(DocumentReference, XWikiContext)}
     */
    @Deprecated
    public List<BaseObject> addObjectsFromRequest(String className, XWikiContext context) throws XWikiException
    {
        return addObjectsFromRequest(className, "", context);
    }

    /**
     * Adds multiple objects from an new objects creation form.
     * 
     * @since 2.2M2
     */
    public List<BaseObject> addXObjectsFromRequest(DocumentReference classReference, String pref, XWikiContext context)
        throws XWikiException
    {
        @SuppressWarnings("unchecked")
        Map<String, String[]> map = context.getRequest().getParameterMap();
        List<Integer> objectsNumberDone = new ArrayList<Integer>();
        List<BaseObject> objects = new ArrayList<BaseObject>();
        String start = pref + this.localEntityReferenceSerializer.serialize(classReference) + "_";

        for (String name : map.keySet()) {
            if (name.startsWith(start)) {
                int pos = name.indexOf('_', start.length() + 1);
                String prefix = name.substring(0, pos);
                int num = Integer.decode(prefix.substring(prefix.lastIndexOf('_') + 1)).intValue();
                if (!objectsNumberDone.contains(Integer.valueOf(num))) {
                    objectsNumberDone.add(Integer.valueOf(num));
                    objects.add(addXObjectFromRequest(classReference, pref, num, context));
                }
            }
        }

        return objects;
    }

    /**
     * @deprecated since 2.2M2 use {@link #addXObjectsFromRequest(DocumentReference, String, XWikiContext)}
     */
    @Deprecated
    public List<BaseObject> addObjectsFromRequest(String className, String pref, XWikiContext context)
        throws XWikiException
    {
        return addXObjectsFromRequest(resolveClassReference(className), pref, context);
    }

    /**
     * Adds object from an new object creation form.
     * 
     * @since 2.2M2
     */
    public BaseObject addXObjectFromRequest(DocumentReference classReference, int num, XWikiContext context)
        throws XWikiException
    {
        return addXObjectFromRequest(classReference, "", num, context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #addXObjectFromRequest(DocumentReference, int, XWikiContext)}
     */
    @Deprecated
    public BaseObject addObjectFromRequest(String className, int num, XWikiContext context) throws XWikiException
    {
        return addObjectFromRequest(className, "", num, context);
    }

    /**
     * Adds object from an new object creation form.
     * 
     * @since 2.2.3
     */
    public BaseObject addXObjectFromRequest(EntityReference classReference, String prefix, int num, XWikiContext context)
        throws XWikiException
    {
        BaseObject object = newXObject(classReference, context);
        BaseClass baseclass = object.getXClass(context);
        String newPrefix =
            prefix + this.localEntityReferenceSerializer.serialize(resolveClassReference(classReference)) + "_" + num;
        baseclass.fromMap(Util.getObject(context.getRequest(), newPrefix), object);

        return object;
    }

    /**
     * @deprecated since 2.2M2 use {@link #addXObjectFromRequest(EntityReference, String, int, XWikiContext)}
     */
    @Deprecated
    public BaseObject addObjectFromRequest(String className, String prefix, int num, XWikiContext context)
        throws XWikiException
    {
        return addXObjectFromRequest(resolveClassReference(className), prefix, num, context);
    }

    /**
     * Adds an object from an new object creation form.
     * 
     * @since 2.2.3
     */
    public BaseObject updateXObjectFromRequest(EntityReference classReference, XWikiContext context)
        throws XWikiException
    {
        return updateXObjectFromRequest(classReference, "", context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #updateXObjectFromRequest(EntityReference, XWikiContext)}
     */
    @Deprecated
    public BaseObject updateObjectFromRequest(String className, XWikiContext context) throws XWikiException
    {
        return updateObjectFromRequest(className, "", context);
    }

    /**
     * Adds an object from an new object creation form.
     * 
     * @since 2.2.3
     */
    public BaseObject updateXObjectFromRequest(EntityReference classReference, String prefix, XWikiContext context)
        throws XWikiException
    {
        return updateXObjectFromRequest(classReference, prefix, 0, context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #updateXObjectFromRequest(EntityReference, String, XWikiContext)}
     */
    @Deprecated
    public BaseObject updateObjectFromRequest(String className, String prefix, XWikiContext context)
        throws XWikiException
    {
        return updateObjectFromRequest(className, prefix, 0, context);
    }

    /**
     * Adds an object from an new object creation form.
     * 
     * @since 2.2.3
     */
    public BaseObject updateXObjectFromRequest(EntityReference classReference, String prefix, int num,
        XWikiContext context) throws XWikiException
    {
        DocumentReference absoluteClassReference = resolveClassReference(classReference);
        int nb;
        BaseObject oldobject = getXObject(absoluteClassReference, num);
        if (oldobject == null) {
            nb = createXObject(classReference, context);
            oldobject = getXObject(absoluteClassReference, nb);
        } else {
            nb = oldobject.getNumber();
        }
        BaseClass baseclass = oldobject.getXClass(context);
        String newPrefix = prefix + this.localEntityReferenceSerializer.serialize(absoluteClassReference) + "_" + nb;
        BaseObject newobject =
            (BaseObject) baseclass.fromMap(Util.getObject(context.getRequest(), newPrefix), oldobject);
        newobject.setNumber(oldobject.getNumber());
        newobject.setGuid(oldobject.getGuid());
        newobject.setDocumentReference(getDocumentReference());
        setXObject(nb, newobject);

        return newobject;
    }

    /**
     * @deprecated since 2.2M2 use {@link #updateXObjectFromRequest(EntityReference, String, int, XWikiContext)}
     */
    @Deprecated
    public BaseObject updateObjectFromRequest(String className, String prefix, int num, XWikiContext context)
        throws XWikiException
    {
        return updateXObjectFromRequest(
            this.xClassEntityReferenceResolver.resolve(className, EntityType.DOCUMENT, getDocumentReference()), prefix,
            num, context);
    }

    /**
     * Adds an object from an new object creation form.
     * 
     * @since 2.2.3
     */
    public List<BaseObject> updateXObjectsFromRequest(EntityReference classReference, XWikiContext context)
        throws XWikiException
    {
        return updateXObjectsFromRequest(classReference, "", context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #updateXObjectsFromRequest(EntityReference, XWikiContext)}
     */
    @Deprecated
    public List<BaseObject> updateObjectsFromRequest(String className, XWikiContext context) throws XWikiException
    {
        return updateObjectsFromRequest(className, "", context);
    }

    /**
     * Adds multiple objects from an new objects creation form.
     * 
     * @since 2.2.3
     */
    public List<BaseObject> updateXObjectsFromRequest(EntityReference classReference, String pref, XWikiContext context)
        throws XWikiException
    {
        DocumentReference absoluteClassReference = resolveClassReference(classReference);
        @SuppressWarnings("unchecked")
        Map<String, String[]> map = context.getRequest().getParameterMap();
        List<Integer> objectsNumberDone = new ArrayList<Integer>();
        List<BaseObject> objects = new ArrayList<BaseObject>();
        String start = pref + this.localEntityReferenceSerializer.serialize(absoluteClassReference) + "_";

        for (String name : map.keySet()) {
            if (name.startsWith(start)) {
                int pos = name.indexOf('_', start.length() + 1);
                String prefix = name.substring(0, pos);
                int num = Integer.decode(prefix.substring(prefix.lastIndexOf('_') + 1)).intValue();
                if (!objectsNumberDone.contains(Integer.valueOf(num))) {
                    objectsNumberDone.add(Integer.valueOf(num));
                    objects.add(updateXObjectFromRequest(classReference, pref, num, context));
                }
            }
        }

        return objects;
    }

    /**
     * @deprecated since 2.2M2 use {@link #updateXObjectsFromRequest(EntityReference, String, XWikiContext)}
     */
    @Deprecated
    public List<BaseObject> updateObjectsFromRequest(String className, String pref, XWikiContext context)
        throws XWikiException
    {
        return updateXObjectsFromRequest(
            this.xClassEntityReferenceResolver.resolve(className, EntityType.DOCUMENT, getDocumentReference()), pref,
            context);
    }

    public boolean isAdvancedContent()
    {
        String[] matches =
            {"<%", "#set", "#include", "#if", "public class", "/* Advanced content */", "## Advanced content",
            "/* Programmatic content */", "## Programmatic content"};
        String content2 = getContent().toLowerCase();
        for (int i = 0; i < matches.length; i++) {
            if (content2.indexOf(matches[i].toLowerCase()) != -1) {
                return true;
            }
        }

        if (HTML_TAG_PATTERN.matcher(content2).find()) {
            return true;
        }

        return false;
    }

    public boolean isProgrammaticContent()
    {
        String[] matches =
            {"<%", "\\$xwiki.xWiki", "$context.context", "$doc.document", "$xwiki.getXWiki()", "$context.getContext()",
            "$doc.getDocument()", "WithProgrammingRights(", "/* Programmatic content */", "## Programmatic content",
            "$xwiki.search(", "$xwiki.createUser", "$xwiki.createNewWiki", "$xwiki.addToAllGroup",
            "$xwiki.sendMessage", "$xwiki.copyDocument", "$xwiki.copyWikiWeb", "$xwiki.copySpaceBetweenWikis",
            "$xwiki.parseGroovyFromString", "$doc.toXML()", "$doc.toXMLDocument()",};
        String content2 = getContent().toLowerCase();
        for (int i = 0; i < matches.length; i++) {
            if (content2.indexOf(matches[i].toLowerCase()) != -1) {
                return true;
            }
        }

        return false;
    }

    /**
     * Remove an XObject from the document. The changes are not persisted until the document is saved.
     * 
     * @param object the object to remove
     * @return {@code true} if the object was successfully removed, {@code false} if the object was not found in the
     *         current document.
     * @since 2.2M1
     */
    public boolean removeXObject(BaseObject object)
    {
        List<BaseObject> objects = getXObjects(object.getXClassReference());
        // No objects at all, nothing to remove
        if (objects == null) {
            return false;
        }
        // Sometimes the object vector is wrongly indexed, meaning that objects are not at the right position
        // Check if the right object is in place
        int objectPosition = object.getNumber();
        if (objectPosition < objects.size()) {
            BaseObject storedObject = objects.get(objectPosition);
            if (storedObject == null || !storedObject.equals(object)) {
                // Try to find the correct position
                objectPosition = objects.indexOf(object);
            }
        } else {
            // The object position is greater than the array, that's invalid!
            objectPosition = -1;
        }
        // If the object is not in the document, simply ignore this request
        if (objectPosition < 0) {
            return false;
        }
        // We don't remove objects, but set null in their place, so that the object number corresponds to its position
        // in the vector
        objects.set(objectPosition, null);
        // Schedule the object for removal from the storage
        addObjectsToRemove(object);

        return true;
    }

    /**
     * Remove an XObject from the document. The changes are not persisted until the document is saved.
     * 
     * @param object the object to remove
     * @return {@code true} if the object was successfully removed, {@code false} if the object was not found in the
     *         current document.
     * @deprecated since 2.2M1, use {@link #removeXObject(com.xpn.xwiki.objects.BaseObject)} instead
     */
    @Deprecated
    public boolean removeObject(BaseObject object)
    {
        return removeXObject(object);
    }

    /**
     * Remove all the objects of a given type (XClass) from the document. The object counter is left unchanged, so that
     * future objects will have new (different) numbers. However, on some storage engines the counter will be reset if
     * the document is removed from the cache and reloaded from the persistent storage.
     * 
     * @param classReference The XClass reference of the XObjects to be removed.
     * @return {@code true} if the objects were successfully removed, {@code false} if no object from the target class
     *         was in the current document.
     * @since 2.2M1
     */
    public boolean removeXObjects(DocumentReference classReference)
    {
        List<BaseObject> objects = getXObjects(classReference);
        // No objects at all, nothing to remove
        if (objects == null) {
            return false;
        }
        // Schedule the object for removal from the storage
        for (BaseObject object : objects) {
            if (object != null) {
                addObjectsToRemove(object);
            }
        }
        // Empty the vector, retaining its size
        int currentSize = objects.size();
        objects.clear();
        for (int i = 0; i < currentSize; i++) {
            objects.add(null);
        }

        return true;
    }

    /**
     * Remove all the objects of a given type (XClass) from the document. The object counter is left unchanged, so that
     * future objects will have new (different) numbers. However, on some storage engines the counter will be reset if
     * the document is removed from the cache and reloaded from the persistent storage.
     * 
     * @param className The class name of the objects to be removed.
     * @return {@code true} if the objects were successfully removed, {@code false} if no object from the target class
     *         was in the current document.
     * @deprecated since 2.2M1 use {@link #removeXObjects(org.xwiki.model.reference.DocumentReference)} instead
     */
    @Deprecated
    public boolean removeObjects(String className)
    {
        return removeXObjects(resolveClassReference(className));
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
        if (is10Syntax()) {
            return getSections10();
        } else {
            List<DocumentSection> splitSections = new ArrayList<DocumentSection>();
            List<HeaderBlock> headers = getFilteredHeaders();

            int sectionNumber = 1;
            for (HeaderBlock header : headers) {
                // put -1 as index since there is no way to get the position of the header in the source
                int documentSectionIndex = -1;

                // Need to do the same thing than 1.0 content here
                String documentSectionLevel = StringUtils.repeat("1.", header.getLevel().getAsInt() - 1) + "1";

                DocumentSection docSection = new DocumentSection(sectionNumber++, documentSectionIndex,
                    documentSectionLevel, renderXDOM(new XDOM(header.getChildren()), getSyntax()));
                splitSections.add(docSection);
            }

            return splitSections;
        }
    }

    /**
     * Get XWiki context from execution context.
     * 
     * @return the XWiki context for the current thread
     */
    private XWikiContext getXWikiContext()
    {
        Execution execution = Utils.getComponent(Execution.class);

        ExecutionContext ec = execution.getContext();

        XWikiContext context = null;
        if (ec != null) {
            context = (XWikiContext) ec.getProperty("xwikicontext");
        }

        return context;
    }

    /**
     * Filter the headers from a document XDOM based on xwiki.section.depth property from xwiki.cfg file.
     * 
     * @return the filtered headers
     */
    private List<HeaderBlock> getFilteredHeaders()
    {
        List<HeaderBlock> filteredHeaders = new ArrayList<HeaderBlock>();

        // get the headers
        List<HeaderBlock> headers = getXDOM().getChildrenByType(HeaderBlock.class, true);

        // get the maximum header level
        int sectionDepth = 2;
        XWikiContext context = getXWikiContext();
        if (context != null) {
            sectionDepth = (int) context.getWiki().getSectionEditingDepth();
        }

        // filter the headers
        for (HeaderBlock header : headers) {
            if (header.getLevel().getAsInt() <= sectionDepth) {
                filteredHeaders.add(header);
            }
        }

        return filteredHeaders;
    }

    /**
     * @return the sections in the current document
     */
    private List<DocumentSection> getSections10()
    {
        // Pattern to match the title. Matches only level 1 and level 2 headings.
        Pattern headingPattern = Pattern.compile("^[ \\t]*+(1(\\.1){0,1}+)[ \\t]++(.++)$", Pattern.MULTILINE);
        Matcher matcher = headingPattern.matcher(getContent());
        List<DocumentSection> splitSections = new ArrayList<DocumentSection>();
        int sectionNumber = 0;
        // find title to split
        while (matcher.find()) {
            ++sectionNumber;
            String sectionLevel = matcher.group(1);
            String sectionTitle = matcher.group(3);
            int sectionIndex = matcher.start();
            // Initialize a documentSection object.
            DocumentSection docSection = new DocumentSection(sectionNumber, sectionIndex, sectionLevel, sectionTitle);
            // Add the document section to list.
            splitSections.add(docSection);
        }

        return splitSections;
    }

    /**
     * Return a Document section with parameter is sectionNumber.
     * 
     * @param sectionNumber the index (+1) of the section in the list of all sections in the document.
     * @return
     * @throws XWikiException error when extracting sections from document
     */
    public DocumentSection getDocumentSection(int sectionNumber) throws XWikiException
    {
        // return a document section according to section number
        return getSections().get(sectionNumber - 1);
    }

    /**
     * Return the content of a section.
     * 
     * @param sectionNumber the index (+1) of the section in the list of all sections in the document.
     * @return the content of a section or null if the section can't be found.
     * @throws XWikiException error when trying to extract section content
     */
    public String getContentOfSection(int sectionNumber) throws XWikiException
    {
        String content = null;

        if (is10Syntax()) {
            content = getContentOfSection10(sectionNumber);
        } else {
            List<HeaderBlock> headers = getFilteredHeaders();

            if (headers.size() >= sectionNumber) {
                SectionBlock section = headers.get(sectionNumber - 1).getSection();
                content = renderXDOM(new XDOM(Collections.<Block> singletonList(section)), getSyntax());
            }
        }

        return content;
    }

    /**
     * Return the content of a section.
     * 
     * @param sectionNumber the index (+1) of the section in the list of all sections in the document.
     * @return the content of a section
     * @throws XWikiException error when trying to extract section content
     */
    private String getContentOfSection10(int sectionNumber) throws XWikiException
    {
        List<DocumentSection> splitSections = getSections();
        int indexEnd = 0;
        // get current section
        DocumentSection section = splitSections.get(sectionNumber - 1);
        int indexStart = section.getSectionIndex();
        String sectionLevel = section.getSectionLevel();
        // Determine where this section ends, which is at the start of the next section of the
        // same or a higher level.
        for (int i = sectionNumber; i < splitSections.size(); i++) {
            DocumentSection nextSection = splitSections.get(i);
            String nextLevel = nextSection.getSectionLevel();
            if (sectionLevel.equals(nextLevel) || sectionLevel.length() > nextLevel.length()) {
                indexEnd = nextSection.getSectionIndex();
                break;
            }
        }
        String sectionContent = null;
        if (indexStart < 0) {
            indexStart = 0;
        }

        if (indexEnd == 0) {
            sectionContent = getContent().substring(indexStart);
        } else {
            sectionContent = getContent().substring(indexStart, indexEnd);
        }

        return sectionContent;
    }

    /**
     * Update a section content in document.
     * 
     * @param sectionNumber the index (starting at 1) of the section in the list of all sections in the document.
     * @param newSectionContent the new section content.
     * @return the new document content.
     * @throws XWikiException error when updating content
     */
    public String updateDocumentSection(int sectionNumber, String newSectionContent) throws XWikiException
    {
        String content;
        if (is10Syntax()) {
            content = updateDocumentSection10(sectionNumber, newSectionContent);
        } else {
            // Get the current section block
            HeaderBlock header = getFilteredHeaders().get(sectionNumber - 1);

            XDOM xdom = (XDOM) header.getRoot();

            // newSectionContent -> Blocks
            List<Block> blocks = parseContent(newSectionContent).getChildren();
            int sectionLevel = header.getLevel().getAsInt();
            for (int level = 1; level < sectionLevel && blocks.size() == 1 && blocks.get(0) instanceof SectionBlock; ++level) {
                blocks = blocks.get(0).getChildren();
            }

            // replace old current SectionBlock with new Blocks
            Block section = header.getSection();
            section.getParent().replaceChild(blocks, section);

            // render back XDOM to document's content syntax
            content = renderXDOM(xdom, getSyntax());
        }

        return content;
    }

    /**
     * Update a section content in document.
     * 
     * @param sectionNumber the index (+1) of the section in the list of all sections in the document.
     * @param newSectionContent the new section content.
     * @return the new document content.
     * @throws XWikiException error when updating document content with section content
     */
    private String updateDocumentSection10(int sectionNumber, String newSectionContent) throws XWikiException
    {
        StringBuffer newContent = new StringBuffer();
        // get document section that will be edited
        DocumentSection docSection = getDocumentSection(sectionNumber);
        int numberOfSections = getSections().size();
        int indexSection = docSection.getSectionIndex();
        if (numberOfSections == 1) {
            // there is only a sections in document
            String contentBegin = getContent().substring(0, indexSection);
            newContent = newContent.append(contentBegin).append(newSectionContent);
            return newContent.toString();
        } else if (sectionNumber == numberOfSections) {
            // edit lastest section that doesn't contain subtitle
            String contentBegin = getContent().substring(0, indexSection);
            newContent = newContent.append(contentBegin).append(newSectionContent);
            return newContent.toString();
        } else {
            String sectionLevel = docSection.getSectionLevel();
            int nextSectionIndex = 0;
            // get index of next section
            for (int i = sectionNumber; i < numberOfSections; i++) {
                DocumentSection nextSection = getDocumentSection(i + 1); // get next section
                String nextSectionLevel = nextSection.getSectionLevel();
                if (sectionLevel.equals(nextSectionLevel)) {
                    nextSectionIndex = nextSection.getSectionIndex();
                    break;
                } else if (sectionLevel.length() > nextSectionLevel.length()) {
                    nextSectionIndex = nextSection.getSectionIndex();
                    break;
                }
            }

            if (nextSectionIndex == 0) {// edit the last section
                newContent = newContent.append(getContent().substring(0, indexSection)).append(newSectionContent);
                return newContent.toString();
            } else {
                String contentAfter = getContent().substring(nextSectionIndex);
                String contentBegin = getContent().substring(0, indexSection);
                newContent = newContent.append(contentBegin).append(newSectionContent).append(contentAfter);
            }

            return newContent.toString();
        }
    }

    /**
     * Computes a document hash, taking into account all document data: content, objects, attachments, metadata... TODO:
     * cache the hash value, update only on modification.
     */
    public String getVersionHashCode(XWikiContext context)
    {
        MessageDigest md5 = null;

        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex) {
            LOGGER.error("Cannot create MD5 object", ex);
            return hashCode() + "";
        }

        try {
            String valueBeforeMD5 = toXML(true, false, true, false, context);
            md5.update(valueBeforeMD5.getBytes());

            byte[] array = md5.digest();
            StringBuffer sb = new StringBuffer();
            for (int j = 0; j < array.length; ++j) {
                int b = array[j] & 0xFF;
                if (b < 0x10) {
                    sb.append('0');
                }
                sb.append(Integer.toHexString(b));
            }

            return sb.toString();
        } catch (Exception ex) {
            LOGGER.error("Exception while computing document hash", ex);
        }

        return hashCode() + "";
    }

    public static String getInternalPropertyName(String propname, XWikiContext context)
    {
        XWikiMessageTool msg = context.getMessageTool();
        String cpropname = StringUtils.capitalize(propname);

        return (msg == null) ? cpropname : msg.get(cpropname);
    }

    public String getInternalProperty(String propname)
    {
        String methodName = "get" + StringUtils.capitalize(propname);
        try {
            Method method = getClass().getDeclaredMethod(methodName, (Class[]) null);
            return (String) method.invoke(this, (Object[]) null);
        } catch (Exception e) {
            return null;
        }
    }

    public String getCustomClass()
    {
        if (this.customClass == null) {
            return "";
        }

        return this.customClass;
    }

    public void setCustomClass(String customClass)
    {
        this.customClass = customClass;
        setMetaDataDirty(true);
    }

    public void setValidationScript(String validationScript)
    {
        this.validationScript = validationScript;

        setMetaDataDirty(true);
    }

    public String getValidationScript()
    {
        if (this.validationScript == null) {
            return "";
        } else {
            return this.validationScript;
        }
    }

    public String getComment()
    {
        if (this.comment == null) {
            return "";
        }

        return this.comment;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public boolean isMinorEdit()
    {
        return this.isMinorEdit;
    }

    public void setMinorEdit(boolean isMinor)
    {
        this.isMinorEdit = isMinor;
    }

    // methods for easy table update. It is need only for hibernate.
    // when hibernate update old database without minorEdit field, hibernate will create field with
    // null in despite of notnull in hbm.
    // (http://opensource.atlassian.com/projects/hibernate/browse/HB-1151)
    // so minorEdit will be null for old documents. But hibernate can't convert null to boolean.
    // so we need convert Boolean to boolean
    protected Boolean getMinorEdit1()
    {
        return Boolean.valueOf(isMinorEdit());
    }

    protected void setMinorEdit1(Boolean isMinor)
    {
        this.isMinorEdit = (isMinor != null && isMinor.booleanValue());
    }

    /**
     * @since 2.2.3
     */
    public BaseObject newXObject(EntityReference classReference, XWikiContext context) throws XWikiException
    {
        int nb = createXObject(classReference, context);
        return getXObject(resolveClassReference(classReference), nb);
    }

    /**
     * @deprecated since 2.2M2 use {@link #newXObject(EntityReference, XWikiContext)}
     */
    @Deprecated
    public BaseObject newObject(String className, XWikiContext context) throws XWikiException
    {
        return newXObject(
            this.xClassEntityReferenceResolver.resolve(className, EntityType.DOCUMENT, getDocumentReference()), context);
    }

    /**
     * @since 2.2M2
     */
    public BaseObject getXObject(DocumentReference classReference, boolean create, XWikiContext context)
    {
        try {
            BaseObject obj = getXObject(classReference);

            if ((obj == null) && create) {
                return newXObject(classReference, context);
            }

            if (obj == null) {
                return null;
            } else {
                return obj;
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * @deprecated since 2.2M2 use {@link #getXObject(DocumentReference, boolean, XWikiContext)}
     */
    @Deprecated
    public BaseObject getObject(String className, boolean create, XWikiContext context)
    {
        return getXObject(resolveClassReference(className), create, context);
    }

    public boolean validate(XWikiContext context) throws XWikiException
    {
        return validate(null, context);
    }

    public boolean validate(String[] classNames, XWikiContext context) throws XWikiException
    {
        boolean isValid = true;
        if ((classNames == null) || (classNames.length == 0)) {
            for (DocumentReference classReference : getXObjects().keySet()) {
                BaseClass bclass = context.getWiki().getXClass(classReference, context);
                List<BaseObject> objects = getXObjects(classReference);
                for (BaseObject obj : objects) {
                    if (obj != null) {
                        isValid &= bclass.validateObject(obj, context);
                    }
                }
            }
        } else {
            for (int i = 0; i < classNames.length; i++) {
                List<BaseObject> objects =
                    getXObjects(this.currentMixedDocumentReferenceResolver.resolve(classNames[i]));
                if (objects != null) {
                    for (BaseObject obj : objects) {
                        if (obj != null) {
                            BaseClass bclass = obj.getXClass(context);
                            isValid &= bclass.validateObject(obj, context);
                        }
                    }
                }
            }
        }

        String validationScript = "";
        XWikiRequest req = context.getRequest();
        if (req != null) {
            validationScript = req.get("xvalidation");
        }

        if ((validationScript == null) || (validationScript.trim().equals(""))) {
            validationScript = getValidationScript();
        }

        if ((validationScript != null) && (!validationScript.trim().equals(""))) {
            isValid &= executeValidationScript(context, validationScript);
        }

        return isValid;
    }

    public static void backupContext(Map<String, Object> backup, XWikiContext context)
    {
        backup.put("doc", context.getDoc());
        VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
        VelocityContext vcontext = velocityManager.getVelocityContext();
        if (vcontext != null) {
            backup.put("vdoc", vcontext.get("doc"));
            backup.put("vcdoc", vcontext.get("cdoc"));
            backup.put("vtdoc", vcontext.get("tdoc"));
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> gcontext = (Map<String, Object>) context.get("gcontext");
        if (gcontext != null) {
            backup.put("gdoc", gcontext.get("doc"));
            backup.put("gcdoc", gcontext.get("cdoc"));
            backup.put("gtdoc", gcontext.get("tdoc"));
        }

        // Clone the Execution Context to provide isolation
        Execution execution = Utils.getComponent(Execution.class);
        ExecutionContext clonedEc;
        try {
            clonedEc = Utils.getComponent(ExecutionContextManager.class).clone(execution.getContext());
        } catch (ExecutionContextException e) {
            throw new RuntimeException("Failed to clone the Execution Context", e);
        }
        execution.pushContext(clonedEc);
    }

    public static void restoreContext(Map<String, Object> backup, XWikiContext context)
    {
        // Restore the Execution Context
        Execution execution = Utils.getComponent(Execution.class);
        execution.popContext();

        @SuppressWarnings("unchecked")
        Map<String, Object> gcontext = (Map<String, Object>) context.get("gcontext");
        if (gcontext != null) {
            if (backup.get("gdoc") != null) {
                gcontext.put("doc", backup.get("gdoc"));
            }

            if (backup.get("gcdoc") != null) {
                gcontext.put("cdoc", backup.get("gcdoc"));
            }

            if (backup.get("gtdoc") != null) {
                gcontext.put("tdoc", backup.get("gtdoc"));
            }
        }

        VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
        VelocityContext vcontext = velocityManager.getVelocityContext();
        if (vcontext != null) {
            if (backup.get("vdoc") != null) {
                vcontext.put("doc", backup.get("vdoc"));
            }

            if (backup.get("vcdoc") != null) {
                vcontext.put("cdoc", backup.get("vcdoc"));
            }

            if (backup.get("vtdoc") != null) {
                vcontext.put("tdoc", backup.get("vtdoc"));
            }
        }

        if (backup.get("doc") != null) {
            context.setDoc((XWikiDocument) backup.get("doc"));
        }
    }

    public void setAsContextDoc(XWikiContext context)
    {
        try {
            context.setDoc(this);
            com.xpn.xwiki.api.Document apidoc = newDocument(context);
            com.xpn.xwiki.api.Document tdoc = apidoc.getTranslatedDocument();
            VelocityManager velocityManager = Utils.getComponent(VelocityManager.class);
            VelocityContext vcontext = velocityManager.getVelocityContext();
            @SuppressWarnings("unchecked")
            Map<String, Object> gcontext = (Map<String, Object>) context.get("gcontext");
            if (vcontext != null) {
                vcontext.put("doc", apidoc);
                vcontext.put("tdoc", tdoc);
            }

            if (gcontext != null) {
                gcontext.put("doc", apidoc);
                gcontext.put("tdoc", tdoc);
            }
        } catch (XWikiException ex) {
            LOGGER.warn("Unhandled exception setting context", ex);
        }
    }

    /**
     * @return the String representation of the previous version of this document or null if this is the first version.
     */
    public String getPreviousVersion()
    {
        XWikiDocumentArchive archive = loadDocumentArchive();
        if (archive != null) {
            Version prevVersion = archive.getPrevVersion(getRCSVersion());
            if (prevVersion != null) {
                return prevVersion.toString();
            }
        }
        return null;
    }

    @Override
    public String toString()
    {
        return getFullName();
    }

    /**
     * Indicates whether the document should be 'hidden' or not, meaning that it should not be returned in public search
     * results. WARNING: this is a temporary hack until the new data model is designed and implemented. No code should
     * rely on or use this property, since it will be replaced with a generic metadata.
     * 
     * @param hidden The new value of the {@link #hidden} property.
     */
    public void setHidden(Boolean hidden)
    {
        if (hidden == null) {
            this.hidden = false;
        } else {
            this.hidden = hidden;
        }
    }

    /**
     * Indicates whether the document is 'hidden' or not, meaning that it should not be returned in public search
     * results. WARNING: this is a temporary hack until the new data model is designed and implemented. No code should
     * rely on or use this property, since it will be replaced with a generic metadata.
     * 
     * @return <code>true</code> if the document is hidden and does not appear among the results of
     *         {@link com.xpn.xwiki.api.XWiki#searchDocuments(String)}, <code>false</code> otherwise.
     */
    public Boolean isHidden()
    {
        return this.hidden;
    }

    /**
     * Convert the current document content from its current syntax to the new syntax passed as parameter.
     * 
     * @param targetSyntaxId the syntax to convert to (eg "xwiki/2.0", "xhtml/1.0", etc)
     * @throws XWikiException if an exception occurred during the conversion process
     */
    public void convertSyntax(String targetSyntaxId, XWikiContext context) throws XWikiException
    {
        try {
            convertSyntax(this.syntaxFactory.createSyntaxFromIdString(targetSyntaxId), context);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to convert document to syntax [" + targetSyntaxId + "]", e);
        }
    }

    /**
     * Convert the current document content from its current syntax to the new syntax passed as parameter.
     * 
     * @param targetSyntax the syntax to convert to (eg "xwiki/2.0", "xhtml/1.0", etc)
     * @throws XWikiException if an exception occurred during the conversion process
     */
    public void convertSyntax(Syntax targetSyntax, XWikiContext context) throws XWikiException
    {
        // convert content
        setContent(performSyntaxConversion(getContent(), getSyntaxId(), targetSyntax));

        // convert objects
        Map<DocumentReference, List<BaseObject>> objectsByClass = getXObjects();

        for (List<BaseObject> objects : objectsByClass.values()) {
            for (BaseObject bobject : objects) {
                if (bobject != null) {
                    BaseClass bclass = bobject.getXClass(context);
                    for (Object fieldClass : bclass.getProperties()) {
                        if (fieldClass instanceof TextAreaClass && ((TextAreaClass) fieldClass).isWikiContent()) {
                            TextAreaClass textAreaClass = (TextAreaClass) fieldClass;
                            LargeStringProperty field = (LargeStringProperty) bobject.getField(textAreaClass.getName());

                            if (field != null) {
                                field.setValue(performSyntaxConversion(field.getValue(), getSyntaxId(), targetSyntax));
                            }
                        }
                    }
                }
            }
        }

        // change syntax
        setSyntax(targetSyntax);
    }

    /**
     * @return the XDOM corresponding to the document's string content.
     */
    public XDOM getXDOM()
    {
        if (this.xdom == null) {
            try {
                this.xdom = parseContent(getContent());
            } catch (XWikiException e) {
                LOGGER.error("Failed to parse document content to XDOM", e);
            }
        }

        return this.xdom.clone();
    }

    /**
     * @return true if the document has a xwiki/1.0 syntax content
     */
    public boolean is10Syntax()
    {
        return is10Syntax(getSyntaxId());
    }

    /**
     * @return true if the document has a xwiki/1.0 syntax content
     */
    public boolean is10Syntax(String syntaxId)
    {
        return Syntax.XWIKI_1_0.toIdString().equalsIgnoreCase(syntaxId);
    }

    private void init(DocumentReference reference)
    {
        // if the passed reference is null consider it points to the default reference
        if (reference == null) {
            setDocumentReference(Utils.getComponent(DocumentReferenceResolver.class).resolve(""));
        } else {
            setDocumentReference(reference);
        }

        this.updateDate = new Date();
        this.updateDate.setTime((this.updateDate.getTime() / 1000) * 1000);
        this.contentUpdateDate = new Date();
        this.contentUpdateDate.setTime((this.contentUpdateDate.getTime() / 1000) * 1000);
        this.creationDate = new Date();
        this.creationDate.setTime((this.creationDate.getTime() / 1000) * 1000);
        this.content = "";
        this.format = "";
        this.language = "";
        this.defaultLanguage = "";
        this.attachmentList = new ArrayList<XWikiAttachment>();
        this.customClass = "";
        this.comment = "";

        // Note: As there's no notion of an Empty document we don't set the original document
        // field. Thus getOriginalDocument() may return null.
    }

    private boolean executeValidationScript(XWikiContext context, String validationScript) throws XWikiException
    {
        try {
            XWikiValidationInterface validObject =
                (XWikiValidationInterface) context.getWiki().parseGroovyFromPage(validationScript, context);

            return validObject.validateDocument(this, context);
        } catch (Throwable e) {
            XWikiValidationStatus.addExceptionToContext(getFullName(), "", e, context);
            return false;
        }
    }

    /**
     * Convert the passed content from the passed syntax to the passed new syntax.
     * 
     * @param content the content to convert
     * @param source the reference to where the content comes from (eg document reference)
     * @param targetSyntax the new syntax after the conversion
     * @param txContext the context when Transformation are executed or null if transformation shouldn't be executed
     * @return the converted content in the new syntax
     * @throws XWikiException if an exception occurred during the conversion process
     * @since 2.4M2
     */
    private static String performSyntaxConversion(String content, String source, Syntax targetSyntax,
        TransformationContext txContext) throws XWikiException
    {
        try {
            XDOM dom = parseContent(txContext.getSyntax().toIdString(), content);

            // Set the source metadata for the parsed XDOM so that Renderers can resolve relative links/images based
            // on it.
            dom.getMetaData().addMetaData(MetaData.SOURCE, source);

            return performSyntaxConversion(dom, targetSyntax, txContext);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to convert document to syntax [" + targetSyntax + "]", e);
        }
    }

    /**
     * Convert the passed content from the passed syntax to the passed new syntax.
     * 
     * @param content the content to convert
     * @param currentSyntaxId the syntax of the current content to convert
     * @param targetSyntax the new syntax after the conversion
     * @return the converted content in the new syntax
     * @throws XWikiException if an exception occurred during the conversion process
     * @since 2.4M2
     */
    private static String performSyntaxConversion(String content, String currentSyntaxId, Syntax targetSyntax)
        throws XWikiException
    {
        try {
            XDOM dom = parseContent(currentSyntaxId, content);

            return performSyntaxConversion(dom, targetSyntax, null);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to convert document to syntax [" + targetSyntax + "]", e);
        }
    }

    /**
     * Convert the passed content from the passed syntax to the passed new syntax.
     * 
     * @param content the XDOM content to convert, the XDOM can be modified during the transformation
     * @param targetSyntax the new syntax after the conversion
     * @param txContext the context when Transformation are executed or null if transformation shouldn't be executed
     * @return the converted content in the new syntax
     * @throws XWikiException if an exception occurred during the conversion process
     * @since 2.4M2
     */
    private static String performSyntaxConversion(XDOM content, Syntax targetSyntax, TransformationContext txContext)
        throws XWikiException
    {
        try {
            if (txContext != null) {
                // Transform XDOM
                TransformationManager transformations = Utils.getComponent(TransformationManager.class);
                if (txContext.getXDOM() == null) {
                    txContext.setXDOM(content);
                }
                try {
                    transformations.performTransformations(content, txContext);
                } catch (TransformationException te) {
                    // An error happened during one of the transformations. Since the error has been logged
                    // continue
                    // TODO: We should have a visual clue for the user in the future to let him know something
                    // didn't work as expected.
                }
            }

            // Render XDOM
            return renderXDOM(content, targetSyntax);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to convert document to syntax [" + targetSyntax + "]", e);
        }
    }

    /**
     * Render privided XDOM into content of the provided syntax identifier.
     * 
     * @param content the XDOM content to render
     * @param targetSyntax the syntax identifier of the rendered content
     * @return the rendered content
     * @throws XWikiException if an exception occurred during the rendering process
     */
    private static String renderXDOM(XDOM content, Syntax targetSyntax) throws XWikiException
    {
        try {
            BlockRenderer renderer = Utils.getComponent(BlockRenderer.class, targetSyntax.toIdString());
            WikiPrinter printer = new DefaultWikiPrinter();
            renderer.render(content, printer);
            return printer.toString();
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to render document to syntax [" + targetSyntax + "]", e);
        }
    }

    private XDOM parseContent(String content) throws XWikiException
    {
        return parseContent(getSyntaxId(), content);
    }

    private static XDOM parseContent(String syntaxId, String content) throws XWikiException
    {
        try {
            Parser parser = Utils.getComponent(Parser.class, syntaxId);

            return parser.parse(new StringReader(content));
        } catch (ParseException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_RENDERING, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to parse content of syntax [" + syntaxId + "]", e);
        }
    }

    private Syntax getDefaultDocumentSyntax()
    {
        // If there's no parser available for the specified syntax default to XWiki 2.0 syntax
        Syntax syntax = Utils.getComponent(CoreConfiguration.class).getDefaultDocumentSyntax();

        try {
            Utils.getComponent(Parser.class, syntax.toIdString());
        } catch (Exception e) {
            LOGGER.warn("Failed to find parser for the default syntax [" + syntax.toIdString()
                + "]. Defaulting to xwiki/2.0 syntax.");
            syntax = Syntax.XWIKI_2_0;
        }

        return syntax;
    }

    private String serializeReference(DocumentReference reference, EntityReferenceSerializer<String> serializer,
        DocumentReference defaultReference)
    {
        XWikiContext xcontext = getXWikiContext();

        String originalWikiName = xcontext.getDatabase();
        XWikiDocument originalCurentDocument = xcontext.getDoc();
        try {
            xcontext.setDatabase(defaultReference.getWikiReference().getName());
            xcontext.setDoc(new XWikiDocument(defaultReference));

            return serializer.serialize(reference);
        } finally {
            xcontext.setDoc(originalCurentDocument);
            xcontext.setDatabase(originalWikiName);
        }
    }

    /**
     * Backward-compatibility method to use in order to resolve a class reference passed as a String into a
     * DocumentReference proper.
     * 
     * @return the resolved class reference but using this document's wiki if the passed String doesn't specify a wiki,
     *         the "XWiki" space if the passed String doesn't specify a space and this document's page if the passed
     *         String doesn't specify a page.
     */
    public DocumentReference resolveClassReference(String documentName)
    {
        DocumentReference defaultReference = new DocumentReference(getDocumentReference().getWikiReference().getName(),
            XWiki.SYSTEM_SPACE, getDocumentReference().getName());
        return this.explicitDocumentReferenceResolver.resolve(documentName, defaultReference);
    }

    /**
     * Transforms a XClass reference relative to this document into an absolute reference.
     */
    private DocumentReference resolveClassReference(EntityReference reference)
    {
        DocumentReference defaultReference = new DocumentReference(getDocumentReference().getWikiReference().getName(),
            XWiki.SYSTEM_SPACE, getDocumentReference().getName());
        return this.explicitReferenceDocumentReferenceResolver.resolve(reference, defaultReference);
    }

    /**
     * @return the relative parent reference, this method should stay private since this the relative saving of the
     *         parent reference is an implementation detail and from the outside we should only see absolute references
     * @since 2.2.3
     */
    protected EntityReference getRelativeParentReference()
    {
        return this.parentReference;
    }

    private BaseObject prepareXObject(EntityReference classReference)
    {
        DocumentReference absoluteClassReference = resolveClassReference(classReference);
        BaseObject bobject = getXObject(absoluteClassReference);
        if (bobject == null) {
            bobject = new BaseObject();
            bobject.setXClassReference(classReference);

            addXObject(bobject);
        }
        bobject.setDocumentReference(getDocumentReference());
        setContentDirty(true);
        return bobject;
    }
}
