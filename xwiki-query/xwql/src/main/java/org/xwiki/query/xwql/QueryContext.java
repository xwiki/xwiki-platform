package org.xwiki.query.xwql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xwiki.query.jpql.node.PPath;
import org.xwiki.query.jpql.node.PXObjectDecl;
import org.xwiki.query.jpql.node.Start;

/**
 * Contains information retrieved from query.
 */
public class QueryContext
{
    /**
     * Parse tree of a query.
     */
    public Start tree;

    /**
     * Set of document aliases.
     */
    Map<String, DocumentInfo> documents = new HashMap<String, DocumentInfo>();

    /**
     * Map from object alias to its description.
     */
    Map<String, ObjectInfo> objects = new HashMap<String, ObjectInfo>();

    AliasGenerator aliasGenerator = new AliasGenerator();

    public QueryContext(Start tree)
    {
        this.tree = tree;
    }

    public AliasGenerator getAliasGenerator()
    {
        return aliasGenerator;
    }

    public class DocumentInfo {
        public String alias;

        /**
         * Map from unnamed object's class name to description.
         * unnamed object is object declaration in where clause: "where doc.object('Class').prop=1"
         */
        public Map<String, ObjectInfo> unnamedObjects = new HashMap<String, ObjectInfo>();
    }

    public class ObjectInfo {
        public String docAlias;
        public String className;
        public String alias;
        /**
         * Properties appeared in query
         */
        public Map<String, PropertyInfo> properties = new HashMap<String, PropertyInfo>();

        public ObjectInfo(String docAlias, String className, String objAlias)
        {
            this.className = className;
            this.docAlias = docAlias;
            this.alias = objAlias;
        }

        public PropertyInfo addProperty(String propname, PPath location) {
            PropertyInfo prop = properties.get(propname);
            if (prop==null) {
                prop = new PropertyInfo(propname, this);
                properties.put(propname, prop);
            }
            prop.locations.add(location);
            nodeToProperty.put(location, prop);
            return prop;
        }
    }

    public class PropertyInfo {
        public ObjectInfo object;
        public String name;
        public String alias;
        public String defaultField = "value";

        public PropertyInfo(String name, ObjectInfo object) {
            this.name = name;
            this.object = object;
        }

        public List<PPath> locations = new ArrayList<PPath>();
    }

    /**
     * Map from tree node to object it represent.
     */
    Map<PXObjectDecl, ObjectInfo> nodeToObject = new HashMap<PXObjectDecl, ObjectInfo>();

    Map<PPath, PropertyInfo> nodeToProperty = new HashMap<PPath, PropertyInfo>();

    public ObjectInfo addObject(String docAlias, String className, String objAlias, PXObjectDecl node) {
        DocumentInfo di = getDocument(docAlias);
        if (di == null) {
            throw new InvalidQueryException("Can't find document alias [" + docAlias + "]");
        }
        ObjectInfo res = new ObjectInfo(docAlias, className, objAlias);
        if (objAlias != null) {
            objects.put(objAlias, res);
        } else if (di.unnamedObjects.get(className) == null) {
            di.unnamedObjects.put(className, res);
        }
        nodeToObject.put(node, res);
        return res;
    }

    public void addDocument(String alias) {
        if (documents.get(alias) != null && !"doc".equals(alias)) {
            throw new InvalidQueryException("Redeclaration of document ["+alias+"]");
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

    public ObjectInfo getObject(PXObjectDecl node) {
        return nodeToObject.get(node);
    }

    public PropertyInfo getProperty(PPath node) {
        return nodeToProperty.get(node);
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
