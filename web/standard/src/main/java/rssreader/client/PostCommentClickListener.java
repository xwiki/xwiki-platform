package rssreader.client;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.Widget;

/**
 * Created by IntelliJ IDEA.
 * User: ldubost
 * Date: 20 janv. 2007
 * Time: 16:40:10
 * To change this template use File | Settings | File Templates.
 */
public class PostCommentClickListener implements ClickListener {
    RSSReader reader;
    String page;
    TextArea commentbox;

    public PostCommentClickListener(RSSReader reader, String page, TextArea commentbox) {
        this.reader = reader;
        this.page = page;
        this.commentbox = commentbox;
    }

    public void onClick(Widget widget) {
        reader.postComment(page, commentbox.getText());
    }
}
