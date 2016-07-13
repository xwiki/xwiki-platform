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
package org.xwiki.crypto.store.wiki.internal;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.store.SignatureStore;
import org.xwiki.crypto.store.SignatureStoreException;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Default implementation of {@link SignatureStore}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@Component
@Singleton
public class DefaultSignatureStore implements SignatureStore
{
    /**
     * Reference to the signature class.
     */
    public static final LocalDocumentReference SIGNATURECLASS = new LocalDocumentReference("Crypto", "SignatureClass");

    /**
     * Name of the reference field.
     */
    public static final String SIGNATURECLASS_PROP_REFERENCE = "reference";

    /**
     * Name of the signature field.
     */
    public static final String SIGNATURECLASS_PROP_SIGNATURE = "signature";

    /**
     * Context provider used to manipulate XWikiDocuments and XObjects.
     */
    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Document reference resolver used to resolved provided entity reference to document reference.
     */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<EntityReference> resolver;

    /**
     * Encoder/decoder used to convert byte arrays to/from String.
     */
    @Inject
    @Named("Base64")
    private BinaryStringEncoder base64;

    @Override
    public void store(EntityReference entity, byte[] signature) throws SignatureStoreException
    {
        checkArguments(entity);

        // TODO: Support object property as parent
        DocumentReference docRef = this.resolver.resolve(entity.getParent());
        XWikiContext context = this.contextProvider.get();

        try {
            XWikiDocument document = context.getWiki().getDocument(docRef, context);

            DocumentReference signatureClass = new DocumentReference(SIGNATURECLASS, docRef.getWikiReference());
            // TODO: Support any kind of entity by serializing the reference and its type properly
            String entityReference = entity.getName();

            BaseObject object = document.getXObject(signatureClass, SIGNATURECLASS_PROP_REFERENCE, entityReference);
            if (object == null) {
                object = document.newXObject(SIGNATURECLASS, context);
                object.setStringValue(SIGNATURECLASS_PROP_REFERENCE, entityReference);
            }

            object.setLargeStringValue(SIGNATURECLASS_PROP_SIGNATURE, this.base64.encode(signature, 64));

            context.getWiki().saveDocument(document, context);
        } catch (XWikiException e) {
            throw new SignatureStoreException("Error while storing signature for entity [" + entity + "]", e);
        } catch (IOException e) {
            throw new SignatureStoreException("Error while encoding signature for entity [" + entity + "]", e);
        }
    }

    @Override
    public byte[] retrieve(EntityReference entity) throws SignatureStoreException
    {
        checkArguments(entity);

        DocumentReference docRef = this.resolver.resolve(entity);
        XWikiContext context = this.contextProvider.get();

        try {
            XWikiDocument document = context.getWiki().getDocument(docRef, context);
            DocumentReference signatureClass = new DocumentReference(SIGNATURECLASS, docRef.getWikiReference());
            // TODO: Support any kind of entity by serializing the reference and its type properly
            String entityReference = entity.getName();

            BaseObject object = document.getXObject(signatureClass, SIGNATURECLASS_PROP_REFERENCE, entityReference);
            if (object == null) {
                return null;
            }

            String signature = object.getLargeStringValue(SIGNATURECLASS_PROP_SIGNATURE);
            if (signature == null) {
                return null;
            }

            return this.base64.decode(signature);
        } catch (XWikiException e) {
            throw new SignatureStoreException("Error while retrieving signature for entity [" + entity + "]", e);
        } catch (IOException e) {
            throw new SignatureStoreException("Error while decoding signature for entity [" + entity + "]", e);
        }
    }

    private void checkArguments(EntityReference entity)
    {
        // TODO: Support any kind of entity
        if (entity.getType() != EntityType.BLOCK || entity.getParent() == null
            || entity.getParent().getType() != EntityType.DOCUMENT) {
            throw new IllegalArgumentException("Unsupported reference type [" + entity.getType() + "] or parent ["
                + "]. Only blocks of documents are supported.");
        }
    }
}
