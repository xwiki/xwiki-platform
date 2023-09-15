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
package org.xwiki.model.validation.edit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * This class provides the script services for handling document edit confirmation.
 *
 * @version $Id$
 * @since 15.8RC1
 */
@Component
@Singleton
@Named("editConfirmation")
@Unstable
public class EditConfirmationScriptService implements ScriptService
{
    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    /**
     * Performs a check by invoking the check method of all available {@link EditConfirmationChecker} components and
     * aggregating their results.
     *
     * @return a {@link EditConfirmationCheckerResults} object containing the results of the check
     */
    public EditConfirmationCheckerResults check()
    {
        try {
            EditConfirmationCheckerResults result = new EditConfirmationCheckerResults();
            // The list is ordered by the priority of the components.
            this.componentManager.<EditConfirmationChecker>getInstanceList(EditConfirmationChecker.class)
                .stream()
                .flatMap(checker -> checker.check().stream())
                .forEach(result::append);
            return result;
        } catch (ComponentLookupException e) {
            this.logger.warn("Failed to resolve the list of [{}]. Cause: [{}]", EditConfirmationChecker.class,
                getRootCauseMessage(e));
            return new EditConfirmationCheckerResults();
        }
    }
}
