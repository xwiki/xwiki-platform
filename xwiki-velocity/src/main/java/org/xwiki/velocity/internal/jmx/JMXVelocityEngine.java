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
package org.xwiki.velocity.internal.jmx;

import org.xwiki.velocity.VelocityEngine;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Uses non-stable (ie might need to be modified when we upgrade the Velocity JAR) introspection to access private
 * fields of Velocity. This is needed since Velocity doesn't offer a way to access information about the template
 * namespaces and the macros within them.
 *
 * @version $Id$
 * @since 2.4M2
 */
public class JMXVelocityEngine implements JMXVelocityEngineMBean
{
    /**
     * The Velocity Engine for which to return management data.
     */
    private VelocityEngine engine;

    /**
     * @param engine the Velocity Engine for which to return management data
     */
    public JMXVelocityEngine(VelocityEngine engine)
    {
        this.engine = engine;
    }

    /**
     * {@inheritDoc}
     * @see JMXVelocityEngineMBean#getTemplates()
     */
    public TabularData getTemplates()
    {
        TabularData data;

        try {
            Map<String, String[]> result = getInternalTemplates();

            // Note: JDK 1.6 has the notion of MXBean which support returning a Map type but since we must use
            // JDK 1.5 for now we have to output a TabularData value.

            // Represents the list of macro names
            ArrayType macroNameType = new ArrayType(1, SimpleType.STRING);

            // Represents one row (template name, macro names) in the returned table data
            String[] columnNames = new String[] {"templateName", "macroNames"};
            String[] descriptions = new String[] {"The Template Name (namespace)", "The names of registered Macros"};
            CompositeType rowType = new CompositeType("template",
                "Template management data (namespaces, macros) for a row", columnNames, descriptions,
                new OpenType[]{SimpleType.STRING, macroNameType});

            TabularType type = new TabularType("templates", "Template management data (namespaces, macros)", rowType,
                columnNames);
            data = new TabularDataSupport(type);

            for (Map.Entry<String, String[]> entry : result.entrySet()) {

                String templateName = entry.getKey();
                String[] macroNames = entry.getValue();

                CompositeData rowData = new CompositeDataSupport(rowType, columnNames, new Object[]{
                    templateName, macroNames});
                data.put(rowData);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to gather information on Velocity Templates/Macros", e);
        }

        return data;
    }

    /**
     * @return the data using standard Java classes, {@link #getTemplates()} wraps it in generic Open types to make the
     *         returned data portable and accessible remotely from a JMX management console
     * @throws NoSuchFieldException in case of an exception in getting the data
     * @throws IllegalAccessException in case of an exception in getting the data
     * @see #getTemplates()
     */
    private Map<String, String[]> getInternalTemplates() throws NoSuchFieldException, IllegalAccessException
    {
        // Get the internal Velocity Engine (not the XWiki wrapping one)
        Object velocityEngine = getField(this.engine, "engine");

        Object runtimeInstance = getField(velocityEngine, "ri");
        Object vmFactory = getField(runtimeInstance, "vmFactory");
        Object vmManager = getField(vmFactory, "vmManager");

        Map<String, Map<String, ?>> namespaceHash =
            (Map<String, Map<String, ?>>) getField(vmManager, "namespaceHash");

        Map<String, ?> globalNamespace = (Map<String, ?>) getField(vmManager, "globalNamespace");

        Map<String, String[]> result = new HashMap<String, String[]>();

        for (String name : namespaceHash.keySet()) {
            String nameSpaceName = name;
            if (globalNamespace.equals(namespaceHash.get(name))) {
                nameSpaceName = "<global>";
            }
            String[] macroNames = new String[namespaceHash.get(name).size()];
            int i = 0;
            for (String macroName : namespaceHash.get(name).keySet()) {
                macroNames[i] = macroName;
                i++;
            }

            result.put(nameSpaceName, macroNames);
        }

        return result;
    }

    /**
     * Helper method to access a private field.
     *
     * @param instance the instance containing the field to access 
     * @param fieldName the name of the field to access
     * @return the field object
     * @throws NoSuchFieldException in case of an error when accessing the private field
     * @throws IllegalAccessException in case of an error when accessing the private field
     */
    private Object getField(Object instance, String fieldName) throws NoSuchFieldException, IllegalAccessException
    {
        Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }
}
