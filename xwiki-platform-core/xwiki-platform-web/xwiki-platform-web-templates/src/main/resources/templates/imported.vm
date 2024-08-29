## ---------------------------------------------------------------------------
## See the NOTICE file distributed with this work for additional
## information regarding copyright ownership.
##
## This is free software; you can redistribute it and/or modify it
## under the terms of the GNU Lesser General Public License as
## published by the Free Software Foundation; either version 2.1 of
## the License, or (at your option) any later version.
##
## This software is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
## Lesser General Public License for more details.
##
## You should have received a copy of the GNU Lesser General Public
## License along with this software; if not, write to the Free
## Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
## 02110-1301 USA, or see the FSF site: http://www.fsf.org.
## ---------------------------------------------------------------------------
#template("xwikivars.vm")
#macro(showfilelist $list $text)
  #if($list.size()>0)
    <h4 class="legend">
      $escapetool.xml($services.localization.render("import_listof${text}files"))
    </h4>
    <ul>
      #foreach($item in $list)
        <li><a href="$xwiki.getURL($item)">$escapetool.xml($item)</a></li>
      #end
    </ul>
  #end
#end
#if($hasAdmin)
  #set($status = $escapetool.xml($services.localization.render("import_install_${xwiki.package.status}")))
  #info("$escapetool.xml($services.localization.render('importing')) $!escapetool.xml($request.name): $status")
  <ul>
    <li>$xwiki.package.installed.size() $escapetool.xml($services.localization.render('import_documentinstalled'))</li>
    <li>$xwiki.package.skipped.size() $escapetool.xml($services.localization.render('import_documentskipped'))</li>
    <li>$xwiki.package.errors.size() $escapetool.xml($services.localization.render('import_documenterrors'))</li>
  </ul>
  #showfilelist($xwiki.package.installed 'installed')
  #showfilelist($xwiki.package.skipped 'skipped')
  #showfilelist($xwiki.package.errors 'error')
#else
  ## If the current user does not have admin and this template is being displayed
  ## it means security settings have been changed with the import (probably a defaut XE XAR import)
  ## We display a warning and invite the user to log in to further administrate the wiki
  #warning("$services.localization.render('core.importer.securitySettingsChanged', [$xwiki.getURL('XWiki.XWikiLogin', 'login', 'loginLink=1')])")
#end