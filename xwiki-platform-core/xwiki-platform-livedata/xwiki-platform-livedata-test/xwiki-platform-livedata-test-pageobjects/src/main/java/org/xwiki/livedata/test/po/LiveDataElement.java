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
package org.xwiki.livedata.test.po;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.openqa.selenium.WebElement;
import org.xwiki.test.ui.po.BaseElement;

/**
 * Live Data page object. Provides the operations to obtain the page objects for the different live data layouts, and to
 * switch between them.
 *
 * @version $Id$
 * @since 13.4RC1
 */
public class LiveDataElement extends BaseElement
{
    // TODO: add the operations to switch between the layouts.

    private final String id;

    /**
     * Lazily initialized the first time {@link #getLiveData()} is called.
     */
    private WebElement root;

    /**
     * Default constructor. Initializes a Live Data page object by its id.
     *
     * @param id the live data id
     */
    public LiveDataElement(String id)
    {
        this.id = id;
    }

    /**
     * @return a table layout page object for the live data
     */
    public TableLayoutElement getTableLayout()
    {
        TableLayoutElement tableLayoutElement = new TableLayoutElement(getLiveData());
        tableLayoutElement.waitUntilReady();
        return tableLayoutElement;
    }

    /**
     * @return a cart layout page object for the live data
     */
    public CardLayoutElement getCardLayout()
    {
        return new CardLayoutElement(getLiveData());
    }

    /**
     * Get the livedata of this page object. The corresponding {@link WebElement} is initialized on the first call to
     * this method and stored in a field. Subsequents calls to this methods returns the value of the field.
     *
     * @return the livedata of this page object
     */
    @Nonnull
    private WebElement getLiveData()
    {
        if (this.root != null) {
            this.root = getDriver().findElementById(this.id);
        }
        return Objects.requireNonNull(this.root);
    }
}
