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
package com.xpn.xwiki.doc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.io.OutputFormat;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.HeaderBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.match.ClassBlockMatcher;
import org.xwiki.rendering.listener.HeaderLevel;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.DocumentSection;
import com.xpn.xwiki.internal.xml.XMLWriter;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.web.EditForm;
import com.xpn.xwiki.web.Utils;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.doc.XWikiDocument} class.
 *
 * @version $Id$
 */
privileged public aspect XWikiDocumentCompatibilityAspect
{
    /**
     * @deprecated since 3.0M3 use {@code Syntax.XWIKI_1_0} instead
     */
    @Deprecated
    public static final String XWikiDocument.XWIKI10_SYNTAXID = Syntax.XWIKI_1_0.toIdString();

    /**
     * @deprecated since 3.0M3 use {@code Syntax.XWIKI_2_0} instead
     */
    @Deprecated
    public static final String XWikiDocument.XWIKI20_SYNTAXID = Syntax.XWIKI_2_0.toIdString();

    /**
     * @deprecated use setStringListValue or setDBStringListProperty
     */
    @Deprecated
    public void XWikiDocument.setListValue(String className, String fieldName, List value)
    {
        BaseObject bobject = getObject(className);
        if (bobject == null) {
            bobject = new BaseObject();
            addObject(className, bobject);
        }
        bobject.setName(getFullName());
        bobject.setClassName(className);
        bobject.setListValue(fieldName, value);
        setMetaDataDirty(true);
    }
    
    /**
     * This method to split section according to title.
     * 
     * @return the sections in the current document
     * @throws XWikiException
     * @deprecated use {@link #getSections()} instead, since 1.6M1
     */
    @Deprecated
    public List<DocumentSection> XWikiDocument.getSplitSectionsAccordingToTitle() throws XWikiException
    {
        return getSections();
    }
    
    /**
     * @deprecated use {@link #getUniqueLinkedPages(XWikiContext)}
     */
    @Deprecated
    public List<String> XWikiDocument.getLinkedPages(XWikiContext context)
    {
        return new ArrayList<String>(getUniqueLinkedPages(context));
    }
    
    /**
     * @deprecated use {@link #getUniqueWikiLinkedPages(XWikiContext)} instead
     */
    @Deprecated
    public List<XWikiLink> XWikiDocument.getLinks(XWikiContext context) throws XWikiException
    {
        return getWikiLinkedPages(context);
    }

    /**
     * @deprecated use {@link #getUniqueWikiLinkedPages(XWikiContext)} instead
     */
    @Deprecated
    public List<XWikiLink> XWikiDocument.getWikiLinkedPages(XWikiContext context) throws XWikiException
    {
        return new ArrayList<XWikiLink>(getUniqueWikiLinkedPages(context));
    }
    
    /**
     * @deprecated use {@link #getBackLinkedPages(XWikiContext)} instead
     */
    @Deprecated
    public List<String> XWikiDocument.getBacklinks(XWikiContext context) throws XWikiException
    {
        return getBackLinkedPages(context);
    }
    
    /**
     * @param text the text to render
     * @param context the XWiki Context object
     * @return the given text rendered in the context of this document
     * @deprecated since 1.6M1 use {@link #getRenderedContent(String, String, com.xpn.xwiki.XWikiContext)}
     */
    @Deprecated
    public String XWikiDocument.getRenderedContent(String text, XWikiContext context)
    {
        return getRenderedContent(text, Syntax.XWIKI_1_0.toIdString(), context);
    }

    @Deprecated
    public String XWikiDocument.displaySearch(PropertyClass pclass, String prefix, XWikiCriteria criteria, XWikiContext context)
    {
        return (pclass == null) ? "" : pclass.displaySearch(pclass.getName(), prefix, criteria, context);
    }

    /**
     * @param context the XWiki context used to get access to the com.xpn.xwiki.render.XWikiRenderingEngine object
     * @return the document title. If a title has not been provided, look for a section title in the document's content
     *         and if not found return the page name. The returned title is also interpreted which means it's allowed to
     *         use Velocity, Groovy, etc syntax within a title.
     * @deprecated use {@link #getRenderedTitle(Syntax, XWikiContext)} instead
     */
    @Deprecated
    public String XWikiDocument.getDisplayTitle(XWikiContext context)
    {
        return getRenderedTitle(Syntax.XHTML_1_0, context);
    }

    /**
     * @deprecated since 2.2M1, use {@link #getXClass()} instead
     */
    @Deprecated
    public BaseClass XWikiDocument.getxWikiClass()
    {
        return getXClass();
    }

    /**
     * @deprecated since 2.2M2 use {@link #addXObjectFromRequest(XWikiContext)}
     */
    @Deprecated
    public BaseObject XWikiDocument.addObjectFromRequest(XWikiContext context) throws XWikiException
    {
        return addXObjectFromRequest(context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #setXObjectsToRemove(List)} instead
     */
    @Deprecated
    public void XWikiDocument.setObjectsToRemove(ArrayList<BaseObject> objectsToRemove)
    {
        setXObjectsToRemove(objectsToRemove);
    }

    /**
     * @deprecated since 2.2M1, use {@link #setXClass(BaseClass)} instead
     */
    @Deprecated
    public void XWikiDocument.setxWikiClass(BaseClass xwikiClass)
    {
        setXClass(xwikiClass);
    }

    /**
     * @deprecated since 2.2M1 use {@link #getXObjects()} instead. Warning: if you used to modify the returned Map note
     *             that since 2.2M1 this will no longer work and you'll need to call the setXObject methods instead (or
     *             setxWikiObjects()). Obviously the best is to move to the new API.
     */
    @Deprecated
    public Map<String, Vector<BaseObject>> XWikiDocument.getxWikiObjects()
    {
        // Use a liked hash map to ensure we keep the order stored from the internal objects map.
        Map<String, Vector<BaseObject>> objects = new LinkedHashMap<String, Vector<BaseObject>>();

        for (Map.Entry<DocumentReference, List<BaseObject>> entry : getXObjects().entrySet()) {
            objects.put(getCompactWikiEntityReferenceSerializer().serialize(entry.getKey()), new Vector<BaseObject>(
                entry.getValue()));
        }

        return objects;
    }

    /**
     * @deprecated since 2.2M1 use {@link #setXObjects(Map)} instead
     */
    @Deprecated
    public void XWikiDocument.setxWikiObjects(Map<String, Vector<BaseObject>> objects)
    {
        // Use a liked hash map to ensure we keep the order stored from the internal objects map.
        Map<DocumentReference, List<BaseObject>> newObjects = new LinkedHashMap<DocumentReference, List<BaseObject>>();

        for (Map.Entry<String, Vector<BaseObject>> entry : objects.entrySet()) {
            newObjects.put(resolveClassReference(entry.getKey()), new ArrayList<BaseObject>(entry.getValue()));
        }

        setXObjects(newObjects);
    }

    /**
     * @deprecated since 2.2M1 use {@link #getXClasses(XWikiContext)} instead
     */
    @Deprecated
    public List<BaseClass> XWikiDocument.getxWikiClasses(XWikiContext context)
    {
        return getXClasses(context);
    }

    /**
     * @deprecated since 2.2M1 use {@link #setXObjects(DocumentReference, List)} instead
     */
    @Deprecated
    public void XWikiDocument.setObjects(String className, Vector<BaseObject> objects)
    {
        setXObjects(resolveClassReference(className), new ArrayList<BaseObject>(objects));
    }

    /**
     * @deprecated since 3.2M3, use {@link #getRenderedTitle(Syntax, XWikiContext)} instead
     */
    @Deprecated
    public String XWikiDocument.extractTitle()
    {
        String title = "";

        try {
            if (is10Syntax()) {
                title = extractTitle10();
            } else {
                List<HeaderBlock> blocks =
                    getXDOM().getBlocks(new ClassBlockMatcher(HeaderBlock.class), Block.Axes.DESCENDANT);
                if (!blocks.isEmpty()) {
                    HeaderBlock header = blocks.get(0);
                    if (header.getLevel().compareTo(HeaderLevel.LEVEL2) <= 0) {
                        XDOM headerXDOM = new XDOM(Collections.<Block> singletonList(header));

                        // transform
                        TransformationContext context =
                            new TransformationContext(headerXDOM, getSyntax(), isRestricted());
                        Utils.getComponent(TransformationManager.class).performTransformations(headerXDOM, context);

                        // render
                        Block headerBlock = headerXDOM.getChildren().get(0);
                        if (headerBlock instanceof HeaderBlock) {
                            title = renderXDOM(new XDOM(headerBlock.getChildren()), Syntax.XHTML_1_0);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Don't stop when there's a problem rendering the title.
        }

        return title;
    }

    /**
     * Regex for finding the first level 1 or 2 heading in the document title, to be used as the document title.
     * 
     * @deprecated since 3.2M3
     **/
    @Deprecated
    private static final Pattern HEADING_PATTERN_10 = Pattern.compile("^\\s*+1(?:\\.1)?\\s++(.++)$", Pattern.MULTILINE);

    /**
     * @return the first level 1 or level 1.1 title text in the document's content or "" if none are found
     * @deprecated since 3.2M3
     */
    @Deprecated
    private String XWikiDocument.extractTitle10()
    {
        String content = getContent();
        Matcher m = HEADING_PATTERN_10.matcher(content);
        if (m.find()) {
            return m.group(1).trim();
        }

        return "";
    }

    /**
     * Convert a {@link Document} into an XML string. You should prefer
     * {@link #toXML(OutputStream, boolean, boolean, boolean, boolean, XWikiContext)} or
     * {@link #toXML(com.xpn.xwiki.internal.xml.XMLWriter, boolean, boolean, boolean, boolean, XWikiContext)} when
     * possible to avoid memory load.
     * 
     * @param doc the {@link Document} to convert to a String
     * @param context current XWikiContext
     * @return an XML representation of the {@link Document}
     * @deprecated this method has nothing to do here and is apparently unused
     */
    @Deprecated
    public String XWikiDocument.toXML(Document doc, XWikiContext context)
    {
        String encoding = context.getWiki().getEncoding();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            XMLWriter wr = new XMLWriter(os, new OutputFormat("", true, encoding));
            wr.write(doc);
            return os.toString(encoding);
        } catch (IOException e) {
            LOGGER.error("Exception while doc.toXML", e);
            return "";
        }
    }

    /**
     * @deprecated since 2.2M1 use {@link #cloneXObjects(XWikiDocument)} instead
     */
    @Deprecated
    public void XWikiDocument.clonexWikiObjects(XWikiDocument templatedoc)
    {
        cloneXObjects(templatedoc);
    }
    
    /**
     * @deprecated since 5.2M1 use {@link #removeAttachment(XWikiAttachment)} instead
     */
    @Deprecated
    public void XWikiDocument.deleteAttachment(XWikiAttachment attachment, XWikiContext context) throws XWikiException
    {
        deleteAttachment(attachment, true, context);
    }

    /**
     * @deprecated since 5.2M1 use {@link #removeAttachment(XWikiAttachment)} instead
     */
    @Deprecated
    public void XWikiDocument.deleteAttachment(XWikiAttachment attachment, boolean toRecycleBin, XWikiContext context)
        throws XWikiException
    {
        deleteAttachment(attachment, true, toRecycleBin, context);
    }

    /**
     * @deprecated since 5.2M1 use {@link #removeAttachment(XWikiAttachment)} instead
     */
    @Deprecated
    private void XWikiDocument.deleteAttachment(XWikiAttachment attachment, boolean saveDocument, boolean toRecycleBin,
        XWikiContext context) throws XWikiException
    {
        removeAttachment(attachment, toRecycleBin);

        if (saveDocument) {
            // Save the document
            context.getWiki().saveDocument(this, "Deleted attachment [" + attachment.getFilename() + "]", context);
        }
    }

    private String XWikiDocument.serializeReference(DocumentReference reference, EntityReferenceSerializer<String> serializer,
        DocumentReference defaultReference)
    {
        XWikiContext xcontext = getXWikiContext();

        String originalWikiName = xcontext.getWikiId();
        XWikiDocument originalCurentDocument = xcontext.getDoc();
        try {
            xcontext.setWikiId(defaultReference.getWikiReference().getName());
            xcontext.setDoc(new XWikiDocument(defaultReference));

            return serializer.serialize(reference);
        } finally {
            xcontext.setDoc(originalCurentDocument);
            xcontext.setWikiId(originalWikiName);
        }
    }

    /**
     * Convert a full document reference into the proper relative document reference (wiki part is removed if it's the
     * same as document wiki) to store as parent.
     * 
     * @deprecated since 2.2.3 use {@link #setParentReference(org.xwiki.model.reference.EntityReference)} instead
     */
    @Deprecated
    public void XWikiDocument.setParentReference(DocumentReference parentReference)
    {
        if (parentReference != null) {
            setParent(serializeReference(parentReference, getCompactWikiEntityReferenceSerializer(),
                getDocumentReference()));
        } else {
            setParentReference((EntityReference) null);
        }
    }

    /**
     * @deprecated since 2.2M2 use {@link #rename(DocumentReference, XWikiContext)}
     */
    @Deprecated
    public void XWikiDocument.rename(String newDocumentName, XWikiContext context) throws XWikiException
    {
        rename(newDocumentName, getBackLinkedPages(context), context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #rename(DocumentReference, java.util.List, com.xpn.xwiki.XWikiContext)}
     */
    @Deprecated
    public void XWikiDocument.rename(String newDocumentName, List<String> backlinkDocumentNames, XWikiContext context)
        throws XWikiException
    {
        rename(newDocumentName, backlinkDocumentNames, getChildren(context), context);
    }

    /**
     * @deprecated since 2.2M2 use {@link #rename(DocumentReference, List, List, com.xpn.xwiki.XWikiContext)}
     */
    @Deprecated
    public void XWikiDocument.rename(String newDocumentName, List<String> backlinkDocumentNames,
        List<String> childDocumentNames, XWikiContext context) throws XWikiException
    {
        List<DocumentReference> backlinkDocumentReferences = new ArrayList<DocumentReference>();
        for (String backlinkDocumentName : backlinkDocumentNames) {
            backlinkDocumentReferences.add(getCurrentMixedDocumentReferenceResolver().resolve(backlinkDocumentName));
        }

        List<DocumentReference> childDocumentReferences = new ArrayList<DocumentReference>();
        for (String childDocumentName : childDocumentNames) {
            childDocumentReferences.add(getCurrentMixedDocumentReferenceResolver().resolve(childDocumentName));
        }

        rename(getCurrentMixedDocumentReferenceResolver().resolve(newDocumentName), backlinkDocumentReferences,
            childDocumentReferences, context);
    }

    /**
     * Read the document data from the template without performing permission check on the template.
     *
     * @param eform the form containing a template information.
     * @param context current context
     * @throws XWikiException in case of problem to read the information.
     * @deprecated Since 14.1RC1 prefer using {@link #readFromTemplate(DocumentReference, XWikiContext)} and be careful
     *             to check the template rights before.
     */
    @Deprecated
    public void XWikiDocument.readFromTemplate(EditForm eform, XWikiContext context) throws XWikiException
    {
        String template = eform.getTemplate();
        readFromTemplate(template, context);
    }

    /**
     * @deprecated since 2.2M1 use {@link #readFromTemplate(DocumentReference, XWikiContext)} instead
     */
    @Deprecated
    public void XWikiDocument.readFromTemplate(String template, XWikiContext context) throws XWikiException
    {
        // Keep the same behavior for backward compatibility
        DocumentReference templateDocumentReference = null;
        if (StringUtils.isNotEmpty(template)) {
            templateDocumentReference = getCurrentMixedDocumentReferenceResolver().resolve(template);
        }
        readFromTemplate(templateDocumentReference, context);
    }

    /**
     * Rename the current document and all the backlinks leading to it. Will also change parent field in all documents
     * which list the document we are renaming as their parent.
     * <p>
     * See {@link #rename(DocumentReference, List, List, XWikiContext)} for more details.
     *
     * @param newDocumentReference the new document reference
     * @param context the ubiquitous XWiki Context
     * @throws XWikiException in case of an error
     * @since 2.2M2
     * @deprecated use
     *     {@link XWiki#renameDocument(DocumentReference, DocumentReference, boolean, List, List, XWikiContext)} instead
     */
    @Deprecated(since = "12.5RC1")
    public void XWikiDocument.rename(DocumentReference newDocumentReference, XWikiContext context) throws XWikiException
    {
        rename(newDocumentReference, getBackLinkedReferences(context), context);
    }

    /**
     * Rename the current document and all the links pointing to it in the list of passed backlink documents. The
     * renaming algorithm takes into account the fact that there are several ways to write a link to a given page and
     * all those forms need to be renamed. For example the following links all point to the same page:
     * <ul>
     * <li>[Page]</li>
     * <li>[Page?param=1]</li>
     * <li>[currentwiki:Page]</li>
     * <li>[CurrentSpace.Page]</li>
     * <li>[currentwiki:CurrentSpace.Page]</li>
     * </ul>
     * <p>
     * Note: links without a space are renamed with the space added and all documents which have the document being
     * renamed as parent have their parent field set to "currentwiki:CurrentSpace.Page".
     * </p>
     *
     * @param newDocumentReference the new document reference
     * @param backlinkDocumentReferences the list of references of documents to parse and for which links will be
     *            modified to point to the new document reference
     * @param context the ubiquitous XWiki Context
     * @throws XWikiException in case of an error
     * @since 2.2M2
     * @deprecated use
     *     {@link XWiki#renameDocument(DocumentReference, DocumentReference, boolean, List, List, XWikiContext)} instead
     */
    @Deprecated(since = "12.5RC1")
    public void XWikiDocument.rename(DocumentReference newDocumentReference,
        List<DocumentReference> backlinkDocumentReferences, XWikiContext context) throws XWikiException
    {
        rename(newDocumentReference, backlinkDocumentReferences, getChildrenReferences(context), context);
    }

    /**
     * Same as {@link #rename(DocumentReference, List, XWikiContext)} but the list of documents having the current
     * document as their parent is passed in parameter.
     *
     * @param newDocumentReference the new document reference
     * @param backlinkDocumentReferences the list of references of documents to parse and for which links will be
     *            modified to point to the new document reference
     * @param childDocumentReferences the list of references of document whose parent field will be set to the new
     *            document reference
     * @param context the ubiquitous XWiki Context
     * @throws XWikiException in case of an error
     * @since 2.2M2
     * @deprecated use
     *     {@link XWiki#renameDocument(DocumentReference, DocumentReference, boolean, List, List, XWikiContext)} instead
     */
    @Deprecated(since = "12.5RC1")
    public void XWikiDocument.rename(DocumentReference newDocumentReference,
        List<DocumentReference> backlinkDocumentReferences, List<DocumentReference> childDocumentReferences,
        XWikiContext context) throws XWikiException
    {
        // TODO: Do all this in a single DB transaction as otherwise the state will be unknown if
        // something fails in the middle...

        // TODO: Why do we verify if the document has just been created and not been saved.
        // If the user is trying to rename to the same name... In that case, simply exits for efficiency.
        if (isNew() || getDocumentReference().equals(newDocumentReference)) {
            return;
        }
        context.getWiki().renameByCopyAndDelete(this,
            newDocumentReference,
            backlinkDocumentReferences,
            childDocumentReferences, context);
    }

    @Deprecated(since = "16.0RC1")
    public String XWikiDocument.displayTooltip(String fieldname, XWikiContext context)
        {
            try {
                BaseObject object = getXObject();
                if (object == null) {
                    object = getFirstObject(fieldname, context);
                }
                return displayTooltip(fieldname, object, context);
            } catch (Exception e) {
                return "";
            }
        }

        @Deprecated(since = "16.0RC1")
        public String XWikiDocument.displayTooltip(String fieldname, BaseObject obj, XWikiContext context)
        {
            String result = "";

            try {
                PropertyClass pclass = (PropertyClass) obj.getXClass(context).get(fieldname);
                String tooltip = pclass.getTooltip(context);
                if ((tooltip != null) && (!tooltip.trim().equals(""))) {
                    String img = "<img src=\"" + context.getWiki().getSkinFile("info.gif", context)
                        + "\" class=\"tooltip_image\" align=\"middle\" />";
                    result = context.getWiki().addTooltip(img, tooltip, context);
                }
            } catch (Exception e) {

            }

            return result;
        }
}
