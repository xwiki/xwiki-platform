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
package org.xwiki.gwt.wysiwyg.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;

/**
 * A collection of {@link Constants} used to localize the user interface.
 * 
 * @version $Id$
 */
public interface Strings extends Constants
{
    /**
     * An instance of this string bundle that can be used anywhere in the code to obtain i18n strings.
     */
    Strings INSTANCE = GWT.create(Strings.class);

    String yes();

    String no();

    String wysiwyg();

    String source();

    String apply();

    String select();

    String attachment();

    String backColor();

    String bold();

    String cancel();

    String charmap();

    String colorPicker();

    String copy();

    String cut();

    String deleteCol();

    String deleteRow();

    String deleteTable();

    String font();

    String fontNameOther();

    String fontSize();

    String fontSizeOther();

    String foreColor();

    String format();

    String formatPlainText();

    String formatHeader1();

    String formatHeader2();

    String formatHeader3();

    String formatHeader4();

    String formatHeader5();

    String formatHeader6();

    String hr();

    String indent();

    String insertColAfter();

    String insertColBefore();

    String insertRowAfter();

    String insertRowBefore();

    String insertTable();

    String italic();

    String justifyCenter();

    String justifyFull();

    String justifyLeft();

    String justifyRight();

    String macro();

    String macroCollapse();

    String macroCollapseShortcutKeyLabel();

    String macroCollapseAll();

    String macroCollapseAllShortcutKeyLabel();

    String macroExpand();

    String macroExpandShortcutKeyLabel();

    String macroExpandAll();

    String macroExpandAllShortcutKeyLabel();

    String macroRefresh();

    String macroRefreshShortcutKeyLabel();

    String macroEdit();

    String macroEditShortcutKeyLabel();

    String macroEditDialogCaption();

    String macroInsert();

    String macroInsertShortcutKeyLabel();

    String macroInsertDialogCaption();

    String macroInsertDialogTitle();

    String macroInsertActionLabel();

    String macroParameterMandatory();

    String macroCategories();

    String macroCategoriesToolTip();

    String macroCategoryAll();

    String macroCategoryOther();

    String macroCategoryUsed();

    String macroNoMacroSelected();

    String ol();

    String outdent();

    String paste();

    String redo();

    String removeFormat();

    String strikeThrough();

    String subscript();

    String superscript();

    String teletype();

    String ul();

    String underline();

    String undo();

    String quickSearch();

    String mandatory();

    String link();

    String unlink();

    String linkEdit();

    String linkCreateLinkButton();

    String linkToEmail();

    String linkToWebPage();

    String linkToWikiPage();

    String linkToAttachment();

    String linkLabelLabel();

    String linkTooltipLabel();

    String linkOpenInNewWindowLabel();

    String linkOpenInNewWindowHelpLabel();

    String linkWebPageLabel();

    String linkWebPageHelpLabel();

    String linkEmailLabel();

    String linkEmailHelpLabel();

    String linkEmailAddressError();

    String linkWebPageAddressError();

    String linkNoLabelError();

    String linkConfigLabelTextBoxTooltip();

    String linkConfigTooltipTextBoxTooltip();

    String linkURLToEmailAddressTextBoxTooltip();

    String linkURLToWebPageTextBoxTooltip();

    String linkWikipageSearchTooltip();

    String linkWikipageSearchButton();

    String linkSelectWikipageTitle();

    String linkSelectWikipageHelpLabel();

    String linkNoPageSelectedError();

    String linkSelectAttachmentTitle();

    String linkSelectAttachmentHelpLabel();

    String linkAttachmentUploadHelpLabel();

    String linkNoAttachmentSelectedError();

    String linkConfigTitle();

    String linkNewPageOptionLabel();

    String linkCreateNewPageTitle();

    String linkNewPageLabel();

    String linkNewPageTextBoxTooltip();

    String linkNewPageError();

    String linkSettingsLabel();

    String selectorSelectFromCurrentPage();

    String selectorSelectFromAllPages();

    String selectorSelectFromRecentPages();

    String selectorSelectFromSearchPages();

    String linkErrorLoadingData();

    String fileUploadLabel();

    String fileUploadNewFileLabel();

    String fileUploadTitle();

    String fileChooseLabel();

    String fileListFetchError();

    String fileUploadSubmitLabel();

    String fileUploadSubmitError();

    String fileUploadNoPathError();

    String image();

    String imageTooltip();

    String imageInsertAttachedImage();

    String imageInsertURLImage();

    String imageEditImage();

    String imageRemoveImage();

    String imageSelectImageTitle();

    String imageSelectImageHelpLabel();

    String imageSelectImageLocationHelpLabel();

    String imageUploadNewFileLabel();

    String imageConfigTitle();

    String imageCreateImageButton();

    String imageUpdateListButton();

    String imageWidthLabel();

    String imageWidthHelpLabel();

    String imageHeightLabel();

    String imageHeightHelpLabel();

    String imageHorizontalAlignmentLabel();

    String imageHorizontalAlignmentHelpLabel();

    String imageVerticalAlignmentLabel();

    String imageVerticalAlignmentHelpLabel();

    String imageAlignLeftLabel();

    String imageAlignCenterLabel();

    String imageAlignRightLabel();

    String imageAlignTopLabel();

    String imageAlignMiddleLabel();

    String imageAlignBottomLabel();

    String imageSettingsLabel();

    String imageAltTextLabel();

    String imageAltTextHelpLabel();

    String imageUploadHelpLabel();

    String imageNoImageSelectedError();

    String imageChangeImageButton();

    String imageExternal();

    String imageExternalLocationLabel();

    String imageExternalLocationHelpLabel();

    String imageExternalLocationNotSpecifiedError();

    String table();

    String tableRowsLabel();

    String tableRowsHelpLabel();

    String tableRowsToolTip();

    String tableColsLabel();

    String tableColsHelpLabel();

    String tableColsToolTip();

    String tableBorderLabel();

    String tableBorderHelpLabel();

    String tableHeaderLabel();

    String tableHeaderHelpLabel();

    String tablePixel();

    String tableInsertButton();

    String tableInsertDialogCaption();

    String tableInsertDialogTitle();

    String tableInsertStrictPositiveIntegerRequired();

    String importMenuEntryCaption();

    String importWizardTitle();

    String importWizardImportButtonCaption();

    String importOfficeContentFilterStylesCheckBoxLabel();

    String importOfficeFileMenuItemCaption();

    String importOfficeFileFeatureNotAvailable();

    String importOfficeFileWizardStepTitle();

    String importOfficeFileHelpLabel();

    String importOfficePasteWizardStepTitle();

    String importOfficePasteInfoLabel();

    String importOfficePasteHelpLabel();

    String embeddedObject();

    String entityLocatedIn();

    String stylePickerLabel();

    String stylePickerTitle();

    String styleInlineGroupLabel();

    String styleBlockGroupLabel();

    String gadgetInsertActionLabel();

    String gadgetInsertDialogCaption();

    String gadgetInsertDialogTitle();

    String gadgetNoGadgetSelected();

    String gadget();

    String gadgetTitleLabel();

    String gadgetTitleDescription();

    String gadgetEditDialogCaption();
}
