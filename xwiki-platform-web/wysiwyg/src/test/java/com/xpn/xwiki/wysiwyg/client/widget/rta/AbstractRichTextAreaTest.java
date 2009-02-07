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
package com.xpn.xwiki.wysiwyg.client.widget.rta;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.LoadListener;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SourcesLoadEvents;
import com.google.gwt.user.client.ui.Widget;
import com.xpn.xwiki.wysiwyg.client.AbstractWysiwygClientTest;
import com.xpn.xwiki.wysiwyg.client.dom.Element;
import com.xpn.xwiki.wysiwyg.client.dom.Range;
import com.xpn.xwiki.wysiwyg.client.dom.Selection;

/**
 * Base class for tests running on a rich text area.
 * 
 * @version $Id$
 */
public class AbstractRichTextAreaTest extends AbstractWysiwygClientTest implements LoadListener
{
    /**
     * The number of milliseconds we delay the test finish. This delay is needed because in some browsers the rich text
     * area is initialized after a timeout.
     */
    public static final int FINISH_DELAY = 400;

    /**
     * The number of milliseconds we delay the test start. This delay is needed because in some browsers the rich text
     * area is initialized after a timeout and we want to start the test after the rich text area is fully initialized.
     */
    public static final int START_DELAY = 300;

    /**
     * The rich text area on which we run the tests.
     */
    protected RichTextArea rta;

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWysiwygClientTest#gwtSetUp()
     */
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        if (rta == null) {
            rta = new RichTextArea();
            // Workaround till GWT provides a way to detect when the rich text area has finished loading.
            if (rta.getBasicFormatter() != null && rta.getBasicFormatter() instanceof SourcesLoadEvents) {
                ((SourcesLoadEvents) rta.getBasicFormatter()).addLoadListener(this);
            }
        }
        RootPanel.get().add(rta);
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadListener#onLoad(Widget)
     */
    public void onLoad(Widget sender)
    {
        // We have to remove the default body border because it affects the range detection in IE.
        rta.getElement().getStyle().setProperty("border", "1px solid gray");
        rta.getDocument().getBody().getStyle().setProperty("borderStyle", "none");
        // http://wiki.codetalks.org/wiki/index.php/Docs/Keyboard_navigable_JS_widgets
        // #Use_setTimeout_with_element.focus.28.29_to_set_focus
        (new Timer()
        {
            public void run()
            {
                rta.setFocus(true);
            }
        }).schedule(1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see LoadListener#onError(Widget)
     */
    public void onError(Widget sender)
    {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWysiwygClientTest#gwtTearDown()
     */
    protected void gwtTearDown() throws Exception
    {
        super.gwtTearDown();

        RootPanel.get().remove(rta);
    }

    /**
     * Selects the given range.
     * 
     * @param range The range to be selected.
     */
    protected void select(Range range)
    {
        Selection selection = rta.getDocument().getSelection();
        selection.removeAllRanges();
        selection.addRange(range);
    }

    /**
     * Cleans the HTML input. This is needed in order to have uniform tests between Firefox and Internet Explorer.
     * 
     * @param html The HTML fragment to be cleaned.
     * @return The input string in lower case, stripped of new lines.
     */
    protected String clean(String html)
    {
        return html.replaceAll("\r\n", "").toLowerCase();
    }

    /**
     * Removes the non-breaking spaces, {@code &nbsp;}, from the given HTML. The is method is needed because the
     * Selection implementation for Internet Explorer adds {@code &nbsp;} when it cannot place the caret at the
     * specified place.
     * 
     * @param html the HTML fragment to be cleaned of non-breaking spaces
     * @return the input HTML fragment without any non-breaking spaces
     */
    protected String removeNonBreakingSpaces(String html)
    {
        return html.replace("&nbsp;", "");
    }

    /**
     * @return The body element of the DOM document edited with the rich text area.
     */
    protected Element getBody()
    {
        return rta.getDocument().getBody().cast();
    }
}
