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

import java.math.BigInteger;
import java.util.Collections;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.crypto.AsymmetricKeyFactory;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.params.cipher.asymmetric.PrivateKeyParameters;
import org.xwiki.crypto.password.PrivateKeyPasswordBasedEncryptor;
import org.xwiki.crypto.pkix.CertificateFactory;
import org.xwiki.crypto.pkix.params.CertifiedKeyPair;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.DistinguishedName;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Extensions;
import org.xwiki.crypto.store.StoreReference;
import org.xwiki.crypto.store.WikiStoreReference;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentStringEntityReferenceResolver;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link X509KeyWikiStore}.
 *
 * @version $Id$
 * @since 6.0
 */
@ComponentTest
@ComponentList({
    CurrentReferenceDocumentReferenceResolver.class,
    CurrentReferenceEntityReferenceResolver.class,
    CurrentStringEntityReferenceResolver.class,
    DefaultSymbolScheme.class
})
class X509KeyWikiStoreTest
{
    private static final byte[] PASSWORD = "password".getBytes();
    private static final byte[] PRIVATEKEY = "privatekey".getBytes();
    private static final String ENCODED_PRIVATEKEY = "encoded_privatekey";
    private static final byte[] ENCRYPTED_PRIVATEKEY = "encrypted_privatekey".getBytes();
    private static final String ENCODED_ENCRYPTED_PRIVATEKEY = "encoded_encrypted_privatekey";
    private static final byte[] CERTIFICATE = "certificate".getBytes();
    private static final String ENCODED_CERTIFICATE = "encoded_certificate";
    private static final byte[] SUBJECT_KEYID = "subjectKeyId".getBytes();
    private static final String ENCODED_SUBJECTKEYID = "encoded_subjectKeyId";
    private static final String SUBJECT = "CN=Subject";
    private static final String ISSUER = "CN=Issuer";
    private static final BigInteger SERIAL = new BigInteger("1234567890");

    private static final String WIKI = "wiki";
    private static final String SPACE = "space";
    private static final String DOCUMENT = "document";

    private static final WikiReference WIKI_REFERENCE = new WikiReference(WIKI);
    private static final EntityReference SPACE_REFERENCE = new EntityReference(SPACE, EntityType.WIKI);
    private static final EntityReference DOCUMENT_REFERENCE = new EntityReference(DOCUMENT, EntityType.DOCUMENT);

    private static final LocalDocumentReference DOC_STORE_ENTREF = new LocalDocumentReference("space", DOCUMENT);
    private static final EntityReference SPACE_STORE_ENTREF = new EntityReference(SPACE, EntityType.SPACE);

    private static final StoreReference DOC_STORE_REF = new WikiStoreReference(DOC_STORE_ENTREF);
    private static final StoreReference SPACE_STORE_REF = new WikiStoreReference(SPACE_STORE_ENTREF);

    @InjectMockComponents
    private X509KeyWikiStore store;

    @MockComponent
    @Named("default")
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @MockComponent
    @Named("current")
    private EntityReferenceProvider currentEntityReferenceProvider;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("Base64")
    private BinaryStringEncoder encoder;

    @MockComponent
    private AsymmetricKeyFactory keyFactory;

    @MockComponent
    private PrivateKeyPasswordBasedEncryptor encryptor;

    @MockComponent
    @Named("X509")
    private CertificateFactory certificateFactory;

    @MockComponent
    private QueryManager queryManager;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private Query query;

    private PrivateKeyParameters privateKey;

    private X509CertifiedPublicKey certificate;

    private CertifiedKeyPair keyPair;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.currentEntityReferenceProvider.getDefaultReference(EntityType.WIKI)).thenReturn(WIKI_REFERENCE);
        when(this.currentEntityReferenceProvider.getDefaultReference(EntityType.SPACE)).thenReturn(SPACE_REFERENCE);
        when(this.currentEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(DOCUMENT_REFERENCE);

        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        this.xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        when(this.encoder.encode(PRIVATEKEY, 64)).thenReturn(ENCODED_PRIVATEKEY);
        when(this.encoder.decode(ENCODED_PRIVATEKEY)).thenReturn(PRIVATEKEY);
        when(this.encoder.encode(ENCRYPTED_PRIVATEKEY, 64)).thenReturn(ENCODED_ENCRYPTED_PRIVATEKEY);
        when(this.encoder.decode(ENCODED_ENCRYPTED_PRIVATEKEY)).thenReturn(ENCRYPTED_PRIVATEKEY);
        when(this.encoder.encode(CERTIFICATE, 64)).thenReturn(ENCODED_CERTIFICATE);
        when(this.encoder.decode(ENCODED_CERTIFICATE)).thenReturn(CERTIFICATE);
        when(this.encoder.encode(SUBJECT_KEYID)).thenReturn(ENCODED_SUBJECTKEYID);
        when(this.encoder.decode(ENCODED_SUBJECTKEYID)).thenReturn(SUBJECT_KEYID);

        this.privateKey = mock(PrivateKeyParameters.class);
        when(this.privateKey.getEncoded()).thenReturn(PRIVATEKEY);

        when(this.keyFactory.fromPKCS8(PRIVATEKEY)).thenReturn(this.privateKey);

        when(this.encryptor.encrypt(PASSWORD, this.privateKey)).thenReturn(ENCRYPTED_PRIVATEKEY);
        when(this.encryptor.decrypt(PASSWORD, ENCRYPTED_PRIVATEKEY)).thenReturn(this.privateKey);

        this.certificate = mock(X509CertifiedPublicKey.class);
        when(this.certificate.getSerialNumber()).thenReturn(SERIAL);
        when(this.certificate.getIssuer()).thenReturn(new DistinguishedName(ISSUER));
        when(this.certificate.getSubject()).thenReturn(new DistinguishedName(SUBJECT));
        when(this.certificate.getEncoded()).thenReturn(CERTIFICATE);

        when(this.certificateFactory.decode(CERTIFICATE)).thenReturn(this.certificate);

        X509Extensions extensions = mock(X509Extensions.class);
        when(this.certificate.getExtensions()).thenReturn(extensions);
        when(extensions.getSubjectKeyIdentifier()).thenReturn(SUBJECT_KEYID);
        when(this.certificate.getSubjectKeyIdentifier()).thenReturn(SUBJECT_KEYID);

        this.keyPair = new CertifiedKeyPair(this.privateKey, this.certificate);

        this.query = mock(Query.class);
        when(this.query.bindValue(any(String.class), any())).thenReturn(this.query);
        when(this.query.setWiki(WIKI)).thenReturn(this.query);
        when(this.queryManager.createQuery(any(String.class), any(String.class))).thenReturn(this.query);
    }

    @Test
    void storingPrivateKeyToEmptyDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), this.xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509CertificateWikiStore.CERTIFICATECLASS, this.xcontext)).thenReturn(certObj);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509KeyWikiStore.PRIVATEKEYCLASS, this.xcontext)).thenReturn(pkObj);

        this.store.store(DOC_STORE_REF, this.keyPair);

        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_KEYID, ENCODED_SUBJECTKEYID);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_ISSUER, ISSUER);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SERIAL, SERIAL.toString());
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SUBJECT, SUBJECT);
        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE,
            ENCODED_CERTIFICATE);
        verify(pkObj).setLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY, ENCODED_PRIVATEKEY);

        verify(this.xwiki).saveDocument(storeDoc, this.xcontext);
    }

    @Test
    void storingPrivateKeyToCertificateDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), this.xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS, 0)).thenReturn(certObj);

        when(this.query.<Object[]>execute()).thenReturn(Collections.singletonList(new Object[]{"space.document", 0}));

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509KeyWikiStore.PRIVATEKEYCLASS, this.xcontext)).thenReturn(pkObj);

        this.store.store(DOC_STORE_REF, this.keyPair);

        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE,
            ENCODED_CERTIFICATE);
        verify(pkObj).setLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY, ENCODED_PRIVATEKEY);

        verify(this.xwiki).saveDocument(storeDoc, this.xcontext);
    }

    @Test
    void storingPrivateKeyToEmptySpace() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference(WIKI, SPACE, ENCODED_SUBJECTKEYID), this.xcontext))
            .thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509CertificateWikiStore.CERTIFICATECLASS, this.xcontext)).thenReturn(certObj);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509KeyWikiStore.PRIVATEKEYCLASS, this.xcontext)).thenReturn(pkObj);

        this.store.store(SPACE_STORE_REF, this.keyPair);

        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_KEYID, ENCODED_SUBJECTKEYID);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_ISSUER, ISSUER);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SERIAL, SERIAL.toString());
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SUBJECT, SUBJECT);
        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE,
            ENCODED_CERTIFICATE);
        verify(pkObj).setLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY, ENCODED_PRIVATEKEY);

        verify(this.xwiki).saveDocument(storeDoc, this.xcontext);
    }

    @Test
    void storingPrivateKeyToCertificateSpace() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference(WIKI, SPACE, ENCODED_SUBJECTKEYID), this.xcontext))
            .thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS, 0)).thenReturn(certObj);

        when(this.query.<Object[]>execute())
            .thenReturn(Collections.singletonList(new Object[]{"space." + ENCODED_SUBJECTKEYID, 0}));

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509KeyWikiStore.PRIVATEKEYCLASS, this.xcontext)).thenReturn(pkObj);

        this.store.store(SPACE_STORE_REF, this.keyPair);

        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE,
            ENCODED_CERTIFICATE);
        verify(pkObj).setLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY, ENCODED_PRIVATEKEY);

        verify(this.xwiki).saveDocument(storeDoc, this.xcontext);
    }

    @Test
    void storingEncryptedPrivateKey() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), this.xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509CertificateWikiStore.CERTIFICATECLASS, this.xcontext)).thenReturn(certObj);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509KeyWikiStore.PRIVATEKEYCLASS, this.xcontext)).thenReturn(pkObj);

        this.store.store(DOC_STORE_REF, this.keyPair, PASSWORD);

        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_KEYID, ENCODED_SUBJECTKEYID);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_ISSUER, ISSUER);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SERIAL, SERIAL.toString());
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SUBJECT, SUBJECT);
        verify(certObj).setLargeStringValue(
            X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);
        verify(pkObj).setLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY, ENCODED_ENCRYPTED_PRIVATEKEY);

        verify(this.xwiki).saveDocument(storeDoc, this.xcontext);
    }

    @Test
    void updatingPrivateKey() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), this.xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS, 0)).thenReturn(certObj);

        when(this.query.<Object[]>execute()).thenReturn(Collections.singletonList(new Object[]{"space.document", 0}));

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509KeyWikiStore.PRIVATEKEYCLASS)).thenReturn(pkObj);

        this.store.store(DOC_STORE_REF, this.keyPair);

        verify(certObj).setLargeStringValue(
            X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);
        verify(pkObj).setLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY, ENCODED_PRIVATEKEY);

        verify(this.xwiki).saveDocument(storeDoc, this.xcontext);
    }

    @Test
    void retrievePrivateKeyFromDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), this.xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS)).thenReturn(certObj);
        when(certObj.getLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE))
            .thenReturn(ENCODED_CERTIFICATE);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509KeyWikiStore.PRIVATEKEYCLASS)).thenReturn(pkObj);
        when(pkObj.getLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY)).thenReturn(ENCODED_PRIVATEKEY);

        CertifiedKeyPair ckp = this.store.retrieve(DOC_STORE_REF);
        assertNotNull(ckp);
        assertEquals(this.privateKey, ckp.getPrivateKey());
        assertEquals(this.certificate, ckp.getCertificate());
    }

    @Test
    void retrieveEncryptedPrivateKeyFromDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), this.xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS)).thenReturn(certObj);
        when(certObj.getLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE))
            .thenReturn(ENCODED_CERTIFICATE);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509KeyWikiStore.PRIVATEKEYCLASS)).thenReturn(pkObj);
        when(pkObj.getLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY))
            .thenReturn(ENCODED_ENCRYPTED_PRIVATEKEY);

        CertifiedKeyPair ckp = this.store.retrieve(DOC_STORE_REF, PASSWORD);
        assertNotNull(ckp);
        assertEquals(this.privateKey, ckp.getPrivateKey());
        assertEquals((CertifiedPublicKey) this.certificate, ckp.getCertificate());
    }

    @Test
    void retrievePrivateKeyFromSpace() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference(WIKI, SPACE, ENCODED_SUBJECTKEYID), this.xcontext))
            .thenReturn(storeDoc);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509KeyWikiStore.PRIVATEKEYCLASS)).thenReturn(pkObj);
        when(pkObj.getLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY)).thenReturn(ENCODED_PRIVATEKEY);

        when(this.query.<Object[]>execute())
            .thenReturn(Collections.singletonList(new Object[]{"space." + ENCODED_SUBJECTKEYID, 0}));

        CertifiedKeyPair ckp = this.store.retrieve(SPACE_STORE_REF, this.certificate);
        assertNotNull(ckp);
        assertEquals(this.privateKey, ckp.getPrivateKey());
        assertEquals(this.certificate, ckp.getCertificate());
    }

    @Test
    void retrieveEncryptedPrivateKeyFromSpace() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference(WIKI, SPACE, ENCODED_SUBJECTKEYID), this.xcontext))
            .thenReturn(storeDoc);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509KeyWikiStore.PRIVATEKEYCLASS)).thenReturn(pkObj);
        when(pkObj.getLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY))
            .thenReturn(ENCODED_ENCRYPTED_PRIVATEKEY);

        when(this.query.<Object[]>execute())
            .thenReturn(Collections.singletonList(new Object[]{"space." + ENCODED_SUBJECTKEYID, 0}));

        CertifiedKeyPair ckp = this.store.retrieve(SPACE_STORE_REF, this.certificate, PASSWORD);
        assertNotNull(ckp);
        assertEquals(this.privateKey, ckp.getPrivateKey());
        assertEquals(this.certificate, ckp.getCertificate());
    }

    @Test
    void retrieveMissingPrivateKeyFromDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), this.xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS)).thenReturn(certObj);
        when(certObj.getLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE))
            .thenReturn(ENCODED_CERTIFICATE);

        CertifiedKeyPair ckp = this.store.retrieve(DOC_STORE_REF);
        assertNull(ckp);
    }

    @Test
    void retrieveMissingCertificateFromDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), this.xcontext)).thenReturn(storeDoc);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509KeyWikiStore.PRIVATEKEYCLASS)).thenReturn(pkObj);
        when(pkObj.getLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY))
            .thenReturn(ENCODED_ENCRYPTED_PRIVATEKEY);

        CertifiedKeyPair ckp = this.store.retrieve(DOC_STORE_REF);
        assertNull(ckp);
    }

    @Test
    void retrieveMissingPrivateKeyFromSpace() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference(WIKI, SPACE, ENCODED_SUBJECTKEYID), this.xcontext))
            .thenReturn(storeDoc);

        when(this.query.<Object[]>execute())
            .thenReturn(Collections.singletonList(new Object[]{"space." + ENCODED_SUBJECTKEYID, 0}));

        CertifiedKeyPair ckp = this.store.retrieve(SPACE_STORE_REF, this.certificate);
        assertNull(ckp);
    }

    @Test
    void retrieveMissingCertificateFromSpace() throws Exception
    {
        CertifiedKeyPair ckp = this.store.retrieve(SPACE_STORE_REF, this.certificate);
        assertNull(ckp);
    }
}
