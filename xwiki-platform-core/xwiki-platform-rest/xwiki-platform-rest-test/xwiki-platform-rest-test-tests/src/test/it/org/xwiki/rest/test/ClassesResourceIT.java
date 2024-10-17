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

import java.util.List;
import java.util.Map;

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.rest.model.jaxb.Class;
import org.xwiki.rest.model.jaxb.Classes;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.rest.model.jaxb.PropertyValue;
import org.xwiki.rest.model.jaxb.PropertyValues;
import org.xwiki.rest.resources.classes.ClassPropertyValuesResource;
import org.xwiki.rest.resources.classes.ClassesResource;
import org.xwiki.rest.resources.objects.ObjectsResource;
import org.xwiki.rest.test.framework.AbstractHttpIT;
import org.xwiki.test.ui.TestUtils;

public class ClassesResourceIT extends AbstractHttpIT
{
    @Override
    @Test
    public void testRepresentation() throws Exception
    {
        CloseableHttpResponse response = executeGet(buildURI(ClassesResource.class, getWiki()));
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());

        Classes classes = (Classes) unmarshaller.unmarshal(response.getEntity().getContent());

        for (Class clazz : classes.getClazzs()) {
            checkLinks(clazz);

            for (Property property : clazz.getProperties()) {
                checkLinks(property);
            }
        }
    }

    @Test
    public void testClassesResourcePaginationAndErrors() throws Exception
    {
        // Test: number=-1 should return error
        CloseableHttpResponse response = executeGet(buildURI(ClassesResource.class, getWiki()) + "?number=-1");
        Assert.assertEquals(400, response.getCode());
        Assert.assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(response.getEntity()));

        // Test: number=1001 should return error
        response = executeGet(buildURI(ClassesResource.class, getWiki()) + "?number=1001");
        Assert.assertEquals(400, response.getCode());
        Assert.assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(response.getEntity()));

        // Test: pagination with number=1
        response = executeGet(buildURI(ClassesResource.class, getWiki()) + "?number=1");
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
        Classes classes = (Classes) this.unmarshaller.unmarshal(response.getEntity().getContent());
        Assert.assertEquals(1, classes.getClazzs().size());

        String firstName = classes.getClazzs().get(0).getName();

        // Test: pagination with number=1 and start=1
        response = executeGet(buildURI(ClassesResource.class, getWiki()) + "?number=1&start=1");
        Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
        classes = (Classes) this.unmarshaller.unmarshal(response.getEntity().getContent());
        Assert.assertEquals(1, classes.getClazzs().size());
        Assert.assertNotEquals(firstName, classes.getClazzs().get(0).getName());
    }

    @Test
    public void testClassPropertyValuesResourceBasicAndPagination() throws Exception
    {
        // Setup: create two pages with TagClass objects and different tag values
        String className = "XWiki.TagClass";
        String propertyName = "tags";
        List<String> spaces1 = List.of(getTestClassName() + "A");
        List<String> spaces2 = List.of(getTestClassName() + "B");
        String pageName1 = getTestMethodName() + "1";
        String pageName2 = getTestMethodName() + "2";

        DocumentReference reference1 = new DocumentReference(getWiki(), spaces1, pageName1);
        DocumentReference reference2 = new DocumentReference(getWiki(), spaces2, pageName2);

        Map<String, DocumentReference> tagReferences = Map.of("tag1", reference1, "tag2", reference2);

        try {
            for (Map.Entry<String, DocumentReference> value : tagReferences.entrySet()) {
                String tagName = value.getKey();
                DocumentReference reference = value.getValue();

                this.testUtils.rest().delete(reference);
                this.testUtils.rest().savePage(reference, "content", "Title " + reference.getName());

                // Add the tag to the page
                Object tagObject = this.objectFactory.createObject().withClassName(className);
                Property tagProperty = this.objectFactory.createProperty()
                    .withName(propertyName)
                    .withValue(tagName);
                tagObject.getProperties().add(tagProperty);

                List<String> spaces = reference.getSpaceReferences().stream()
                    .map(SpaceReference::getName)
                    .toList();

                CloseableHttpResponse response = executePostXml(
                    buildURI(ObjectsResource.class, getWiki(), spaces, reference.getName()), tagObject,
                    TestUtils.SUPER_ADMIN_CREDENTIALS.getUserName(), TestUtils.SUPER_ADMIN_CREDENTIALS.getPassword());
                Assert.assertEquals(HttpStatus.SC_CREATED, response.getCode());
            }

            // Test: basic retrieval
            CloseableHttpResponse response = executeGet(
                buildURI(ClassPropertyValuesResource.class, getWiki(), className, propertyName) + "?fp=tag");
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            PropertyValues values = (PropertyValues) this.unmarshaller.unmarshal(response.getEntity().getContent());

            List<String> foundValues = values.getPropertyValues().stream()
                .map(PropertyValue::getValue)
                .map(java.lang.Object::toString)
                .toList();
            for (String tagValue : tagReferences.keySet()) {
                Assert.assertTrue(
                    "Property values [%s] should contain tag value: [%s]".formatted(String.join(", ", foundValues),
                        tagValue),
                    foundValues.contains(tagValue));
            }

            // Test: pagination with limit=1
            response = executeGet(
                buildURI(ClassPropertyValuesResource.class, getWiki(), className, propertyName) + "?fp=tag&limit=1");
            Assert.assertEquals(HttpStatus.SC_OK, response.getCode());
            values = (PropertyValues) this.unmarshaller.unmarshal(response.getEntity().getContent());
            Assert.assertEquals(1, values.getPropertyValues().size());

            // Test: error for limit=-1
            response = executeGet(
                buildURI(ClassPropertyValuesResource.class, getWiki(), className, propertyName) + "?limit=-1");
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_MINUS_1, EntityUtils.toString(response.getEntity()));

            // Test: error for limit=1001
            response = executeGet(
                buildURI(ClassPropertyValuesResource.class, getWiki(), className, propertyName) + "?limit=1001");
            Assert.assertEquals(400, response.getCode());
            Assert.assertEquals(INVALID_LIMIT_1001, EntityUtils.toString(response.getEntity()));
        } finally {
            this.testUtils.rest().delete(reference1);
            this.testUtils.rest().delete(reference2);
        }
    }
}
