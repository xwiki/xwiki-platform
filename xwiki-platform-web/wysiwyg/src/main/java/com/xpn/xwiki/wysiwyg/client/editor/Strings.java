package com.xpn.xwiki.wysiwyg.client.editor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;

/**
 * This {@link Constants} interface is used to make user interface strings internationalizable.
 * 
 * @version $Id$
 */
public interface Strings extends Constants
{
    /**
     * An instance of this string bundle that can be used anywhere in the code to obtain i18n strings.
     */
    Strings INSTANCE = (Strings) GWT.create(Strings.class);

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

    String close();

    String colorPicker();

    String copy();

    String cut();

    String deleteCol();

    String deleteRow();

    String deleteTable();

    String font();

    String fontSize();

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

    String macroCollapseAll();

    String macroExpand();

    String macroExpandAll();

    String macroRefresh();

    String macroEdit();

    String macroEditDialogCaption();

    String macroInsert();

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

    String sync();

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

    String fileGetSubmitError();

    String image();

    String imageTooltip();

    String imageInsertImage();

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

    String importerToolTip();

    String importerCaption();

    String importerClipboardTabCaption();

    String importerClipboardTabInfoLabel();

    String importerClipboardTabHelpLabel();

    String importerFileTabCaption();

    String importerFileTabInfoLabel();

    String importerFileTabHelpLabel();
    
    String importerFileTabNotAvailableLabel();

    String importerFilterStylesCheckBoxCaption();

    String importerImportButtonCaption();

    String importerCancelButtonCaption();

    String wizardCancel();

    String wizardPrevious();

    String wizardNext();

    String wizardFinish();

    String errorServerRequestFailed();
}
