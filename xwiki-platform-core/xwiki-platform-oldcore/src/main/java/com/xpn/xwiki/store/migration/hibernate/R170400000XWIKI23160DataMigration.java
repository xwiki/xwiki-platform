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
package com.xpn.xwiki.store.migration.hibernate;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * @version $Id$
 * @since 17.4.0RC1
 */
@Component
@Named("R170400000XWIKI23160")
@Singleton
public class R170400000XWIKI23160DataMigration extends AbstractHibernateDataMigration
{
    public static final String META_FIELD = "meta";

    public static final String CONFIRMATION_EMAIL_CONTENT_FIELD = "confirmation_email_content";

    public static final String VALIDATION_EMAIL_CONTENT_FIELD = "validation_email_content";

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(170400000);
    }

    @Override
    public String getDescription()
    {
        return "Cleanup XWikiProperties fields used as templates when they match the default value.";
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        XWikiContext xWikiContext = getXWikiContext();
        XWiki wiki = xWikiContext.getWiki();
        DocumentReference xwikiPreferencesDocumentReference =
            new DocumentReference(wiki.getName(), "XWiki", "XWikiPreferences");
        XWikiDocument xwikiPreferencesDocument =
            wiki.getDocument(xwikiPreferencesDocumentReference, xWikiContext);
        BaseObject xwikiPreferenceXObject = xwikiPreferencesDocument.getXObject(xwikiPreferencesDocumentReference);
        boolean metaChanged = clearField(xwikiPreferenceXObject, META_FIELD);
        boolean confirmationEmailContentChanged = clearField(xwikiPreferenceXObject, CONFIRMATION_EMAIL_CONTENT_FIELD);
        boolean validationEmailContentChanged = clearField(xwikiPreferenceXObject, VALIDATION_EMAIL_CONTENT_FIELD);
        List<String> changedFields = new ArrayList<>();
        if (metaChanged) {
            changedFields.add(META_FIELD);
        }
        if (confirmationEmailContentChanged) {
            changedFields.add(CONFIRMATION_EMAIL_CONTENT_FIELD);
        }
        if (validationEmailContentChanged) {
            changedFields.add(VALIDATION_EMAIL_CONTENT_FIELD);
        }

        if (metaChanged || confirmationEmailContentChanged || validationEmailContentChanged) {
            wiki.saveDocument(xwikiPreferencesDocument,
                "[UPGRADE] empty fields [%s] because they match the default values".formatted(
                    String.join(", ", changedFields)), xWikiContext);
        }
    }

    private boolean clearField(BaseObject object, String field)
    {
        String template = object.getStringValue(field);
        if (StringUtils.isNotEmpty(template)) {
            // TODO: load XAR and compare.
            String fromXAr = "...";
            if (template.equals(fromXAr)) {
                object.setStringValue(field, "");
                return true;
            }
        }
        return false;
    }
}
