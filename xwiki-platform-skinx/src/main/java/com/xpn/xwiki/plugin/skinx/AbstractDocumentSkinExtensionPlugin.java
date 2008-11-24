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
package com.xpn.xwiki.plugin.skinx;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.DocChangeRule;
import com.xpn.xwiki.notify.XWikiActionNotificationInterface;
import com.xpn.xwiki.notify.XWikiActionRule;
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.objects.classes.BaseClass;

/**
 * Abstract SX plugin for wiki-document-based extensions (Extensions written as object of a XWiki Extension class).
 * Provides a generic method to initialize the XWiki class upon plugin initialization if needed. Provide a notification
 * mecanism for extensions marked as "use-always".
 * 
 * @version $Id: $
 * @since 1.4
 * @see JsSkinExtensionPlugin
 * @see CssSkinExtensionPlugin
 */
public abstract class AbstractDocumentSkinExtensionPlugin extends SkinExtensionPlugin implements
    XWikiDocChangeNotificationInterface, XWikiActionNotificationInterface
{

    /** Log helper for logging messages in this class. */
    protected static final Log LOG = LogFactory.getLog(JsSkinExtensionPlugin.class);

    /**
     * A Map with wiki/database name as keys and sets of extensions to use always for this wiki as values
     */
    private Map<String, Set<String>> alwaysUsedExtensions;

    public AbstractDocumentSkinExtensionPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    protected abstract String getExtensionClassName();

    protected abstract String getExtensionName();

    /**
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#init(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void init(XWikiContext context)
    {
        super.init(context);
        alwaysUsedExtensions = new HashMap<String, Set<String>>();
        getExtensionClass(context);
        context.getWiki().getNotificationManager().addGeneralRule(new DocChangeRule(this));
        context.getWiki().getNotificationManager().addGeneralRule(new XWikiActionRule(this, true, true));
    }

    /**
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#virtualInit(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void virtualInit(XWikiContext context)
    {
        super.virtualInit(context);
        getExtensionClass(context);
    }

    /**
     * @param context the XWiki context
     * @return a set of extensions that should always be used for the context wiki. The extensions are lazily in a cache
     * for each wiki. If the extension set for the context wiki is null, then they will be looked up in the database and
     * added to the cache if any.  
     */
    public Set<String> getAlwaysUsedExtensions(XWikiContext context)
    {
        // retrieve the current wiki name from the XWiki context
        String currentWiki = !context.getDatabase().equals("") ? context.getDatabase() : context.getMainXWiki();
        // if we already have extensions defined for this wiki, we return them
        if (this.alwaysUsedExtensions.get(currentWiki) != null) {
            return this.alwaysUsedExtensions.get(currentWiki);
        } else {
            // otherwise, we look them up in the database.
            Set<String> extensions = new HashSet<String>();
            String query =
                ", BaseObject as obj, StringProperty as use where obj.className='" + getExtensionClassName() + "'"
                    + " and obj.name=doc.fullName and use.id.id=obj.id and use.id.name='use' and use.value='always'";
            try {
                for (String extension : context.getWiki().getStore().searchDocumentsNames(query, context)) {
                    try {
                        XWikiDocument doc = context.getWiki().getDocument(extension, context);
                        // only add the extension as being "always used" if the page holding it has been saved with programming rights.
                        if (context.getWiki().getRightService().hasProgrammingRights(doc, context)) {
                            extensions.add(extension);
                        }
                    } catch (XWikiException e1) {
                        LOG.error("Error while adding JS extension [" + extension
                            + "] as always used. It will be ignored", e1);
                    }
                }
                this.alwaysUsedExtensions.put(currentWiki, extensions);
                return extensions;
            } catch (XWikiException e) {
                LOG.error("Error while retrieving always used JS extensions", e);
                return Collections.emptySet();
            }
        }
    }

    @Override
    public void beginParsing(XWikiContext context)
    {
        super.beginParsing(context);
    }

    @Override
    public String endParsing(String content, XWikiContext context)
    {
        return super.endParsing(content, context);
    }

    public BaseClass getExtensionClass(XWikiContext context)
    {
        try {
            XWikiDocument doc = context.getWiki().getDocument(getExtensionClassName(), context);
            boolean needsUpdate = false;

            BaseClass bclass = doc.getxWikiClass();
            if (context.get("initdone") != null) {
                return bclass;
            }

            bclass.setName(getExtensionClassName());

            needsUpdate |= bclass.addTextField("name", "Name", 30);
            needsUpdate |= bclass.addTextAreaField("code", "Code", 50, 20);
            needsUpdate |= bclass.addStaticListField("use", "Use this extension", "onDemand=On demand|always=Always");
            needsUpdate |= bclass.addBooleanField("parse", "Parse content", "yesno");
            needsUpdate |= bclass.addStaticListField("cache", "Caching policy", "long|short|default|forbid");

            if (StringUtils.isBlank(doc.getAuthor())) {
                needsUpdate = true;
                doc.setAuthor("XWiki.Admin");
            }
            if (StringUtils.isBlank(doc.getCreator())) {
                needsUpdate = true;
                doc.setCreator("XWiki.Admin");
            }
            if (StringUtils.isBlank(doc.getParent())) {
                needsUpdate = true;
                doc.setParent("XWiki.XWikiClasses");
            }
            if (StringUtils.isBlank(doc.getContent())) {
                needsUpdate = true;
                doc.setContent("1 XWiki " + getExtensionName() + " Extension Class");
            }

            if (needsUpdate) {
                context.getWiki().saveDocument(doc, context);
            }
            return bclass;
        } catch (Exception ex) {
            LOG.error("Cannot initialize skin extenstion class [" + getExtensionClassName() + "]", ex);
        }
        return null;
    }

    /**
     * Notification method called upon document changed. This method is used to keep the {@link #alwaysUsedExtensions}
     * map consistent when the database changes. Upon each document save, it looks in the newly saved document
     * for an extension object. If one is found, then if the use field is set to "always" and the document has been
     * saved with programming rights, we put the document in the map (considering it could already be there). If those
     * last two conditions are not verified, we remove the document from the map (considering it could not have been there already).
     * Last, if the old document (before the save) contained an extension object, but the new one (after the save) does not,
     * it means the object has been deleted, so again we remove this document from the map.
     * 
     * @see XWikiNotificationInterface#notify(XWikiNotificationRule, XWikiDocument, XWikiDocument, int, XWikiContext)
     */
    public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc, int event,
        XWikiContext context)
    {
        boolean remove = false;
        if (newdoc.getObject(getExtensionClassName()) != null) {
            // new or already existing object
            if (newdoc.getObject(getExtensionClassName()).getStringValue("use").equals("always")) {
                if (context.getWiki().getRightService().hasProgrammingRights(newdoc, context)) {
                    this.getAlwaysUsedExtensions(context).add(newdoc.getFullName());
                    return;
                } else {
                    // in case the extension losed its programming rights upon this save.
                    remove = true;
                }
            } else {
                // remove if exists but use onDemand
                remove = true;
            }
        } else if (olddoc.getObject(getExtensionClassName()) != null) {
            // object removed
            remove = true;
        }
        if (remove) {
            this.getAlwaysUsedExtensions(context).remove(newdoc.getFullName());
        }
    }
    
    /**
     * Resets the list of "Use always" extensions every time a XAR is imported. This way, next time a page is rendered,
     * such extensions will be retrieved from databse, and if one has been imported during last import, it will be taken
     * into consideration. (We have to this since the importer does not generate DocChanged notification).
     * See http://jira.xwiki.org/jira/browse/XWIKI-2868
     * 
     * @see XWikiActionNotificationInterface#notify(XWikiNotificationRule, XWikiDocument, String, XWikiContext)
     */
    public void notify(XWikiNotificationRule rule, XWikiDocument doc, String action, XWikiContext context)
    {
        // if the action is not import, this notification does not concern this method.
        if (!action.equals("import")) {
            return;
        }
        // if the action is import, we check the request parameter to see if it is an actual XAR import.
        // if it is the case, we flush the always used extensions cache for the context wiki.
        if (context.getRequest().getParameter("action") != null 
            && context.getRequest().getParameter("action").equals("import")) {
            String currentWiki = !context.getDatabase().equals("") ? context.getDatabase() : context.getMainXWiki();
            this.alwaysUsedExtensions.remove(currentWiki);
        }
    }

}
