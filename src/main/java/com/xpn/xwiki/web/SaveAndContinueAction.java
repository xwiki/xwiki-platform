/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author ludovic
 * @author namphunghai
 * @author sdumitriu
 */
package com.xpn.xwiki.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

public class SaveAndContinueAction extends XWikiAction {
	public boolean propUpdate(XWikiContext context) throws XWikiException {
		XWiki xwiki = context.getWiki();
		XWikiDocument doc = context.getDoc();
		XWikiForm form = context.getForm();
		XWikiDocument olddoc = (XWikiDocument) doc.clone();

		// Prepare new class
		BaseClass bclass = doc.getxWikiClass();
		BaseClass bclass2 = (BaseClass) bclass.clone();
		bclass2.setFields(new HashMap());

		doc.setxWikiClass(bclass2);

		// Prepare a Map for field renames
		Map fieldsToRename = new HashMap();

		Iterator it = bclass.getFieldList().iterator();
		while (it.hasNext()) {
			PropertyClass property = (PropertyClass) it.next();
			PropertyClass origproperty = (PropertyClass) property.clone();
			String name = property.getName();
			Map map = ((EditForm) form).getObject(name);
			property.getxWikiClass(context).fromMap(map, property);
			String newname = property.getName();

			if (newname == null || newname.equals("") || !newname.matches("[\\w\\.\\-\\_]+")) {
				context.put("message", "propertynamenotcorrect");
				return true;
			}

			if (newname.indexOf(" ") != -1) {
				newname = newname.replaceAll(" ", "");
				property.setName(newname);
			}
			bclass2.addField(newname, property);
			if (!newname.equals(name)) {
				fieldsToRename.put(name, newname);
				bclass2.addPropertyForRemoval(origproperty);
			}
		}
		doc.renameProperties(bclass.getName(), fieldsToRename);
		xwiki.saveDocument(doc, olddoc, context);

		// We need to load all documents that use this property and rename it
		if (fieldsToRename.size() > 0) {
			List list = xwiki.getStore().searchDocumentsNames(
					", BaseObject as obj where obj.name=" + xwiki.getFullNameSQL() + " and obj.className='" + Utils.SQLFilter(bclass.getName()) + "' and "
							+ xwiki.getFullNameSQL() + "<> '" + Utils.SQLFilter(bclass.getName()) + "'", context);
			for (int i = 0; i < list.size(); i++) {
				XWikiDocument doc2 = xwiki.getDocument((String) list.get(i), context);
				doc2.renameProperties(bclass.getName(), fieldsToRename);
				xwiki.saveDocument(doc2, doc2, context);
			}
		}
		xwiki.flushCache();
		return false;
	}
	public boolean save(XWikiContext context) throws XWikiException {
		XWiki xwiki = context.getWiki();
		XWikiDocument doc = context.getDoc();
		XWikiForm form = context.getForm();

		// This is pretty useless, since contexts aren't shared between threads.
		// It just slows down execution.
		synchronized (doc) {
			String language = ((EditForm) form).getLanguage();
			// FIXME Which one should be used: doc.getDefaultLanguage or
			// form.getDefaultLanguage()?
			// String defaultLanguage = ((EditForm) form).getDefaultLanguage();
			XWikiDocument tdoc;

			if ((language == null) || (language.equals("")) || (language.equals("default")) || (language.equals(doc.getDefaultLanguage()))) {
				// Need to save parent and defaultLanguage if they have changed
				tdoc = doc;
			} else {
				tdoc = doc.getTranslatedDocument(language, context);
				if ((tdoc == doc) && xwiki.isMultiLingual(context)) {
					tdoc = new XWikiDocument(doc.getWeb(), doc.getName());
					tdoc.setLanguage(language);
					tdoc.setStore(doc.getStore());
				}
				tdoc.setTranslation(1);
			}

			XWikiDocument olddoc = (XWikiDocument) tdoc.clone();
			try {
				tdoc.readFromTemplate(((EditForm) form).getTemplate(), context);
			} catch (XWikiException e) {
				if (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY) {
					context.put("exception", e);
					return true;
				}
			}

			tdoc.readFromForm((EditForm) form, context);

			// TODO: handle Author
			String username = context.getUser();
			tdoc.setAuthor(username);
			if (tdoc.isNew())
				tdoc.setCreator(username);

			xwiki.saveDocument(tdoc, olddoc, context);
			XWikiLock lock = tdoc.getLock(context);
			if (lock != null)
				tdoc.removeLock(context);
		}
		return false;
	}

	public boolean action(XWikiContext context) throws XWikiException {
		XWikiRequest request = context.getRequest();
		XWikiResponse response = context.getResponse();

		String back = request.getParameter("xredirect");
		if (back == null || back.equals("")) {
			back = request.getHeader("Referer");
			if (back == null || back.equals("")) {
				back = context.getDoc().getURL("edit", context);
			}
		}

		if (back != null && back.indexOf("editor=class") >= 0) {
			if (propUpdate(context)) {
				return true;
			}
		} else {
			if (save(context)) {
				return true;
			}
		}
		// Forward back to the originating page
		try {
			response.sendRedirect(back);
		} catch (IOException ignored) {
		}
		return false;
	}

	public String render(XWikiContext context) throws XWikiException {
		XWikiException e = (XWikiException) context.get("exception");
		if ((e != null) && (e.getCode() == XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY)) {
			return "docalreadyexists";
		}
		return "exception";
	}
}
