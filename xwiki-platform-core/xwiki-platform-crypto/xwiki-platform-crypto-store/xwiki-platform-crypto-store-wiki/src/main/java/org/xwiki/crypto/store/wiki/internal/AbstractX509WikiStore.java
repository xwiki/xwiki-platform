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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.pkix.CertificateFactory;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertifiedPublicKey;
import org.xwiki.crypto.store.CertificateStoreException;
import org.xwiki.crypto.store.StoreReference;
import org.xwiki.crypto.store.WikiStoreReference;
import org.xwiki.crypto.store.wiki.internal.query.CertificateObjectReference;
import org.xwiki.crypto.store.wiki.internal.query.X509CertificateReferenceIssuerAndSerialQuery;
import org.xwiki.crypto.store.wiki.internal.query.X509CertificateReferenceKeyIdentifierQuery;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Abstract base class for X509 stores.
 *
 * @version $Id$
 * @since 6.1M2
 */
public abstract class AbstractX509WikiStore
{
    /**
     * Used while accessing documents and objects.
     */
    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Used to resolve store references to full references.
     */
    @Inject
    @Named("current")
    private EntityReferenceResolver<EntityReference> referenceResolver;

    /**
     * Used to convert document references returned by query.
     */
    @Inject
    @Named("current")
    private EntityReferenceResolver<String> stringReferenceResolver;

    /**
     * Used to encode/decode certificates, private keys and subject keys.
     */
    @Inject
    @Named("Base64")
    private BinaryStringEncoder base64;

    /**
     * Used to create certificate from encoded bytes.
     */
    @Inject
    @Named("X509")
    private CertificateFactory certificateFactory;

    /**
     * Use to query certificate objects.
     */
    @Inject
    private QueryManager queryManager;

    /**
     * @return the XWikiContext.
     */
    protected XWikiContext getXWikiContext()
    {
        return this.contextProvider.get();
    }

    /**
     * @return the base64 encoder.
     */
    protected BinaryStringEncoder getEncoder()
    {
        return this.base64;
    }

    /**
     * @return the certificate factory.
     */
    protected CertificateFactory getCertificateFactory()
    {
        return this.certificateFactory;
    }

    /**
     * @return the query manager.
     */
    protected QueryManager getQueryManager()
    {
        return this.queryManager;
    }

    /**
     * Create or update a certificate into the appropriate document of the given store, and return the unsaved document.
     *
     * @param store the reference of a document or a space where the certificate should be stored.
     * @param certificate the certificate to store.
     * @param context the XWikiContext.
     * @return the XWiki document to be saved where the object was updated or created.
     * @throws CertificateStoreException on error.
     */
    protected XWikiDocument storeCertificate(StoreReference store, CertifiedPublicKey certificate,
        XWikiContext context) throws CertificateStoreException
    {
        if (!(certificate instanceof X509CertifiedPublicKey)) {
            throw new IllegalArgumentException("Certificate should be X509 certificates.");
        }

        X509CertifiedPublicKey publicKey = (X509CertifiedPublicKey) certificate;

        try {
            CertificateObjectReference certRef = findCertificate(store, publicKey);

            XWikiDocument document;
            BaseObject obj;

            if (certRef != null) {
                document = getDocument(store, certRef, context);
                obj = document.getXObject(X509CertificateWikiStore.CERTIFICATECLASS, certRef.getObjectNumber());
            } else {
                document = context.getWiki().getDocument(getDocumentReference(store, publicKey), context);
                obj = document.newXObject(X509CertificateWikiStore.CERTIFICATECLASS, context);

                byte[] keyId = publicKey.getSubjectKeyIdentifier();
                if (keyId != null) {
                    obj.setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_KEYID, this.base64.encode(keyId));
                }
                obj.setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_ISSUER,
                    publicKey.getIssuer().getName());
                obj.setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SERIAL,
                    publicKey.getSerialNumber().toString());
                obj.setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SUBJECT,
                    publicKey.getSubject().getName());
            }

            obj.setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE,
                this.base64.encode(certificate.getEncoded(), 64));

            return document;
        } catch (Exception e) {
            throw new CertificateStoreException("Error while preparing certificate for store [" + store + "]", e);
        }
    }

    /**
     * Find the reference of the XObject storing a matching certificate in the given store.
     *
     * @param store the reference to a document or space used to store certificates.
     * @param publicKey the certificate to find the storage for.
     * @return a reference to the XObject storing a matching certificate, or null if none were found.
     * @throws CertificateStoreException on error.
     */
    protected CertificateObjectReference findCertificate(StoreReference store, X509CertifiedPublicKey publicKey)
        throws CertificateStoreException
    {
        byte[] keyId = publicKey.getSubjectKeyIdentifier();
        CertificateObjectReference certRef;
        if (keyId != null) {
            certRef =
                new X509CertificateReferenceKeyIdentifierQuery(resolveStore(store), this.base64, this.queryManager)
                    .getReference(keyId);
        } else {
            certRef =
                new X509CertificateReferenceIssuerAndSerialQuery(resolveStore(store), this.base64, this.queryManager)
                    .getReference(publicKey.getIssuer(), publicKey.getSerialNumber());
        }

        return certRef;
    }

    /**
     * Retrieve the document corresponding to a given certificate object reference.
     *
     * @param store the reference of a document or a space where the certificates are stored.
     * @param certRef the certificate object reference to get document from.
     * @param context the XWikiContext.
     * @return the document corresponding to the certificate reference.
     * @throws XWikiException
     */
    protected XWikiDocument getDocument(StoreReference store, CertificateObjectReference certRef, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument document;
        document = context.getWiki().getDocument(
            new DocumentReference(this.stringReferenceResolver.resolve(certRef.getDocumentName(),
                EntityType.DOCUMENT, store)),
            context);
        return document;
    }

    /**
     * Resolve the given store into a document reference.
     *
     * @param store the store to be resolved.
     * @return a document reference.
     */
    protected DocumentReference getDocumentReference(StoreReference store)
    {
        return new DocumentReference(this.referenceResolver.resolve(getStoreReference(store), EntityType.DOCUMENT));
    }

    /**
     * Create a document reference appropriate to store the certificate in the given store.
     *
     * @param store the reference to a document or space used to store certificates.
     * @param publicKey the certificate to store.
     * @return a document reference to an appropriate document for storage.
     * @throws Exception on error.
     */
    protected DocumentReference getDocumentReference(StoreReference store, X509CertifiedPublicKey publicKey)
        throws Exception
    {
        EntityReference reference = getStoreReference(store);

        if (reference.getType() == EntityType.DOCUMENT) {
            return getDocumentReference(store);
        }
        return new DocumentReference(this.referenceResolver.resolve(
            new EntityReference(getCertIdentifier(publicKey), EntityType.DOCUMENT), EntityType.DOCUMENT, reference));
    }

    /**
     * Return a unique identifier appropriate for a document name.
     * If the certificate as a subject key identifier, the result is this encoded identifier.
     * Else, use the concatenation of the certificate serial number and the issuer name.
     *
     * @param publicKey the certificate.
     * @return a unique identifier.
     * @throws Exception on error.
     */
    private String getCertIdentifier(X509CertifiedPublicKey publicKey) throws Exception
    {
        byte[] keyId = publicKey.getSubjectKeyIdentifier();
        if (keyId != null) {
            return this.base64.encode(keyId);
        }
        return publicKey.getSerialNumber().toString() + ", " + publicKey.getIssuer().getName();
    }

    /**
     * Resolve a given store into a complete reference (either space or document).
     *
     * @param store the store to be resolved.
     * @return a complete entity reference.
     */
    protected EntityReference resolveStore(StoreReference store)
    {
        EntityReference reference = getStoreReference(store);

        if (reference.getType() == EntityType.DOCUMENT) {
            return this.referenceResolver.resolve(reference, EntityType.DOCUMENT);
        }

        return this.referenceResolver.resolve(reference, EntityType.SPACE);
    }

    private EntityReference getStoreReference(StoreReference store)
    {
        if (store instanceof WikiStoreReference) {
            return ((WikiStoreReference) store).getReference();
        }
        throw new IllegalArgumentException("Unsupported store reference [" + store.getClass().getName()
            + "] for this implementation.");
    }
}
