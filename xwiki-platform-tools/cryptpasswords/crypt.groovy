package xwiki.export;

import com.xpn.xwiki.*;
import com.xpn.xwiki.objects.classes.*;

println "Starting password encryption"
println "===== WARNING!!! ====="
println "This action will encrypt all the plaintext passwords in the database."
println ""
println "Depending on the encryption method, you may not be able to restore"
println "  the original passwords, disabling some features like retrieving"
println "  forgot passwords. However, it is recommended to encrypt the passwords,"
println "  as it increases the security of the wiki."

println "Continue? [y/n]"
cont = System.in.readLine()

if(cont.toLowerCase().startsWith("n")) {
    println "Good."
    System.exit(0);
}

context = new XWikiContext();

context.setUser("XWiki.superadmin");
config = new XWikiConfig();
config.put("xwiki.store.class", "com.xpn.xwiki.store.XWikiHibernateStore");
config.put("xwiki.store.hibernate.path", "./hibernate.cfg.xml");
config.put("xwiki.store.hibernate.updateschema", "1");
config.put("xwiki.virtual", "1");
println "Starting xwiki"
xwiki = new XWiki(config, context);
context.setWiki(xwiki);

db = "xwiki"
// Change this to export a different database
// db = "mydb"

println("Encrypting all passwords in database " + db)
context.setDatabase(db);

classes = xwiki.getClassList(context)
for(xclassname in classes) {
  // See if this class has password fields
  xclass = xwiki.getDocument(xclassname, context).getxWikiClass();
  props = xclass.getProperties();
  for(prop in props) {
    if(prop instanceof com.xpn.xwiki.objects.classes.PasswordClass){
      sql = ", BaseObject as obj where obj.name=doc.fullName and obj.className='" + xclassname + "'";
      results = xwiki.getStore().searchDocumentsNames(sql, context);
      for(docname in results) {
        doc = xwiki.getDocument(docname, context);
        for(obj in doc.getObjects(xclassname)){
	  pwdprop = obj.get(prop.getName());
	  unencryptedPassword = pwdprop.getValue();
          if(unencryptedPassword.startsWith("hash:") || unencryptedPassword.startsWith("crypt:")) {
            println("Skipping already encrypted password for " + doc.getFullName());
          } else {
            encryptedPassword = prop.getProcessedPassword(unencryptedPassword);
            println("Encrypting password for " + doc.getFullName());
            pwdprop.setValue(encryptedPassword);
            doc.setMetaDataDirty(true);
            xwiki.saveDocument(doc, context);
          }
	}
      }
    }
  }
}
/*
sql = ", BaseObject as obj where obj.name="+ xwiki.getFullNameSQL()+ " and obj.className='XWiki.XWikiUsers'";
results = xwiki.getStore().searchDocumentsNames(sql, context);
results.each { item -> print "${item}-" }
println "";
*/
println "Finished."
System.exit(0)