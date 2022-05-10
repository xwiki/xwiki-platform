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
package org.xwiki.activeinstalls2.internal.data;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import co.elastic.clients.elasticsearch._types.mapping.Property;

/**
 * Provide the JVM's vendor and version.
 *
 * @version $Id$
 * @since 6.1M1
 */
@Component
@Named("java")
@Singleton
public class JavaPingDataProvider extends AbstractPingDataProvider
{
    private static final String PROPERTY_JAVA_VENDOR = "vendor";

    private static final String PROPERTY_JAVA_VERSION = "version";

    private static final String PROPERTY_JAVA_SPECIFICATION_VERSION = "specificationVersion";

    private static final String PROPERTY_JAVA = "java";

    @Override
    public Map<String, Property> provideMapping()
    {
        Map<String, Property> propertiesMap = new HashMap<>();
        propertiesMap.put(PROPERTY_JAVA_VENDOR, Property.of(b1 -> b1.keyword(b2 -> b2)));
        propertiesMap.put(PROPERTY_JAVA_VERSION, Property.of(b1 -> b1.keyword(b2 -> b2)));
        propertiesMap.put(PROPERTY_JAVA_SPECIFICATION_VERSION, Property.of(b1 -> b1.keyword(b2 -> b2)));

        return Collections.singletonMap(PROPERTY_JAVA, Property.of(b0 -> b0.object(b1 ->
            b1.properties(propertiesMap))));
    }

    @Override
    public void provideData(Ping ping)
    {
        JavaPing javaPing = new JavaPing();
        javaPing.setVendor(System.getProperty("java.vendor"));
        javaPing.setVersion(System.getProperty("java.version"));
        javaPing.setSpecificationVersion(System.getProperty("java.specification.version"));
        ping.setJava(javaPing);
    }
}
