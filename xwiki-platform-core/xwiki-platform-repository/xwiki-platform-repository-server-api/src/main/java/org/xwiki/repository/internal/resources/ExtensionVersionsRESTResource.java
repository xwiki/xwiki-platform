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

package org.xwiki.repository.internal.resources;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Named;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersionSummary;
import org.xwiki.extension.repository.xwiki.model.jaxb.ExtensionVersions;
import org.xwiki.extension.version.InvalidVersionRangeException;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.VersionConstraint;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.repository.Resources;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * @version $Id$
 * @since 3.2M3
 */
@Component
@Named("org.xwiki.repository.internal.resources.ExtensionVersionsRESTResource")
@Path(Resources.EXTENSION_VERSIONS)
public class ExtensionVersionsRESTResource extends AbstractExtensionRESTResource
{
    @GET
    public ExtensionVersions getExtensionVersions(@PathParam("extensionId") String extensionId,
        @QueryParam(Resources.QPARAM_LIST_START) @DefaultValue("0") int offset,
        @QueryParam(Resources.QPARAM_LIST_NUMBER) @DefaultValue("-1") int number,
        @QueryParam(Resources.QPARAM_VERSIONS_RANGES) String ranges)
        throws QueryException, InvalidVersionRangeException, XWikiException, ResolveException
    {
        XWikiDocument extensionDocument = getExistingExtensionDocumentById(extensionId);

        checkRights(extensionDocument);

        ExtensionVersions extensions = this.extensionObjectFactory.createExtensionVersions();

        if (this.extensionStore.isVersionProxyingEnabled(extensionDocument)) {
            ExtensionRepository repository = this.repositoryManager.getExtensionRepository(extensionDocument);
            if (repository != null) {
                IterableResult<Version> versions = repository.resolveVersions(extensionId, 0, -1);

                String extensionName = this.extensionStore.getExtensionName(extensionDocument);
                String extensionType = this.extensionStore.getExtensionType(extensionDocument);;

                for (Version version : versions) {
                    extensions.getExtensionVersionSummaries()
                        .add(createExtensionVersionSummary(extensionId, extensionType, extensionName, version));
                }
            }
        } else {
            boolean versionPageEnabled = this.extensionStore.isVersionPageEnabled(extensionDocument);
            Query query = createExtensionsSummariesQuery(null, "extensionVersion.id = :extensionId", 0, -1, true,
                versionPageEnabled);

            query.bindValue("extensionId", extensionId);

            getExtensionSummaries(extensions.getExtensionVersionSummaries(), query);
        }

        // Filter by ranges
        if (StringUtils.isNotBlank(ranges)) {
            VersionConstraint constraint = this.extensionFactory.getVersionConstraint(ranges);

            if (constraint.getVersion() != null) {
                throw new InvalidVersionRangeException("Invalid ranges syntax [" + ranges + "]");
            }

            for (Iterator<ExtensionVersionSummary> it = extensions.getExtensionVersionSummaries().iterator(); it
                .hasNext();) {
                if (!constraint.containsVersion(new DefaultVersion(it.next().getVersion()))) {
                    it.remove();
                }
            }
        }

        extensions.setTotalHits(extensions.getExtensionVersionSummaries().size());
        extensions.setOffset(offset);

        if (offset > 0 || (number > -1 && offset + number < extensions.getExtensionVersionSummaries().size())) {
            if (offset >= extensions.getExtensionVersionSummaries().size() || number == 0) {
                extensions.getExtensionVersionSummaries().clear();
            } else {
                List<ExtensionVersionSummary> limited =
                    new ArrayList<ExtensionVersionSummary>(extensions.getExtensionVersionSummaries());
                extensions.getExtensionVersionSummaries().clear();
                extensions.withExtensionVersionSummaries(limited.subList(offset < 0 ? 0 : offset,
                    number < 0 ? extensions.getExtensionVersionSummaries().size() : offset + number));
            }
        }

        return extensions;
    }
}
