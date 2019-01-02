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

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
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
import org.xwiki.crypto.store.KeyStore;
import org.xwiki.crypto.store.StoreReference;
import org.xwiki.crypto.store.WikiStoreReference;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultEntityReferenceProvider;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentStringEntityReferenceResolver;
import com.xpn.xwiki.objects.BaseObject;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
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
@ComponentList({
    CurrentReferenceDocumentReferenceResolver.class,
    CurrentReferenceEntityReferenceResolver.class,
    CurrentStringEntityReferenceResolver.class,
    DefaultSymbolScheme.class
})
public class X509KeyWikiStoreTest
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

    @Rule
    public MockitoComponentMockingRule<KeyStore> mocker = new MockitoComponentMockingRule<>(X509KeyWikiStore.class);

    private XWikiContext xcontext;
    private XWiki xwiki;

    private Query query;

    private KeyStore store;

    PrivateKeyParameters privateKey;
    X509CertifiedPublicKey certificate;
    CertifiedKeyPair keyPair;

    @Before
    public void setUp() throws Exception
    {
        this.mocker.registerMockComponent(EntityReferenceProvider.class, "default");

        EntityReferenceProvider valueProvider = this.mocker.registerMockComponent(EntityReferenceProvider.class, "current");
        when(valueProvider.getDefaultReference(EntityType.WIKI)).thenReturn(WIKI_REFERENCE);
        when(valueProvider.getDefaultReference(EntityType.SPACE)).thenReturn(SPACE_REFERENCE);
        when(valueProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(DOCUMENT_REFERENCE);

        mocker.registerComponent(EntityReferenceProvider.class, "current", valueProvider);

        Provider<XWikiContext> xcontextProvider =
            mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        BinaryStringEncoder encoder = mocker.getInstance(BinaryStringEncoder.class, "Base64");
        when(encoder.encode(PRIVATEKEY, 64)).thenReturn(ENCODED_PRIVATEKEY);
        when(encoder.decode(ENCODED_PRIVATEKEY)).thenReturn(PRIVATEKEY);
        when(encoder.encode(ENCRYPTED_PRIVATEKEY, 64)).thenReturn(ENCODED_ENCRYPTED_PRIVATEKEY);
        when(encoder.decode(ENCODED_ENCRYPTED_PRIVATEKEY)).thenReturn(ENCRYPTED_PRIVATEKEY);
        when(encoder.encode(CERTIFICATE, 64)).thenReturn(ENCODED_CERTIFICATE);
        when(encoder.decode(ENCODED_CERTIFICATE)).thenReturn(CERTIFICATE);
        when(encoder.encode(SUBJECT_KEYID)).thenReturn(ENCODED_SUBJECTKEYID);
        when(encoder.decode(ENCODED_SUBJECTKEYID)).thenReturn(SUBJECT_KEYID);

        privateKey = mock(PrivateKeyParameters.class);
        when(privateKey.getEncoded()).thenReturn(PRIVATEKEY);

        AsymmetricKeyFactory keyFactory = mocker.getInstance(AsymmetricKeyFactory.class);
        when(keyFactory.fromPKCS8(PRIVATEKEY)).thenReturn(privateKey);

        PrivateKeyPasswordBasedEncryptor encryptor = mocker.getInstance(PrivateKeyPasswordBasedEncryptor.class);
        when(encryptor.encrypt(PASSWORD, privateKey)).thenReturn(ENCRYPTED_PRIVATEKEY);
        when(encryptor.decrypt(PASSWORD, ENCRYPTED_PRIVATEKEY)).thenReturn(privateKey);

        certificate = mock(X509CertifiedPublicKey.class);
        when(certificate.getSerialNumber()).thenReturn(SERIAL);
        when(certificate.getIssuer()).thenReturn(new DistinguishedName(ISSUER));
        when(certificate.getSubject()).thenReturn(new DistinguishedName(SUBJECT));
        when(certificate.getEncoded()).thenReturn(CERTIFICATE);

        CertificateFactory certificateFactory = mocker.getInstance(CertificateFactory.class, "X509");
        when(certificateFactory.decode(CERTIFICATE)).thenReturn(certificate);

        X509Extensions extensions = mock(X509Extensions.class);
        when(certificate.getExtensions()).thenReturn(extensions);
        when(extensions.getSubjectKeyIdentifier()).thenReturn(SUBJECT_KEYID);
        when(certificate.getSubjectKeyIdentifier()).thenReturn(SUBJECT_KEYID);

        keyPair = new CertifiedKeyPair(privateKey, certificate);

        QueryManager queryManager = mocker.getInstance(QueryManager.class);
        query = mock(Query.class);
        when(query.bindValue(any(String.class), any())).thenReturn(query);
        when(query.setWiki(WIKI)).thenReturn(query);
        when(queryManager.createQuery(any(String.class), any(String.class))).thenReturn(query);

        store = mocker.getComponentUnderTest();
    }

    @Test
    public void storingPrivateKeyToEmptyDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509CertificateWikiStore.CERTIFICATECLASS, xcontext)).thenReturn(certObj);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509KeyWikiStore.PRIVATEKEYCLASS, xcontext)).thenReturn(pkObj);

        store.store(DOC_STORE_REF, keyPair);

        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_KEYID, ENCODED_SUBJECTKEYID);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_ISSUER, ISSUER);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SERIAL, SERIAL.toString());
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SUBJECT, SUBJECT);
        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);
        verify(pkObj).setLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY, ENCODED_PRIVATEKEY);

        verify(xwiki).saveDocument(storeDoc, xcontext);
    }

    @Test
    public void storingPrivateKeyToCertificateDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS, 0)).thenReturn(certObj);

        when(query.<Object[]>execute()).thenReturn(Collections.singletonList(new Object[]{"space.document", 0}));

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509KeyWikiStore.PRIVATEKEYCLASS, xcontext)).thenReturn(pkObj);

        store.store(DOC_STORE_REF, keyPair);

        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);
        verify(pkObj).setLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY, ENCODED_PRIVATEKEY);

        verify(xwiki).saveDocument(storeDoc, xcontext);
    }

    @Test
    public void storingPrivateKeyToEmptySpace() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, ENCODED_SUBJECTKEYID), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509CertificateWikiStore.CERTIFICATECLASS, xcontext)).thenReturn(certObj);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509KeyWikiStore.PRIVATEKEYCLASS, xcontext)).thenReturn(pkObj);

        store.store(SPACE_STORE_REF, keyPair);

        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_KEYID, ENCODED_SUBJECTKEYID);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_ISSUER, ISSUER);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SERIAL, SERIAL.toString());
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SUBJECT, SUBJECT);
        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);
        verify(pkObj).setLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY, ENCODED_PRIVATEKEY);

        verify(xwiki).saveDocument(storeDoc, xcontext);
    }

    @Test
    public void storingPrivateKeyToCertificateSpace() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, ENCODED_SUBJECTKEYID), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS, 0)).thenReturn(certObj);

        when(query.<Object[]>execute()).thenReturn(Collections.singletonList(new Object[]{"space." + ENCODED_SUBJECTKEYID, 0}));

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509KeyWikiStore.PRIVATEKEYCLASS, xcontext)).thenReturn(pkObj);

        store.store(SPACE_STORE_REF, keyPair);

        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);
        verify(pkObj).setLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY, ENCODED_PRIVATEKEY);

        verify(xwiki).saveDocument(storeDoc, xcontext);
    }

    @Test
    public void storingEncryptedPrivateKey() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509CertificateWikiStore.CERTIFICATECLASS, xcontext)).thenReturn(certObj);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509KeyWikiStore.PRIVATEKEYCLASS, xcontext)).thenReturn(pkObj);

        store.store(DOC_STORE_REF, keyPair, PASSWORD);

        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_KEYID, ENCODED_SUBJECTKEYID);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_ISSUER, ISSUER);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SERIAL, SERIAL.toString());
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SUBJECT, SUBJECT);
        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);
        verify(pkObj).setLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY, ENCODED_ENCRYPTED_PRIVATEKEY);

        verify(xwiki).saveDocument(storeDoc, xcontext);
    }

    @Test
    public void updatingPrivateKey() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS, 0)).thenReturn(certObj);

        when(query.<Object[]>execute()).thenReturn(Collections.singletonList(new Object[]{"space.document", 0}));

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509KeyWikiStore.PRIVATEKEYCLASS)).thenReturn(pkObj);

        store.store(DOC_STORE_REF, keyPair);

        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);
        verify(pkObj).setLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY, ENCODED_PRIVATEKEY);

        verify(xwiki).saveDocument(storeDoc, xcontext);
    }

    @Test
    public void retrievePrivateKeyFromDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS)).thenReturn(certObj);
        when(certObj.getLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE)).thenReturn(ENCODED_CERTIFICATE);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509KeyWikiStore.PRIVATEKEYCLASS)).thenReturn(pkObj);
        when(pkObj.getLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY)).thenReturn(ENCODED_PRIVATEKEY);

        CertifiedKeyPair keyPair = store.retrieve(DOC_STORE_REF);
        assertThat(keyPair, notNullValue());
        assertThat(keyPair.getPrivateKey(), equalTo(privateKey));
        assertThat(keyPair.getCertificate(), equalTo((CertifiedPublicKey) certificate));
    }

    @Test
    public void retrieveEncryptedPrivateKeyFromDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS)).thenReturn(certObj);
        when(certObj.getLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE)).thenReturn(ENCODED_CERTIFICATE);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509KeyWikiStore.PRIVATEKEYCLASS)).thenReturn(pkObj);
        when(pkObj.getLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY)).thenReturn(ENCODED_ENCRYPTED_PRIVATEKEY);

        CertifiedKeyPair keyPair = store.retrieve(DOC_STORE_REF, PASSWORD);
        assertThat(keyPair, notNullValue());
        assertThat(keyPair.getPrivateKey(), equalTo(privateKey));
        assertThat(keyPair.getCertificate(), equalTo((CertifiedPublicKey) certificate));
    }

    @Test
    public void retrievePrivateKeyFromSpace() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, ENCODED_SUBJECTKEYID), xcontext)).thenReturn(storeDoc);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509KeyWikiStore.PRIVATEKEYCLASS)).thenReturn(pkObj);
        when(pkObj.getLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY)).thenReturn(ENCODED_PRIVATEKEY);

        when(query.<Object[]>execute()).thenReturn(Collections.singletonList(new Object[]{"space." + ENCODED_SUBJECTKEYID, 0}));

        CertifiedKeyPair keyPair = store.retrieve(SPACE_STORE_REF, certificate);
        assertThat(keyPair, notNullValue());
        assertThat(keyPair.getPrivateKey(), equalTo(privateKey));
        assertThat(keyPair.getCertificate(), equalTo((CertifiedPublicKey) certificate));
    }

    @Test
    public void retrieveEncryptedPrivateKeyFromSpace() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, ENCODED_SUBJECTKEYID), xcontext)).thenReturn(storeDoc);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509KeyWikiStore.PRIVATEKEYCLASS)).thenReturn(pkObj);
        when(pkObj.getLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY)).thenReturn(ENCODED_ENCRYPTED_PRIVATEKEY);

        when(query.<Object[]>execute()).thenReturn(Collections.singletonList(new Object[]{"space." + ENCODED_SUBJECTKEYID, 0}));

        CertifiedKeyPair keyPair = store.retrieve(SPACE_STORE_REF, certificate, PASSWORD);
        assertThat(keyPair, notNullValue());
        assertThat(keyPair.getPrivateKey(), equalTo(privateKey));
        assertThat(keyPair.getCertificate(), equalTo((CertifiedPublicKey) certificate));
    }

    @Test
    public void retrieveMissingPrivateKeyFromDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS)).thenReturn(certObj);
        when(certObj.getLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE)).thenReturn(ENCODED_CERTIFICATE);

        CertifiedKeyPair keyPair = store.retrieve(DOC_STORE_REF);
        assertThat(keyPair, nullValue());
    }

    @Test
    public void retrieveMissingCertificateFromDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), xcontext)).thenReturn(storeDoc);

        BaseObject pkObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509KeyWikiStore.PRIVATEKEYCLASS)).thenReturn(pkObj);
        when(pkObj.getLargeStringValue(X509KeyWikiStore.PRIVATEKEYCLASS_PROP_KEY)).thenReturn(ENCODED_ENCRYPTED_PRIVATEKEY);

        CertifiedKeyPair keyPair = store.retrieve(DOC_STORE_REF);
        assertThat(keyPair, nullValue());
    }

    @Test
    public void retrieveMissingPrivateKeyFromSpace() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, ENCODED_SUBJECTKEYID), xcontext)).thenReturn(storeDoc);

        when(query.<Object[]>execute()).thenReturn(Collections.singletonList(new Object[]{"space." + ENCODED_SUBJECTKEYID, 0}));

        CertifiedKeyPair keyPair = store.retrieve(SPACE_STORE_REF, certificate);
        assertThat(keyPair, nullValue());
    }

    @Test
    public void retrieveMissingCertificateFromSpace() throws Exception
    {
        CertifiedKeyPair keyPair = store.retrieve(SPACE_STORE_REF, certificate);
        assertThat(keyPair, nullValue());
    }

}
