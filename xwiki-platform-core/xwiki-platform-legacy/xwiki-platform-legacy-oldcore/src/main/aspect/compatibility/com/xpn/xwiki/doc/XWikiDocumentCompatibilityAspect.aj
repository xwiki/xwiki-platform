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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.model.reference.DocumentReference;
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
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.web.Utils;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.doc.XWikiDocument} class.
 *
 * @version $Id$
 */
public aspect XWikiDocumentCompatibilityAspect
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
        setContentDirty(true);
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
            objects.put(this.compactWikiEntityReferenceSerializer.serialize(entry.getKey()), new Vector<BaseObject>(
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
                        TransformationContext context = new TransformationContext(headerXDOM, getSyntax());
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
}
