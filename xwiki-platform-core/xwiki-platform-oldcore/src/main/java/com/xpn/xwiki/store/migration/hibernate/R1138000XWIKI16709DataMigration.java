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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * Migration for XWIKI-16709: the disable property is removed and a checked_email property is added from XWikiUsers.
 *
 * Migration does this for each XWikiUser documents:
 *   - Get the disable property
 *   - Get the active property
 *   - Create a new checked_email property with the active property value
 *   - If the disable property existed:
 *       * Set the active property with the disable property value
 *       * Remove the disable property
 * Note that we willingly don't put the new email_checked property in the XWikiUser class,
 * but we only put the property values in the user objects.
 * The reason is that we don't want the XClassMigratorListener to be called by modifying the XClass since it would
 * iterate over all user objects and save them a second time.
 * Instead, we rely solely on the {@link XWikiUsersDocumentInitializer} to put the property on the xclass: when the
 * XClassMigratorListener will be called, the properties will be already present in the objects, so we avoid a new save.
 *
 * NB: The minor version of the migration is incremented with 30 because of a previous migration in 11 cycle badly
 * named.
 *
 * @since 11.8RC1
 * @version $Id$
 */
@Component
@Named("R1138000XWIKI16709")
@Singleton
public class R1138000XWIKI16709DataMigration extends AbstractHibernateDataMigration
{
    private static final String OLD_DISABLED_PROPERTY = "disabled";

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Logger logger;

    private BaseClass userClass;

    @Override
    public String getDescription()
    {
        return "Remove disable property and add checked_email property in XWikiUser documents.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(1138000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // Get all users
        List<String> allUsers = getStore().executeRead(getXWikiContext(), this::getAllUsers);

        logger.info("Migration needed for [{}] users on database [{}].",
            allUsers.size(), getXWikiContext().getWikiId());

        // Remove the old property from XWikiUsers class
        removeDisableProperty();

        int i = 0;
        // Migrate all users objects
        for (String user : allUsers) {
            applyMigrationsOnUser(user);
            if (++i % 100 == 0) {
                logger.info("[{}] users on [{}] have been migrated on database [{}]...", i, allUsers.size(),
                    getXWikiContext().getWikiId());
            }
        }
        logger.info("[{}] users on [{}] have been migrated on database [{}].", allUsers.size(), allUsers.size(),
            getXWikiContext().getWikiId());
    }

    private List<String> getAllUsers(Session session) throws HibernateException, XWikiException
    {
        // We select only XWikiUsers documents that have not been migrated yet:
        // i.e. those that does not have an email_checked property yet.
        Query<String> query = session.createQuery("select doc.fullName from XWikiDocument doc, BaseObject obj"
            + " where doc.fullName = obj.name and obj.className = 'XWiki.XWikiUsers'"
            + " and obj.id not in (select prop.id.id from IntegerProperty prop where prop.id.name='email_checked')",
            String.class);

        return query.list();
    }

    private void applyMigrationsOnUser(String docUser) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        DocumentReference userDocReference = documentReferenceResolver.resolve(docUser);
        XWikiDocument userDocument = xwiki.getDocument(userDocReference, context);
        BaseObject userObject = userDocument.getXObject(getUserClass().getReference());

        // By default, we consider users are enabled and active.
        int disable = userObject.getIntValue(OLD_DISABLED_PROPERTY, 0);
        int active = userObject.getIntValue(XWikiUser.ACTIVE_PROPERTY, 1);

        // If the user was marked as active and enabled, then its new status is necessarily active.
        // In any other case it should be marked as inactive.
        int newActive = (active == 1 && disable == 0) ? 1 : 0;

        // If the user was marked as inactive, it means its mail was not checked: the UI those users were seeing
        // in that case was indeed the UI to insert email token.
        userObject.setIntValue(XWikiUser.EMAIL_CHECKED_PROPERTY, active);
        userObject.setIntValue(XWikiUser.ACTIVE_PROPERTY, newActive);

        // We remove the deprecated property.
        userObject.removeField(OLD_DISABLED_PROPERTY);

        xwiki.saveDocument(userDocument, context);
    }

    private BaseClass getUserClass() throws XWikiException
    {
        if (this.userClass == null) {
            XWikiDocument userClassDoc = getXWikiContext().getWiki()
                .getDocument(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE, getXWikiContext());
            this.userClass = userClassDoc.getXClass();
        }
        return this.userClass;
    }

    private void removeDisableProperty() throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        getUserClass().removeField(OLD_DISABLED_PROPERTY);
        xwiki.saveDocument(getUserClass().getOwnerDocument(), context);
    }

}
