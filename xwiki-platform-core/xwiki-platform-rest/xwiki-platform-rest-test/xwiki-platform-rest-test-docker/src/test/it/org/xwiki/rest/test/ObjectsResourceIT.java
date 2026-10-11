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
package org.xwiki.rest.test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.Relations;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.ObjectSummary;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.rest.resources.objects.AllObjectsForClassNameResource;
import org.xwiki.rest.resources.objects.ObjectAtPageVersionResource;
import org.xwiki.rest.resources.objects.ObjectResource;
import org.xwiki.rest.resources.objects.ObjectsResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.test.framework.AbstractHttpIT;
import org.xwiki.test.ui.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObjectsResourceIT extends AbstractHttpIT
{
    private String wikiName;

    private List<String> spaces;

    private String pageName;

    private DocumentReference reference;

    @BeforeEach
    @Override
    protected void setUp(TestUtils setup, TestInfo info) throws Exception
    {
        super.setUp(setup, info);

        this.wikiName = getWiki();
        this.spaces = Arrays.asList(getTestClassName());
        this.pageName = getTestMethodName();

        this.reference = new DocumentReference(this.wikiName, this.spaces, this.pageName);

        // Create a clean test page.
        getUtil().rest().delete(this.reference);
        getUtil().rest().savePage(this.reference);

        CloseableHttpResponse getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Page page = (Page) unmarshaller.unmarshal(getMethod.getEntity().getContent());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);

        /* Create a tag object if it doesn't exist yet */
        if (link == null) {
            Object object = objectFactory.createObject();
            object.setClassName("XWiki.TagClass");

            CloseableHttpResponse postMethod = executePostXml(
                buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName).toString(), object,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
            assertEquals(HttpStatus.SC_CREATED, postMethod.getCode(), getHttpResponseInfo(postMethod));
        }
    }

    @Override
    @Test
    protected void testRepresentation() throws Exception
    {
        CloseableHttpResponse getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Page page = (Page) unmarshaller.unmarshal(getMethod.getEntity().getContent());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        assertNotNull(link);

        getMethod = executeGet(link.getHref());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        assertFalse(objects.getObjectSummaries().isEmpty());

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
            getMethod = executeGet(link.getHref());
            assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

            Object object = (Object) unmarshaller.unmarshal(getMethod.getEntity().getContent());

            checkLinks(objectSummary);

            for (Property property : object.getProperties()) {
                checkLinks(property);
            }
        }
    }

    @Test
    void testGETNotExistingObject() throws Exception
    {
        CloseableHttpResponse getMethod = executeGet(
            buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, "NOTEXISTING", 0).toString());
        assertEquals(HttpStatus.SC_NOT_FOUND, getMethod.getCode(), getHttpResponseInfo(getMethod));
    }

    private Property getProperty(Object object, String propertyName)
    {
        for (Property property : object.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return property;
            }
        }

        return null;
    }

    @Test
    void testPOSTObject() throws Exception
    {
        final String TAG_VALUE = "TAG";

        Property property = new Property();
        property.setName("tags");
        property.setValue(TAG_VALUE);
        Object object = objectFactory.createObject();
        object.setClassName("XWiki.TagClass");
        object.getProperties().add(property);

        CloseableHttpResponse postMethod =
            executePostXml(buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName).toString(), object,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postMethod.getCode(), getHttpResponseInfo(postMethod));

        object = (Object) unmarshaller.unmarshal(postMethod.getEntity().getContent());

        assertEquals(TAG_VALUE, getProperty(object, "tags").getValue());

        CloseableHttpResponse getMethod =
            executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            object.getClassName(), object.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        object = (Object) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        assertEquals(TAG_VALUE, getProperty(object, "tags").getValue());
    }

    @Test
    void testPOSTInvalidObject() throws Exception
    {
        final String TAG_VALUE = "TAG";

        Property property = new Property();
        property.setName("tags");
        property.setValue(TAG_VALUE);
        Object object = objectFactory.createObject();
        object.getProperties().add(property);

        CloseableHttpResponse postMethod =
            executePostXml(buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName).toString(), object,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_BAD_REQUEST, postMethod.getCode(), getHttpResponseInfo(postMethod));
    }

    @Test
    void testPOSTObjectNotAuthorized() throws Exception
    {
        final String TAG_VALUE = "TAG";

        Property property = new Property();
        property.setName("tags");
        property.setValue(TAG_VALUE);
        Object object = objectFactory.createObject();
        object.setClassName("XWiki.TagClass");
        object.getProperties().add(property);

        CloseableHttpResponse postMethod =
            executePostXml(buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName).toString(), object);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, postMethod.getCode(), getHttpResponseInfo(postMethod));
    }

    @Test
    void testPUTObject() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        Object objectToBePut = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse getMethod =
            executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Object objectSummary = (Object) unmarshaller.unmarshal(getMethod.getEntity().getContent());
        getProperty(objectSummary, "tags").setValue(TAG_VALUE);

        CloseableHttpResponse putMethod = executePutXml(
            buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, objectToBePut.getClassName(),
                objectToBePut.getNumber()).toString(),
            objectSummary, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getCode(), getHttpResponseInfo(putMethod));

        Object updatedObjectSummary = (Object) unmarshaller.unmarshal(putMethod.getEntity().getContent());

        assertEquals(TAG_VALUE, getProperty(updatedObjectSummary, "tags").getValue());
        assertEquals(objectSummary.getClassName(), updatedObjectSummary.getClassName());
        assertEquals(objectSummary.getNumber(), updatedObjectSummary.getNumber());
    }

    @Test
    void testPUTObjectUnauthorized() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        Object objectToBePut = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse getMethod =
            executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Object object = (Object) unmarshaller.unmarshal(getMethod.getEntity().getContent());
        String originalTagValue = getProperty(object, "tags").getValue();
        getProperty(object, "tags").setValue(TAG_VALUE);

        CloseableHttpResponse putMethod =
            executePutXml(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()).toString(), object);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, putMethod.getCode(), getHttpResponseInfo(putMethod));

        getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        object = (Object) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        assertEquals(originalTagValue, getProperty(object, "tags").getValue());
    }

    @Test
    void testDELETEObject() throws Exception
    {
        Object objectToBeDeleted = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse deleteMethod = executeDelete(
            buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, objectToBeDeleted.getClassName(),
                objectToBeDeleted.getNumber()).toString(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_NO_CONTENT, deleteMethod.getCode(), getHttpResponseInfo(deleteMethod));

        CloseableHttpResponse getMethod =
            executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBeDeleted.getClassName(), objectToBeDeleted.getNumber()).toString());
        assertEquals(HttpStatus.SC_NOT_FOUND, getMethod.getCode(), getHttpResponseInfo(getMethod));
    }

    @Test
    void testDELETEObjectUnAuthorized() throws Exception
    {
        Object objectToBeDeleted = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse deleteMethod =
            executeDelete(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBeDeleted.getClassName(), objectToBeDeleted.getNumber()).toString());
        assertEquals(HttpStatus.SC_UNAUTHORIZED, deleteMethod.getCode(), getHttpResponseInfo(deleteMethod));

        CloseableHttpResponse getMethod =
            executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBeDeleted.getClassName(), objectToBeDeleted.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));
    }

    @Test
    void testPUTProperty() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        /* Make sure that an Object with the TagClass exists. */
        createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Page page = (Page) unmarshaller.unmarshal(getMethod.getEntity().getContent());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        assertNotNull(link);

        getMethod = executeGet(link.getHref());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        assertFalse(objects.getObjectSummaries().isEmpty());

        Object currentObject = null;

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals("XWiki.TagClass")) {
                link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                assertNotNull(link);
                getMethod = executeGet(link.getHref());
                assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

                currentObject = (Object) unmarshaller.unmarshal(getMethod.getEntity().getContent());
                break;
            }
        }

        assertNotNull(currentObject);

        Property tagsProperty = getProperty(currentObject, "tags");

        assertNotNull(tagsProperty);

        Link tagsPropertyLink = getFirstLinkByRelation(tagsProperty, Relations.SELF);

        assertNotNull(tagsPropertyLink);

        Property newTags = objectFactory.createProperty();
        newTags.setValue(TAG_VALUE);

        CloseableHttpResponse putMethod = executePutXml(tagsPropertyLink.getHref(), newTags,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getCode(), getHttpResponseInfo(putMethod));

        getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            currentObject.getClassName(), currentObject.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode());

        currentObject = (Object) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        tagsProperty = getProperty(currentObject, "tags");

        assertEquals(TAG_VALUE, tagsProperty.getValue());
    }

    @Test
    void testPUTPropertyWithTextPlain() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        /* Make sure that an Object with the TagClass exists. */
        createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Page page = (Page) unmarshaller.unmarshal(getMethod.getEntity().getContent());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        assertNotNull(link);

        getMethod = executeGet(link.getHref());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        assertFalse(objects.getObjectSummaries().isEmpty());

        Object currentObject = null;

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals("XWiki.TagClass")) {
                link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                assertNotNull(link);
                getMethod = executeGet(link.getHref());
                assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

                currentObject = (Object) unmarshaller.unmarshal(getMethod.getEntity().getContent());
                break;
            }
        }

        assertNotNull(currentObject);

        Property tagsProperty = getProperty(currentObject, "tags");

        assertNotNull(tagsProperty);

        Link tagsPropertyLink = getFirstLinkByRelation(tagsProperty, Relations.SELF);

        assertNotNull(tagsPropertyLink);

        CloseableHttpResponse putMethod = executePut(tagsPropertyLink.getHref(), TAG_VALUE, MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getCode(), getHttpResponseInfo(putMethod));

        getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            currentObject.getClassName(), currentObject.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode());

        currentObject = (Object) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        tagsProperty = getProperty(currentObject, "tags");

        assertEquals(TAG_VALUE, tagsProperty.getValue());
    }

    private Object createObjectIfDoesNotExists(String className, List<String> spaces, String pageName) throws Exception
    {
        createPageIfDoesntExist(spaces, pageName, "");

        CloseableHttpResponse getMethod =
            executeGet(buildURI(ObjectsResource.class, getWiki(), spaces, pageName).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals(className)) {
                Link link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                assertNotNull(link);
                getMethod = executeGet(link.getHref());
                assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

                Object object = (Object) unmarshaller.unmarshal(getMethod.getEntity().getContent());

                return object;
            }
        }

        /* If no object of that class is found, then create a new one */
        Object object = objectFactory.createObject();
        object.setClassName(className);

        CloseableHttpResponse postMethod =
            executePostXml(buildURI(ObjectsResource.class, getWiki(), spaces, pageName).toString(),
            object, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postMethod.getCode(), getHttpResponseInfo(postMethod));

        object = (Object) unmarshaller.unmarshal(postMethod.getEntity().getContent());

        return object;
    }

    @Test
    void testPUTObjectFormUrlEncoded() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        Object objectToBePut = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse getMethod =
            executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Object object = (Object) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        NameValuePair[] nameValuePairs = new NameValuePair[1];
        nameValuePairs[0] = new BasicNameValuePair("property#tags", TAG_VALUE);

        CloseableHttpResponse postMethod = executePostForm(
            String.format("%s?method=PUT",
                buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, objectToBePut.getClassName(),
                    objectToBePut.getNumber()).toString()),
            nameValuePairs, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());

        assertEquals(HttpStatus.SC_ACCEPTED, postMethod.getCode(), getHttpResponseInfo(postMethod));

        Object updatedObjectSummary = (Object) unmarshaller.unmarshal(postMethod.getEntity().getContent());

        assertEquals(TAG_VALUE, getProperty(updatedObjectSummary, "tags").getValue());
        assertEquals(object.getClassName(), updatedObjectSummary.getClassName());
        assertEquals(object.getNumber(), updatedObjectSummary.getNumber());
    }

    @Test
    void testPOSTObjectFormUrlEncoded() throws Exception
    {
        final String TAG_VALUE = "TAG";

        NameValuePair[] nameValuePairs = new NameValuePair[2];
        nameValuePairs[0] = new BasicNameValuePair("className", "XWiki.TagClass");
        nameValuePairs[1] = new BasicNameValuePair("property#tags", TAG_VALUE);

        CloseableHttpResponse postMethod = executePostForm(
            buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName).toString(), nameValuePairs,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postMethod.getCode(), getHttpResponseInfo(postMethod));

        Object object = (Object) unmarshaller.unmarshal(postMethod.getEntity().getContent());

        assertEquals(TAG_VALUE, getProperty(object, "tags").getValue());

        CloseableHttpResponse getMethod =
            executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            object.getClassName(), object.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        object = (Object) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        assertEquals(TAG_VALUE, getProperty(object, "tags").getValue());
    }

    @Test
    void testPOSTObjectFormUrlEncodedNoCSRF() throws Exception
    {
        final String tagValue = "TAG";
        NameValuePair[] nameValuePairs = new NameValuePair[2];
        String className = "XWiki.TagClass";
        nameValuePairs[0] = new BasicNameValuePair("className", className);
        nameValuePairs[1] = new BasicNameValuePair("property#tags", tagValue);

        String objectGetURI = buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName, className);

        // Count objects before to ensure nothing is added on the failed request.
        CloseableHttpResponse getMethod = executeGet(objectGetURI);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));
        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getEntity().getContent());
        int numObjects = objects.getObjectSummaries().size();

        CloseableHttpResponse postMethod = executePostForm(
            buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName), nameValuePairs,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword(), null);
        assertEquals(HttpStatus.SC_FORBIDDEN, postMethod.getCode(), getHttpResponseInfo(postMethod));
        assertEquals("Invalid or missing form token.", EntityUtils.toString(postMethod.getEntity()));

        getMethod = executeGet(objectGetURI);
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        objects = (Objects) unmarshaller.unmarshal(getMethod.getEntity().getContent());
        assertEquals(numObjects, objects.getObjectSummaries().size());
    }


    @Test
    void testPUTPropertyFormUrlEncoded() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        /* Make sure that an Object with the TagClass exists. */
        createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Page page = (Page) unmarshaller.unmarshal(getMethod.getEntity().getContent());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        assertNotNull(link);

        getMethod = executeGet(link.getHref());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        assertFalse(objects.getObjectSummaries().isEmpty());

        Object currentObject = null;

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals("XWiki.TagClass")) {
                link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                assertNotNull(link);
                getMethod = executeGet(link.getHref());
                assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

                currentObject = (Object) unmarshaller.unmarshal(getMethod.getEntity().getContent());
                break;
            }
        }

        assertNotNull(currentObject);

        Property tagsProperty = getProperty(currentObject, "tags");

        assertNotNull(tagsProperty);

        Link tagsPropertyLink = getFirstLinkByRelation(tagsProperty, Relations.SELF);

        assertNotNull(tagsPropertyLink);

        NameValuePair[] nameValuePairs = new NameValuePair[1];
        nameValuePairs[0] = new BasicNameValuePair("property#tags", TAG_VALUE);

        CloseableHttpResponse postMethod =
            executePostForm(String.format("%s?method=PUT", tagsPropertyLink.getHref()), nameValuePairs,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, postMethod.getCode(), getHttpResponseInfo(postMethod));

        getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            currentObject.getClassName(), currentObject.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getCode());

        currentObject = (Object) unmarshaller.unmarshal(getMethod.getEntity().getContent());

        tagsProperty = getProperty(currentObject, "tags");

        assertEquals(TAG_VALUE, tagsProperty.getValue());
    }

    @Test
    void testGETObjectAtPageVersion() throws Exception
    {
        Object objectToBePut = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        Map<String, String> versionToValueMap = new HashMap<String, String>();
        for (int i = 0; i < 5; i++) {
            String value = String.format("Value%d", i);

            Property property = getProperty(objectToBePut, "tags");
            property.setValue(value);

            CloseableHttpResponse putMethod = executePutXml(
                buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, objectToBePut.getClassName(),
                    objectToBePut.getNumber()).toString(),
                objectToBePut, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
                TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
            assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getCode(), getHttpResponseInfo(putMethod));

            CloseableHttpResponse getMethod =
                executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
            assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

            Page page = (Page) unmarshaller.unmarshal(getMethod.getEntity().getContent());

            versionToValueMap.put(page.getVersion(), value);
        }

        for (String version : versionToValueMap.keySet()) {
            CloseableHttpResponse getMethod =
                executeGet(buildURI(ObjectAtPageVersionResource.class, getWiki(), this.spaces,
                this.pageName, version, objectToBePut.getClassName(), objectToBePut.getNumber()).toString());
            assertEquals(HttpStatus.SC_OK, getMethod.getCode(), getHttpResponseInfo(getMethod));

            Object currentObject = (Object) unmarshaller.unmarshal(getMethod.getEntity().getContent());

            Property property = getProperty(currentObject, "tags");

            assertEquals(versionToValueMap.get(version), property.getValue());

            checkLinks(currentObject);
            for (Property p : currentObject.getProperties()) {
                checkLinks(p);
            }
        }
    }

    @Test
    void testAllObjectsForClassNameResourcePaginationAndErrors() throws Exception
    {
        // Setup: Create two pages with TagClass objects
        String className = "XWiki.TagClass";
        List<String> spaces1 = List.of(getTestClassName() + "A");
        List<String> spaces2 = List.of(getTestClassName() + "B");
        String pageName1 = getTestMethodName() + "1";
        String pageName2 = getTestMethodName() + "2";
        DocumentReference ref1 = new DocumentReference(getWiki(), spaces1, pageName1);
        DocumentReference ref2 = new DocumentReference(getWiki(), spaces2, pageName2);

        try {
            getUtil().rest().delete(ref1);
            getUtil().rest().delete(ref2);
            getUtil().rest().savePage(ref1);
            getUtil().rest().savePage(ref2);

            // Add TagClass objects to both pages
            createObjectIfDoesNotExists(className, spaces1, pageName1);
            createObjectIfDoesNotExists(className, spaces2, pageName2);

            // Test: basic retrieval
            CloseableHttpResponse getMethod = executeGet(
                buildURI(AllObjectsForClassNameResource.class, getWiki(), className));
            assertEquals(HttpStatus.SC_OK, getMethod.getCode());
            Objects objects = (Objects) this.unmarshaller.unmarshal(getMethod.getEntity().getContent());
            assertTrue(objects.getObjectSummaries().size() >= 2);

            // Test: pagination with number=2
            getMethod = executeGet(
                buildURI(AllObjectsForClassNameResource.class, getWiki(), className) + "?number=2");
            assertEquals(HttpStatus.SC_OK, getMethod.getCode());
            objects = (Objects) this.unmarshaller.unmarshal(getMethod.getEntity().getContent());
            assertEquals(2, objects.getObjectSummaries().size());

            String secondPage = objects.getObjectSummaries().get(1).getPageName();

            // Test: pagination with number=1 and start=1
            getMethod = executeGet(
                buildURI(AllObjectsForClassNameResource.class, getWiki(), className) + "?number=1&start=1");
            assertEquals(HttpStatus.SC_OK, getMethod.getCode());
            objects = (Objects) this.unmarshaller.unmarshal(getMethod.getEntity().getContent());
            assertEquals(1, objects.getObjectSummaries().size());
            assertEquals(secondPage, objects.getObjectSummaries().get(0).getPageName());

            // Test: error for number=-1
            getMethod = executeGet(
                buildURI(AllObjectsForClassNameResource.class, getWiki(), className) + "?number=-1");
            assertEquals(400, getMethod.getCode());
            assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(getMethod.getEntity()));

            // Test: error for number=1001
            getMethod = executeGet(
                buildURI(AllObjectsForClassNameResource.class, getWiki(), className) + "?number=1001");
            assertEquals(400, getMethod.getCode());
            assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(getMethod.getEntity()));
        } finally {
            getUtil().rest().delete(ref1);
            getUtil().rest().delete(ref2);
        }
    }
}
