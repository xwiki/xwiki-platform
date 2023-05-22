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
package org.xwiki.security.test.po;

import java.util.Map;

import org.openqa.selenium.By;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.ui.po.ViewPage;

/**
 * Page Object for the required rights administration.Allows to activate the required rights, and to select the allowed
 * required rights.
 *
 * @version $Id$
 * @since 15.5RC1
 */
public class RequiredRightsAdministration extends ViewPage
{
    /**
     * @param reference the document reference to administration
     * @return the administration page object
     */
    public static RequiredRightsAdministration goToRights(DocumentReference reference)
    {
        getUtil().gotoPage(reference, "edit", Map.of("editor", "rights"));
        return new RequiredRightsAdministration();
    }

    /**
     * Submit the required right form, either to activate them, or to persist the updated required rights.
     */
    public void submit()
    {
        getDriver().findElement(By.cssSelector("[name='required_rights_form'] input[type='submit']")).click();
    }

    /**
     * Check/Uncheck the {@code Script} required right.
     *
     * @return the current page object
     */
    public RequiredRightsAdministration clickScriptRequiredRight()
    {
        getDriver().findElement(By.id("scriptRequiredRight")).click();
        return this;
    }

    /**
     * Check/Uncheck the {@code Programming} required right.
     *
     * @return the current page object
     */
    public RequiredRightsAdministration clickProgrammingRequiredRight()
    {
        getDriver().findElement(By.id("programmingRequiredRight")).click();
        return this;
    }
}
