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

import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.rest.model.jaxb.Class;
import org.xwiki.rest.model.jaxb.Classes;
import org.xwiki.rest.model.jaxb.Property;
import org.xwiki.rest.resources.classes.ClassesResource;
import org.xwiki.rest.test.framework.AbstractHttpIT;

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
}
