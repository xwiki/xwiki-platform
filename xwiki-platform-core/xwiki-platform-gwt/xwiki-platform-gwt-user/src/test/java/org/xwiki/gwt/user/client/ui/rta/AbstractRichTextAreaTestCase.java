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
package org.xwiki.gwt.user.client.ui.rta;

import org.xwiki.gwt.dom.client.Element;
import org.xwiki.gwt.dom.client.Range;
import org.xwiki.gwt.dom.client.Selection;
import org.xwiki.gwt.user.client.FocusCommand;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.junit.DoNotRunWith;
import com.google.gwt.junit.Platform;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Base class for tests running on a rich text area.
 * <p>
 * NOTE: HtmlUnit doesn't fire the load event on in-line frames. For now all test cases derived from this class are
 * skipped when running on HtmlUnit.
 * 
 * @version $Id$
 */
@DoNotRunWith(Platform.HtmlUnitBug)
public abstract class AbstractRichTextAreaTestCase extends GWTTestCase implements LoadHandler
{
    /**
     * The number of milliseconds we delay the test finish. This delay is needed because the test start is delayed till
     * the rich text area is fully initialized.
     */
    public static final int FINISH_DELAY = 400;

    /**
     * The rich text area on which we run the tests.
     */
    protected RichTextArea rta;

    @Override
    protected void gwtSetUp() throws Exception
    {
        super.gwtSetUp();

        if (rta == null) {
            rta = new RichTextArea();
            rta.addLoadHandler(this);
        }
        RootPanel.get().add(rta);
        initializeRichTextArea();
    }

    /**
     * @return the initial content of the rich text area
     */
    protected String getInitialContent()
    {
        StringBuffer content = new StringBuffer();
        content.append("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n");
        content.append("  \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n");
        content.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
        content.append("<head></head>\n");
        content.append("  <title>Rich Text Area Test</title>\n");
        content.append("</head>\n");
        content.append("<body></body>\n");
        content.append("</html>");
        return content.toString();
    }

    /**
     * Initializes the rich text area used for testing.
     */
    private native void initializeRichTextArea()
    /*-{
        var rta = this.@org.xwiki.gwt.user.client.ui.rta.AbstractRichTextAreaTestCase::rta;
        var iframe = rta.@com.google.gwt.user.client.ui.UIObject::getElement()();
        var content = this.@org.xwiki.gwt.user.client.ui.rta.AbstractRichTextAreaTestCase::getInitialContent()();
        iframe.contentWindow.content = content;
        iframe.contentWindow.location = 'javascript:window.content';
    }-*/;

    @Override
    public void onLoad(LoadEvent event)
    {
        // http://wiki.codetalks.org/wiki/index.php/Docs/Keyboard_navigable_JS_widgets
        // #Use_setTimeout_with_element.focus.28.29_to_set_focus
        Scheduler.get().scheduleDeferred(new FocusCommand(rta));
    }

    @Override
    protected void gwtTearDown() throws Exception
    {
        super.gwtTearDown();

        RootPanel.get().remove(rta);
    }

    /**
     * Runs the test specified by the given command after the rich text area finished loading.
     * 
     * @param command the test to be deferred
     */
    protected void deferTest(final Command command)
    {
        delayTestFinish(FINISH_DELAY);
        final HandlerRegistration[] registrations = new HandlerRegistration[1];
        registrations[0] = rta.addLoadHandler(new LoadHandler()
        {
            public void onLoad(LoadEvent event)
            {
                // Make sure this handler is called only once.
                registrations[0].removeHandler();
                // Run the test after the rich text area is focused.
                Scheduler.get().scheduleDeferred(new Command()
                {
                    public void execute()
                    {
                        command.execute();
                        finishTest();
                    }
                });
            }
        });
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
     * Inserts the given HTML fragment at the caret position or in place of the current selection. This method doesn't
     * use the insert HTML command, but instead it uses the Range API, which is DOM oriented and doesn't know about the
     * HTML syntax. As a consequence this method can lead to an invalid HTML DOM if it is called on the wrong selection.
     * 
     * @param html the HTML fragment to be inserted
     */
    protected void insertHTML(String html)
    {
        Selection selection = rta.getDocument().getSelection();
        Range range = selection.getRangeAt(0);
        // Parse the given HTML fragment.
        Element container = Element.as(rta.getDocument().createDivElement());
        container.xSetInnerHTML(html);
        // Replace the selection.
        range.deleteContents();
        range.insertNode(container.extractContents());
        // Update the selection.
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
