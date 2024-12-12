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
package com.xpn.xwiki.web;

import java.io.IOException;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.tika.mime.MediaType;
import org.xwiki.component.annotation.Component;
import org.xwiki.filter.FilterException;
import org.xwiki.filter.input.InputSource;
import org.xwiki.internal.filter.Importer;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSet;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.xar.XarException;
import org.xwiki.xar.XarPackage;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.filter.input.XWikiAttachmentContentInputSource;
import com.xpn.xwiki.plugin.packaging.DocumentInfo;
import com.xpn.xwiki.util.Util;

/**
 * XWiki Action responsible for importing XAR archives.
 *
 * @version $Id$
 */
@Component
@Named("import")
@Singleton
public class ImportAction extends XWikiAction
{
    private static final String TEMPLATE_ADMIN = "admin";

    private static final String TEMPLATE_IMPORTED = "imported";

    @Override
    public String render(XWikiContext context) throws XWikiException
    {
        String result = null;

        try {
            XWikiRequest request = context.getRequest();
            XWikiResponse response = context.getResponse();
            XWikiDocument doc = context.getDoc();
            String name = request.get("name");
            String action = request.get("action");

            if (!context.getWiki().getRightService().hasWikiAdminRights(context)) {
                context.put("message", "needadminrights");
                return "exception";
            }

            if (name == null) {
                return TEMPLATE_ADMIN;
            }

            if ("getPackageInfos".equals(action)) {
                getPackageInfos(doc.getAttachment(name), response, context);
            } else if ("import".equals(action)) {
                result = importPackage(doc.getAttachment(name), request, context);
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_EXPORT,
                "Exception while importing", e);
        }

        return result;
    }

    private void getPackageInfos(XWikiAttachment packFile, XWikiResponse response, XWikiContext xcontext)
        throws IOException, XWikiException, XarException
    {
        String encoding = xcontext.getWiki().getEncoding();
        response.setContentType(MediaType.APPLICATION_XML.toString());
        response.setCharacterEncoding(encoding);

        XarPackage xarPackage = new XarPackage(packFile.getContentInputStream(xcontext));

        xarPackage.write(response.getOutputStream(), encoding);
    }

    private String importPackage(XWikiAttachment packFile, XWikiRequest request, XWikiContext context)
        throws IOException, XWikiException, FilterException
    {
        String all = request.get("all");
        if (!"1".equals(all)) {
            importPackageFilterStream(packFile, request, context);
            if (!StringUtils.isBlank(request.getParameter("ajax"))) {
                // If the import is done from an AJAX request we don't want to return a whole HTML page,
                // instead we return "inline" the list of imported documents,
                // evaluating imported.vm template.
                return TEMPLATE_IMPORTED;
            } else {
                return TEMPLATE_ADMIN;
            }
        }

        return null;
    }

    private String getLocale(String pageName, XWikiRequest request)
    {
        return Util.normalizeLanguage(request.get("language_" + pageName));
    }

    private int getAction(String pageName, String language, XWikiRequest request)
    {
        String actionName = "action_" + pageName;
        if (!StringUtils.isBlank(language)) {
            actionName += ("_" + language);
        }
        String defaultAction = request.get(actionName);
        int iAction;
        if (StringUtils.isBlank(defaultAction)) {
            iAction = DocumentInfo.ACTION_OVERWRITE;
        } else {
            try {
                iAction = Integer.parseInt(defaultAction);
            } catch (Exception e) {
                iAction = DocumentInfo.ACTION_SKIP;
            }
        }

        return iAction;
    }

    private String getDocumentReference(String pageEntry)
    {
        return pageEntry.replaceAll(":[^:]*$", "");
    }

    private void importPackageFilterStream(XWikiAttachment packFile, XWikiRequest request, XWikiContext context)
        throws IOException, XWikiException, FilterException
    {
        // Define the data source to import
        @SuppressWarnings("resource")
        InputSource source = new XWikiAttachmentContentInputSource(packFile.getAttachmentContent(context));

        // Get the history handling stategy
        String historyStrategy = request.getParameter("historyStrategy");

        // Get the backup switch value
        boolean importAsBackup = StringUtils.equals(request.getParameter("importAsBackup"), "true");

        // Configure pages to import
        EntityReferenceSet entities;
        String[] pages = request.getParameterValues("pages");
        if (pages != null) {
            entities = new EntityReferenceSet();

            EntityReferenceResolver<String> resolver =
                Utils.getComponent(EntityReferenceResolver.TYPE_STRING, "relative");

            for (String pageEntry : pages) {
                if (StringUtils.isNotEmpty(pageEntry)) {
                    String locale = getLocale(pageEntry, request);
                    int iAction = getAction(pageEntry, locale, request);

                    String documentReference = getDocumentReference(pageEntry);
                    if (iAction == DocumentInfo.ACTION_OVERWRITE) {
                        entities.includes(new LocalDocumentReference(
                            resolver.resolve(documentReference, EntityType.DOCUMENT), LocaleUtils.toLocale(locale)));
                    }
                }
            }
        } else {
            entities = null;
        }

        // Execute the import
        Utils.getComponent(Importer.class).importXAR(source, entities, historyStrategy, importAsBackup, context);
    }
}
