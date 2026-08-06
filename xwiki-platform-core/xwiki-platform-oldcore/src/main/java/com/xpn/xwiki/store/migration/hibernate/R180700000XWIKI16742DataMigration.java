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
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration for XWIKI-16742: the user type property was not consistently stored on user profiles created before the
 * fix in {@link XWiki#createUser}, which means that it's not properly displayed, filtered or sorted (e.g. in the
 * User Directory live table) until the user explicitly saves their profile preferences at least once.
 * <p>
 * This migration sets the user type property to the wiki's current default value for that property (as configured
 * on the {@code XWiki.XWikiUsers} class, in case it was customized) for every {@code XWiki.XWikiUsers} object that
 * doesn't have it set yet.
 *
 * @version $Id$
 * @since 18.7.0RC1
 */
@Component
@Named("R180700000XWIKI16742")
@Singleton
public class R180700000XWIKI16742DataMigration extends AbstractHibernateDataMigration
{
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Set the user type property to its default value for user profiles that don't have it set yet.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(180700000);
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // Get all the users that don't have the user type property set yet.
        List<String> allUsers = getStore().executeRead(getXWikiContext(), this::getUsersWithoutUserType);

        this.logger.info("Migration needed for [{}] users on database [{}].",
            allUsers.size(), getXWikiContext().getWikiId());

        DocumentReference wikiUserClassReference =
            new DocumentReference(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE,
                getXWikiContext().getWikiReference());
        BaseClass xwikiUserClass = getXWikiContext().getWiki().getXClass(wikiUserClassReference, getXWikiContext());
        ListClass userTypeProperty = getUserTypeProperty(xwikiUserClass, wikiUserClassReference);

        int i = 0;
        int failures = 0;
        for (String user : allUsers) {
            try {
                applyMigrationOnUser(user, xwikiUserClass, userTypeProperty);
            } catch (Exception e) {
                this.logger.error("Error while migrating the user type for user [{}] on database [{}]", user,
                    getXWikiContext().getWikiId(), e);
                failures++;
            }
            if (++i % 100 == 0) {
                this.logger.info("[{}] users on [{}] have been migrated on database [{}]...", i - failures,
                    allUsers.size(), getXWikiContext().getWikiId());
            }
        }
        this.logger.info("[{}] users on [{}] have been migrated on database [{}].", allUsers.size() - failures,
            allUsers.size(), getXWikiContext().getWikiId());
        if (failures > 0) {
            this.logger.warn("[{}] users have not been properly migrated, please check the logs above.", failures);
        }
    }

    private List<String> getUsersWithoutUserType(Session session) throws HibernateException, XWikiException
    {
        Query<String> query = session.createQuery("select obj.name from BaseObject obj"
            + " where obj.className = '" + XWikiUsersDocumentInitializer.CLASS_REFERENCE_STRING + "'"
            + " and obj.id not in (select prop.id.id from StringProperty prop where prop.id.name='"
            + XWikiUsersDocumentInitializer.USERTYPE_FIELD + "')",
            String.class);

        return query.list();
    }

    private ListClass getUserTypeProperty(BaseClass xwikiUserClass, DocumentReference wikiUserClassReference)
        throws XWikiException
    {
        PropertyClass userTypeProperty =
            (PropertyClass) xwikiUserClass.get(XWikiUsersDocumentInitializer.USERTYPE_FIELD);
        if (userTypeProperty instanceof ListClass listUserTypeProperty) {
            return listUserTypeProperty;
        }

        throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_MIGRATION,
            String.format("The [%s] property of the XWikiUsers XClass with reference [%s] has not been found or is "
                + "not a list property, the migration [%s] cannot be performed.",
                XWikiUsersDocumentInitializer.USERTYPE_FIELD, wikiUserClassReference, getName()));
    }

    private void applyMigrationOnUser(String docUser, BaseClass xwikiUserXClass, ListClass userTypeProperty)
        throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWiki xwiki = context.getWiki();
        DocumentReference userDocReference = this.documentReferenceResolver.resolve(docUser);
        XWikiDocument userDocument = xwiki.getDocument(userDocReference, context);
        // Avoid modifying the cached document.
        userDocument = userDocument.clone();
        BaseObject userObject = userDocument.getXObject(xwikiUserXClass.getReference());

        // This condition should never happen normally, but we might imagine so DB with stale objects for some
        // reasons, so we won't take the chance.
        if (userObject != null) {
            userObject.safeput(XWikiUsersDocumentInitializer.USERTYPE_FIELD,
                userTypeProperty.fromString(userTypeProperty.getDefaultValue()));

            xwiki.saveDocument(userDocument, "Set the user type to its default value (XWIKI-16742)", true, context);
        }
    }
}
