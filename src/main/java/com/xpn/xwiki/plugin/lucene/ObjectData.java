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
package com.xpn.xwiki.plugin.lucene;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.objects.classes.StaticListClass;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Field;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Hold the property values of the XWiki.ArticleClass Objects.
 */
public class ObjectData extends IndexData
{
    private static final Log LOG = LogFactory.getLog(ObjectData.class);

    public ObjectData(final XWikiDocument doc, final XWikiContext context)
    {
        super(doc, context);
        setAuthor(doc.getAuthor());
        setCreator(doc.getCreator());
        setModificationDate(doc.getDate());
        setCreationDate(doc.getCreationDate());
    }

    /**
     * @see com.xpn.xwiki.plugin.lucene.IndexData#getType()
     */
    public String getType()
    {
        return LucenePlugin.DOCTYPE_OBJECTS;
    }

    public String getId() {
        return new StringBuffer(super.getId()).append(".objects").toString();
    }

    /**
     * @return a string containing the result of {@link IndexData#getFullText(XWikiDocument,XWikiContext)}plus
     *         the full text content (values of title,category,content and extract )
     *         XWiki.ArticleClass Object, as far as it could be extracted.
     */
    public String getFullText(XWikiDocument doc, XWikiContext context)
    {
        StringBuffer retval = new StringBuffer(super.getFullText(doc, context));
        String contentText = getContentAsText(doc, context);
        if (contentText != null) {
            retval.append(" ").append(contentText).toString();
        }
        return retval.toString();
    }

    /**
     * @return string containing value of title,category,content and extract of XWiki.ArticleClass
     */
    private String getContentAsText(XWikiDocument doc, XWikiContext context)
    {
        StringBuffer contentText = new StringBuffer();
        try {
            LOG.info(doc.getFullName());
            Map objects = doc.getxWikiObjects();
            Iterator itKey = objects.keySet().iterator();
            while (itKey.hasNext()) {
                String className = (String) itKey.next();
                Iterator itObj = doc.getObjects(className).iterator();
                while (itObj.hasNext()) {
                    extractContent(contentText, (BaseObject) itObj.next(), context);
                }
            }
        } catch (Exception e) {
            LOG.error("error getting content from  XWiki Objects ", e);
            e.printStackTrace();
        }
        return contentText.toString();
    }

    private void extractContent(StringBuffer contentText, BaseObject baseObject,
        XWikiContext context)
    {
        try {
            if (baseObject != null) {
                Object[] propertyNames = baseObject.getPropertyNames();
                for (int i = 0; i < propertyNames.length; i++) {
                    BaseProperty baseProperty =
                        (BaseProperty) baseObject.getField((String) propertyNames[i]);
                    if ((baseProperty != null) && (baseProperty.getValue() != null)) {
                        contentText.append(baseProperty.getValue().toString());
                    }
                    contentText.append(" ");
                }
            }
        } catch (Exception e) {
            LOG.error("error getting content from  XWiki Object ", e);
            e.printStackTrace();
        }
    }

    public void addDataToLuceneDocument(org.apache.lucene.document.Document luceneDoc,
        XWikiDocument doc,
        XWikiContext context)
    {

        super.addDataToLuceneDocument(luceneDoc, doc, context);
        Map objects = doc.getxWikiObjects();
        String className;
        Iterator itObj;
        BaseObject baseObject;
        for (Iterator itr = objects.keySet().iterator(); itr.hasNext();) {
            className = (String) itr.next();
            itObj = doc.getObjects(className).iterator();

            while (itObj.hasNext()) {
                baseObject = (BaseObject) itObj.next();
                if (baseObject != null) {
                    Object[] propertyNames = baseObject.getPropertyNames();
                    for (int i = 0; i < propertyNames.length; i++) {
                        try {
                            indexProperty(luceneDoc, baseObject, (String) propertyNames[i],
                                context);
                        } catch (Exception e) {
                            LOG.error("error extracting fulltext for document " + this, e);
                        }
                    }
                }
            }
        }
    }

    private void indexProperty(org.apache.lucene.document.Document luceneDoc, BaseObject baseObject,
        String propertyName, XWikiContext context)
    {
        String fieldFullName = baseObject.getClassName() + "." + propertyName;
        BaseClass bClass = baseObject.getxWikiClass(context);
        PropertyInterface prop = bClass.getField(propertyName);

        if (prop instanceof StaticListClass && ((StaticListClass) prop).isMultiSelect()) {
            indexStaticList(luceneDoc, baseObject, (StaticListClass) prop, propertyName, context);
        } else {
            final String ft = getContentAsText(baseObject, propertyName);
            if (ft != null) {
                luceneDoc.add(new Field(fieldFullName, ft, Field.Store.YES, Field.Index.TOKENIZED));
            }
        }
    }

    private void indexStaticList(org.apache.lucene.document.Document luceneDoc,
        BaseObject baseObject, StaticListClass prop, String propertyName, XWikiContext context)
    {
        Map possibleValues = prop.getMap(context);
        List keys = baseObject.getListValue(propertyName);
        String fieldFullName = baseObject.getClassName() + "." + propertyName;
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            String value = (String) it.next();
            ListItem item = (ListItem) possibleValues.get(value);
            if (item != null) {
                // we index the key of the list
                String fieldName = fieldFullName + ".key";
                luceneDoc.add(
                    new Field(fieldName, item.getId(), Field.Store.YES, Field.Index.TOKENIZED));
                //we index the value
                fieldName = fieldFullName + ".value";
                luceneDoc.add(
                    new Field(fieldName, item.getValue(), Field.Store.YES, Field.Index.TOKENIZED));
                if (!item.getId().equals(item.getValue())) {
                    luceneDoc.add(new Field(fieldFullName, item.getValue(), Field.Store.YES,
                        Field.Index.TOKENIZED));
                }
            }
            //we index both if value is not equal to the id(key)
            luceneDoc.add(new Field(fieldFullName, value, Field.Store.YES, Field.Index.TOKENIZED));
        }
    }

    public String getFullText(XWikiDocument doc, BaseObject baseObject, String property,
        XWikiContext context)
    {
        return getContentAsText(baseObject, property);
    }

    private String getContentAsText(BaseObject baseObject, String property)
    {

        StringBuffer contentText = new StringBuffer();
        try {
            BaseProperty baseProperty;
            baseProperty = (BaseProperty) baseObject.getField(property);
            if (baseProperty.getValue() != null) {
                contentText.append(baseProperty.getValue().toString());
            }
        } catch (Exception e) {
            LOG.error("error getting content from  XWiki Objects ", e);
            e.printStackTrace();
        }
        return contentText.toString();
    }
}
