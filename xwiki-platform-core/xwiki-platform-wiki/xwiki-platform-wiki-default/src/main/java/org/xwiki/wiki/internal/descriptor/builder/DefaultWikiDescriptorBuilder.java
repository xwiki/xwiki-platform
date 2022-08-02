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
package org.xwiki.wiki.internal.descriptor.builder;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.internal.descriptor.document.XWikiServerClassDocumentInitializer;
import org.xwiki.wiki.internal.descriptor.properties.WikiPropertyGroupManager;
import org.xwiki.wiki.manager.WikiManagerException;
import org.xwiki.wiki.properties.WikiPropertyGroupException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation for {@link org.xwiki.wiki.internal.descriptor.builder.WikiDescriptorBuilder}.
 * 
 * @version $Id$
 * @since 5.3M2
 */
@Component
@Singleton
public class DefaultWikiDescriptorBuilder implements WikiDescriptorBuilder
{
    /**
     * Page prefix for all descriptors.
     */
    static final String VALID_PAGE_PREFIX = "XWikiServer";

    /**
     * XWikiContext provider.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Reference serializer.
     */
    @Inject
    private EntityReferenceSerializer<String> referenceSerializer;

    @Inject
    private DocumentReferenceResolver<String> referenceResolver;

    @Inject
    @Named("user")
    private DocumentReferenceResolver<String> userReferenceResolver;

    @Inject
    private Provider<WikiPropertyGroupManager> wikiPropertyGroupManagerProvider;

    @Inject
    private WikiDescriptorDocumentHelper wikiDescriptorDocumentHelper;

    @Inject
    private EntityReferenceFactory referenceFactory;

    @Inject
    private Logger logger;

    private String getFullReference(String userId, String wikiId)
    {
        String result = null;

        if (!StringUtils.isBlank(userId)) {
            DocumentReference userReference = userReferenceResolver.resolve(userId, new WikiReference(wikiId));
            result = referenceSerializer.serialize(userReference);
        }

        return result;
    }

    @Override
    public DefaultWikiDescriptor buildDescriptorObject(List<BaseObject> serverClassObjects, XWikiDocument document)
    {
        List<BaseObject> normalizedServerClassObjects = normalizeServerClassObjects(serverClassObjects);
        BaseObject mainServerClassObject = normalizedServerClassObjects.remove(0);
        DefaultWikiDescriptor descriptor = extractWikiDescriptor(mainServerClassObject, document);

        if (descriptor != null) {
            // Create WikiAlias instances for the other XWikiServerClass objects
            for (BaseObject serverClassObject : normalizedServerClassObjects) {
                if (serverClassObject != null) {
                    String descriptorAlias = extractWikiAlias(serverClassObject);
                    descriptor.addAlias(descriptorAlias);
                }
            }

            // load properties
            descriptor.setMainPageReference(this.referenceFactory.getReference(referenceResolver
                .resolve(mainServerClassObject.getStringValue(XWikiServerClassDocumentInitializer.FIELD_HOMEPAGE))));
            descriptor.setPrettyName(
                mainServerClassObject.getStringValue(XWikiServerClassDocumentInitializer.FIELD_WIKIPRETTYNAME));
            descriptor.setOwnerId(
                getFullReference(mainServerClassObject.getStringValue(XWikiServerClassDocumentInitializer.FIELD_OWNER),
                    descriptor.getId()));
            descriptor.setDescription(
                mainServerClassObject.getStringValue(XWikiServerClassDocumentInitializer.FIELD_DESCRIPTION));
            int secure = mainServerClassObject.getIntValue(XWikiServerClassDocumentInitializer.FIELD_SECURE, -1);
            descriptor.setSecure(secure != -1 ? secure == 1 : null);
            descriptor.setPort(mainServerClassObject.getIntValue(XWikiServerClassDocumentInitializer.FIELD_PORT, -1));

            // load the property groups
            try {
                WikiPropertyGroupManager wikiPropertyGroupManager = wikiPropertyGroupManagerProvider.get();
                wikiPropertyGroupManager.loadForDescriptor(descriptor);
            } catch (WikiPropertyGroupException e) {
                logger.error("Failed to load wiki property groups for wiki [{}].", descriptor.getId(), e);
            }
        }

        return descriptor;
    }

    private List<BaseObject> normalizeServerClassObjects(List<BaseObject> serverClassObjects)
    {
        // Remove null entries. Nulls can happen due to how the serverClassObjects parameter has been retrieved. If it
        // was retrieved using the XWikiDocument#getXObjects() then it can have holes in it with null values.
        List<BaseObject> result = new ArrayList<>();
        for (BaseObject serverClassObject : serverClassObjects) {
            if (serverClassObject != null) {
                result.add(serverClassObject);
            }
        }
        return result;
    }

    private DefaultWikiDescriptor extractWikiDescriptor(BaseObject serverClassObject, XWikiDocument document)
    {
        DefaultWikiDescriptor descriptor = null;

        // If the page name doesn't start with "XWikiServer" then consider we have an invalid Wiki
        String wikiId = extractWikiId(document);
        if (wikiId != null) {
            descriptor = new DefaultWikiDescriptor(wikiId, extractWikiAlias(serverClassObject));
        }

        return descriptor;
    }

    private String extractWikiAlias(BaseObject serverClassObject)
    {
        return serverClassObject.getStringValue(XWikiServerClassDocumentInitializer.FIELD_SERVER);
    }

    private String extractWikiId(XWikiDocument document)
    {
        String wikiId = null;
        String pageName = document.getDocumentReference().getName();
        if (pageName.startsWith(VALID_PAGE_PREFIX)) {
            wikiId = StringUtils.removeStart(pageName, VALID_PAGE_PREFIX).toLowerCase();
        }
        return wikiId;
    }

    @Override
    public XWikiDocument save(WikiDescriptor descriptor) throws WikiDescriptorBuilderException
    {
        XWikiContext context = xcontextProvider.get();
        XWiki xwiki = context.getWiki();
        XWikiDocument descriptorDoc = null;

        try {
            // Create the descriptor document
            descriptorDoc = wikiDescriptorDocumentHelper.getDocumentFromWikiId(descriptor.getId());

            // Create the server class object
            BaseObject obj = descriptorDoc.getXObject(DefaultWikiDescriptor.SERVER_CLASS, true, context);
            obj.set(XWikiServerClassDocumentInitializer.FIELD_SERVER, descriptor.getDefaultAlias(), context);
            obj.set(XWikiServerClassDocumentInitializer.FIELD_HOMEPAGE,
                referenceSerializer.serialize(descriptor.getMainPageReference()), context);
            obj.set(XWikiServerClassDocumentInitializer.FIELD_OWNER,
                getFullReference(descriptor.getOwnerId(), descriptor.getId()), context);
            obj.set(XWikiServerClassDocumentInitializer.FIELD_WIKIPRETTYNAME, descriptor.getPrettyName(), context);
            obj.set(XWikiServerClassDocumentInitializer.FIELD_DESCRIPTION, descriptor.getDescription(), context);
            Boolean secure = descriptor.isSecure();
            obj.set(XWikiServerClassDocumentInitializer.FIELD_SECURE,
                secure != null ? (secure == Boolean.TRUE ? 1 : 0) : -1, context);
            obj.set(XWikiServerClassDocumentInitializer.FIELD_PORT, descriptor.getPort(), context);

            // Create the aliases
            List<String> aliases = descriptor.getAliases();
            for (int i = 1; i < aliases.size(); ++i) {
                String alias = aliases.get(i);
                // We use failover = false, otherwise it fall backs to the first SERVER_CLASS object of the document
                // instead of creating a new one.
                BaseObject objAlias = descriptorDoc.getXObject(DefaultWikiDescriptor.SERVER_CLASS,
                    XWikiServerClassDocumentInitializer.FIELD_SERVER, alias, false);
                if (objAlias == null) {
                    // Manually create a new alias
                    objAlias = descriptorDoc.getXObject(DefaultWikiDescriptor.SERVER_CLASS,
                        descriptorDoc.createXObject(DefaultWikiDescriptor.SERVER_CLASS, context));
                }
                objAlias.set(XWikiServerClassDocumentInitializer.FIELD_SERVER, alias, context);
            }

            // Set the meta-data (creator, hidden flag, parent, etc...)
            setDescriptorDocMetadata(descriptorDoc);

            // Save the document
            xwiki.saveDocument(descriptorDoc, context);

            // Save the property groups
            WikiPropertyGroupManager wikiPropertyGroupManager = wikiPropertyGroupManagerProvider.get();
            wikiPropertyGroupManager.saveForDescriptor(descriptor);

        } catch (WikiManagerException e) {
            throw new WikiDescriptorBuilderException("Unable to load the descriptor document", e);
        } catch (XWikiException e) {
            throw new WikiDescriptorBuilderException("Unable to save the descriptor document", e);
        } catch (WikiPropertyGroupException e) {
            throw new WikiDescriptorBuilderException("Unable to save the property groups", e);
        }

        return descriptorDoc;
    }

    private void setDescriptorDocMetadata(XWikiDocument descriptorDoc)
    {
        XWikiContext context = xcontextProvider.get();

        // Set the document as hidden
        descriptorDoc.setHidden(true);

        // The document must have a creator
        if (descriptorDoc.getCreatorReference() == null) {
            descriptorDoc.setCreatorReference(context.getUserReference());
        }
        // The document must have an author
        if (descriptorDoc.getAuthorReference() == null) {
            descriptorDoc.setAuthorReference(context.getUserReference());
        }

        // Set the document parent
        if (descriptorDoc.getParentReference() == null) {
            EntityReference parentReference = new EntityReference("WebHome", EntityType.DOCUMENT);
            parentReference.appendParent(new EntityReference("WikiManager", EntityType.SPACE));
            descriptorDoc.setParentReference(parentReference);
        }
    }
}
