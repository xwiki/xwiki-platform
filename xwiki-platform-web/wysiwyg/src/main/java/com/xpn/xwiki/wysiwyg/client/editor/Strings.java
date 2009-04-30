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

    String formatInline();

    String formatParagraph();

    String formatHeader1();

    String formatHeader2();

    String formatHeader3();

    String formatHeader4();

    String formatHeader5();

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

    String macroParameterMandatory();

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

    String chooseWiki();

    String chooseSpace();
    
    String choosePage();

    String chooseAnchor();

    String chooseVersion();

    String chooseAttachment();

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

    String linkWebPageLabel();

    String linkEmailLabel();

    String linkEmailAddressTextBox();

    String linkWebPageTextBox();

    String linkEmailAddressError();

    String linkWebPageAddressError();

    String linkNoLabelError();

    String linkConfigLabelTextBoxTooltip();

    String linkConfigTooltipTextBoxTooltip();

    String linkWebPageLabelTextBoxTooltip();

    String linkEmailAddressLabelTextBoxTooltip();

    String linkURLToEmailAddressTextBoxTooltip();

    String linkURLToWebPageTextBoxTooltip();

    String linkSelectWikipageTitle();

    String linkNoPageSelectedError();

    String linkSelectAttachmentTitle();

    String linkNoAttachmentSelectedError();    

    String linkConfigTitle();

    String linkCreateNewPageTitle();

    String linkNewPageLabel();

    String linkNewPageTextBoxTooltip();

    String linkNewPageError();

    String selectorSelectFromCurrentPage();
    
    String selectorSelectFromAllPages();
    
    String linkErrorLoadingData();
    
    String image();

    String fileUploadLabel();

    String fileUploadNewFileLabel();    

    String fileUploadTitle();    

    String fileChooseLabel();

    String fileListFetchError();

    String fileUploadSubmitLabel();
    
    String fileUploadSubmitError();

    String imageInsertButton();

    String fileUploadNoPathError();

    String fileUpdateListButton();

    String imageSizeLabel();

    String imageAlignmentLabel();

    String imageHorizontalAlignmentLabel();

    String imageVerticalAlignmentLabel();

    String imageAlignLeftLabel();

    String imageAlignCenterLabel();

    String imageAlignRightLabel();

    String imageAlignTopLabel();

    String imageAlignMiddleLabel();

    String imageAlignBottomLabel();

    String imageSettingsLabel();

    String imageAltTextLabel();

    String imageSelectTabTitle();

    String imageSettingsTabTitle();

    String imageNoImageSelectedError();

    String table();

    String tableRowsLabel();

    String tableRowsDefault();

    String tableColsLabel();

    String tableColsDefault();

    String tableBorderLabel();

    String tableBorderDefault();

    String tableHeaderLabel();

    String tablePixel();

    String tableInsertButton();

    String importerToolTip();

    String importerCaption();

    String importerClipboardTabCaption();

    String importerClipboardTabInfoLabel();

    String importerFileTabCaption();

    String importerFileTabInfoLabel();

    String importerFilterStylesCheckBoxCaption();

    String importerImportButtonCaption();

    String importerCancelButtonCaption();

    String wizardCancel();
    
    String wizardPrevious();    
    
    String wizardNext();
    
    String wizardFinish();

    String errorServerRequestFailed();
}
