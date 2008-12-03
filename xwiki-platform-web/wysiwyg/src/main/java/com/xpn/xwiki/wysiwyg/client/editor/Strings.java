package com.xpn.xwiki.wysiwyg.client.editor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.Constants;

/**
 * This {@link Constants} interface is used to make user interface strings internationalizable.
 */
public interface Strings extends Constants
{
    Strings INSTANCE = (Strings) GWT.create(Strings.class);

    String apply();

    String attachment();

    String backColor();

    String bold();

    String cancel();

    String charmap();

    String close();

    String code();

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

    String h1();

    String h2();

    String h3();

    String h4();

    String h5();

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

    String normal();

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

    String linkCreateLinkButon();

    String linkNewPageTab();

    String linkEmailTab();

    String linkExistingPageTab();

    String linkWebPageTab();
    
    String linkLabelLabel();

    String linkWebPageLabel();

    String linkEmailLabel();

    String linkNewSpaceLabel();

    String linkNewPageLabel();

    String linkEmailAddressTextBox();

    String linkWebPageTextBox();

    String linkNewSpaceTextBox();

    String linkNewPageTextBox();

    String linkCreateNewSpaceText();

    String linkNewSpaceError();

    String linkNewPageError();

    String linkEmailAddressError();

    String linkWebPageAddressError();
   
    String linkNoLabelError();
    
    String linkToWikiButtonTooltip();

    String linkToSpaceButtonTooltip();

    String linkToNewPageButtonTooltip();

    String linkToExistingPageButtonTooltip();

    String linkToWebPageButtonTooltip();

    String linkToEmailAddressButtonTooltip();

    String linkWikiSelectorTooltip();

    String linkSpaceSelectorTooltip();

    String linkExistingSpacesListBoxTooltip();

    String linkPageSelectorTooltip();

    String linkExistingPageLabelTextBoxTooltip();

    String linkNewPageLabelTextBoxTooltip();

    String linkWebPageLabelTextBoxTooltip();

    String linkEmailAddressLabelTextBoxTooltip();

    String linkUriToEmailAddressTextBoxTooltip();

    String linkUriToWebPageTextBoxTooltip();

    String linkNewSpaceTextBoxTooltip();

    String linkNewPageTextBoxTooltip();

    String image();
    
    String fileUploadLabel();
    
    String fileChooseLabel();
    
    String fileListFetchError();
    
    String fileUploadSubmitLabel();
    
    String fileInsertImageButton();
    
    String fileUploadNoPathError();
    
    String fileUpdateListButton();
}