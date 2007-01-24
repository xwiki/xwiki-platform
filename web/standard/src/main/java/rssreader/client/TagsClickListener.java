package rssreader.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.HTML;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 21 janv. 2007
 * Time: 20:35:13
 * To change this template use File | Settings | File Templates.
 */
public class TagsClickListener implements ClickListener {
    RSSReader reader;
    String page;
    String tags;
    HTML tagshtml;

    public TagsClickListener(RSSReader rssReader, String page, String tags, HTML tagshtml) {
        this.reader = rssReader;
        this.page = page;
        this.tags = tags;
        this.tagshtml = tagshtml;
    }

    public void onClick(Widget widget) {
        reader.showTagsForm(page, tags, tagshtml, widget.getAbsoluteLeft(), widget.getAbsoluteTop());
    }
}
