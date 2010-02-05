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

package com.xpn.xwiki.objects;

import java.io.Serializable;

import com.xpn.xwiki.web.Utils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;

/**
 * Base class for representing an element having a name (either a reference of a free form name) and a pretty name.
 *
 * @version $Id$
 */
public abstract class BaseElement implements ElementInterface, Serializable
{
    private static final Log LOG = LogFactory.getLog(BaseElement.class);

    /**
     * Reference to the document in which this element is defined (for elements where this make sense, for example
     * for an XClass or a XObject).
     */
    private DocumentReference reference;

    /**
     * Free form name (for elements which don't point to a reference, for example for instances of
     * {@link BaseProperty}).
     */
    private String name;

    private String prettyName;

    /**
     * Used to resolve a string into a proper Document Reference using the current document's reference to fill the
     * blanks, except for the page name for which the default page name is used instead and for the wiki name for which
     * the current wiki is used instead of the current document reference's wiki.
     */
    private DocumentReferenceResolver currentMixedDocumentReferenceResolver =
        Utils.getComponent(DocumentReferenceResolver.class, "currentmixed");

    /**
     * Used to convert a Document Reference to string (compact form without the wiki part if it matches the current
     * wiki).
     */
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer =
        Utils.getComponent(EntityReferenceSerializer.class, "compactwiki");

    /**
     * Used to convert a proper Document Reference to a string but without the wiki name.
     */
    private EntityReferenceSerializer<String> localEntityReferenceSerializer =
        Utils.getComponent(EntityReferenceSerializer.class, "local");

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.objects.ElementInterface#getDocumentReference()
     * @since 2.2M2
     */
    public DocumentReference getDocumentReference()
    {
        DocumentReference reference = this.reference;

        // If the reference is null then parse the name as a reference.
        if (reference == null && this.name != null) {
            reference = this.currentMixedDocumentReferenceResolver.resolve(this.name);
        }

        return reference;
    }

    /**
     * Note that this method is used by Hibernate for saving an element.
     *
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.objects.ElementInterface#getName()
     */
    public String getName()
    {
        String name = this.name;

        // If the name is null then serialize the reference as a string.
        if (name == null && this.reference != null) {
            name = this.localEntityReferenceSerializer.serialize(this.reference);
        }

        return name;
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.objects.ElementInterface#setDocumentReference(DocumentReference)
     * @since 2.2M2
     */
    public void setDocumentReference(DocumentReference reference)
    {
        // If the name is already set then reset it since we're now using a reference
        this.reference = reference;
        this.name = null;
    }

    /**
     * Note that this method is used by Hibernate for loading an element.
     *
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.objects.ElementInterface#setName(java.lang.String)
     */
    public void setName(String name)
    {
        // If the reference is already set, then continue using it. Ideally code calling setName should be modified to
        // call setReference in this case.
        if (this.reference != null) {
            this.reference = this.currentMixedDocumentReferenceResolver.resolve(name); 
        } else {
            this.name = name;
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
     * @return the name of the wiki where this element is stored. If null, the context's wiki is used.
     * @deprecated since 2.2M2 use {@link #getDocumentReference()} and
     *             {@link org.xwiki.model.reference.DocumentReference#getWikiReference()}
     */
    @Deprecated
    public String getWiki()
    {
        return getDocumentReference().getWikiReference().getName();
    }

    /**
     * @param wiki the name of the wiki where this element is stored. If null, the context's wiki is used.
     * @deprecated since 2.2M2 use {@link #setDocumentReference(DocumentReference)}
     */
    @Deprecated
    public void setWiki(String wiki)
    {
        if (!StringUtils.isEmpty(wiki)) {
            getDocumentReference().setWikiReference(new WikiReference(wiki));
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object el)
    {
        // Same Java object, they sure are equal
        if (this == el) {
            return true;
        }

        if (el == null || !(el instanceof BaseElement)) {
            return false;
        }

        BaseElement element = (BaseElement) el;

        DocumentReference elementReference = element.getDocumentReference();
        if (elementReference == null) {
            if (getDocumentReference() != null) {
                return false;
            }
        } else if (!elementReference.equals(getDocumentReference())) {
            return false;
        }

        if (element.getPrettyName() == null) {
            if (getPrettyName() != null) {
                return false;
            }
        } else if (!element.getPrettyName().equals(getPrettyName())) {
            return false;
        }

        if (!(element.getClass().equals(this.getClass()))) {
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     *
     * @see java.lang.Object#clone()
     */
    @Override
    public Object clone()
    {
        BaseElement element;
        try {
            element = getClass().newInstance();

            // Make sure we clone either the reference or the name depending on which one is used.
            if (this.reference != null) {
                element.setDocumentReference(getDocumentReference());
            } else if (this.name != null) {
                element.setName(getName());
            }

            element.setPrettyName(getPrettyName());
        } catch (Exception e) {
            // This should not happen
            element = null;
        }

        return element;
    }

    /**
     * Return the document where this element is stored.
     *
     * @param context the XWiki context
     * @return the document
     * @deprecated since 2.2M2 use {@link #getDocumentReference()} instead
     */
    @Deprecated
    public XWikiDocument getDocument(XWikiContext context) throws XWikiException
    {
        return context.getWiki().getDocument(getDocumentReference(), context);
    }

    /**
     * @return the syntax id of the document containing this element. If an error occurs while retrieving the document a
     *         syntax id of "xwiki/1.0" is assumed.
     * @deprecated since 2.2M2 use <code>context.getWiki().getDocument(object.getDocumentReference())</code> and then
     *             get the syntax on the document object
     */
    @Deprecated
    public String getDocumentSyntaxId(XWikiContext context)
    {
        String syntaxId;
        try {
            syntaxId = getDocument(context).getSyntaxId();
        } catch (Exception e) {
            LOG.warn("Error while getting the syntax corresponding to object ["
                + this.compactWikiEntityReferenceSerializer.serialize(getDocumentReference())
                + "]. Defaulting to using XWiki 1.0 syntax. Internal error [" + e.getMessage() + "]");
            syntaxId = "xwiki/1.0";
        }

        return syntaxId;
    }
}
