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

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.pkix.CertificateFactory;
import org.xwiki.crypto.pkix.params.CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.DistinguishedName;
import org.xwiki.crypto.pkix.params.x509certificate.X509CertifiedPublicKey;
import org.xwiki.crypto.pkix.params.x509certificate.extension.X509Extensions;
import org.xwiki.crypto.store.CertificateStore;
import org.xwiki.crypto.store.StoreReference;
import org.xwiki.crypto.store.WikiStoreReference;
import org.xwiki.crypto.store.wiki.internal.query.AbstractX509IssuerAndSerialQuery;
import org.xwiki.crypto.store.wiki.internal.query.AbstractX509KeyIdentifierQuery;
import org.xwiki.crypto.store.wiki.internal.query.AbstractX509StoreQuery;
import org.xwiki.crypto.store.wiki.internal.query.AbstractX509SubjectQuery;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
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
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link X509CertificateWikiStore}.
 *
 * @version $Id$
 * @since 6.0
 */
@ComponentList({
    CurrentReferenceDocumentReferenceResolver.class,
    CurrentReferenceEntityReferenceResolver.class,
    CurrentStringEntityReferenceResolver.class,
    LocalStringEntityReferenceSerializer.class,
    DefaultSymbolScheme.class
})
public class X509CertificateWikiStoreTest
{
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
    private static final String FULLNAME = SPACE + '.' + DOCUMENT;

    private static final WikiReference WIKI_REFERENCE = new WikiReference(WIKI);
    private static final EntityReference SPACE_REFERENCE = new EntityReference(SPACE, EntityType.WIKI);
    private static final EntityReference DOCUMENT_REFERENCE = new EntityReference(DOCUMENT, EntityType.DOCUMENT);

    private static final LocalDocumentReference DOC_STORE_ENTREF = new LocalDocumentReference("space", DOCUMENT);
    private static final EntityReference SPACE_STORE_ENTREF = new EntityReference(SPACE, EntityType.SPACE);

    private static final StoreReference DOC_STORE_REF = new WikiStoreReference(DOC_STORE_ENTREF);
    private static final StoreReference SPACE_STORE_REF = new WikiStoreReference(SPACE_STORE_ENTREF);

    protected static final String BIND_KEYID = getFieldValue(AbstractX509KeyIdentifierQuery.class, "KEYID");
    protected static final String BIND_ISSUER = getFieldValue(AbstractX509IssuerAndSerialQuery.class, "ISSUER");
    protected static final String BIND_SERIAL = getFieldValue(AbstractX509IssuerAndSerialQuery.class, "SERIAL");
    protected static final String BIND_SUBJECT = getFieldValue(AbstractX509SubjectQuery.class, "SUBJECT");

    private static final String BIND_STORE = getFieldValue(AbstractX509StoreQuery.class, "STORE");

    @Rule
    public MockitoComponentMockingRule<CertificateStore> mocker =
        new MockitoComponentMockingRule<CertificateStore>(X509CertificateWikiStore.class);

    private XWikiContext xcontext;
    private XWiki xwiki;

    private Query query;

    private CertificateStore store;

    private static String getFieldValue(Class<?> clazz, String fieldName)
    {
        try {
            Field field = ReflectionUtils.getField(clazz, fieldName);
            boolean isAccessible = field.isAccessible();
            try {
                field.setAccessible(true);
                return (String) field.get(field);
            } finally {
                field.setAccessible(isAccessible);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Before
    public void setUp() throws Exception
    {
        this.mocker.registerMockComponent(EntityReferenceProvider.class, "default");

        EntityReferenceProvider valueProvider = this.mocker.registerMockComponent(EntityReferenceProvider.class, "current");
        when(valueProvider.getDefaultReference(EntityType.WIKI)).thenReturn(WIKI_REFERENCE);
        when(valueProvider.getDefaultReference(EntityType.SPACE)).thenReturn(SPACE_REFERENCE);
        when(valueProvider.getDefaultReference(EntityType.DOCUMENT)).thenReturn(DOCUMENT_REFERENCE);

        Provider<XWikiContext> xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        BinaryStringEncoder encoder = mocker.getInstance(BinaryStringEncoder.class, "Base64");
        when(encoder.encode(CERTIFICATE, 64)).thenReturn(ENCODED_CERTIFICATE);
        when(encoder.decode(ENCODED_CERTIFICATE)).thenReturn(CERTIFICATE);
        when(encoder.encode(SUBJECT_KEYID)).thenReturn(ENCODED_SUBJECTKEYID);
        when(encoder.decode(ENCODED_SUBJECTKEYID)).thenReturn(SUBJECT_KEYID);

        QueryManager queryManager = mocker.getInstance(QueryManager.class);
        query = mock(Query.class);
        when(query.bindValue(any(String.class), any())).thenReturn(query);
        when(query.setWiki(WIKI)).thenReturn(query);
        when(queryManager.createQuery(any(String.class), any(String.class))).thenReturn(query);

        store = mocker.getComponentUnderTest();
    }

    private CertifiedPublicKey getMockedCertificate(boolean hasKeyId) throws Exception
    {
        X509CertifiedPublicKey certificate = mock(X509CertifiedPublicKey.class);

        when(certificate.getSerialNumber()).thenReturn(SERIAL);
        when(certificate.getIssuer()).thenReturn(new DistinguishedName(ISSUER));
        when(certificate.getSubject()).thenReturn(new DistinguishedName(SUBJECT));
        when(certificate.getEncoded()).thenReturn(CERTIFICATE);

        if (hasKeyId) {
            X509Extensions extensions = mock(X509Extensions.class);
            when(certificate.getExtensions()).thenReturn(extensions);
            when(extensions.getSubjectKeyIdentifier()).thenReturn(SUBJECT_KEYID);
            when(certificate.getSubjectKeyIdentifier()).thenReturn(SUBJECT_KEYID);
        }

        return certificate;
    }

    @Test
    public void testStoringNewCertificateWithKeyIdToDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509CertificateWikiStore.CERTIFICATECLASS, xcontext)).thenReturn(certObj);

        store.store(DOC_STORE_REF, getMockedCertificate(true));

        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_KEYID, ENCODED_SUBJECTKEYID);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_ISSUER, ISSUER);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SERIAL, SERIAL.toString());
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SUBJECT, SUBJECT);
        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);

        verify(xwiki).saveDocument(storeDoc, xcontext);
    }

    @Test
    public void testStoringNewCertificateWithKeyIdToSpace() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, ENCODED_SUBJECTKEYID), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509CertificateWikiStore.CERTIFICATECLASS, xcontext)).thenReturn(certObj);

        store.store(SPACE_STORE_REF, getMockedCertificate(true));

        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_KEYID, ENCODED_SUBJECTKEYID);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_ISSUER, ISSUER);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SERIAL, SERIAL.toString());
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SUBJECT, SUBJECT);
        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);

        verify(xwiki).saveDocument(storeDoc, xcontext);
    }

    @Test
    public void testStoringNewCertificateWithoutKeyIdToDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509CertificateWikiStore.CERTIFICATECLASS, xcontext)).thenReturn(certObj);

        store.store(DOC_STORE_REF, getMockedCertificate(false));

        verify(certObj, never()).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_KEYID, ENCODED_SUBJECTKEYID);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_ISSUER, ISSUER);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SERIAL, SERIAL.toString());
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SUBJECT, SUBJECT);
        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);

        verify(xwiki).saveDocument(storeDoc, xcontext);
    }

    @Test
    public void testStoringNewCertificateWithoutKeyIdToSpace() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, SERIAL.toString() + ", " + ISSUER), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.newXObject(X509CertificateWikiStore.CERTIFICATECLASS, xcontext)).thenReturn(certObj);

        store.store(SPACE_STORE_REF, getMockedCertificate(false));

        verify(certObj, never()).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_KEYID, ENCODED_SUBJECTKEYID);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_ISSUER, ISSUER);
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SERIAL, SERIAL.toString());
        verify(certObj).setStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SUBJECT, SUBJECT);
        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);

        verify(xwiki).saveDocument(storeDoc, xcontext);
    }

    @Test
    public void testUpdatingCertificateWithKeyIdToDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS, 123)).thenReturn(certObj);

        when(query.<Object[]>execute()).thenReturn(Collections.singletonList(new Object[]{"space.document", 123}));

        store.store(DOC_STORE_REF, getMockedCertificate(true));

        verify(query).bindValue(BIND_KEYID, ENCODED_SUBJECTKEYID);
        verify(query).bindValue(BIND_STORE, FULLNAME);

        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_KEYID), any(String.class));
        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_ISSUER), any(String.class));
        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SERIAL), any(String.class));
        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SUBJECT), any(String.class));
        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);

        verify(xwiki).saveDocument(storeDoc, xcontext);
    }

    @Test
    public void testUpdatingCertificateWithKeyIdToSpace() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, ENCODED_SUBJECTKEYID), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS, 0)).thenReturn(certObj);

        when(query.<Object[]>execute()).thenReturn(Collections.singletonList(new Object[]{"space." + ENCODED_SUBJECTKEYID, 0}));

        store.store(SPACE_STORE_REF, getMockedCertificate(true));

        verify(query).bindValue(BIND_KEYID, ENCODED_SUBJECTKEYID);
        verify(query).bindValue(BIND_STORE, SPACE);

        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_KEYID), any(String.class));
        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_ISSUER), any(String.class));
        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SERIAL), any(String.class));
        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SUBJECT), any(String.class));
        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);

        verify(xwiki).saveDocument(storeDoc, xcontext);
    }

    @Test
    public void testUpdatingCertificateWithoutKeyIdToDocument() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, DOCUMENT), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS, 123)).thenReturn(certObj);

        when(query.<Object[]>execute()).thenReturn(Collections.singletonList(new Object[]{"space.document", 123}));

        store.store(DOC_STORE_REF, getMockedCertificate(false));

        verify(query).bindValue(BIND_ISSUER, ISSUER);
        verify(query).bindValue(BIND_SERIAL, SERIAL.toString());
        verify(query).bindValue(BIND_STORE, FULLNAME);

        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_KEYID), any(String.class));
        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_ISSUER), any(String.class));
        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SERIAL), any(String.class));
        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SUBJECT), any(String.class));
        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);

        verify(xwiki).saveDocument(storeDoc, xcontext);
    }

    @Test
    public void testUpdatingCertificateWithoutKeyIdToSpace() throws Exception
    {
        XWikiDocument storeDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(new DocumentReference(WIKI, SPACE, SERIAL.toString() + ", " + ISSUER), xcontext)).thenReturn(storeDoc);

        BaseObject certObj = mock(BaseObject.class);
        when(storeDoc.getXObject(X509CertificateWikiStore.CERTIFICATECLASS, 0)).thenReturn(certObj);

        when(query.<Object[]>execute()).thenReturn(
            Collections.singletonList(new Object[]{"space." + SERIAL.toString() + ", " + ISSUER, 0}));

        store.store(SPACE_STORE_REF, getMockedCertificate(false));

        verify(query).bindValue(BIND_ISSUER, ISSUER);
        verify(query).bindValue(BIND_SERIAL, SERIAL.toString());
        verify(query).bindValue(BIND_STORE, SPACE);

        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_KEYID), any(String.class));
        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_ISSUER), any(String.class));
        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SERIAL), any(String.class));
        verify(certObj, never()).setStringValue(eq(X509CertificateWikiStore.CERTIFICATECLASS_PROP_SUBJECT), any(String.class));
        verify(certObj).setLargeStringValue(X509CertificateWikiStore.CERTIFICATECLASS_PROP_CERTIFICATE, ENCODED_CERTIFICATE);

        verify(xwiki).saveDocument(storeDoc, xcontext);
    }

    private CertifiedPublicKey mockSingleCertQuery() throws Exception
    {
        CertifiedPublicKey certificate = getMockedCertificate(true);
        CertificateFactory factory = mocker.getInstance(CertificateFactory.class, "X509");
        when(factory.decode(CERTIFICATE)).thenReturn(certificate);

        when(query.<String>execute()).thenReturn(Collections.singletonList(ENCODED_CERTIFICATE));
        return certificate;
    }

    @Test
    public void testRetrievingCertificateUsingKeyIdFromDocument() throws Exception
    {
        CertifiedPublicKey certificate = mockSingleCertQuery();

        assertThat(this.store.getCertificateProvider(DOC_STORE_REF).getCertificate(SUBJECT_KEYID),
            equalTo(certificate));

        verify(this.query).bindValue(BIND_KEYID, ENCODED_SUBJECTKEYID);
        verify(this.query, times(3)).bindValue(BIND_STORE, FULLNAME);
    }

    @Test
    public void testRetrievingCertificateUsingKeyIdFromSpace() throws Exception
    {
        CertifiedPublicKey certificate = mockSingleCertQuery();

        assertThat(this.store.getCertificateProvider(SPACE_STORE_REF).getCertificate(SUBJECT_KEYID),
            equalTo(certificate));

        verify(this.query).bindValue(BIND_KEYID, ENCODED_SUBJECTKEYID);
        verify(this.query, times(3)).bindValue(BIND_STORE, SPACE);
    }

    @Test
    public void testRetrievingCertificateUsingIssueAndSerialFromDocument() throws Exception
    {
        CertifiedPublicKey certificate = mockSingleCertQuery();

        assertThat(store.getCertificateProvider(DOC_STORE_REF).getCertificate(new DistinguishedName(ISSUER), SERIAL),
            equalTo(certificate));

        verify(query).bindValue(BIND_ISSUER, ISSUER);
        verify(query).bindValue(BIND_SERIAL, SERIAL.toString());
        verify(query, times(3)).bindValue(BIND_STORE, FULLNAME);
    }

    @Test
    public void testRetrievingCertificateUsingIssueAndSerialFromSpace() throws Exception
    {
        CertifiedPublicKey certificate = mockSingleCertQuery();

        assertThat(store.getCertificateProvider(SPACE_STORE_REF).getCertificate(new DistinguishedName(ISSUER), SERIAL),
            equalTo(certificate));

        verify(query).bindValue(BIND_ISSUER, ISSUER);
        verify(query).bindValue(BIND_SERIAL, SERIAL.toString());
        verify(query, times(3)).bindValue(BIND_STORE, SPACE);
    }

    private CertifiedPublicKey[] mockMultiCertsQuery() throws Exception
    {
        CertifiedPublicKey[] certs = new CertifiedPublicKey[2];
        byte[] cert2 = "certificate2".getBytes();
        String encodedCert2 = "encoded_certificate2";
        certs[0] = getMockedCertificate(true);
        certs[1] = getMockedCertificate(false);
        CertificateFactory factory = this.mocker.getInstance(CertificateFactory.class, "X509");
        when(factory.decode(CERTIFICATE)).thenReturn(certs[0]);
        when(factory.decode(cert2)).thenReturn(certs[1]);

        BinaryStringEncoder encoder = this.mocker.getInstance(BinaryStringEncoder.class, "Base64");
        when(encoder.encode(cert2, 64)).thenReturn(encodedCert2);
        when(encoder.decode(encodedCert2)).thenReturn(cert2);

        when(this.query.<String>execute()).thenReturn(Arrays.asList(ENCODED_CERTIFICATE, encodedCert2));

        return certs;
    }

    @Test
    public void testRetrievingCertificatesUsingSubjectFromDocument() throws Exception
    {
        CertifiedPublicKey[] certs = mockMultiCertsQuery();

        assertThat(this.store.getCertificateProvider(DOC_STORE_REF).getCertificate(new DistinguishedName(SUBJECT)),
            contains(certs));

        verify(this.query).bindValue(BIND_SUBJECT, SUBJECT);
        verify(this.query, times(3)).bindValue(BIND_STORE, FULLNAME);
    }

    @Test
    public void testRetrievingCertificatesUsingSubjectFromSpace() throws Exception
    {
        CertifiedPublicKey[] certs = mockMultiCertsQuery();

        assertThat(this.store.getCertificateProvider(SPACE_STORE_REF).getCertificate(new DistinguishedName(SUBJECT)),
            contains(certs));

        verify(this.query).bindValue(BIND_SUBJECT, SUBJECT);
        verify(this.query, times(3)).bindValue(BIND_STORE, SPACE);
    }

    @Test
    public void testRetrievingAllCertificatesFromDocument() throws Exception
    {
        CertifiedPublicKey[] certs = mockMultiCertsQuery();

        assertThat(this.store.getAllCertificates(DOC_STORE_REF), contains(certs));

        verify(this.query).bindValue(BIND_STORE, FULLNAME);
    }

    @Test
    public void testRetrievingAllCertificatesFromSpace() throws Exception
    {
        CertifiedPublicKey[] certs = mockMultiCertsQuery();

        assertThat(this.store.getAllCertificates(SPACE_STORE_REF), contains(certs));

        verify(this.query).bindValue(BIND_STORE, SPACE);
    }

}
