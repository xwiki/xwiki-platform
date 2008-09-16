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
package com.xpn.xwiki.wysiwyg.client.ui;

import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ChangeListenerCollection;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.ClickListenerCollection;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusListener;
import com.google.gwt.user.client.ui.FocusListenerCollection;
import com.google.gwt.user.client.ui.HasFocus;
import com.google.gwt.user.client.ui.HasHTML;
import com.google.gwt.user.client.ui.HasName;
import com.google.gwt.user.client.ui.Hidden;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerCollection;
import com.google.gwt.user.client.ui.MouseListener;
import com.google.gwt.user.client.ui.MouseListenerCollection;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.SourcesChangeEvents;
import com.google.gwt.user.client.ui.SourcesClickEvents;
import com.google.gwt.user.client.ui.SourcesFocusEvents;
import com.google.gwt.user.client.ui.SourcesMouseEvents;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.CommandManager;
import com.xpn.xwiki.wysiwyg.client.ui.cmd.internal.DefaultCommandManager;
import com.xpn.xwiki.wysiwyg.client.ui.wrap.WrappedRichTextArea;

public class XRichTextArea extends Composite implements HasHTML, HasName, HasFocus, SourcesMouseEvents,
    SourcesClickEvents, SourcesFocusEvents, SourcesChangeEvents, ClickListener, FocusListener, KeyboardListener,
    MouseListener
{
    protected final WrappedRichTextArea rta;

    protected final Hidden value;

    protected CommandManager cm;

    protected final ClickListenerCollection clickListeners = new ClickListenerCollection();

    protected final FocusListenerCollection focusListeners = new FocusListenerCollection();

    protected final KeyboardListenerCollection keyboardListeners = new KeyboardListenerCollection();

    protected final MouseListenerCollection mouseListeners = new MouseListenerCollection();

    protected final ChangeListenerCollection changeListeners = new ChangeListenerCollection();

    public XRichTextArea()
    {
        rta = new WrappedRichTextArea();
        rta.addClickListener(this);
        rta.addFocusListener(this);
        rta.addKeyboardListener(this);
        rta.addMouseListener(this);

        value = new Hidden();
        value.setDefaultValue("");

        cm = new DefaultCommandManager(rta);

        FlowPanel container = new FlowPanel();
        container.add(value);
        container.add(rta);
        initWidget(container);
    }

    /**
     * Custom constructor allowing us to inject a mock command manager. It was mainly added to be used in unit tests.
     * 
     * @param cm Custom command manager
     */
    public XRichTextArea(CommandManager cm)
    {
        this();
        this.cm = cm;
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#getHTML()
     */
    public String getHTML()
    {
        return rta.getHTML();
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#setHTML(String)
     */
    public void setHTML(String html)
    {
        rta.setHTML(html);
        value.setValue(rta.getHTML());
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#getText()
     */
    public String getText()
    {
        return rta.getText();
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#setText(String)
     */
    public void setText(String text)
    {
        rta.setText(text);
        value.setValue(rta.getHTML());
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#addMouseListener(MouseListener)
     */
    public void addMouseListener(MouseListener listener)
    {
        mouseListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#removeMouseListener(MouseListener)
     */
    public void removeMouseListener(MouseListener listener)
    {
        mouseListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#addClickListener(ClickListener)
     */
    public void addClickListener(ClickListener listener)
    {
        clickListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#removeClickListener(ClickListener)
     */
    public void removeClickListener(ClickListener listener)
    {
        clickListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#addFocusListener(FocusListener)
     */
    public void addFocusListener(FocusListener listener)
    {
        focusListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#removeFocusListener(FocusListener)
     */
    public void removeFocusListener(FocusListener listener)
    {
        focusListeners.remove(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#getTabIndex()
     */
    public int getTabIndex()
    {
        return rta.getTabIndex();
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#setAccessKey(char)
     */
    public void setAccessKey(char key)
    {
        rta.setAccessKey(key);
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#setFocus(boolean)
     */
    public void setFocus(boolean focused)
    {
        rta.setFocus(focused);
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#setTabIndex(int)
     */
    public void setTabIndex(int index)
    {
        rta.setTabIndex(index);
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#addKeyboardListener(KeyboardListener)
     */
    public void addKeyboardListener(KeyboardListener listener)
    {
        keyboardListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#removeKeyboardListener(KeyboardListener)
     */
    public void removeKeyboardListener(KeyboardListener listener)
    {
        keyboardListeners.remove(listener);
    }

    /**
     * @see RichTextArea#isEnabled()
     */
    public boolean isEnabled()
    {
        return rta.isEnabled();
    }

    /**
     * @see RichTextArea#setEnabled(boolean)
     */
    public void setEnabled(boolean enabled)
    {
        rta.setEnabled(enabled);
    }

    /**
     * @see RichTextArea#getBasicFormatter()
     */
    public RichTextArea.BasicFormatter getBasicFormatter()
    {
        return rta.getBasicFormatter();
    }

    /**
     * @see RichTextArea#getExtendedFormatter()
     */
    public RichTextArea.ExtendedFormatter getExtendedFormatter()
    {
        return rta.getExtendedFormatter();
    }

    /**
     * @return the {@link CommandManager} associated with this instance.
     */
    public CommandManager getCommandManager()
    {
        return cm;
    }

    /**
     * @see WrappedRichTextArea#addShortcutKey(XShortcutKey)
     */
    public void addShortcutKey(XShortcutKey shortcutKey)
    {
        rta.addShortcutKey(shortcutKey);
    }

    /**
     * @see WrappedRichTextArea#removeShortcutKey(XShortcutKey)
     */
    public void removeShortcutKey(XShortcutKey shortcutKey)
    {
        rta.removeShortcutKey(shortcutKey);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ClickListener#onClick(Widget)
     */
    public void onClick(Widget sender)
    {
        if (sender == rta) {
            clickListeners.fireClick(this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see FocusListener#onFocus(Widget)
     */
    public void onFocus(Widget sender)
    {
        if (sender == rta) {
            focusListeners.fireFocus(this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see FocusListener#onLostFocus(Widget)
     */
    public void onLostFocus(Widget sender)
    {
        if (sender == rta) {
            value.setValue(rta.getHTML());
            focusListeners.fireLostFocus(this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyDown(Widget, char, int)
     */
    public void onKeyDown(Widget sender, char keyCode, int modifiers)
    {
        if (sender == rta) {
            keyboardListeners.fireKeyDown(this, keyCode, modifiers);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyPress(Widget, char, int)
     */
    public void onKeyPress(Widget sender, char keyCode, int modifiers)
    {
        if (sender == rta) {
            keyboardListeners.fireKeyPress(this, keyCode, modifiers);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see KeyboardListener#onKeyUp(Widget, char, int)
     */
    public void onKeyUp(Widget sender, char keyCode, int modifiers)
    {
        if (sender == rta) {
            keyboardListeners.fireKeyUp(this, keyCode, modifiers);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseDown(Widget, int, int)
     */
    public void onMouseDown(Widget sender, int x, int y)
    {
        if (sender == rta) {
            mouseListeners.fireMouseDown(this, x, y);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseEnter(Widget)
     */
    public void onMouseEnter(Widget sender)
    {
        if (sender == rta) {
            mouseListeners.fireMouseEnter(this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseLeave(Widget)
     */
    public void onMouseLeave(Widget sender)
    {
        if (sender == rta) {
            mouseListeners.fireMouseLeave(this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseMove(Widget, int, int)
     */
    public void onMouseMove(Widget sender, int x, int y)
    {
        if (sender == rta) {
            mouseListeners.fireMouseMove(this, x, y);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see MouseListener#onMouseUp(Widget, int, int)
     */
    public void onMouseUp(Widget sender, int x, int y)
    {
        if (sender == rta) {
            mouseListeners.fireMouseUp(this, x, y);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasName#getName()
     */
    public String getName()
    {
        return value.getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see HasName#setName(String)
     */
    public void setName(String name)
    {
        if (name != value.getName() && (name == null || !name.equals(value.getName()))) {
            value.setName(name);
            value.setID(name);
            changeListeners.fireChange(this);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#setWidth(String)
     */
    public void setWidth(String width)
    {
        rta.setWidth(width);
    }

    /**
     * {@inheritDoc}
     * 
     * @see RichTextArea#setHeight(String)
     */
    public void setHeight(String height)
    {
        rta.setHeight(height);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesChangeEvents#addChangeListener(ChangeListener)
     */
    public void addChangeListener(ChangeListener listener)
    {
        changeListeners.add(listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see SourcesChangeEvents#removeChangeListener(ChangeListener)
     */
    public void removeChangeListener(ChangeListener listener)
    {
        changeListeners.remove(listener);
    }
}
