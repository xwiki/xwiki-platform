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
package org.xwiki.query.xwql.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.query.internal.jpql.node.PPath;
import org.xwiki.query.internal.jpql.node.PXObjectDecl;
import org.xwiki.query.internal.jpql.node.Start;

/**
 * Contains information retrieved from query.
 */
public class QueryContext
{
    /**
     * Parse tree of a query.
     */
    private Start tree;

    /**
     * Map from document alias to its description.
     */
    private Map<String, DocumentInfo> documents = new HashMap<String, DocumentInfo>();

    /**
     * Map from object alias to its description.
     */
    private Map<String, ObjectInfo> objects = new HashMap<String, ObjectInfo>();

    private AliasGenerator aliasGenerator = new AliasGenerator();

    private DocumentAccessBridge documentAccessBridge;

    public QueryContext(Start tree, DocumentAccessBridge documentAccessBridge)
    {
        this.tree = tree;
        this.documentAccessBridge = documentAccessBridge;
    }

    public Start getTree()
    {
        return tree;
    }

    public AliasGenerator getAliasGenerator()
    {
        return aliasGenerator;
    }

    public class DocumentInfo
    {
        public String alias;

        /**
         * Map from unnamed object's class name to description. unnamed object is object declaration in where clause:
         * "where doc.object('Class').prop=1"
         */
        public Map<String, ObjectInfo> unnamedObjects = new HashMap<String, ObjectInfo>();
    }

    public class ObjectInfo
    {
        public String docAlias;

        public String className;

        public String alias;

        public String customMappingAlias;

        /**
         * Properties appeared in query
         */
        public Map<String, PropertyInfo> properties = new LinkedHashMap<String, PropertyInfo>();

        public ObjectInfo(String docAlias, String className, String objAlias)
        {
            this.className = className;
            this.docAlias = docAlias;
            this.alias = objAlias;
        }

        public PropertyInfo addProperty(String propname, PPath location)
        {
            PropertyInfo prop = properties.get(propname);
            if (prop == null) {
                prop = new PropertyInfo(propname, this);
                properties.put(propname, prop);
            }
            prop.locations.add(location);
            return prop;
        }

        public boolean isCustomMapped() throws Exception
        {
            for (PropertyInfo p : properties.values()) {
                if (p.isCustomMapped()) {
                    return true;
                }
            }
            return false;
        }
    }

    public class PropertyInfo
    {
        public ObjectInfo object;

        public String name;

        public String alias;

        public PropertyInfo(String name, ObjectInfo object)
        {
            this.name = name;
            this.object = object;
        }

        public List<PPath> locations = new ArrayList<PPath>();

        public String getType() throws Exception
        {
            return documentAccessBridge.getPropertyType(object.className, name);
        }

        public boolean isCustomMapped() throws Exception
        {
            return documentAccessBridge.isPropertyCustomMapped(object.className, name);
        }

        public String getValueField() throws Exception
        {
            String type = getType();
            if (type.endsWith("DBStringListProperty")) {
                return "list";
            } else if (type.endsWith("StringListProperty")) {
                return "textValue";
            } else {
                return "value";
            }
        }
    }

    /**
     * Map from tree node to object it represent.
     */
    private Map<PXObjectDecl, ObjectInfo> nodeToObject = new HashMap<PXObjectDecl, ObjectInfo>();

    public ObjectInfo addObject(String docAlias, String className, String objAlias, PXObjectDecl node)
    {
        DocumentInfo di = getDocument(docAlias);
        if (di == null) {
            throw new InvalidQueryException("Can't find document alias [" + docAlias + "]");
        }
        ObjectInfo res = new ObjectInfo(docAlias, className, objAlias);
        if (objAlias != null) {
            objects.put(objAlias, res);
        } else if (di.unnamedObjects.get(className) == null) {
            di.unnamedObjects.put(className, res);
        } else {
            res = di.unnamedObjects.get(className);
        }
        nodeToObject.put(node, res);
        return res;
    }

    public void addDocument(String alias)
    {
        if (documents.get(alias) != null && !"doc".equals(alias)) {
            throw new InvalidQueryException("Redeclaration of document [" + alias + "]");
        }
        documents.put(alias, new DocumentInfo());
    }

    public DocumentInfo getDocument(String alias)
    {
        return documents.get(alias);
    }

    public ObjectInfo getObject(String objAlias)
    {
        return objects.get(objAlias);
    }

    public ObjectInfo getObject(PXObjectDecl node)
    {
        return nodeToObject.get(node);
    }

    /**
     * @return all objects used in query.
     */
    public Collection<ObjectInfo> getObjects()
    {
        List<ObjectInfo> res = new ArrayList<ObjectInfo>();
        res.addAll(objects.values());
        for (DocumentInfo di : documents.values()) {
            res.addAll(di.unnamedObjects.values());
        }
        return res;
    }

    public Set<String> getDocuments()
    {
        return documents.keySet();
    }

    String DocumentFromName = "Document";
}
