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

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

        GetMethod getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);

        /* Create a tag object if it doesn't exist yet */
        if (link == null) {
            Object object = objectFactory.createObject();
            object.setClassName("XWiki.TagClass");

            PostMethod postMethod = executePostXml(
                buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName).toString(), object,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
            assertEquals(HttpStatus.SC_CREATED, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));
        }
    }

    @Override
    @Test
    protected void testRepresentation() throws Exception
    {
        GetMethod getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        assertNotNull(link);

        getMethod = executeGet(link.getHref());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertFalse(objects.getObjectSummaries().isEmpty());

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
            getMethod = executeGet(link.getHref());
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

            Object object = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

            checkLinks(objectSummary);

            for (Property property : object.getProperties()) {
                checkLinks(property);
            }
        }
    }

    @Test
    void testGETNotExistingObject() throws Exception
    {
        GetMethod getMethod = executeGet(
            buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, "NOTEXISTING", 0).toString());
        assertEquals(HttpStatus.SC_NOT_FOUND, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
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

        PostMethod postMethod =
            executePostXml(buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName).toString(), object,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));

        object = (Object) unmarshaller.unmarshal(postMethod.getResponseBodyAsStream());

        assertEquals(TAG_VALUE, getProperty(object, "tags").getValue());

        GetMethod getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            object.getClassName(), object.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        object = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

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

        PostMethod postMethod =
            executePostXml(buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName).toString(), object,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_BAD_REQUEST, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));
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

        PostMethod postMethod =
            executePostXml(buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName).toString(), object);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));
    }

    @Test
    void testPUTObject() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        Object objectToBePut = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        GetMethod getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Object objectSummary = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        getProperty(objectSummary, "tags").setValue(TAG_VALUE);

        PutMethod putMethod = executePutXml(
            buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, objectToBePut.getClassName(),
                objectToBePut.getNumber()).toString(),
            objectSummary, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));

        Object updatedObjectSummary = (Object) unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        assertEquals(TAG_VALUE, getProperty(updatedObjectSummary, "tags").getValue());
        assertEquals(objectSummary.getClassName(), updatedObjectSummary.getClassName());
        assertEquals(objectSummary.getNumber(), updatedObjectSummary.getNumber());
    }

    @Test
    void testPUTObjectUnauthorized() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        Object objectToBePut = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        GetMethod getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Object object = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        String originalTagValue = getProperty(object, "tags").getValue();
        getProperty(object, "tags").setValue(TAG_VALUE);

        PutMethod putMethod = executePutXml(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()).toString(), object);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));

        getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        object = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals(originalTagValue, getProperty(object, "tags").getValue());
    }

    @Test
    void testDELETEObject() throws Exception
    {
        Object objectToBeDeleted = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        DeleteMethod deleteMethod = executeDelete(
            buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, objectToBeDeleted.getClassName(),
                objectToBeDeleted.getNumber()).toString(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_NO_CONTENT, deleteMethod.getStatusCode(), getHttpMethodInfo(deleteMethod));

        GetMethod getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBeDeleted.getClassName(), objectToBeDeleted.getNumber()).toString());
        assertEquals(HttpStatus.SC_NOT_FOUND, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
    }

    @Test
    void testDELETEObjectUnAuthorized() throws Exception
    {
        Object objectToBeDeleted = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        DeleteMethod deleteMethod = executeDelete(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBeDeleted.getClassName(), objectToBeDeleted.getNumber()).toString());
        assertEquals(HttpStatus.SC_UNAUTHORIZED, deleteMethod.getStatusCode(), getHttpMethodInfo(deleteMethod));

        GetMethod getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBeDeleted.getClassName(), objectToBeDeleted.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
    }

    @Test
    void testPUTProperty() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        /* Make sure that an Object with the TagClass exists. */
        createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        GetMethod getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        assertNotNull(link);

        getMethod = executeGet(link.getHref());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertFalse(objects.getObjectSummaries().isEmpty());

        Object currentObject = null;

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals("XWiki.TagClass")) {
                link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                assertNotNull(link);
                getMethod = executeGet(link.getHref());
                assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

                currentObject = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
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

        PutMethod putMethod = executePutXml(tagsPropertyLink.getHref(), newTags,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));

        getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            currentObject.getClassName(), currentObject.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());

        currentObject = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        tagsProperty = getProperty(currentObject, "tags");

        assertEquals(TAG_VALUE, tagsProperty.getValue());
    }

    @Test
    void testPUTPropertyWithTextPlain() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        /* Make sure that an Object with the TagClass exists. */
        createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        GetMethod getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        assertNotNull(link);

        getMethod = executeGet(link.getHref());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertFalse(objects.getObjectSummaries().isEmpty());

        Object currentObject = null;

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals("XWiki.TagClass")) {
                link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                assertNotNull(link);
                getMethod = executeGet(link.getHref());
                assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

                currentObject = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
                break;
            }
        }

        assertNotNull(currentObject);

        Property tagsProperty = getProperty(currentObject, "tags");

        assertNotNull(tagsProperty);

        Link tagsPropertyLink = getFirstLinkByRelation(tagsProperty, Relations.SELF);

        assertNotNull(tagsPropertyLink);

        PutMethod putMethod = executePut(tagsPropertyLink.getHref(), TAG_VALUE, MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));

        getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            currentObject.getClassName(), currentObject.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());

        currentObject = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        tagsProperty = getProperty(currentObject, "tags");

        assertEquals(TAG_VALUE, tagsProperty.getValue());
    }

    private Object createObjectIfDoesNotExists(String className, List<String> spaces, String pageName) throws Exception
    {
        createPageIfDoesntExist(spaces, pageName, "");

        GetMethod getMethod = executeGet(buildURI(ObjectsResource.class, getWiki(), spaces, pageName).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals(className)) {
                Link link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                assertNotNull(link);
                getMethod = executeGet(link.getHref());
                assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

                Object object = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

                return object;
            }
        }

        /* If no object of that class is found, then create a new one */
        Object object = objectFactory.createObject();
        object.setClassName(className);

        PostMethod postMethod = executePostXml(buildURI(ObjectsResource.class, getWiki(), spaces, pageName).toString(),
            object, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));

        object = (Object) unmarshaller.unmarshal(postMethod.getResponseBodyAsStream());

        return object;
    }

    @Test
    void testPUTObjectFormUrlEncoded() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        Object objectToBePut = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        GetMethod getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Object object = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        NameValuePair[] nameValuePairs = new NameValuePair[1];
        nameValuePairs[0] = new NameValuePair("property#tags", TAG_VALUE);

        PostMethod postMethod = executePostForm(
            String.format("%s?method=PUT",
                buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, objectToBePut.getClassName(),
                    objectToBePut.getNumber()).toString()),
            nameValuePairs, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());

        assertEquals(HttpStatus.SC_ACCEPTED, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));

        Object updatedObjectSummary = (Object) unmarshaller.unmarshal(postMethod.getResponseBodyAsStream());

        assertEquals(TAG_VALUE, getProperty(updatedObjectSummary, "tags").getValue());
        assertEquals(object.getClassName(), updatedObjectSummary.getClassName());
        assertEquals(object.getNumber(), updatedObjectSummary.getNumber());
    }

    @Test
    void testPOSTObjectFormUrlEncoded() throws Exception
    {
        final String TAG_VALUE = "TAG";

        NameValuePair[] nameValuePairs = new NameValuePair[2];
        nameValuePairs[0] = new NameValuePair("className", "XWiki.TagClass");
        nameValuePairs[1] = new NameValuePair("property#tags", TAG_VALUE);

        PostMethod postMethod = executePostForm(
            buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName).toString(), nameValuePairs,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));

        Object object = (Object) unmarshaller.unmarshal(postMethod.getResponseBodyAsStream());

        assertEquals(TAG_VALUE, getProperty(object, "tags").getValue());

        GetMethod getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            object.getClassName(), object.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        object = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertEquals(TAG_VALUE, getProperty(object, "tags").getValue());
    }

    @Test
    void testPOSTObjectFormUrlEncodedNoCSRF() throws Exception
    {
        final String tagValue = "TAG";
        NameValuePair[] nameValuePairs = new NameValuePair[2];
        String className = "XWiki.TagClass";
        nameValuePairs[0] = new NameValuePair("className", className);
        nameValuePairs[1] = new NameValuePair("property#tags", tagValue);

        String objectGetURI = buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName, className);

        // Count objects before to ensure nothing is added on the failed request.
        GetMethod getMethod = executeGet(objectGetURI);
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));
        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        int numObjects = objects.getObjectSummaries().size();

        PostMethod postMethod = executePostForm(
            buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName), nameValuePairs,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword(), null);
        assertEquals(HttpStatus.SC_FORBIDDEN, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));
        assertEquals("Invalid or missing form token.", postMethod.getResponseBodyAsString());

        getMethod = executeGet(objectGetURI);
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        objects = (Objects) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        assertEquals(numObjects, objects.getObjectSummaries().size());
    }


    @Test
    void testPUTPropertyFormUrlEncoded() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        /* Make sure that an Object with the TagClass exists. */
        createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        GetMethod getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        assertNotNull(link);

        getMethod = executeGet(link.getHref());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        assertFalse(objects.getObjectSummaries().isEmpty());

        Object currentObject = null;

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals("XWiki.TagClass")) {
                link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                assertNotNull(link);
                getMethod = executeGet(link.getHref());
                assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

                currentObject = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
                break;
            }
        }

        assertNotNull(currentObject);

        Property tagsProperty = getProperty(currentObject, "tags");

        assertNotNull(tagsProperty);

        Link tagsPropertyLink = getFirstLinkByRelation(tagsProperty, Relations.SELF);

        assertNotNull(tagsPropertyLink);

        NameValuePair[] nameValuePairs = new NameValuePair[1];
        nameValuePairs[0] = new NameValuePair("property#tags", TAG_VALUE);

        PostMethod postMethod =
            executePostForm(String.format("%s?method=PUT", tagsPropertyLink.getHref()), nameValuePairs,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, postMethod.getStatusCode(), getHttpMethodInfo(postMethod));

        getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            currentObject.getClassName(), currentObject.getNumber()).toString());
        assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());

        currentObject = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

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

            PutMethod putMethod = executePutXml(
                buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, objectToBePut.getClassName(),
                    objectToBePut.getNumber()).toString(),
                objectToBePut, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
                TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
            assertEquals(HttpStatus.SC_ACCEPTED, putMethod.getStatusCode(), getHttpMethodInfo(putMethod));

            GetMethod getMethod =
                executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

            Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

            versionToValueMap.put(page.getVersion(), value);
        }

        for (String version : versionToValueMap.keySet()) {
            GetMethod getMethod = executeGet(buildURI(ObjectAtPageVersionResource.class, getWiki(), this.spaces,
                this.pageName, version, objectToBePut.getClassName(), objectToBePut.getNumber()).toString());
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode(), getHttpMethodInfo(getMethod));

            Object currentObject = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

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
            GetMethod getMethod = executeGet(
                buildURI(AllObjectsForClassNameResource.class, getWiki(), className));
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());
            Objects objects = (Objects) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            assertTrue(objects.getObjectSummaries().size() >= 2);

            // Test: pagination with number=2
            getMethod = executeGet(
                buildURI(AllObjectsForClassNameResource.class, getWiki(), className) + "?number=2");
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());
            objects = (Objects) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            assertEquals(2, objects.getObjectSummaries().size());

            String secondPage = objects.getObjectSummaries().get(1).getPageName();

            // Test: pagination with number=1 and start=1
            getMethod = executeGet(
                buildURI(AllObjectsForClassNameResource.class, getWiki(), className) + "?number=1&start=1");
            assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());
            objects = (Objects) this.unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
            assertEquals(1, objects.getObjectSummaries().size());
            assertEquals(secondPage, objects.getObjectSummaries().get(0).getPageName());

            // Test: error for number=-1
            getMethod = executeGet(
                buildURI(AllObjectsForClassNameResource.class, getWiki(), className) + "?number=-1");
            assertEquals(400, getMethod.getStatusCode());
            assertEquals(INVALID_LIMIT_MINUS_1, getMethod.getResponseBodyAsString());

            // Test: error for number=1001
            getMethod = executeGet(
                buildURI(AllObjectsForClassNameResource.class, getWiki(), className) + "?number=1001");
            assertEquals(400, getMethod.getStatusCode());
            assertEquals(INVALID_LIMIT_1001, getMethod.getResponseBodyAsString());
        } finally {
            getUtil().rest().delete(ref1);
            getUtil().rest().delete(ref2);
        }
    }
}
