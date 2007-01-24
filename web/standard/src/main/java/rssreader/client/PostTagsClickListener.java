package rssreader.client;

import com.google.gwt.user.client.ui.*;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 21 janv. 2007
 * Time: 20:42:59
 * To change this template use File | Settings | File Templates.
 */
public class PostTagsClickListener implements ClickListener {
    RSSReader reader;
    String page;
    TextBox textbox;
    HTML tagshtml;
    
    public PostTagsClickListener(RSSReader rssReader, String page, TextBox textbox, HTML tagshtml) {
        this.reader = rssReader;
        this.page = page;
        this.textbox = textbox;
        this.tagshtml = tagshtml;
    }

    public void onClick(Widget widget) {
        reader.postTags(page, textbox.getText(), tagshtml);
    }
}
