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
package org.xwiki.internal.migration;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.internal.extension.XARExtensionIndex;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

import static org.xwiki.query.Query.XWQL;

/**
 * Search for documents with a known invalid content produced by code generation (i.e., that cannot be fixed by editing
 * a xar file), and apply an automatic escaping fix.
 *
 * @version $Id$
 * @since 15.0RC1
 * @since 14.4.8
 * @since 14.10.4
 */
@Component
@Named("R150000000XWIKI20285")
@Singleton
public class R150000000XWIKI20285DataMigration extends AbstractDocumentsMigration
{
    @Inject
    private XARExtensionIndex installedXARs;

    @Override
    public String getDescription()
    {
        return "Patch the InvitationConfig documents with improper escaping.";
    }

    @Override
    public XWikiDBVersion getVersion()
    {
        // Version updated because XWIKI-21091 was preventing the migration to work correctly on sub-wikis.
        return new XWikiDBVersion(150700000);
    }

    @Override
    protected String getTaskType()
    {
        return InvitationInternalDocumentParameterEscapingTaskConsumer.HINT;
    }

    @Override
    protected List<DocumentReference> selectDocuments() throws DataMigrationException
    {
        XWiki wiki = getXWikiContext().getWiki();
        try {
            // We select potentially impacted documents using like wildcards. This selection might lead to false 
            // positive that wild be filtered out by the more accurate regex in
            // InvitationInternalDocumentParameterEscapingFixer.
            return wiki.getStore().getQueryManager()
                .createQuery("select doc.fullName, doc.language from Document doc where doc.content "
                    + "like '%{{info}}%services.localization.render%xe.invitation.internalDocument%{{/info}}%'", XWQL)
                .<Object[]>execute()
                .stream()
                .flatMap(array -> {
                    // Oracle returns null for the empty string. Therefore, we need to convert back the null value
                    // to the empty string.
                    String locale = Objects.toString(array[1], "");
                    return resolveDocumentReference(String.valueOf(array[0]), locale).stream();
                })
                // Exclude document that are provided by extensions, because they are fixed using the usual xar upgrade
                // mechanism.
                .filter(documentReference -> !this.installedXARs.isExtensionDocument(documentReference))
                .collect(Collectors.toList());
        } catch (QueryException e) {
            throw new DataMigrationException(
                String.format("Failed retrieve the list of all the documents for wiki [%s].", wiki.getName()), e);
        }
    }
}
