/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 23 avr. 2005
 * Time: 00:57:33
 */
package com.xpn.xwiki.plugin.mail;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

import javax.mail.*;
import java.util.Properties;

public class MailPluginApi extends Api {
    private MailPlugin plugin;

    public MailPluginApi(MailPlugin plugin, XWikiContext context) {
            super(context);
            setPlugin(plugin);
        }

    public MailPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(MailPlugin plugin) {
        this.plugin = plugin;
    }

    public int checkMail(String provider, String server, String user, String password) throws MessagingException {

        // Get a session.  Use a blank Properties object.
        Session session = Session.getInstance(new Properties());

        // Get a Store object
        Store store = session.getStore(provider);
        store.connect(server, user, password);

        try {
            // Get "INBOX"
            Folder fldr = store.getFolder("INBOX");
            fldr.open(Folder.READ_ONLY);
            int count = fldr.getMessageCount();
            return count;
        } finally {
            store.close();
        }
    }

    public Message[] getMailHeaders(String provider, String server, String user, String password) throws MessagingException {
         // Get a session.  Use a blank Properties object.
         Session session = Session.getInstance(new Properties());

         // Get a Store object
         Store store = session.getStore(provider);
         store.connect(server, user, password);

         try {
             // Get "INBOX"
             Folder fldr = store.getFolder("INBOX");
             fldr.open(Folder.READ_ONLY);
             Message[] messages = new Message[fldr.getMessageCount()];
             FetchProfile profile = new FetchProfile();
             profile.add(FetchProfile.Item.CONTENT_INFO);
             profile.add(FetchProfile.Item.ENVELOPE);
             profile.add(FetchProfile.Item.FLAGS);
             fldr.fetch(messages, profile);
             return messages;
         } finally {
             store.close();
         }
     }

    public Message[] getMail(String provider, String server, String user, String password) throws MessagingException {
        // Get a session.  Use a blank Properties object.
        Session session = Session.getInstance(new Properties());

        // Get a Store object
        Store store = session.getStore(provider);
        store.connect(server, user, password);

        try {
            // Get "INBOX"
            Folder fldr = store.getFolder("INBOX");
            fldr.open(Folder.READ_ONLY);
            return fldr.getMessages();
        } finally {
            store.close();
        }
    }

    public Store getStore(String provider) throws MessagingException {
        // Get a session.  Use a blank Properties object.
        Session session = Session.getInstance(new Properties());

        // Get a Store object
        Store store = session.getStore(provider);
        return store;
    }
}