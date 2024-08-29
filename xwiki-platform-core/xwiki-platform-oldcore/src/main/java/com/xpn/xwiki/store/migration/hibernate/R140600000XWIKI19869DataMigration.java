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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryFilter;
import org.xwiki.query.QueryManager;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.doc.rcs.XWikiRCSNodeInfo;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

/**
 * Migration responsible for ensuring the password fields are properly hashed.
 *
 * @version $Id$
 * @since 14.6RC1
 * @since 14.4.3
 * @since 13.10.8
 */
@Component
@Singleton
@Named(R140600000XWIKI19869DataMigration.HINT)
public class R140600000XWIKI19869DataMigration extends AbstractHibernateDataMigration
{
    /**
     * The hint for this component.
     */
    public static final String HINT = "140600000XWIKI19869";

    private static final String FILENAME = HINT + "DataMigration-users.txt";

    private static final String XWQL_QUERY = "select distinct doc.fullName from Document doc, "
        + "doc.object(XWiki.XWikiUsers) objUser where objUser.password not like 'hash:%' and objUser.password <> '' "
        + "order by doc.fullName";

    private static final String PASSWORD_FIELD = "password";

    private static final int BATCH_SIZE = 100;

    @Inject
    private QueryManager queryManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    @Inject
    private Provider<UserReferenceSerializer<String>> userReferenceSerializerProvider;

    @Inject
    @Named("count")
    private Provider<QueryFilter> countFilterProvider;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private Environment environment;

    @Inject
    @Named("xwikiproperties")
    private Provider<ConfigurationSource> propertiesConfigurationProvider;

    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Migrate wrongly stored passwords information.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        return new XWikiDBVersion(140600000);
    }

    @Override
    public boolean shouldExecute(XWikiDBVersion startupVersion)
    {
        // The bug we discovered only impact the main wiki users thanks to another bug (XWIKI-19591),
        // so we can safely ignore subwikis
        if (getXWikiContext().isMainWiki()) {
            int version = startupVersion.getVersion();
            // The migration has been cherry-picked in 13.10.8 and 14.4.3
            return !((version >= 131008000 && version < 140000000) || (version >= 140403000 && version < 140500000));
        } else {
            return false;
        }
    }

    @Override
    protected void hibernateMigrate() throws DataMigrationException, XWikiException
    {
        // 1. Find users with a plain text password and then for each user:
        // 2. Set an empty password OR recompute a hash of the password and store it (this choice is made through
        //    the xwiki.properties security.migration.R140600000XWIKI19869.resetPassword)
        // 3. If the reset password feature is enabled force a reset, else log a warning in the console
        // 4. Rewrite the history of the user page to replace plain text passwords info with new password hash from it
        //    (we don't wipe out to avoid problem with rollback)
        ConfigurationSource configurationSource = this.propertiesConfigurationProvider.get();
        boolean resetPassword = configurationSource
            .getProperty("security.migration.R140600000XWIKI19869.resetPassword", true);
        long numberOfUsersToMigrate = this.getNumberOfUsersToMigrate();
        this.logger.info("The migration will need to process [{}] user documents", numberOfUsersToMigrate);
        File migrationFile = new File(this.environment.getPermanentDirectory(), FILENAME);

        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(migrationFile.toPath(),
            StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            List<DocumentReference> users;
            do {
                users = this.getUsers();

                if (!users.isEmpty()) {
                    this.logger.info("Start processing [{}] users over [{}]", users.size(), numberOfUsersToMigrate);

                    for (DocumentReference userReference : users) {
                        this.handleUser(userReference, resetPassword, bufferedWriter);
                    }
                    bufferedWriter.flush();
                }
            } while (!users.isEmpty());
        } catch (IOException e) {
            throw new DataMigrationException("Error while trying to create the migration file for sending users "
                + "instructions to reset their password. If you cannot resolve the problem, you can skip this check"
                + " by setting the property 'security.migration.R140600000XWIKI19869.requireMigrationFile' "
                + "in xwiki.properties.", e);
        }
    }

    private long getNumberOfUsersToMigrate()
    {
        long result = -1;
        try {
            Query query = this.queryManager.createQuery(XWQL_QUERY, Query.XWQL)
                .addFilter(this.countFilterProvider.get());
            List<Long> countValue = query.execute();
            result = countValue.get(0);
        } catch (QueryException e) {
            // We don't throw an exception because it might be only a problem with the count
            // and this is only used for log purpose.
            // In case of real issue on the query, it will throw an exception when actually getting the users
            this.logger.warn("Error while trying to count the number of users", e);
        }
        return result;
    }

    private List<DocumentReference> getUsers() throws DataMigrationException
    {
        try {
            // Note: we don't need to set an offset, since we call back the query after having processed the users
            // so we shouldn't retrieve back the users already processed.
            Query query = this.queryManager
                .createQuery(XWQL_QUERY, Query.XWQL)
                .setLimit(BATCH_SIZE);
            List<String> usersList = query.execute();

            return usersList.stream().map(this.documentReferenceResolver::resolve).collect(Collectors.toList());
        } catch (QueryException e) {
            throw new DataMigrationException("Error while querying the list of users", e);
        }
    }

    private void handleUser(DocumentReference userDocReference, boolean resetPassword, BufferedWriter bufferedWriter)
        throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWikiDocument userDoc = context.getWiki().getDocument(userDocReference, context);

        if (fixPasswordHash(userDoc, true, resetPassword)) {
            String serializedUserRef = this.entityReferenceSerializer.serialize(userDocReference);
            this.handleHistory(userDoc);
            context.getWiki().saveDocument(userDoc, context);
            try {
                bufferedWriter.write(serializedUserRef + "\n");
            } catch (IOException e) {
                logger.warn("Error when writing in migration file (root cause was [{}]. Please reach "
                        + "individually [{}] for resetting their password.",
                    ExceptionUtils.getRootCauseMessage(e), serializedUserRef);
            }
        }
    }

    private void handleHistory(XWikiDocument userDoc) throws XWikiException
    {
        XWikiContext context = getXWikiContext();
        XWikiDocumentArchive documentArchive = userDoc.getDocumentArchive(context);
        Collection<XWikiRCSNodeInfo> archiveNodes = documentArchive.getNodes();

        for (XWikiRCSNodeInfo node : new ArrayList<>(archiveNodes)) {
            try {
                XWikiDocument revision =
                    this.documentRevisionProvider.getRevision(userDoc, node.getVersion().toString());
                if (fixPasswordHash(revision, false, false)) {
                    String author = userReferenceSerializerProvider.get()
                        .serialize(revision.getAuthors().getOriginalMetadataAuthor());
                    documentArchive.updateArchive(revision, author, revision.getDate(), revision.getComment(),
                        revision.getRCSVersion(), context);
                }
            } catch (Exception e) {
                this.logger.warn(
                    "Failed to handler revision [{}] for user page [{}]: {}. It's recommended to delete this version.",
                    node.getVersion().toString(), userDoc.getDocumentReference(),
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    /**
     * Set the password value using the appropriate API.
     * The first boolean flag is used to know if the reset is called for the last version of the doc or for one of the
     * history version. The second flag is used to define if the password should be just reset, or if the hash of the
     * defined password should be computed.
     *
     * @param userDoc the document to update
     * @param isMain is {@code true} if the reset is called for the last version of the doc or for one of the
     *               history version.
     * @param resetPassword {@code true} if the password should be just reset, or {@code false} if the hash of the
     *                      defined password should be computed.
     */
    private boolean fixPasswordHash(XWikiDocument userDoc, boolean isMain, boolean resetPassword)
    {
        boolean result = false;
        XWikiContext context = getXWikiContext();
        BaseObject userObj = userDoc.getXObject(XWikiUsersDocumentInitializer.XWIKI_USERS_DOCUMENT_REFERENCE);
        if (userObj != null) {
            String password = userObj.getStringValue(PASSWORD_FIELD);
            if (!password.startsWith("hash:")) {
                if (isMain && resetPassword) {
                    userObj.set(PASSWORD_FIELD, "", context);
                } else {
                    // The set method should automatically compute the hash
                    userObj.set(PASSWORD_FIELD, password, context);
                }
                result = true;
            } else if (isMain) {
                this.logger.warn("User document was wrongly retrieved [{}] it won't be modified.",
                    userDoc.getDocumentReference());
            }
        } else if (isMain) {
            this.logger.warn("Null user object for document [{}] this should never happen.",
                userDoc.getDocumentReference());
        }
        return result;
    }
}
