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

import org.xwiki.component.annotation.Component;
import org.xwiki.model.validation.edit.internal.EditConfirmationCheckersManager;
import org.xwiki.model.validation.script.ModelValidationScriptService;
import org.xwiki.script.service.ScriptService;

/**
 * This class provides the script services for handling document edit confirmation.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component
@Singleton
@Named(ModelValidationScriptService.ID + "." + EditConfirmationScriptService.ID)
public class EditConfirmationScriptService implements ScriptService
{
    /**
     * This component hint.
     */
    public static final String ID = "edit";

    @Inject
    private EditConfirmationCheckersManager editConfirmationCheckersManager;

    /**
     * Performs a check by invoking the check method of all available {@link EditConfirmationChecker} components and
     * aggregating their results.
     *
     * @return a {@link EditConfirmationCheckerResults} object containing the results of the check
     */
    public EditConfirmationCheckerResults check()
    {
        return this.editConfirmationCheckersManager.check();
    }

    /**
     * Force the last {@link EditConfirmationChecker} components checks. The results of the last call to
     * {@link #check()} are persisted, and new checks are skipped as long as they match the persisted results.
     *
     * @since 15.10RC1
     */
    public void force()
    {
        this.editConfirmationCheckersManager.force();
    }
}
