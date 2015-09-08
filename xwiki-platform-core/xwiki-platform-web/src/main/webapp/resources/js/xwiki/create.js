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
require(['jquery', 'xwiki-meta'], function($, xm) {
  $(document).ready(function() {

    var form = $('#create');
    
    /**
     * Compute the page name of the target
     */
    var computeTargetPageName = function() {
      var documentReference = new XWiki.DocumentReference();
      var parentReference = $('#ParentReference').val();
      if (parentReference == '') {
        // No need to check if the document is not terminal because of the document is terminal the parent reference
        // cannot be null and the form is not submitted
        documentReference = new XWiki.DocumentReference(xm.wiki, $('#Name').val(), 'WebHome');
      } else {
        var spaceReference = XWiki.Model.resolve(parentReference, XWiki.EntityType.SPACE);
        if ($('#terminal').prop('checked')) {
          documentReference = new XWiki.EntityReference($('#Name').val(), XWiki.EntityType.DOCUMENT, spaceReference);
        } else {
          var parent = new XWiki.EntityReference($('#Name').val(), XWiki.EntityType.SPACE, spaceReference);
          documentReference = new XWiki.EntityReference('WebHome', XWiki.EntityType.DOCUMENT, parent);
        }
      }
      
      return XWiki.Model.serialize(documentReference.relativeTo(new XWiki.WikiReference(xm.wiki)));
    };
    
    /**
     * Add an hidden input that indicates which template to use
     */
    var setTemplateProvider = function (value) {
      // Note: the input might already exists if the user press the "back" button after having submitted the form once.
      var templateProvider = $('#templateprovider');
      if (templateProvider.length == 0) {
        // So we create it only if it does not already exist
        templateProvider = $('<input type="hidden" name="templateprovider" id="templateprovider"/>').appendTo(form);
      }
      templateProvider.attr('value', value);
    };

    /**
     * Set the correct template or redirect to the correct page when the form is submitted
     */
    form.submit(function() {
      // Get the type of the page to create
      var typeField = $('input[name="type"]:checked');
      var type = typeField.attr('data-type');
      
      // A blank document
      if (type == 'blank') {
        // The 'templateprovider' field is needed by the CreateAction, even empty
        setTemplateProvider('');
        return true;
      }
      
      // A document from a template
      if (type == 'template') {
        var templateName = typeField.val();
        setTemplateProvider(templateName);
        return true;
      }
     
      // An office document: we redirect to the office importer
      // TODO: handle this use-case with an extension point
      if (type == 'office') {
        // The office importer is a wiki page which takes the 'page' parameter.
        // So we compute this parameter and we redirect the form to the Office Importer document.
        window.location = new XWiki.Document(new XWiki.DocumentReference(xm.wiki, ['XWiki'], 'OfficeImporter')).getURL('view', 'page=' + encodeURIComponent(computeTargetPageName()));
        return false;
      }
      
    });
  
  });
});
