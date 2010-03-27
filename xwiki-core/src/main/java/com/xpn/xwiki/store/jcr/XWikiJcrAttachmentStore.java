package com.xpn.xwiki.store.jcr;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiAttachmentContent;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;

import javax.jcr.*;
import javax.transaction.NotSupportedException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

public class XWikiJcrAttachmentStore extends XWikiJcrBaseStore implements XWikiAttachmentStoreInterface {
	public XWikiJcrAttachmentStore(XWiki xwiki, XWikiContext context) throws SecurityException, NoSuchMethodException, ClassNotFoundException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		super(xwiki, context);
	}
	
	public XWikiJcrAttachmentStore(XWikiContext context) throws SecurityException, IllegalArgumentException, NoSuchMethodException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException {
	    this(context.getWiki(), context);
    }
	
	public void saveAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
		saveAttachmentContent(attachment, true, context, bTransaction);		
	}

	public void saveAttachmentContent(final XWikiAttachment attachment, final boolean bParentUpdate, final XWikiContext context, boolean bTransaction) throws XWikiException {
		try {
            final XWikiAttachmentContent content = attachment.getAttachment_content();
            if (content.isContentDirty()) {
                attachment.updateContentArchive(context);
            }
            final XWikiAttachmentArchive archive = attachment.getAttachment_archive();
            
            executeWrite(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws RepositoryException, XWikiException {
					Node docNode = session.getNode( getBaseDocPath(attachment.getDoc()) );
	            	Node dirattNode = JcrUtil.getOrCreateSubNode(docNode, "attach", ntXWikiAttachments);
	            	Node attachNode = JcrUtil.getOrCreateSubNode(dirattNode, attachment.getFilename(), ntXWikiAttachment);
	            	Node attachContentNode = JcrUtil.getOrCreateSubNode(attachNode, "content", ntXWikiAttachmentContent);
                        attachContentNode.setProperty("jcr:data", content.getContentInputStream());
	            	attachContentNode.setProperty("attach", attachNode);
	            	Node attachArchiveNode = JcrUtil.getOrCreateSubNode(attachNode, "archive", ntXWikiAttachmentArchive);
	            	attachArchiveNode.setProperty("jcr:data", new ByteArrayInputStream(archive.getArchive()));
	            	attachArchiveNode.setProperty("attach", attachNode);
	            	if (bParentUpdate)
	                    context.getWiki().getStore().saveXWikiDoc(attachment.getDoc(), context, true);
	            	session.save();
					return null;
				}
            });
        }
        catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_SAVING_ATTACHMENT,
                    "Exception while saving attachment {0} of document {1}", e, args);
        }
	}

    public void saveAttachmentsContent(List attachments, XWikiDocument doc, boolean bParentUpdate, XWikiContext context, boolean bTransaction)  {
        new NotSupportedException().printStackTrace();
    }

    String getAttachmentPath(XWikiAttachment att) {
		return getBaseDocPath( att.getDoc() ) + "/attach/"+att.getFilename();		
	}
	String getAttachmentContentPath(XWikiAttachmentContent ac) {
		return getAttachmentPath( ac.getAttachment() ) + "/content";
	}
	String getAttachmentArchivePath(XWikiAttachmentArchive ac) {
		return getAttachmentPath( ac.getAttachment() ) + "/archive";
	}
	public void loadAttachmentContent(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
		try {
			final XWikiAttachmentContent content = new XWikiAttachmentContent(attachment);
        	attachment.setAttachment_content(content);
        	
        	executeRead(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws RepositoryException, XWikiException, IOException {
					try {
						Node n = session.getNode( getAttachmentContentPath( content ) );
						content.setContent( getBytesFromProp( n.getProperty("jcr:data") ) );
					} catch (PathNotFoundException e) {};
					return content;
				}
        	});
        } catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_LOADING_ATTACHMENT,
                    "Exception while loading attachment {0} of document {1}", e, args);
        }
	}
	
	protected byte[] getBytesFromProp(Property p) throws IOException, ValueFormatException, RepositoryException {		
		byte[] res = new byte[(int) p.getLength()];
		p.getStream().read(res);
		return res;
	}

	public void loadAttachmentArchive(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
		try {
			final XWikiAttachmentArchive archive = new XWikiAttachmentArchive();
        	archive.setAttachment(attachment);
        	attachment.setAttachment_archive(archive);
        	
        	executeRead(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					try {
	            		Node n = session.getNode( getAttachmentArchivePath( archive ) );
	            		archive.setArchive( getBytesFromProp(n.getProperty("jcr:data")) );
	            	} catch (PathNotFoundException e) {}
	            	return archive;
				}
        	});
        } catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_LOADING_ATTACHMENT,
                    "Exception while loading attachment {0} of document {1}", e, args);
        }
	}

	public void deleteXWikiAttachment(XWikiAttachment attachment, XWikiContext context, boolean bTransaction) throws XWikiException {
		deleteXWikiAttachment(attachment, true, context, bTransaction);
	}

	public void deleteXWikiAttachment(final XWikiAttachment attachment, boolean parentUpdate, XWikiContext context, boolean bTransaction) throws XWikiException {
		try {
			executeWrite(context, new JcrCallBack() {
				public Object doInJcr(XWikiJcrSession session) throws Exception {
					session.removeObject(getAttachmentPath( attachment ) );
					return null;
				}
			});
			if (parentUpdate) {
	            List list = attachment.getDoc().getAttachmentList();
	            for (int i=0;i<list.size();i++) {
	                XWikiAttachment attach = (XWikiAttachment) list.get(i);
	                if (attachment.getFilename().equals(attach.getFilename())) {
	                    list.remove(i);
	                    break;
	                }
	            }
	            // save is not needed: context.getWiki().getStore().saveXWikiDoc(attachment.getDoc(), context, false);
            }
        } catch (Exception e) {
            Object[] args = { attachment.getFilename(), attachment.getDoc().getFullName() };
            throw new XWikiException( XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_STORE_JCR_DELETING_ATTACHMENT,
                    "Exception while deleting attachment {0} of document {1}", e, args);
        }
	}

	public void cleanUp(XWikiContext context) {
	}
}
