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

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.xwiki.model.reference.EntityReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.web.Utils;

/**
 * Holds all data but the content of a wiki page to be indexed. The content is retrieved at indexing time, which should
 * save us some memory especially when rebuilding an index for a big wiki.
 * 
 * @version $Id$
 */
public class DocumentData extends AbstractDocumentData
{
    /** The importance of an object classname. **/
    private static final float CLASSNAME_BOOST = 0.5f;

    /** The importance of an object property. **/
    private static final float OBJECT_PROPERTY_BOOST = 0.75f;

    /** Reference serializer which removes the wiki prefix. */
    @SuppressWarnings("unchecked")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer =
        Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");

    public DocumentData(final XWikiDocument doc, final XWikiContext context, final boolean deleted)
    {
        super(LucenePlugin.DOCTYPE_WIKIPAGE, doc, context, deleted);

        setAuthor(doc.getAuthor());
        setCreator(doc.getCreator());
        setModificationDate(doc.getDate());
        setCreationDate(doc.getCreationDate());
    }

    /**
     * Append a string containing the result of {@link AbstractIndexData#getFullText} plus the full text content of
     *         this document (in the given language)
     */
    @Override
    protected void getFullText(StringBuilder sb, XWikiDocument doc, XWikiContext context)
    {
        super.getFullText(sb, doc, context);

        sb.append(" ");
        sb.append(StringUtils.lowerCase(doc.getContent()));
        sb.append(" ");

        getObjectFullText(sb, doc, context);
    }

    /**
     * Add to the string builder, the result of {@link AbstractIndexData#getFullText(XWikiDocument,XWikiContext)}plus
     * the full text content (values of title,category,content and extract ) XWiki.ArticleClass Object, as far as it
     * could be extracted.
     */
    private void getObjectFullText(StringBuilder sb, XWikiDocument doc, XWikiContext context)
    {
        getObjectContentAsText(sb, doc, context);
    }

    /**
     * Add to the string builder the value of title,category,content and extract of XWiki.ArticleClass
     */
    private void getObjectContentAsText(StringBuilder sb, XWikiDocument doc, XWikiContext context)
    {
        for (List<BaseObject> objects : doc.getXObjects().values()) {
            for (BaseObject obj : objects) {
                extractObjectContent(sb, obj, context);
            }
        }
    }

    private void getObjectContentAsText(StringBuilder contentText, BaseObject baseObject, String property,
        XWikiContext context)
    {
        BaseProperty baseProperty = (BaseProperty) baseObject.getField(property);
        // FIXME Can baseProperty really be null?
        if (baseProperty != null && baseProperty.getValue() != null) {
            if (!(baseObject.getXClass(context).getField(property) instanceof PasswordClass)) {
                contentText.append(StringUtils.lowerCase(baseProperty.getValue().toString()));
            }
        }
    }

    private void extractObjectContent(StringBuilder contentText, BaseObject baseObject, XWikiContext context)
    {
        if (baseObject != null) {
            String[] propertyNames = baseObject.getPropertyNames();
            for (String propertyName : propertyNames) {
                getObjectContentAsText(contentText, baseObject, propertyName, context);
                contentText.append(" ");
            }
        }
    }

    @Override
    public void addDocumentDataToLuceneDocument(Document luceneDoc, XWikiDocument doc, XWikiContext context)
    {
        super.addDocumentDataToLuceneDocument(luceneDoc, doc, context);

        for (List<BaseObject> objects : doc.getXObjects().values()) {
            for (BaseObject obj : objects) {
                if (obj != null) {
                    addFieldToDocument(IndexFields.OBJECT,
                        this.localEntityReferenceSerializer.serialize(obj.getXClassReference()).toLowerCase(),
                        Field.Store.YES, Field.Index.NOT_ANALYZED, CLASSNAME_BOOST, luceneDoc);
                    Object[] propertyNames = obj.getPropertyNames();
                    for (int i = 0; i < propertyNames.length; i++) {
                        indexProperty(luceneDoc, obj, (String) propertyNames[i], context);
                    }
                }
            }
        }
    }

    private void indexProperty(Document luceneDoc, BaseObject baseObject, String propertyName, XWikiContext context)
    {
        String fieldFullName = baseObject.getClassName() + "." + propertyName;
        BaseClass bClass = baseObject.getXClass(context);
        PropertyInterface prop = bClass.getField(propertyName);

        if (prop instanceof PasswordClass) {
            // Do not index passwords
        } else if (prop instanceof StaticListClass && ((StaticListClass) prop).isMultiSelect()) {
            indexStaticList(luceneDoc, baseObject, (StaticListClass) prop, propertyName, context);
        } else {
            StringBuilder sb = new StringBuilder();
            getObjectContentAsText(sb, baseObject, propertyName, context);
            addFieldToDocument(fieldFullName, sb.toString(), Field.Store.YES, Field.Index.ANALYZED,
                OBJECT_PROPERTY_BOOST, luceneDoc);
        }
    }

    private void indexStaticList(Document luceneDoc, BaseObject baseObject, StaticListClass prop, String propertyName,
        XWikiContext context)
    {
        Map<String, ListItem> possibleValues = prop.getMap(context);
        String fieldFullName = baseObject.getClassName() + "." + propertyName;

        for (String value : (List<String>) baseObject.getListValue(propertyName)) {
            ListItem item = possibleValues.get(value);
            if (item != null) {
                // We index the key of the list
                String fieldName = fieldFullName + ".key";
                addFieldToDocument(fieldName, item.getId(), Field.Store.YES, Field.Index.ANALYZED,
                    OBJECT_PROPERTY_BOOST, luceneDoc);
                // We index the value
                fieldName = fieldFullName + ".value";
                addFieldToDocument(fieldName, item.getValue(), Field.Store.YES, Field.Index.ANALYZED,
                    OBJECT_PROPERTY_BOOST, luceneDoc);

                // If the key and value are not the same, we index both
                // The key is always indexed outside the if block, so here we just index the value
                if (!item.getId().equals(item.getValue())) {
                    addFieldToDocument(fieldFullName, item.getValue(), Field.Store.YES, Field.Index.ANALYZED,
                        OBJECT_PROPERTY_BOOST, luceneDoc);
                }
            }

            addFieldToDocument(fieldFullName, value, Field.Store.YES, Field.Index.ANALYZED, OBJECT_PROPERTY_BOOST,
                luceneDoc);
        }
    }
}
