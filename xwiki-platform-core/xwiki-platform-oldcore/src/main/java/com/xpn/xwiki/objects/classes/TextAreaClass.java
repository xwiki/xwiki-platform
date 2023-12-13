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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.edit.EditException;
import org.xwiki.edit.Editor;
import org.xwiki.edit.EditorManager;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxContent;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.velocity.VelocityEvaluator;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.web.Utils;

public class TextAreaClass extends StringClass
{
    private static final String FAILED_VELOCITY_EXECUTION_WARNING =
        "Failed to execute velocity code in text area property [{}]: [{}]";

    private static final String RESTRICTED = "restricted";

    /**
     * Possible values for the editor meta property.
     * <p>
     * Indicates which editor should be used to manipulate the content of the property.
     */
    public enum EditorType
    {
        /**
         * Plain text without any known syntax.
         */
        PURE_TEXT("PureText"),

        /**
         * Edit wiki syntax using a text editor.
         */
        TEXT("Text"),

        /**
         * Edit wiki syntax using a visual editor.
         */
        WYSIWYG("Wysiwyg");

        private static final Map<String, EditorType> editorTypeMap =
            Arrays.stream(EditorType.values()).collect(Collectors.toMap(e -> e.value.toLowerCase(), e -> e));

        private final String value;

        /**
         * Retreive the {@link EditorType} based on its value.
         * <p>
         * The search is case insensitive.
         *
         * @param value the value of the editor type
         * @return the editor type matching the value or null
         * @since 10.7RC1
         */
        public static EditorType getByValue(String value)
        {
            return value != null ? editorTypeMap.get(value.toLowerCase()) : null;
        }

        private EditorType(String value)
        {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return this.value;
        }
    }

    /**
     * Possible values for the contenttype meta property.
     * <p>
     * Indicates what kind of content this field contains (wiki, plain text, etc.).
     * 
     * @since 8.3
     */
    public enum ContentType
    {
        /**
         * Plain text without any known syntax.
         */
        PURE_TEXT("PureText"),

        /**
         * Wiki content.
         */
        WIKI_TEXT("FullyRenderedText"),

        /**
         * Velocity content.
         */
        VELOCITY_CODE("VelocityCode"),

        /**
         * Velocity content producing wiki content.
         * 
         * @since 13.0
         */
        VELOCITYWIKI("VelocityWiki");

        private static final Map<String, ContentType> contentTypeMap =
            Arrays.stream(ContentType.values()).collect(Collectors.toMap(c -> c.value.toLowerCase(), c -> c));

        private final String value;

        /**
         * Retreive the {@link ContentType} based on its value.
         * <p>
         * The search is case insensitive.
         *
         * @param value the value of the content type
         * @return the content type matching the value or null
         * @since 10.7RC1
         */
        public static ContentType getByValue(String value)
        {
            return value != null ? contentTypeMap.get(value.toLowerCase()) : null;
        }

        private ContentType(String value)
        {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return this.value;
        }
    }

    private static final long serialVersionUID = 1L;

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

    /**
     * @param contentType the content type
     * @return the editor type compatible with the passed content type, null if several are compatible
     */
    public static EditorType getEditorType(ContentType contentType, EditorType def)
    {
        if (contentType != null && contentType != ContentType.WIKI_TEXT) {
            return EditorType.PURE_TEXT;
        }

        return def;
    }

    /**
     * @param contentType the content type value
     * @param def the current editor type
     * @return the editor type compatible with the passed content type, def if several are compatible
     * @since 10.7RC1
     */
    public static EditorType getEditorType(String contentType, EditorType def)
    {
        return getEditorType(ContentType.getByValue(contentType), def);
    }

    /**
     * @param editorType the editor type
     * @return the content type compatible with the passed editor type, null if several are compatible
     */
    public static ContentType getContentType(EditorType editorType, ContentType def)
    {
        if (editorType != EditorType.PURE_TEXT) {
            return ContentType.WIKI_TEXT;
        }

        return def;
    }

    @Override
    public BaseProperty newProperty()
    {
        BaseProperty<?> property = new LargeStringProperty();
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
        String editor = getStringValue("editor").toLowerCase();
        if (EditorType.getByValue(editor) == null) {
            EditorType compatibleEditor = getEditorType(getContentType(), null);
            if (compatibleEditor != null) {
                return compatibleEditor.value.toLowerCase();
            }
        }

        return editor;
    }

    /**
     * Sets the editor meta property.
     * 
     * @param editor the editor type
     * @since 8.2RC1
     */
    public void setEditor(String editor)
    {
        setStringValue("editor", editor);
    }

    /**
     * Sets the editor meta property.
     * 
     * @param editorType the editor type
     * @since 8.3
     */
    public void setEditor(EditorType editorType)
    {
        setEditor(editorType.toString());

        // Make sure the content type is compatible
        ContentType compatible = getContentType(editorType, null);
        if (compatible != null) {
            setContentType(compatible);
        }
    }

    public String getContentType()
    {
        String result = getStringValue("contenttype").toLowerCase();
        if (result.isEmpty()) {
            result = ContentType.WIKI_TEXT.toString().toLowerCase();
        }

        return result;
    }

    public void setContentType(String contentType)
    {
        setStringValue("contenttype", contentType);
    }

    /**
     * @param contentType the content type
     * @since 8.3
     */
    public void setContentType(ContentType contentType)
    {
        setContentType(contentType.toString());

        // Make sure the editor type is compatible
        EditorType compatible = getEditorType(contentType, null);
        if (compatible != null) {
            setEditor(compatible);
        }
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

        return contentType != null && !contentType.equals("puretext") && !contentType.equals("velocitycode");
    }

    /**
     * Indicate if the content of this property should be executed in a restricted content (provided the type indicate
     * that this content should be executed).
     * 
     * @return true if the content of this property should be executed in a restricted content, false otherwise
     * @since 14.10
     * @since 14.4.7
     * @since 13.10.11
     */
    public boolean isRestricted()
    {
        return getIntValue(RESTRICTED, 0) == 1;
    }

    /**
     * Indicate if the content of this property should be executed in a restricted content (provided the type indicate
     * that this content should be executed).
     * 
     * @param restricted true if the content of this property should be executed in a restricted content, false
     *            otherwise
     * @since 14.10
     * @since 14.4.7
     * @since 13.10.11
     */
    public void setRestricted(boolean restricted)
    {
        setIntValue(RESTRICTED, restricted ? 1 : 0);
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        String editorType = getEditorType(context);
        EditorManager editorManager = Utils.getComponent(EditorManager.class);
        Editor<SyntaxContent> editor = editorManager.getDefaultEditor(SyntaxContent.class, editorType);
        XWikiDocument ownerDocument = getObjectDocument(object, context);
        Map<String, Object> parameters = new HashMap<>();
        String fieldName = prefix + name;
        parameters.put("id", fieldName);
        parameters.put("name", fieldName);
        parameters.put("cols", getSize());
        parameters.put("rows", getRows());
        parameters.put("disabled", isDisabled());
        parameters.put(RESTRICTED, isRestricted() || (ownerDocument != null && ownerDocument.isRestricted()));
        parameters.put("sourceDocumentReference", object.getDocumentReference());
        Syntax syntax = null;
        String contentType = getContentType();

        // We set the syntax by first checking the content type: if it's pure text or velocity code
        // the syntax is necessarily plain syntax.
        // Else we check if the wanted editor is puretext: in such case we also consider that the syntax is plain/text
        // finally we fallback on actual document syntax.
        // FIXME: if the content type is WIKI_TEXT we should probably force the syntax to Wiki syntax, but which one:
        // 2.0, 2.1?
        if (StringUtils.equalsIgnoreCase(ContentType.PURE_TEXT.toString(), contentType)
            || StringUtils.equalsIgnoreCase(ContentType.VELOCITY_CODE.toString(), contentType))
        {
            syntax = Syntax.PLAIN_1_0;
        } else {
            syntax = "puretext".equals(editorType) ? Syntax.PLAIN_1_0 : getObjectDocumentSyntax(object, context);
        }
        SyntaxContent syntaxContent = new SyntaxContent(object.getStringValue(name), syntax);
        try {
            buffer.append(editor.render(syntaxContent, parameters));
        } catch (EditException e) {
            LOGGER.error("Failed to display the text area property.", e);
        }
    }

    @Override
    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object,
        XWikiContext context)
    {
        displayView(buffer, name, prefix, object, true, context);
    }

    @Override
    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, boolean isolated,
        XWikiContext context)
    {
        String contentTypeString = getContentType();
        ContentType contentType = ContentType.getByValue(contentTypeString);

        if (contentType == ContentType.PURE_TEXT) {
            super.displayView(buffer, name, prefix, object, context);
        } else if (contentType == ContentType.VELOCITY_CODE) {
            displayVelocityCode(buffer, name, prefix, object, context);
        } else {
            BaseProperty<?> property = (BaseProperty<?>) object.safeget(name);
            if (property != null) {
                String content = property.toText();
                XWikiDocument sdoc = getObjectDocument(object, context);

                if (contentType == ContentType.VELOCITYWIKI) {
                    content = maybeEvaluateContent(name, isolated, content, sdoc);
                }

                if (sdoc != null) {
                    sdoc = ensureContentAuthorIsMetadataAuthor(sdoc);

                    buffer.append(
                        context.getDoc().getRenderedContent(content, sdoc.getSyntax(), isRestricted(), sdoc,
                            isolated, context));
                } else {
                    buffer.append(XMLUtils.escapeElementText(content));
                }
            }
        }
    }

    private static XWikiDocument ensureContentAuthorIsMetadataAuthor(XWikiDocument sdoc)
    {
        XWikiDocument result;

        // Make sure the right author is used to execute the textarea
        // Clone the document to avoid changing the cached document instance
        if (!Objects.equals(sdoc.getAuthors().getEffectiveMetadataAuthor(), sdoc.getAuthors().getContentAuthor())) {
            result = sdoc.clone();
            result.getAuthors().setContentAuthor(sdoc.getAuthors().getEffectiveMetadataAuthor());
        } else {
            result = sdoc;
        }

        return result;
    }

    private String maybeEvaluateContent(String name, boolean isolated, String content, XWikiDocument sdoc)
    {
        if (sdoc != null) {
            // Start with a pass of Velocity
            // TODO: maybe make velocity+wiki a syntax so that getRenderedContent can directly take care
            // of that
            AuthorExecutor authorExecutor = Utils.getComponent(AuthorExecutor.class);
            VelocityEvaluator velocityEvaluator = Utils.getComponent(VelocityEvaluator.class);
            try {
                return authorExecutor.call(() -> {
                    String result;
                    // Check script right inside the author executor as otherwise the context author might not be
                    // correct.
                    if (isDocumentAuthorAllowedToEvaluateScript(sdoc)) {
                        result = velocityEvaluator.evaluateVelocityNoException(content,
                            isolated ? sdoc.getDocumentReference() : null);
                    } else {
                        result = content;
                    }
                    return result;
                }, sdoc.getAuthorReference(), sdoc.getDocumentReference());
            } catch (Exception e) {
                LOGGER.warn(FAILED_VELOCITY_EXECUTION_WARNING, name, ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return content;
    }

    private void displayVelocityCode(StringBuffer buffer, String name, String prefix, BaseCollection<?> object,
        XWikiContext context)
    {
        StringBuffer result = new StringBuffer();
        super.displayView(result, name, prefix, object, context);
        XWikiDocument sdoc = getObjectDocument(object, context);
        if (getObjectDocumentSyntax(object, context).equals(Syntax.XWIKI_1_0) && sdoc != null) {
            try {
                Utils.getComponent(AuthorExecutor.class).call(() -> {
                    // Check script right inside the author executor as otherwise the context author might not be
                    // correct.
                    if (isDocumentAuthorAllowedToEvaluateScript(sdoc)) {
                        buffer.append(context.getWiki().parseContent(result.toString(), context));
                    } else {
                        buffer.append(result);
                    }
                    return null;
                }, sdoc.getAuthorReference(), sdoc.getDocumentReference());
            } catch (Exception e) {
                LOGGER.warn(FAILED_VELOCITY_EXECUTION_WARNING, name, ExceptionUtils.getRootCauseMessage(e));
                buffer.append(result);
            }
        } else {
            // Don't do anything since this mode is deprecated and not supported in the new rendering.
            buffer.append(result);
        }
    }

    private boolean isDocumentAuthorAllowedToEvaluateScript(XWikiDocument document)
    {
        boolean isAllowed = !isRestricted() && !document.isRestricted();

        if (isAllowed) {
            ContextualAuthorizationManager authorization = Utils.getComponent(ContextualAuthorizationManager.class);
            isAllowed = authorization.hasAccess(Right.SCRIPT);
        }

        return isAllowed;
    }

    private XWikiDocument getObjectDocument(BaseCollection<?> object, XWikiContext context)
    {
        try {
            XWikiDocument doc = object.getOwnerDocument();
            if (doc == null) {
                doc = context.getWiki().getDocument(object.getDocumentReference(), context);
            }

            return doc;
        } catch (Exception e) {
            // Used to convert a Document Reference to string (compact form without the wiki part if it matches
            // the current wiki).
            LOGGER.warn(
                "Error while getting the syntax corresponding to object [{}]. "
                    + "Defaulting to using XWiki 1.0 syntax. Internal error [{}]",
                object.getReference(), ExceptionUtils.getRootCauseMessage(e));
        }

        return null;
    }

    private Syntax getObjectDocumentSyntax(BaseCollection<?> object, XWikiContext context)
    {
        XWikiDocument doc = getObjectDocument(object, context);

        return doc != null && doc.getSyntax() != null ? doc.getSyntax() : Syntax.XWIKI_1_0;
    }
}
