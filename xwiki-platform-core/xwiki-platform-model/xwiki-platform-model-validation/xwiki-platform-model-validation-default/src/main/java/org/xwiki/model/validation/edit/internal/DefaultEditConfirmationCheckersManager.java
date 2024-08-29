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
package org.xwiki.model.validation.edit.internal;

import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.validation.edit.EditConfirmationChecker;
import org.xwiki.model.validation.edit.EditConfirmationCheckerResult;
import org.xwiki.model.validation.edit.EditConfirmationCheckerResults;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Default implementation of {@link EditConfirmationCheckersManager}. Stores the forced results in the user's HTTP
 * session.
 *
 * @version $Id$
 * @since 15.10RC1
 */
@Component
@Singleton
public class DefaultEditConfirmationCheckersManager implements EditConfirmationCheckersManager
{
    private static final String CACHED_QUALIFIER = "cached";

    private static final String FORCED_QUALIFIER = "forced";

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public EditConfirmationCheckerResults check()
    {
        EditConfirmationCheckerResults result = new EditConfirmationCheckerResults();
        XWikiDocument xWikiDocument = this.xcontextProvider.get().getDoc();
        String cachedKeyPrefix = computePrefix(xWikiDocument, CACHED_QUALIFIER);
        String forcedKeyPrefix = computePrefix(xWikiDocument, FORCED_QUALIFIER);

        HttpSession session = getSession();

        // The list is ordered by the priority of the components.
        confirmationCheckersStream()
            .flatMap(entry -> {
                String forcedKey = computeKey(entry, forcedKeyPrefix);
                String cachedKey = computeKey(entry, cachedKeyPrefix);
                Object forcedValue = session.getAttribute(forcedKey);
                EditConfirmationChecker checker = entry.getValue();
                Optional<EditConfirmationCheckerResult> check = checker.check();
                check.ifPresentOrElse(editConfirmationCheckerResult -> {
                    if (!result.isError()) {
                        session.setAttribute(cachedKey, editConfirmationCheckerResult.getSkipValue());
                    }
                }, () -> session.removeAttribute(cachedKey));
                Optional<EditConfirmationCheckerResult> checkResult = check
                    .flatMap(editConfirmationCheckerResult -> {
                        // We ignore the result if this is not an error, the forced value is not null, and the  
                        // forced value stored in session is equal to the skip value returned by the analysis.
                        if (!editConfirmationCheckerResult.isError() && forcedValue != null
                            && Objects.equals(editConfirmationCheckerResult.getSkipValue(), forcedValue))
                        {
                            return Optional.empty();
                        } else {
                            return Optional.of(editConfirmationCheckerResult);
                        }
                    });
                return checkResult.stream();
            }).forEach(result::append);
        return result;
    }

    @Override
    public void force()
    {
        XWikiDocument xWikiDocument = this.xcontextProvider.get().getDoc();
        String cachedKeyPrefix = computePrefix(xWikiDocument, CACHED_QUALIFIER);
        String forcedKeyPrefix = computePrefix(xWikiDocument, FORCED_QUALIFIER);
        HttpSession session = getSession();
        confirmationCheckersStream().forEach(entry -> {
            String cachedKey = computeKey(entry, cachedKeyPrefix);
            String forcedKey = computeKey(entry, forcedKeyPrefix);
            Object lastCheck = session.getAttribute(cachedKey);
            session.removeAttribute(cachedKey);
            session.setAttribute(forcedKey, lastCheck);
        });
    }

    private String computePrefix(XWikiDocument xWikiDocument, String qualifier)
    {
        String documentReference = this.entityReferenceSerializer.serialize(xWikiDocument.getDocumentReference());
        return String.format("force_edit_%s_%s", documentReference, qualifier);
    }

    private static String computeKey(Entry<String, EditConfirmationChecker> entry, String keyPrefix)
    {
        return String.format("%s_%s", keyPrefix, entry.getKey());
    }

    private Stream<Entry<String, EditConfirmationChecker>> confirmationCheckersStream()
    {
        try {
            return this.componentManager.<EditConfirmationChecker>getInstanceMap(EditConfirmationChecker.class)
                .entrySet()
                .stream();
        } catch (ComponentLookupException e) {
            this.logger.warn("Failed to resolve the map of [{}]. Cause: [{}]",
                EditConfirmationChecker.class.getCanonicalName(), getRootCauseMessage(e));
            return Stream.empty();
        }
    }

    private HttpSession getSession()
    {
        return this.xcontextProvider.get().getRequest().getSession();
    }
}
