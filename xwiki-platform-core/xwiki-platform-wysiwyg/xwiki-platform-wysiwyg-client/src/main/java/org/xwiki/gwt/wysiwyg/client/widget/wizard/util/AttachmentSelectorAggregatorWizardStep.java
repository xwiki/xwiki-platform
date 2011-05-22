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
package org.xwiki.gwt.wysiwyg.client.widget.wizard.util;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.gwt.user.client.ui.wizard.WizardStep;
import org.xwiki.gwt.wysiwyg.client.Strings;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityConfig;
import org.xwiki.gwt.wysiwyg.client.wiki.EntityLink;
import org.xwiki.gwt.wysiwyg.client.wiki.WikiPageReference;

/**
 * Generic wizard step that allows the user to select an attachment to link to by aggregating a current page selector
 * step and an "all pages" selector step.
 * 
 * @param <T> the type of entity link configuration managed by this wizard step
 * @version $Id$
 */
public class AttachmentSelectorAggregatorWizardStep<T extends EntityConfig> extends
    AbstractSelectorAggregatorWizardStep<EntityLink<T>>
{
    /**
     * Flag indicating if the selection is limited to the current page or not.
     */
    private final boolean selectionLimitedToCurrentPage;

    /**
     * The wizard step used to select attachments only from the current page.
     */
    private WizardStep currentPageSelector;

    /**
     * The wizard step used to select attachments from any page.
     */
    private WizardStep allPagesSelector;

    /**
     * Creates a new attachment selector wizard step.
     * 
     * @param selectionLimitedToCurrentPage {@code true} to limit the selection to the current page, {@code false}
     *            otherwise
     */
    public AttachmentSelectorAggregatorWizardStep(boolean selectionLimitedToCurrentPage)
    {
        this.selectionLimitedToCurrentPage = selectionLimitedToCurrentPage;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getRequiredStep()
    {
        WikiPageReference originPage = new WikiPageReference(getData().getOrigin());
        WikiPageReference destinationPage = new WikiPageReference(getData().getDestination().getEntityReference());
        if (selectionLimitedToCurrentPage || originPage.equals(destinationPage)) {
            // Selection limited to current page or the targeted attachment is attached to the origin page.
            return Strings.INSTANCE.selectorSelectFromCurrentPage();
        } else {
            return Strings.INSTANCE.selectorSelectFromAllPages();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelectorAggregatorWizardStep#getStepInstance(String)
     */
    @Override
    protected WizardStep getStepInstance(String name)
    {
        if (name.equals(Strings.INSTANCE.selectorSelectFromCurrentPage())) {
            return currentPageSelector;
        } else if (name.equals(Strings.INSTANCE.selectorSelectFromAllPages())) {
            return allPagesSelector;
        }
        return null;
    }

    /**
     * Sets the wizard step to be used for selecting an attachment from the current page.
     * 
     * @param currentPageSelector the new current page attachment selector
     */
    public void setCurrentPageSelector(WizardStep currentPageSelector)
    {
        this.currentPageSelector = currentPageSelector;
    }

    /**
     * Sets the wizard step to be used for selecting an attachment from all pages.
     * 
     * @param allPagesSelector the new all pages attachment selector
     */
    public void setAllPagesSelector(WizardStep allPagesSelector)
    {
        this.allPagesSelector = allPagesSelector;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractSelectorAggregatorWizardStep#getStepNames()
     */
    @Override
    protected List<String> getStepNames()
    {
        List<String> stepNames = new ArrayList<String>();
        stepNames.add(Strings.INSTANCE.selectorSelectFromCurrentPage());
        if (!selectionLimitedToCurrentPage) {
            stepNames.add(Strings.INSTANCE.selectorSelectFromAllPages());
        }
        return stepNames;
    }
}
