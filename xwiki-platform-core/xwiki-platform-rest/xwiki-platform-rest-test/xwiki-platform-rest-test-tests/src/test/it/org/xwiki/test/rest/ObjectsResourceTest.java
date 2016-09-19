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
package org.xwiki.test.rest;

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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rest.Relations;
import org.xwiki.rest.model.jaxb.Link;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.ObjectSummary;
import org.xwiki.rest.model.jaxb.Objects;
import org.xwiki.rest.model.jaxb.Page;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.rest.resources.objects.ObjectAtPageVersionResource;
import org.xwiki.rest.resources.objects.ObjectResource;
import org.xwiki.rest.resources.objects.ObjectsResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.test.rest.framework.AbstractHttpTest;
import org.xwiki.test.ui.TestUtils;

public class ObjectsResourceTest extends AbstractHttpTest
{
    private String wikiName;

    private List<String> spaces;

    private String pageName;

    private DocumentReference reference;

    @Before
    @Override
    public void setUp() throws Exception
    {
        super.setUp();

        this.wikiName = getWiki();
        this.spaces = Arrays.asList(getTestClassName());
        this.pageName = getTestMethodName();

        this.reference = new DocumentReference(this.wikiName, this.spaces, this.pageName);

        // Create a clean test page.
        this.testUtils.rest().delete(this.reference);
        this.testUtils.rest().savePage(this.reference);

        GetMethod getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);

        /* Create a tag object if it doesn't exist yet */
        if (link == null) {
            Object object = objectFactory.createObject();
            object.setClassName("XWiki.TagClass");

            PostMethod postMethod = executePostXml(
                buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName).toString(), object,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
            Assert.assertEquals(getHttpMethodInfo(postMethod), HttpStatus.SC_CREATED, postMethod.getStatusCode());
        }
    }

    @Override
    @Test
    public void testRepresentation() throws Exception
    {
        GetMethod getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        Assert.assertNotNull(link);

        getMethod = executeGet(link.getHref());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertFalse(objects.getObjectSummaries().isEmpty());

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
            getMethod = executeGet(link.getHref());
            Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

            Object object = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

            checkLinks(objectSummary);

            for (Property property : object.getProperties()) {
                checkLinks(property);
            }
        }
    }

    @Test
    public void testGETNotExistingObject() throws Exception
    {
        GetMethod getMethod = executeGet(
            buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, "NOTEXISTING", 0).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_NOT_FOUND, getMethod.getStatusCode());
    }

    public Property getProperty(Object object, String propertyName)
    {
        for (Property property : object.getProperties()) {
            if (property.getName().equals(propertyName)) {
                return property;
            }
        }

        return null;
    }

    @Test
    public void testPOSTObject() throws Exception
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
        Assert.assertEquals(getHttpMethodInfo(postMethod), HttpStatus.SC_CREATED, postMethod.getStatusCode());

        object = (Object) unmarshaller.unmarshal(postMethod.getResponseBodyAsStream());

        Assert.assertEquals(TAG_VALUE, getProperty(object, "tags").getValue());

        GetMethod getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            object.getClassName(), object.getNumber()).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        object = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertEquals(TAG_VALUE, getProperty(object, "tags").getValue());
    }

    @Test
    public void testPOSTInvalidObject() throws Exception
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
        Assert.assertEquals(getHttpMethodInfo(postMethod), HttpStatus.SC_BAD_REQUEST, postMethod.getStatusCode());
    }

    @Test
    public void testPOSTObjectNotAuthorized() throws Exception
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
        Assert.assertEquals(getHttpMethodInfo(postMethod), HttpStatus.SC_UNAUTHORIZED, postMethod.getStatusCode());
    }

    @Test
    public void testPUTObject() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        Object objectToBePut = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        GetMethod getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Object objectSummary = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        getProperty(objectSummary, "tags").setValue(TAG_VALUE);

        PutMethod putMethod = executePutXml(
            buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, objectToBePut.getClassName(),
                objectToBePut.getNumber()).toString(),
            objectSummary, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpMethodInfo(putMethod), HttpStatus.SC_ACCEPTED, putMethod.getStatusCode());

        Object updatedObjectSummary = (Object) unmarshaller.unmarshal(putMethod.getResponseBodyAsStream());

        Assert.assertEquals(TAG_VALUE, getProperty(updatedObjectSummary, "tags").getValue());
        Assert.assertEquals(objectSummary.getClassName(), updatedObjectSummary.getClassName());
        Assert.assertEquals(objectSummary.getNumber(), updatedObjectSummary.getNumber());
    }

    @Test
    public void testPUTObjectUnauthorized() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        Object objectToBePut = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        GetMethod getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Object object = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        String originalTagValue = getProperty(object, "tags").getValue();
        getProperty(object, "tags").setValue(TAG_VALUE);

        PutMethod putMethod = executePutXml(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()).toString(), object);
        Assert.assertEquals(getHttpMethodInfo(putMethod), HttpStatus.SC_UNAUTHORIZED, putMethod.getStatusCode());

        getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        object = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertEquals(originalTagValue, getProperty(object, "tags").getValue());
    }

    @Test
    public void testDELETEObject() throws Exception
    {
        Object objectToBeDeleted = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        DeleteMethod deleteMethod = executeDelete(
            buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, objectToBeDeleted.getClassName(),
                objectToBeDeleted.getNumber()).toString(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpMethodInfo(deleteMethod), HttpStatus.SC_NO_CONTENT, deleteMethod.getStatusCode());

        GetMethod getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBeDeleted.getClassName(), objectToBeDeleted.getNumber()).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_NOT_FOUND, getMethod.getStatusCode());
    }

    @Test
    public void testDELETEObjectUnAuthorized() throws Exception
    {
        Object objectToBeDeleted = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        DeleteMethod deleteMethod = executeDelete(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBeDeleted.getClassName(), objectToBeDeleted.getNumber()).toString());
        Assert.assertEquals(getHttpMethodInfo(deleteMethod), HttpStatus.SC_UNAUTHORIZED, deleteMethod.getStatusCode());

        GetMethod getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBeDeleted.getClassName(), objectToBeDeleted.getNumber()).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());
    }

    @Test
    public void testPUTProperty() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        /* Make sure that an Object with the TagClass exists. */
        createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        GetMethod getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        Assert.assertNotNull(link);

        getMethod = executeGet(link.getHref());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertFalse(objects.getObjectSummaries().isEmpty());

        Object currentObject = null;

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals("XWiki.TagClass")) {
                link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                Assert.assertNotNull(link);
                getMethod = executeGet(link.getHref());
                Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

                currentObject = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
                break;
            }
        }

        Assert.assertNotNull(currentObject);

        Property tagsProperty = getProperty(currentObject, "tags");

        Assert.assertNotNull(tagsProperty);

        Link tagsPropertyLink = getFirstLinkByRelation(tagsProperty, Relations.SELF);

        Assert.assertNotNull(tagsPropertyLink);

        Property newTags = objectFactory.createProperty();
        newTags.setValue(TAG_VALUE);

        PutMethod putMethod = executePutXml(tagsPropertyLink.getHref(), newTags,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpMethodInfo(putMethod), HttpStatus.SC_ACCEPTED, putMethod.getStatusCode());

        getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            currentObject.getClassName(), currentObject.getNumber()).toString());
        Assert.assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());

        currentObject = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        tagsProperty = getProperty(currentObject, "tags");

        Assert.assertEquals(TAG_VALUE, tagsProperty.getValue());
    }

    @Test
    public void testPUTPropertyWithTextPlain() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        /* Make sure that an Object with the TagClass exists. */
        createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        GetMethod getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        Assert.assertNotNull(link);

        getMethod = executeGet(link.getHref());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertFalse(objects.getObjectSummaries().isEmpty());

        Object currentObject = null;

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals("XWiki.TagClass")) {
                link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                Assert.assertNotNull(link);
                getMethod = executeGet(link.getHref());
                Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

                currentObject = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
                break;
            }
        }

        Assert.assertNotNull(currentObject);

        Property tagsProperty = getProperty(currentObject, "tags");

        Assert.assertNotNull(tagsProperty);

        Link tagsPropertyLink = getFirstLinkByRelation(tagsProperty, Relations.SELF);

        Assert.assertNotNull(tagsPropertyLink);

        PutMethod putMethod = executePut(tagsPropertyLink.getHref(), TAG_VALUE, MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpMethodInfo(putMethod), HttpStatus.SC_ACCEPTED, putMethod.getStatusCode());

        getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            currentObject.getClassName(), currentObject.getNumber()).toString());
        Assert.assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());

        currentObject = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        tagsProperty = getProperty(currentObject, "tags");

        Assert.assertEquals(TAG_VALUE, tagsProperty.getValue());
    }

    private Object createObjectIfDoesNotExists(String className, List<String> spaces, String pageName) throws Exception
    {
        createPageIfDoesntExist(spaces, pageName, "");

        GetMethod getMethod = executeGet(buildURI(ObjectsResource.class, getWiki(), spaces, pageName).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals(className)) {
                Link link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                Assert.assertNotNull(link);
                getMethod = executeGet(link.getHref());
                Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

                Object object = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

                return object;
            }
        }

        /* If no object of that class is found, then create a new one */
        Object object = objectFactory.createObject();
        object.setClassName(className);

        PostMethod postMethod = executePostXml(buildURI(ObjectsResource.class, getWiki(), spaces, pageName).toString(),
            object, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpMethodInfo(postMethod), HttpStatus.SC_CREATED, postMethod.getStatusCode());

        object = (Object) unmarshaller.unmarshal(postMethod.getResponseBodyAsStream());

        return object;
    }

    @Test
    public void testPUTObjectFormUrlEncoded() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        Object objectToBePut = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        GetMethod getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Object object = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        NameValuePair[] nameValuePairs = new NameValuePair[1];
        nameValuePairs[0] = new NameValuePair("property#tags", TAG_VALUE);

        PostMethod postMethod = executePostForm(
            String.format("%s?method=PUT",
                buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, objectToBePut.getClassName(),
                    objectToBePut.getNumber()).toString()),
            nameValuePairs, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());

        Assert.assertEquals(getHttpMethodInfo(postMethod), HttpStatus.SC_ACCEPTED, postMethod.getStatusCode());

        Object updatedObjectSummary = (Object) unmarshaller.unmarshal(postMethod.getResponseBodyAsStream());

        Assert.assertEquals(TAG_VALUE, getProperty(updatedObjectSummary, "tags").getValue());
        Assert.assertEquals(object.getClassName(), updatedObjectSummary.getClassName());
        Assert.assertEquals(object.getNumber(), updatedObjectSummary.getNumber());
    }

    @Test
    public void testPOSTObjectFormUrlEncoded() throws Exception
    {
        final String TAG_VALUE = "TAG";

        NameValuePair[] nameValuePairs = new NameValuePair[2];
        nameValuePairs[0] = new NameValuePair("className", "XWiki.TagClass");
        nameValuePairs[1] = new NameValuePair("property#tags", TAG_VALUE);

        PostMethod postMethod = executePostForm(
            buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName).toString(), nameValuePairs,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpMethodInfo(postMethod), HttpStatus.SC_CREATED, postMethod.getStatusCode());

        Object object = (Object) unmarshaller.unmarshal(postMethod.getResponseBodyAsStream());

        Assert.assertEquals(TAG_VALUE, getProperty(object, "tags").getValue());

        GetMethod getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            object.getClassName(), object.getNumber()).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        object = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertEquals(TAG_VALUE, getProperty(object, "tags").getValue());
    }

    @Test
    public void testPUTPropertyFormUrlEncoded() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        /* Make sure that an Object with the TagClass exists. */
        createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        GetMethod getMethod =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        Assert.assertNotNull(link);

        getMethod = executeGet(link.getHref());
        Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

        Objects objects = (Objects) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        Assert.assertFalse(objects.getObjectSummaries().isEmpty());

        Object currentObject = null;

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals("XWiki.TagClass")) {
                link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                Assert.assertNotNull(link);
                getMethod = executeGet(link.getHref());
                Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

                currentObject = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());
                break;
            }
        }

        Assert.assertNotNull(currentObject);

        Property tagsProperty = getProperty(currentObject, "tags");

        Assert.assertNotNull(tagsProperty);

        Link tagsPropertyLink = getFirstLinkByRelation(tagsProperty, Relations.SELF);

        Assert.assertNotNull(tagsPropertyLink);

        NameValuePair[] nameValuePairs = new NameValuePair[1];
        nameValuePairs[0] = new NameValuePair("property#tags", TAG_VALUE);

        PostMethod postMethod =
            executePostForm(String.format("%s?method=PUT", tagsPropertyLink.getHref()), nameValuePairs,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        Assert.assertEquals(getHttpMethodInfo(postMethod), HttpStatus.SC_ACCEPTED, postMethod.getStatusCode());

        getMethod = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            currentObject.getClassName(), currentObject.getNumber()).toString());
        Assert.assertEquals(HttpStatus.SC_OK, getMethod.getStatusCode());

        currentObject = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

        tagsProperty = getProperty(currentObject, "tags");

        Assert.assertEquals(TAG_VALUE, tagsProperty.getValue());
    }

    @Test
    public void testGETObjectAtPageVersion() throws Exception
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
            Assert.assertEquals(getHttpMethodInfo(putMethod), HttpStatus.SC_ACCEPTED, putMethod.getStatusCode());

            GetMethod getMethod =
                executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName).toString());
            Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

            Page page = (Page) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

            versionToValueMap.put(page.getVersion(), value);
        }

        for (String version : versionToValueMap.keySet()) {
            GetMethod getMethod = executeGet(buildURI(ObjectAtPageVersionResource.class, getWiki(), this.spaces,
                this.pageName, version, objectToBePut.getClassName(), objectToBePut.getNumber()).toString());
            Assert.assertEquals(getHttpMethodInfo(getMethod), HttpStatus.SC_OK, getMethod.getStatusCode());

            Object currentObject = (Object) unmarshaller.unmarshal(getMethod.getResponseBodyAsStream());

            Property property = getProperty(currentObject, "tags");

            Assert.assertEquals(versionToValueMap.get(version), property.getValue());

            checkLinks(currentObject);
            for (Property p : currentObject.getProperties()) {
                checkLinks(p);
            }
        }
    }
}
