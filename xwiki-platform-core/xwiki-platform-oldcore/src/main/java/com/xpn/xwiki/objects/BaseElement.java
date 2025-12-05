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
package com.xpn.xwiki.objects;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Objects;

import javax.inject.Provider;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.dom4j.Element;
import org.dom4j.io.DocumentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.filter.input.StringInputSource;
import org.xwiki.filter.xar.output.XAROutputProperties;
import org.xwiki.filter.xml.output.DefaultResultOutputTarget;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.stability.Unstable;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.store.merge.MergeManagerResult;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.merge.MergeConfiguration;
import com.xpn.xwiki.doc.merge.MergeResult;
import com.xpn.xwiki.internal.filter.XWikiDocumentFilterUtils;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;

/**
 * Base class for representing an element having a name (either a reference of a free form name) and a pretty name.
 *
 * @version $Id$
 */
public abstract class BaseElement<R extends EntityReference> implements ElementInterface, Serializable
{
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseElement.class);

    /**
     * Full reference of this element.
     *
     * @since 3.2M1
     */
    protected R referenceCache;

    /**
     * Reference to the document in which this element is defined (for elements where this make sense, for example for
     * an XClass or a XObject).
     *
     * @since 5.3M1
     */
    protected DocumentReference documentReference;

    /**
     * The owner document, if this element was obtained from a document.
     *
     * @since 5.3M1
     */
    protected transient XWikiDocument ownerDocument;

    /**
     * Free form name (for elements which don't point to a reference, for example for instances of {@link BaseProperty}
     * ).
     */
    private String name;

    private String prettyName;

    /**
     * Used to convert a proper Document Reference to a string but without the wiki name.
     */
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    /**
     * Used to build uid string for the getId() hash.
     */
    private EntityReferenceSerializer<String> localUidStringEntityReferenceSerializer;

    private ContextualLocalizationManager localization;

    private transient boolean dirty = true;

    /**
     * @return true of the element was modified (or created)
     * @since 17.1.0RC1
     * @since 16.10.4
     * @since 16.4.7
     */
    @Unstable
    public boolean isDirty()
    {
        return this.dirty;
    }

    /**
     * @param dirty true if the element was modified (or created)
     * @since 17.1.0RC1
     * @since 16.10.4
     * @since 16.4.7
     */
    @Unstable
    public void setDirty(boolean dirty)
    {
        this.dirty = dirty;

        if (dirty && this.ownerDocument != null) {
            this.ownerDocument.setMetaDataDirty(true);
        }
    }

    /**
     * @return a merge manager instance.
     * @since 11.8RC1
     */
    protected MergeManager getMergeManager()
    {
        return Utils.getComponent(MergeManager.class);
    }

    @Override
    public R getReference()
    {
        if (this.referenceCache == null) {
            this.referenceCache = createReference();
        }

        return this.referenceCache;
    }

    /**
     * @since 3.2M1
     */
    protected R createReference()
    {
        return null;
    }

    @Override
    public DocumentReference getDocumentReference()
    {
        // Object using name without setting a reference are not allowed to retrieve the reference
        if (this.documentReference == null && this.name != null) {
            throw new IllegalStateException(
                "BaseElement#getDocumentReference could not be called when a non-reference Name has been set.");
        }

        return this.documentReference;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method is used by Hibernate for saving an element.
     * </p>
     *
     * @see com.xpn.xwiki.objects.ElementInterface#getName()
     */
    @Override
    public String getName()
    {
        // If the name is null then serialize the reference as a string.
        if (this.name == null && this.documentReference != null) {
            this.name = getLocalEntityReferenceSerializer().serialize(this.documentReference);
        }

        return this.name;
    }

    @Override
    public void setDocumentReference(DocumentReference documentReference)
    {
        if (!Objects.equals(documentReference, this.documentReference)) {
            // If the name is already set then reset it since we're now using a reference
            this.documentReference = documentReference;
            this.name = null;
            this.referenceCache = null;

            if (isDirty()) {
                setDirty(true);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that this method is used by Hibernate for loading an element.
     * </p>
     *
     * @see com.xpn.xwiki.objects.ElementInterface#setName(java.lang.String)
     */
    @Override
    public void setName(String name)
    {
        // Empty property name is forbidden because it will cause problem (impossible to create an EntityReference for
        // it)
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("An element name cannot be null or empty");
        }

        // If a reference is already set, then you cannot set a name
        if (this.documentReference != null) {
            throw new IllegalStateException("BaseElement#setName could not be called when a reference has been set.");
        }

        if (!Strings.CS.equals(name, this.name)) {
            this.name = name;
            this.referenceCache = null;

            setDirty(true);
        }
    }

    public String getPrettyName()
    {
        return this.prettyName;
    }

    public void setPrettyName(String name)
    {
        this.prettyName = name;
    }

    /**
     * @return the component used to build uid string for the getId() hash
     * @since 4.0M1
     */
    protected EntityReferenceSerializer<String> getLocalUidStringEntityReferenceSerializer()
    {
        if (this.localUidStringEntityReferenceSerializer == null) {
            this.localUidStringEntityReferenceSerializer =
                Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local/uid");
        }

        return this.localUidStringEntityReferenceSerializer;
    }

    /**
     * @return the component used to convert a proper Document Reference to a string but without the wiki name.
     * @since 6.3M1
     */
    protected EntityReferenceSerializer<String> getLocalEntityReferenceSerializer()
    {
        if (this.localEntityReferenceSerializer == null) {
            this.localEntityReferenceSerializer = Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        }

        return this.localEntityReferenceSerializer;
    }

    protected ContextualLocalizationManager getLocalization()
    {
        if (this.localization == null) {
            this.localization = Utils.getComponent(ContextualLocalizationManager.class);
        }

        return this.localization;
    }

    protected String localizePlain(String key, Object... parameters)
    {
        return getLocalization().getTranslationPlain(key, parameters);
    }

    protected String localizePlainOrKey(String key, Object... parameters)
    {
        return StringUtils.defaultString(getLocalization().getTranslationPlain(key, parameters), key);
    }

    /**
     * @return a unique identifier representing this element reference to be used for {@code hashCode()}.
     * @since 4.0M1
     */
    protected String getLocalKey()
    {
        // The R40000XWIKI6990DataMigration use the same algorithm to compute object id. It should be properly synced.
        return getLocalUidStringEntityReferenceSerializer().serialize(getReference());
    }

    /**
     * Return an truncated MD5 hash of the local key computed in {@link #getLocalKey()}.
     *
     * @return the identifier used by hibernate for storage.
     * @since 4.0M1
     */
    public long getId()
    {
        String key = getLocalKey();

        if (key != null) {
            // The R40000XWIKI6990DataMigration use the same algorithm to compute object id. It should be properly
            // synced.
            return Util.getHash(key);
        }

        return 0;
    }

    /**
     * Dummy function, do hibernate is always happy.
     *
     * @param id the identifier assigned by hibernate.
     * @since 4.0M1
     */
    public void setId(long id)
    {
    }

    @Override
    public int hashCode()
    {
        return (int) Util.getHash(getLocalKey());
    }

    @Override
    public boolean equals(Object el)
    {
        // Same Java object, they sure are equal
        if (this == el) {
            return true;
        }

        if (el == null || !(el.getClass().equals(this.getClass()))) {
            return false;
        }

        BaseElement element = (BaseElement) el;

        if (element.documentReference != null) {
            if (!element.documentReference.equals(this.documentReference)) {
                return false;
            }
        } else {
            if (this.documentReference != null) {
                return false;
            }
            if (element.name == null) {
                if (this.name != null) {
                    return false;
                }
            } else if (!element.name.equals(this.name)) {
                return false;
            }
        }

        if (element.getPrettyName() == null) {
            if (getPrettyName() != null) {
                return false;
            }
        } else if (!element.getPrettyName().equals(getPrettyName())) {
            return false;
        }

        return true;
    }

    /**
     * Reset any reference to the owner of this element.
     * 
     * @since 17.3.0RC1
     * @since 17.2.1
     */
    @Unstable
    protected void detachOwner()
    {
        this.ownerDocument = null;
        this.documentReference = null;
        this.referenceCache = null;
    }

    /**
     * Clone the owner referenced by this element.
     * 
     * @since 17.3.0RC1
     * @since 17.2.1
     */
    @Unstable
    protected void cloneOwner()
    {
        if (this.ownerDocument != null) {
            this.ownerDocument = this.ownerDocument.clone();
        }
    }

    @Override
    public BaseElement<R> clone()
    {
        return clone(false);
    }

    /**
     * @param detach true if the element should be detached from its parent container, if false then the parent
     *            container is cloned too
     * @return a clone of this element
     * @since 17.3.0RC1
     * @since 17.2.1
     */
    @Unstable
    public BaseElement<R> clone(boolean detach)
    {
        BaseElement<R> element;
        try {
            element = (BaseElement<R>) super.clone();

            if (detach) {
                // This element is not attached to any container yet
                element.detachOwner();
            } else {
                // For retro compatibility reasons we clone the owner document
                element.cloneOwner();
            }

            cloneContent(element);

            // Restore the dirty state
            element.setDirty(isDirty());
        } catch (CloneNotSupportedException e) {
            // This should not happen
            element = null;
        }

        return element;
    }

    /**
     * Extra clone related operation between the handling of the owner and the reset of the dirty flag.
     * 
     * @param element the cloned element
     * @since 17.3.0RC1
     * @since 17.2.1
     */
    @Unstable
    protected void cloneContent(BaseElement<R> element)
    {
        element.setPrettyName(getPrettyName());
    }

    @Override
    public void merge(ElementInterface previousElement, ElementInterface newElement, MergeConfiguration configuration,
        XWikiContext context, MergeResult mergeResult)
    {
        MergeManagerResult<ElementInterface, Object> mergeManagerResult =
            this.merge(previousElement, newElement, configuration, context);
        mergeResult.getLog().addAll(mergeManagerResult.getLog());
        mergeResult.setModified(mergeResult.isModified() || mergeManagerResult.isModified());
    }

    @Override
    public MergeManagerResult<ElementInterface, Object> merge(ElementInterface previousElement,
        ElementInterface newElement, MergeConfiguration configuration, XWikiContext context)
    {
        MergeManagerResult<ElementInterface, Object> mergeManagerResult = new MergeManagerResult<>();
        if (configuration.isProvidedVersionsModifiables()) {
            mergeManagerResult.setMergeResult(this);
        } else {
            mergeManagerResult.setMergeResult(this.clone());
        }
        MergeManagerResult<String, Character> mergePrettyNameResult =
            getMergeManager().mergeCharacters(((BaseElement) previousElement).getPrettyName(),
                ((BaseElement) newElement).getPrettyName(), getPrettyName(), configuration);

        // Note that we're loosing conflicts information about prettyname, but it's probably ok, at least it's not a
        // regression since we were not getting that info in the past.
        // Now it might not be a good idea to mix those info with the conflicts related to the value of the element
        // we might need improvment in the design here...
        mergeManagerResult.setLog(mergePrettyNameResult.getLog());
        if (mergePrettyNameResult.isModified()) {
            mergeManagerResult.setModified(true);
            ((BaseElement)mergeManagerResult.getMergeResult()).setPrettyName(mergePrettyNameResult.getMergeResult());
        }
        return mergeManagerResult;
    }

    @Override
    public boolean apply(ElementInterface newElement, boolean clean)
    {
        boolean modified = false;

        BaseElement<R> newBaseElement = (BaseElement<R>) newElement;

        // Pretty name
        if (!Strings.CS.equals(newBaseElement.getPrettyName(), getPrettyName())) {
            setPrettyName(newBaseElement.getPrettyName());
            modified = true;
        }

        return modified;
    }

    /**
     * Set the owner document of this element.
     *
     * @param ownerDocument The owner document.
     * @since 5.3M1
     */
    public void setOwnerDocument(XWikiDocument ownerDocument)
    {
        if (this.ownerDocument != ownerDocument) {
            this.ownerDocument = ownerDocument;

            if (this.ownerDocument != null && isDirty()) {
                this.ownerDocument.setMetaDataDirty(true);
            }
        }
    }

    /**
     * @return the owner document of this element.
     * @since 5.3M1
     */
    public XWikiDocument getOwnerDocument()
    {
        return this.ownerDocument;
    }

    /**
     * Get XWiki context from execution context.
     *
     * @return the XWiki context for the current thread
     * @since 9.0RC1
     */
    protected XWikiContext getXWikiContext()
    {
        Provider<XWikiContext> xcontextProvider = Utils.getComponent(XWikiContext.TYPE_PROVIDER);

        if (xcontextProvider != null) {
            return xcontextProvider.get();
        }

        return null;
    }

    protected void fromXML(Element oel) throws XWikiException
    {
        // Serialize the Document (could not find a way to convert a dom4j Element into a usable StAX source)
        StringWriter writer = new StringWriter();
        try {
            org.dom4j.io.XMLWriter domWriter = new org.dom4j.io.XMLWriter(writer);
            domWriter.write(oel);
            domWriter.flush();
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING,
                "Error parsing xml", e, null);
        }

        // Actually parse the XML
        fromXML(writer.toString());
    }

    /**
     * @param source the XML to read
     * @throws XWikiException when failing to parse XML
     */
    public void fromXML(String source) throws XWikiException
    {
        if (!source.isEmpty()) {
            try {
                Utils.getComponent(XWikiDocumentFilterUtils.class).importEntity(this, new StringInputSource(source));
            } catch (Exception e) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_DOC, XWikiException.ERROR_DOC_XML_PARSING,
                    "Error parsing xml", e, null);
            }
        }
    }

    protected Element toXML()
    {
        DocumentResult domResult = new DocumentResult();

        try {
            Utils.getComponent(XWikiDocumentFilterUtils.class).exportEntity(this,
                new DefaultResultOutputTarget(domResult));
        } catch (Exception e) {
            LOGGER.error("Failed to serialize element to XML", e);

            return null;
        }

        return domResult.getDocument().getRootElement();
    }

    /**
     * @param format true if the XML should be formated
     * @return the XML as a String
     * @since 9.0RC1
     */
    public String toXMLString(boolean format)
    {
        XAROutputProperties xarProperties = new XAROutputProperties();
        xarProperties.setFormat(format);

        try {
            return Utils.getComponent(XWikiDocumentFilterUtils.class).exportEntity(this, xarProperties);
        } catch (Exception e) {
            LOGGER.error("Failed to serialize collection to XML", e);

            return "";
        }
    }

    @Override
    public String toString()
    {
        return toXMLString(true);
    }
}
