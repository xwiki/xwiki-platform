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
package com.xpn.xwiki.objects.classes;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.edit.EditException;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorManager;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxContent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.web.Utils;

public class TextAreaClass extends StringClass
{
    private static final String XCLASSNAME = "textarea";

    private static final Logger LOGGER = LoggerFactory.getLogger(TextAreaClass.class);

    public TextAreaClass(PropertyMetaClass wclass)
    {
        super(XCLASSNAME, "Text Area", wclass);

        setSize(40);
        setRows(5);
    }

    public TextAreaClass()
    {
        this(null);
    }

    @Override
    public BaseProperty newProperty()
    {
        BaseProperty property = new LargeStringProperty();
        property.setName(getName());

        return property;
    }

    public int getRows()
    {
        return getIntValue("rows");
    }

    public void setRows(int rows)
    {
        setIntValue("rows", rows);
    }

    public String getEditor()
    {
        return getStringValue("editor").toLowerCase();
    }

    public void setEditor(String editor)
    {
        setStringValue("editor", editor);
    }

    public String getContentType()
    {
        String result = getStringValue("contenttype").toLowerCase();
        if (result.equals("")) {
            result = "fullyrenderedtext";
        }

        return result;
    }

    public void setContentType(String contentType)
    {
        setStringValue("contenttype", contentType);
    }

    public boolean isWysiwyg(XWikiContext context)
    {
        return "wysiwyg".equals(getEditorType(context));
    }

    private String getEditorType(XWikiContext context)
    {
        String editorType = null;
        if (context != null && context.getRequest() != null) {
            editorType = context.getRequest().get("xeditmode");
        }
        if (isEmptyValue(editorType)) {
            editorType = getEditor();
            if (isEmptyValue(editorType) && context != null && context.getWiki() != null) {
                editorType = context.getWiki().getEditorPreference(context);
            }
        }
        return isEmptyValue(editorType) ? null : editorType.toLowerCase();
    }

    private boolean isEmptyValue(String value)
    {
        // See XWIKI-10853: Some static lists in XWiki.XWikiPreferences have the "---" value
        return StringUtils.isEmpty(value) || "---".equals(value);
    }

    /**
     * @return true if the content of this text area is not a wiki syntax content
     */
    public boolean isWikiContent()
    {
        String contentType = getContentType();

        if (contentType != null && !contentType.equals("puretext") && !contentType.equals("velocitycode")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        String editorType = getEditorType(context);
        EditorManager editorManager = Utils.getComponent(EditorManager.class);
        Editor<SyntaxContent> editor = editorManager.getDefaultEditor(SyntaxContent.class, editorType);
        Map<String, Object> parameters = new HashMap<>();
        String fieldName = prefix + name;
        parameters.put("id", fieldName);
        parameters.put("name", fieldName);
        parameters.put("cols", getSize());
        parameters.put("rows", getRows());
        parameters.put("disabled", isDisabled());
        parameters.put("sourceDocumentReference", object.getDocumentReference());
        Syntax syntax = "puretext".equals(editorType) ? Syntax.PLAIN_1_0 : getObjectDocumentSyntax(object, context);
        SyntaxContent syntaxContent = new SyntaxContent(object.getStringValue(name), syntax);
        try {
            buffer.append(editor.render(syntaxContent, parameters));
        } catch (EditException e) {
            LOGGER.error("Failed to display the text area property.", e);
        }
    }

    @Override
    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        String contentType = getContentType();
        XWikiDocument doc = context.getDoc();

        if ("puretext".equals(contentType) && doc != null) {
            super.displayView(buffer, name, prefix, object, context);
        } else if ("velocitycode".equals(contentType) && context.getWiki() != null) {
            StringBuffer result = new StringBuffer();
            super.displayView(result, name, prefix, object, context);
            if (getObjectDocumentSyntax(object, context).equals(Syntax.XWIKI_1_0)) {
                buffer.append(context.getWiki().parseContent(result.toString(), context));
            } else {
                // Don't do anything since this mode is deprecated and not supported in the new rendering.
                buffer.append(result);
            }
        } else {
            StringBuffer result = new StringBuffer();
            super.displayView(result, name, prefix, object, context);
            if (doc != null) {
                String syntax = getObjectDocumentSyntax(object, context).toIdString();
                buffer.append(context.getDoc().getRenderedContent(result.toString(), syntax, context));
            } else {
                buffer.append(result);
            }
        }
    }

    /**
     * @return the syntax for the document to which the passed objects belongs to or the XWiki Syntax 1.0 if the object
     *         document cannot be retrieved
     */
    private Syntax getObjectDocumentSyntax(BaseCollection object, XWikiContext context)
    {
        Syntax syntax;

        try {
            XWikiDocument doc = object.getOwnerDocument();
            if (doc == null) {
                doc = context.getWiki().getDocument(object.getDocumentReference(), context);
            }

            syntax = doc.getSyntax();
        } catch (Exception e) {
            // Used to convert a Document Reference to string (compact form without the wiki part if it matches the
            // current wiki).
            EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer =
                Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
            LOGGER.warn("Error while getting the syntax corresponding to object ["
                + compactWikiEntityReferenceSerializer.serialize(object.getDocumentReference())
                + "]. Defaulting to using XWiki 1.0 syntax. Internal error [" + e.getMessage() + "]");
            syntax = Syntax.XWIKI_1_0;
        }

        return syntax;
    }
}
