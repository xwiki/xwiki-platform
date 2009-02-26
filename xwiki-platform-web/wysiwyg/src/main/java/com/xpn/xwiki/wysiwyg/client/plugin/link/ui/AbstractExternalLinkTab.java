package com.xpn.xwiki.wysiwyg.client.plugin.link.ui;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.editor.Strings;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkConfig;
import com.xpn.xwiki.wysiwyg.client.plugin.link.LinkHTMLGenerator;

/**
 * Superclass for the tabs to create links to external resources. Subclasses should implement the labels and default
 * texts provider functions, as well as the url creation method from the user input.
 * 
 * @version $Id$
 */
public abstract class AbstractExternalLinkTab extends AbstractHasLinkTab implements ClickListener, FocusListener
{
    /**
     * The text box where the user will insert the uri of the resource.
     */
    private final TextBox uriTextBox;

    /**
     * The link creation button.
     */
    private final Button createLinkButton;

    /**
     * Class constructor.
     */
    public AbstractExternalLinkTab()
    {
        Label urlLabel = new Label(getURILabel());
        Label labelLabel = new Label(Strings.INSTANCE.linkLabelLabel());
        createLinkButton = new Button(Strings.INSTANCE.linkCreateLinkButon());
        createLinkButton.addClickListener(this);
        createLinkButton.setTitle(getCreateLinkButtonTooltip());

        EnterListener enterListener = new EnterListener(createLinkButton);
        uriTextBox = new TextBox();
        uriTextBox.addFocusListener(this);
        uriTextBox.addKeyboardListener(enterListener);
        uriTextBox.setTitle(getUriTextBoxTooltip());

        getLabelTextBox().addKeyboardListener(enterListener);
        getLabelTextBox().setTitle(getLabelTextBoxTooltip());

        FlowPanel mainPanel = new FlowPanel();
        mainPanel.addStyleName("xLinkToUrl");
        FlowPanel labelPanel = new FlowPanel();
        labelPanel.addStyleName("label");
        labelPanel.add(labelLabel);
        labelPanel.add(getLabelTextBox());
        FlowPanel urlPanel = new FlowPanel();
        urlPanel.addStyleName("url");
        urlPanel.add(urlLabel);
        urlPanel.add(uriTextBox);

        mainPanel.add(labelPanel);
        mainPanel.add(urlPanel);
        mainPanel.add(createLinkButton);

        initWidget(mainPanel);
    }

    /**
     * @return the label text for the particular external resource link to be created.
     */
    protected abstract String getURILabel();

    /**
     * @return the default input text for the text box holding the external url.
     */
    protected abstract String getInputDefaultText();

    /**
     * @return the error message to be displayed when the user uri is missing.
     */
    protected abstract String getErrorMessage();

    /**
     * Builds an URI to the external resource to be linked from the user input, adding protocols, parsing user input,
     * etc.
     * 
     * @return the URI to the external resource from the user input.
     */
    protected abstract String buildUri();

    /**
     * @return the tooltip for create link button.
     */
    protected String getCreateLinkButtonTooltip()
    {
        return "";
    }

    /**
     * @return the tooltip for uri text box.
     */
    protected String getUriTextBoxTooltip()
    {
        return "";
    }

    /**
     * @return the tooltip for label text box.
     */
    protected String getLabelTextBoxTooltip()
    {
        return "";
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == createLinkButton) {
            if (!validateUserInput()) {
                setLink(null);
            } else {
                setLink(LinkHTMLGenerator.getInstance().getExternalLink(getLinkLabel(), buildUri()));
                getClickListeners().fireClick(this);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasLink#initialize()
     */
    public void initialize()
    {
        if (uriTextBox.getText().trim().length() == 0) {
            uriTextBox.setText(getInputDefaultText());
        }
        if (getLabelTextBox().getText().trim().length() == 0) {
            getLabelTextBox().setFocus(true);
        } else {
            uriTextBox.setFocus(true);
            uriTextBox.selectAll();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasLink#validateUserInput()
     */
    public boolean validateUserInput()
    {
        // Check the super class validation result
        if (!super.validateUserInput()) {
            return false;
        }
        // The url inserted by the user must not be void. Check that
        if (this.uriTextBox.getText().trim().length() == 0 || this.uriTextBox.getText().equals(getInputDefaultText())) {
            Window.alert(getErrorMessage());
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see FocusListener#onFocus(Widget)
     */
    public void onFocus(Widget sender)
    {
        if (sender == uriTextBox && uriTextBox.getText().trim().equals(getInputDefaultText())) {
            uriTextBox.selectAll();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see FocusListener#onLostFocus(Widget)
     */
    public void onLostFocus(Widget sender)
    {
        // ignore
    }

    /**
     * @return the text box for the url insert
     */
    protected TextBox getUriTextBox()
    {
        return uriTextBox;
    }

    /**
     * @return the button for link creation
     */
    protected Button getCreateLinkButton()
    {
        return createLinkButton;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractHasLinkTab#setLinkConfig(LinkConfig)
     */
    public void setLinkConfig(LinkConfig config)
    {
        if (config.getType() == getLinkType()) {
            // set super's config
            super.setLinkConfig(config);
            // now get the link's url and set it in this tab's text
            if (config.getUrl() != null) {
                uriTextBox.setText(config.getUrl());
            }
        } else {
            setLinkLabel(config);
        }
    }
}
