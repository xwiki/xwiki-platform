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
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
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
import org.xwiki.rest.resources.objects.AllObjectsForClassNameResource;
import org.xwiki.rest.resources.objects.ObjectAtPageVersionResource;
import org.xwiki.rest.resources.objects.ObjectResource;
import org.xwiki.rest.resources.objects.ObjectsResource;
import org.xwiki.rest.resources.pages.PageResource;
import org.xwiki.rest.test.framework.AbstractHttpIT;
import org.xwiki.test.ui.TestUtils;

import static org.junit.Assert.assertEquals;

public class ObjectsResourceIT extends AbstractHttpIT
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

        CloseableHttpResponse response =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName));
        assertEquals(HttpStatus.SC_OK, response.getCode());

        Page page = (Page) unmarshaller.unmarshal(response.getEntity().getContent());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);

        /* Create a tag object if it doesn't exist yet */
        if (link == null) {
            Object object = objectFactory.createObject();
            object.setClassName("XWiki.TagClass");

            response = executePostXml(buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName), object,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
            assertEquals(HttpStatus.SC_CREATED, response.getCode());
        }
    }

    @Override
    @Test
    public void testRepresentation() throws Exception
    {
        CloseableHttpResponse response =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName));
        assertEquals(HttpStatus.SC_OK, response.getCode());

        Page page = (Page) unmarshaller.unmarshal(response.getEntity().getContent());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        Assert.assertNotNull(link);

        response = executeGet(link.getHref());
        assertEquals(HttpStatus.SC_OK, response.getCode());

        Objects objects = (Objects) unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertFalse(objects.getObjectSummaries().isEmpty());

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
            response = executeGet(link.getHref());
            assertEquals(HttpStatus.SC_OK, response.getCode());

            Object object = (Object) unmarshaller.unmarshal(response.getEntity().getContent());

            checkLinks(objectSummary);

            for (Property property : object.getProperties()) {
                checkLinks(property);
            }
        }
    }

    @Test
    public void testGETNotExistingObject() throws Exception
    {
        CloseableHttpResponse response =
            executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, "NOTEXISTING", 0));
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getCode());
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

        CloseableHttpResponse response =
            executePostXml(buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName), object,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, response.getCode());

        object = (Object) unmarshaller.unmarshal(response.getEntity().getContent());

        assertEquals(TAG_VALUE, getProperty(object, "tags").getValue());

        response = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            object.getClassName(), object.getNumber()));
        assertEquals(HttpStatus.SC_OK, response.getCode());

        object = (Object) unmarshaller.unmarshal(response.getEntity().getContent());

        assertEquals(TAG_VALUE, getProperty(object, "tags").getValue());
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

        CloseableHttpResponse response =
            executePostXml(buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName), object,
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getCode());
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

        CloseableHttpResponse response =
            executePostXml(buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName), object);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getCode());
    }

    @Test
    public void testPUTObject() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        Object objectToBePut = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse response = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces,
            this.pageName, objectToBePut.getClassName(), objectToBePut.getNumber()));
        assertEquals(HttpStatus.SC_OK, response.getCode());

        Object objectSummary = (Object) unmarshaller.unmarshal(response.getEntity().getContent());
        getProperty(objectSummary, "tags").setValue(TAG_VALUE);

        response = executePutXml(
            buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, objectToBePut.getClassName(),
                objectToBePut.getNumber()),
            objectSummary, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, response.getCode());

        Object updatedObjectSummary = (Object) unmarshaller.unmarshal(response.getEntity().getContent());

        assertEquals(TAG_VALUE, getProperty(updatedObjectSummary, "tags").getValue());
        assertEquals(objectSummary.getClassName(), updatedObjectSummary.getClassName());
        assertEquals(objectSummary.getNumber(), updatedObjectSummary.getNumber());
    }

    @Test
    public void testPUTObjectUnauthorized() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        Object objectToBePut = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse response = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces,
            this.pageName, objectToBePut.getClassName(), objectToBePut.getNumber()));
        assertEquals(HttpStatus.SC_OK, response.getCode());

        Object object = (Object) unmarshaller.unmarshal(response.getEntity().getContent());
        String originalTagValue = getProperty(object, "tags").getValue();
        getProperty(object, "tags").setValue(TAG_VALUE);

        response = executePutXml(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()), object);
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getCode());

        response = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBePut.getClassName(), objectToBePut.getNumber()));
        assertEquals(HttpStatus.SC_OK, response.getCode());

        object = (Object) unmarshaller.unmarshal(response.getEntity().getContent());

        assertEquals(originalTagValue, getProperty(object, "tags").getValue());
    }

    @Test
    public void testDELETEObject() throws Exception
    {
        Object objectToBeDeleted = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse response = executeDelete(
            buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, objectToBeDeleted.getClassName(),
                objectToBeDeleted.getNumber()),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getCode());

        response = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBeDeleted.getClassName(), objectToBeDeleted.getNumber()));
        assertEquals(HttpStatus.SC_NOT_FOUND, response.getCode());
    }

    @Test
    public void testDELETEObjectUnAuthorized() throws Exception
    {
        Object objectToBeDeleted = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse response = executeDelete(buildURI(ObjectResource.class, getWiki(), this.spaces,
            this.pageName, objectToBeDeleted.getClassName(), objectToBeDeleted.getNumber()));
        assertEquals(HttpStatus.SC_UNAUTHORIZED, response.getCode());

        response = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            objectToBeDeleted.getClassName(), objectToBeDeleted.getNumber()));
        assertEquals(HttpStatus.SC_OK, response.getCode());
    }

    @Test
    public void testPUTProperty() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        /* Make sure that an Object with the TagClass exists. */
        createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse response =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName));
        assertEquals(HttpStatus.SC_OK, response.getCode());

        Page page = (Page) unmarshaller.unmarshal(response.getEntity().getContent());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        Assert.assertNotNull(link);

        response = executeGet(link.getHref());
        assertEquals(HttpStatus.SC_OK, response.getCode());

        Objects objects = (Objects) unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertFalse(objects.getObjectSummaries().isEmpty());

        Object currentObject = null;

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals("XWiki.TagClass")) {
                link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                Assert.assertNotNull(link);
                response = executeGet(link.getHref());
                assertEquals(HttpStatus.SC_OK, response.getCode());

                currentObject = (Object) unmarshaller.unmarshal(response.getEntity().getContent());
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

        response = executePutXml(tagsPropertyLink.getHref(), newTags, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, response.getCode());

        response = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            currentObject.getClassName(), currentObject.getNumber()));
        assertEquals(HttpStatus.SC_OK, response.getCode());

        currentObject = (Object) unmarshaller.unmarshal(response.getEntity().getContent());

        tagsProperty = getProperty(currentObject, "tags");

        assertEquals(TAG_VALUE, tagsProperty.getValue());
    }

    @Test
    public void testPUTPropertyWithTextPlain() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        /* Make sure that an Object with the TagClass exists. */
        createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse response =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName));
        assertEquals(HttpStatus.SC_OK, response.getCode());

        Page page = (Page) unmarshaller.unmarshal(response.getEntity().getContent());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        Assert.assertNotNull(link);

        response = executeGet(link.getHref());
        assertEquals(HttpStatus.SC_OK, response.getCode());

        Objects objects = (Objects) unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertFalse(objects.getObjectSummaries().isEmpty());

        Object currentObject = null;

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals("XWiki.TagClass")) {
                link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                Assert.assertNotNull(link);
                response = executeGet(link.getHref());
                assertEquals(HttpStatus.SC_OK, response.getCode());

                currentObject = (Object) unmarshaller.unmarshal(response.getEntity().getContent());
                break;
            }
        }

        Assert.assertNotNull(currentObject);

        Property tagsProperty = getProperty(currentObject, "tags");

        Assert.assertNotNull(tagsProperty);

        Link tagsPropertyLink = getFirstLinkByRelation(tagsProperty, Relations.SELF);

        Assert.assertNotNull(tagsPropertyLink);

        response = executePut(tagsPropertyLink.getHref(), TAG_VALUE, MediaType.TEXT_PLAIN,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, response.getCode());

        response = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            currentObject.getClassName(), currentObject.getNumber()));
        assertEquals(HttpStatus.SC_OK, response.getCode());

        currentObject = (Object) unmarshaller.unmarshal(response.getEntity().getContent());

        tagsProperty = getProperty(currentObject, "tags");

        assertEquals(TAG_VALUE, tagsProperty.getValue());
    }

    private Object createObjectIfDoesNotExists(String className, List<String> spaces, String pageName) throws Exception
    {
        createPageIfDoesntExist(spaces, pageName, "");

        CloseableHttpResponse response = executeGet(buildURI(ObjectsResource.class, getWiki(), spaces, pageName));
        assertEquals(HttpStatus.SC_OK, response.getCode());

        Objects objects = (Objects) unmarshaller.unmarshal(response.getEntity().getContent());

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals(className)) {
                Link link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                Assert.assertNotNull(link);
                response = executeGet(link.getHref());
                assertEquals(HttpStatus.SC_OK, response.getCode());

                return (Object) unmarshaller.unmarshal(response.getEntity().getContent());
            }
        }

        /* If no object of that class is found, then create a new one */
        Object object = objectFactory.createObject();
        object.setClassName(className);

        response = executePostXml(buildURI(ObjectsResource.class, getWiki(), spaces, pageName), object,
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, response.getCode());

        object = (Object) unmarshaller.unmarshal(response.getEntity().getContent());

        return object;
    }

    @Test
    public void testPUTObjectFormUrlEncoded() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        Object objectToBePut = createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse response = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces,
            this.pageName, objectToBePut.getClassName(), objectToBePut.getNumber()));
        assertEquals(HttpStatus.SC_OK, response.getCode());

        Object object = (Object) unmarshaller.unmarshal(response.getEntity().getContent());

        response = executePostForm(
            String.format("%s?method=PUT",
                buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, objectToBePut.getClassName(),
                    objectToBePut.getNumber())),
            List.of(new BasicNameValuePair("property#tags", TAG_VALUE)),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());

        assertEquals(HttpStatus.SC_ACCEPTED, response.getCode());

        Object updatedObjectSummary = (Object) unmarshaller.unmarshal(response.getEntity().getContent());

        assertEquals(TAG_VALUE, getProperty(updatedObjectSummary, "tags").getValue());
        assertEquals(object.getClassName(), updatedObjectSummary.getClassName());
        assertEquals(object.getNumber(), updatedObjectSummary.getNumber());
    }

    @Test
    public void testPOSTObjectFormUrlEncoded() throws Exception
    {
        final String TAG_VALUE = "TAG";

        CloseableHttpResponse response =
            executePostForm(buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName),
                List.of(new BasicNameValuePair("className", "XWiki.TagClass"),
                    new BasicNameValuePair("property#tags", TAG_VALUE)),
                TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_CREATED, response.getCode());

        Object object = (Object) unmarshaller.unmarshal(response.getEntity().getContent());

        assertEquals(TAG_VALUE, getProperty(object, "tags").getValue());

        response = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            object.getClassName(), object.getNumber()));
        assertEquals(HttpStatus.SC_OK, response.getCode());

        object = (Object) unmarshaller.unmarshal(response.getEntity().getContent());

        assertEquals(TAG_VALUE, getProperty(object, "tags").getValue());
    }

    @Test
    public void testPOSTObjectFormUrlEncodedNoCSRF() throws Exception
    {
        final String tagValue = "TAG";
        String className = "XWiki.TagClass";

        String objectGetURI = buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName, className);

        // Count objects before to ensure nothing is added on the failed request.
        CloseableHttpResponse response = executeGet(objectGetURI);
        assertEquals(HttpStatus.SC_OK, response.getCode());
        Objects objects = (Objects) unmarshaller.unmarshal(response.getEntity().getContent());
        int numObjects = objects.getObjectSummaries().size();

        response = executePostForm(buildURI(ObjectsResource.class, getWiki(), this.spaces, this.pageName),
            List.of(new BasicNameValuePair("className", className), new BasicNameValuePair("property#tags", tagValue)),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword(), null);
        assertEquals(HttpStatus.SC_FORBIDDEN, response.getCode());
        assertEquals("Invalid or missing form token.", EntityUtils.toString(response.getEntity()));

        response = executeGet(objectGetURI);
        assertEquals(HttpStatus.SC_OK, response.getCode());

        objects = (Objects) unmarshaller.unmarshal(response.getEntity().getContent());
        assertEquals(numObjects, objects.getObjectSummaries().size());
    }

    @Test
    public void testPUTPropertyFormUrlEncoded() throws Exception
    {
        final String TAG_VALUE = UUID.randomUUID().toString();

        /* Make sure that an Object with the TagClass exists. */
        createObjectIfDoesNotExists("XWiki.TagClass", this.spaces, this.pageName);

        CloseableHttpResponse response =
            executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName));
        assertEquals(HttpStatus.SC_OK, response.getCode());

        Page page = (Page) unmarshaller.unmarshal(response.getEntity().getContent());
        Link link = getFirstLinkByRelation(page, Relations.OBJECTS);
        Assert.assertNotNull(link);

        response = executeGet(link.getHref());
        assertEquals(HttpStatus.SC_OK, response.getCode());

        Objects objects = (Objects) unmarshaller.unmarshal(response.getEntity().getContent());

        Assert.assertFalse(objects.getObjectSummaries().isEmpty());

        Object currentObject = null;

        for (ObjectSummary objectSummary : objects.getObjectSummaries()) {
            if (objectSummary.getClassName().equals("XWiki.TagClass")) {
                link = getFirstLinkByRelation(objectSummary, Relations.OBJECT);
                Assert.assertNotNull(link);
                response = executeGet(link.getHref());
                assertEquals(HttpStatus.SC_OK, response.getCode());

                currentObject = (Object) unmarshaller.unmarshal(response.getEntity().getContent());
                break;
            }
        }

        Assert.assertNotNull(currentObject);

        Property tagsProperty = getProperty(currentObject, "tags");

        Assert.assertNotNull(tagsProperty);

        Link tagsPropertyLink = getFirstLinkByRelation(tagsProperty, Relations.SELF);

        Assert.assertNotNull(tagsPropertyLink);

        response = executePostForm(String.format("%s?method=PUT", tagsPropertyLink.getHref()),
            List.of(new BasicNameValuePair("property#tags", TAG_VALUE)),
            TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
        assertEquals(HttpStatus.SC_ACCEPTED, response.getCode());

        response = executeGet(buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName,
            currentObject.getClassName(), currentObject.getNumber()));
        assertEquals(HttpStatus.SC_OK, response.getCode());

        currentObject = (Object) unmarshaller.unmarshal(response.getEntity().getContent());

        tagsProperty = getProperty(currentObject, "tags");

        assertEquals(TAG_VALUE, tagsProperty.getValue());
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

            CloseableHttpResponse response = executePutXml(
                buildURI(ObjectResource.class, getWiki(), this.spaces, this.pageName, objectToBePut.getClassName(),
                    objectToBePut.getNumber()),
                objectToBePut, TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(),
                TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
            assertEquals(HttpStatus.SC_ACCEPTED, response.getCode());

            response = executeGet(buildURI(PageResource.class, getWiki(), this.spaces, this.pageName));
            assertEquals(HttpStatus.SC_OK, response.getCode());

            Page page = (Page) unmarshaller.unmarshal(response.getEntity().getContent());

            versionToValueMap.put(page.getVersion(), value);
        }

        for (String version : versionToValueMap.keySet()) {
            CloseableHttpResponse response = executeGet(buildURI(ObjectAtPageVersionResource.class, getWiki(),
                this.spaces, this.pageName, version, objectToBePut.getClassName(), objectToBePut.getNumber()));
            assertEquals(HttpStatus.SC_OK, response.getCode());

            Object currentObject = (Object) unmarshaller.unmarshal(response.getEntity().getContent());

            Property property = getProperty(currentObject, "tags");

            assertEquals(versionToValueMap.get(version), property.getValue());

            checkLinks(currentObject);
            for (Property p : currentObject.getProperties()) {
                checkLinks(p);
            }
        }
    }

    @Test
    public void testAllObjectsForClassNameResourcePaginationAndErrors() throws Exception
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
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
            this.testUtils.rest().savePage(ref1);
            this.testUtils.rest().savePage(ref2);

            // Add TagClass objects to both pages
            createObjectIfDoesNotExists(className, spaces1, pageName1);
            createObjectIfDoesNotExists(className, spaces2, pageName2);

            // Test: basic retrieval
            CloseableHttpResponse response = executeGet(
                buildURI(AllObjectsForClassNameResource.class, getWiki(), className));
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            Objects objects = (Objects) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertTrue(objects.getObjectSummaries().size() >= 2);

            // Test: pagination with number=2
            response = executeGet(
                buildURI(AllObjectsForClassNameResource.class, getWiki(), className) + "?number=2");
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            objects = (Objects) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(2, objects.getObjectSummaries().size());

            String secondPage = objects.getObjectSummaries().get(1).getPageName();

            // Test: pagination with number=1 and start=1
            response = executeGet(
                buildURI(AllObjectsForClassNameResource.class, getWiki(), className) + "?number=1&start=1");
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            objects = (Objects) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, objects.getObjectSummaries().size());
            Assert.assertEquals(secondPage, objects.getObjectSummaries().get(0).getPageName());

            // Test: error for number=-1
            response = executeGet(
                buildURI(AllObjectsForClassNameResource.class, getWiki(), className) + "?number=-1");
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(response.getEntity()));

            // Test: error for number=1001
            response = executeGet(
                buildURI(AllObjectsForClassNameResource.class, getWiki(), className) + "?number=1001");
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(response.getEntity()));
        } finally {
            this.testUtils.rest().delete(ref1);
            this.testUtils.rest().delete(ref2);
        }
    }
}
