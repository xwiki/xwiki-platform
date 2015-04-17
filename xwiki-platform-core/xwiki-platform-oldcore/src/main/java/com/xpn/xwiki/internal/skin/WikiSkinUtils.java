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
package com.xpn.xwiki.internal.skin;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.skin.Resource;
import org.xwiki.skin.Skin;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiSkinFileOverrideClassDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;

/**
 * @version $Id$
 * @since 6.4M1
 */
@Component(roles = WikiSkinUtils.class)
@Singleton
public class WikiSkinUtils
{
    public static final LocalDocumentReference SKINCLASS_REFERENCE = new LocalDocumentReference("XWiki", "XWikiSkins");

    public static final String SKINCLASS_BASESKIN = "baseskin";

    /**
     * Represents no value (ie the default value will be used) in xproperties.
     */
    // TODO: remove when XWIKI-10853 is fixed
    private static final String NO_VALUE = "---";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    @Inject
    private Logger logger;

    public XWikiDocument getSkinDocument(String skin)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        if (xcontext != null) {
            DocumentReference skinReference = this.currentMixedDocumentReferenceResolver.resolve(skin);
            XWiki xwiki = xcontext.getWiki();
            if (xwiki != null && xwiki.getStore() != null) {
                XWikiDocument doc;
                try {
                    doc = xwiki.getDocument(skinReference, xcontext);
                } catch (XWikiException e) {
                    this.logger.error("Faied to get document [{}]", skinReference, e);

                    return null;
                }
                if (!doc.isNew()) {
                    return doc;
                }
            }
        }

        return null;
    }

    private BaseObject getSkinObject(String skin)
    {
        XWikiDocument skinDocument = getSkinDocument(skin);

        return skinDocument != null ? skinDocument.getXObject(SKINCLASS_REFERENCE) : null;
    }

    public boolean isWikiSkin(String id)
    {
        return getSkinDocument(id) != null;
    }

    public Resource<?> getResource(String resourceName, Skin skin)
    {
        Resource<?> source = null;

        XWikiDocument skinDocument = getSkinDocument(skin.getId());
        if (skinDocument != null) {
            source = getSkinResourceFromDocumentSkin(resourceName, skinDocument, skin);
        }

        return source;
    }

    private Resource<?> getSkinResourceFromDocumentSkin(String resource, XWikiDocument skinDocument, Skin skin)
    {
        if (skinDocument != null) {
            // Try to find a XWikiSkinFileOverrideClass object
            BaseObject obj = skinDocument.getXObject(XWikiSkinFileOverrideClassDocumentInitializer.DOCUMENT_REFERENCE, 
                XWikiSkinFileOverrideClassDocumentInitializer.PROPERTY_PATH, resource, false);
            if (obj != null) {
                ObjectPropertyReference reference = new ObjectPropertyReference(
                    XWikiSkinFileOverrideClassDocumentInitializer.PROPERTY_CONTENT,
                        obj.getReference());
                return new ObjectPropertyWikiResource(getPath(reference), skin, reference,
                    skinDocument.getAuthorReference(), this.xcontextProvider,
                        obj.getLargeStringValue(XWikiSkinFileOverrideClassDocumentInitializer.PROPERTY_CONTENT));
            }
            
            // Try parsing the object property
            BaseProperty<ObjectPropertyReference> property = getSkinResourceProperty(resource, skinDocument);
            if (property != null) {
                ObjectPropertyReference reference = property.getReference();
                return new ObjectPropertyWikiResource(getPath(reference), skin, reference,
                    skinDocument.getAuthorReference(), this.xcontextProvider, (String) property.getValue());
            }

            // Try parsing a document attachment
            XWikiAttachment attachment = skinDocument.getAttachment(resource);
            if (attachment != null) {
                AttachmentReference reference = attachment.getReference();
                return new AttachmentWikiResource(getPath(reference), skin, reference, attachment.getAuthorReference(),
                    this.xcontextProvider);
            }
        }

        return null;
    }

    private BaseProperty<ObjectPropertyReference> getSkinResourceProperty(String resource, XWikiDocument skinDocument)
    {
        // Try parsing the object property
        BaseObject skinObject = skinDocument.getXObject(SKINCLASS_REFERENCE);
        if (skinObject != null) {
            BaseProperty<ObjectPropertyReference> resourceProperty =
                (BaseProperty<ObjectPropertyReference>) skinObject.safeget(resource);

            // If not found try by replacing '/' with '.'
            if (resourceProperty == null) {
                String escapedTemplateName = StringUtils.replaceChars(resource, '/', '.');
                resourceProperty = (BaseProperty<ObjectPropertyReference>) skinObject.safeget(escapedTemplateName);
            }

            if (resourceProperty != null) {
                Object value = resourceProperty.getValue();
                if (value instanceof String && StringUtils.isNotEmpty((String) value)) {
                    return resourceProperty;
                }
            }
        }

        return null;
    }
    
    public String getSkinProperty(String skin, String property)
    {
        BaseObject obj = getSkinObject(skin);
        if (obj != null) {
            return obj.getStringValue(property);
        }
        return null;
    }

    public String getParentId(String id)
    {
        BaseObject skinObject = getSkinObject(id);
        if (skinObject != null) {
            String parentId = skinObject.getStringValue(SKINCLASS_BASESKIN);
            // TODO: remove the NO_VALUE test when XWIKI-10853 is fixed
            if (StringUtils.isNotBlank(parentId) && !NO_VALUE.equals(parentId)) {
                return parentId;
            }
        }

        return null;
    }

    private String getPath(EntityReference reference)
    {
        return this.referenceSerializer.serialize(reference);
    }
}
