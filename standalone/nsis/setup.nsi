# NOte: This file must be save using a ISO-8859-1 encoding.

Name XWiki
SetCompressor /SOLID lzma
;ComponentText $(^ComponentsIntro) $(^ComponentsInstType) $(^ComponentsComponent)
UninstallText $(^UninstallWarning)
InstType $(^TypicalInstType)
InstType $(^UpdateInstType)
;InstType $(^Custom)


# Defines
!define REGKEY "SOFTWARE\$(^Name)"
!define VERSION 1.1
!define COMPANY XWiki
!define URL http://www.xwiki.org

# MUI defines
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\modern-install.ico"
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_STARTMENUPAGE_REGISTRY_ROOT HKLM
;!define MUI_STARTMENUPAGE_NODISABLE
!define MUI_STARTMENUPAGE_REGISTRY_KEY Software\$(^Name)
!define MUI_STARTMENUPAGE_REGISTRY_VALUENAME StartMenuGroup
!define MUI_STARTMENUPAGE_DEFAULT_FOLDER $(^Name)
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\modern-uninstall.ico"
!define MUI_UNFINISHPAGE_NOAUTOCLOSE
!define MUI_LANGDLL_REGISTRY_ROOT HKLM
!define MUI_LANGDLL_REGISTRY_KEY Software\$(^Name)
!define MUI_LANGDLL_REGISTRY_VALUENAME LANGDLL
!define MUI_COMPONENTSPAGE_TEXT_INSTTYPE $(^SelectComponent)
;!define MUI_COMPONENTSPAGE_SMALLDESC
!define MUI_FINISHPAGE_LINK $(^XWikiMoreInformation)
!define MUI_FINISHPAGE_LINK_LOCATION "http://www.xwiki.org"
!define MUI_FINISHPAGE_RUN
!define MUI_FINISHPAGE_RUN_TEXT $(^RunText)
!define MUI_FINISHPAGE_RUN_FUNCTION RunXWiki
!define MUI_FINISHPAGE_TEXT $(^FinishText)
!define MUI_FINISHPAGE_TEXT_LARGE
!define MUI_FINISHPAGE_TITLE $(^FinishTitle)

# java detection defines
!define GET_JAVA_URL "http://www.java.com"

# Included files
!include Sections.nsh
!include MUI.nsh


;--------------------------------
;Reserve Files

  ;These files should be inserted before other files in the data block
  ;Keep these lines before any File command
  ;Only for solid compression (by default, solid compression is enabled for BZIP2 and LZMA)

  ReserveFile "config_port_page.ini"
  !insertmacro MUI_RESERVEFILE_INSTALLOPTIONS ;InstallOptions plug-in
  !insertmacro MUI_RESERVEFILE_LANGDLL ;Language selection dialog


# Variables
Var StartMenuGroup
Var ServerPort
Var JAVA_HOME
Var JAVA_VER
Var JAVA_INSTALLATION_MSG
Var StartStopFilesCreated
 

# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE ..\..\license.txt
!insertmacro MUI_PAGE_DIRECTORY
!define MUI_PAGE_CUSTOMFUNCTION_PRE PreComponentPage
!insertmacro MUI_PAGE_COMPONENTS

; allow us to skip the shortcut page if no shortcuts are to be installed
;!define MUI_PAGE_CUSTOMFUNCTION_PRE StartMenuPre
!insertmacro MUI_PAGE_STARTMENU Application $StartMenuGroup
Page custom  ServerPortPage ServerPortPageLeave
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Installer languages
!insertmacro MUI_LANGUAGE English
!insertmacro MUI_LANGUAGE French

# Installer attributes
OutFile setup.exe
InstallDir $PROGRAMFILES\XWiki
CRCCheck on
XPStyle on
ShowInstDetails hide
VIProductVersion 1.1.0.0
VIAddVersionKey /lang=${LANG_ENGLISH} ProductName XWiki
VIAddVersionKey ProductVersion "${VERSION}"
VIAddVersionKey /lang=${LANG_ENGLISH} CompanyName "${COMPANY}"
VIAddVersionKey /lang=${LANG_ENGLISH} CompanyWebsite "${URL}"
VIAddVersionKey /lang=${LANG_ENGLISH} FileVersion ""
VIAddVersionKey /lang=${LANG_ENGLISH} FileDescription ""
VIAddVersionKey /lang=${LANG_ENGLISH} LegalCopyright ""
InstallDirRegKey HKLM "${REGKEY}" Path
ShowUninstDetails hide


# Installer sections


Section -Main SEC_MAIN
    SectionIn 1 2 3 RO
    SetOutPath $INSTDIR\xwikionjetty
    SetOverwrite on
    File /r /x db ..\..\release\xwikionjetty\*
    WriteRegStr HKLM "${REGKEY}\Components" Main 1
SectionEnd

Section -Shortcuts SEC_SHORTCUTS

    IntCmp $StartStopFilesCreated 1 DoS
    Call CreateStartFile
    Call CreateStopFile
    StrCpy $StartStopFilesCreated "1"

    DoS:
    
    ; this won't be executed if the user has checked the 'no shortcuts' box
    !insertmacro MUI_STARTMENU_WRITE_BEGIN Application
    ; this will create the start menu group in which the links will appear
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    ; this set the 'working directory' for the links
    SetOutPath $INSTDIR\xwikionjetty
    CreateShortCut "$SMPROGRAMS\$StartMenuGroup\$(^StartXWikiLink).lnk" $INSTDIR\xwikionjetty\start_xwiki.bat "" "" "" SW_SHOWMINIMIZED "" $(^XWikiStartDescription)
    CreateShortCut "$SMPROGRAMS\$StartMenuGroup\$(^StopXWikiLink).lnk" $INSTDIR\xwikionjetty\stop_xwiki.bat "" "" "" SW_SHOWMINIMIZED "" $(^XWikiStopDescription)
    CreateShortCut "$SMPROGRAMS\$StartMenuGroup\$(^UninstallLink).lnk" $INSTDIR\uninstall.exe
    WriteRegStr HKLM "${REGKEY}\Components" Shortcuts 1
    !insertmacro MUI_STARTMENU_WRITE_END
    

SectionEnd

Section DesktopShortcuts SEC_DESKTOPSHORTCUTS
    IntCmp $StartStopFilesCreated 1 DoDS
    Call CreateStartFile
    Call CreateStopFile
    StrCpy $StartStopFilesCreated "1"
    
    DoDS:
    SetOutPath $INSTDIR\xwikionjetty
    CreateShortCut "$DESKTOP\$(^StartXWikiLink).lnk" $INSTDIR\xwikionjetty\start_xwiki.bat "" "" "" SW_SHOWMINIMIZED "" $(^XWikiStartDescription)
    CreateShortCut "$DESKTOP\$(^StopXWikiLink).lnk" $INSTDIR\xwikionjetty\stop_xwiki.bat "" "" "" SW_SHOWMINIMIZED "" $(^XWikiStopDescription)
    WriteRegStr HKLM "${REGKEY}\Components" DesktopShortcuts 1

SectionEnd

Section DatabaseBootstrap SEC_DATABASEBOOTSTRAP
    SectionIn 1 3
    
    SetOutPath $INSTDIR\xwikionjetty\db
    SetOverwrite on
    File ..\..\release\xwikionjetty\db\*
    
SectionEnd

Section -post SEC0003
    WriteRegStr HKLM "${REGKEY}" Path $INSTDIR
    WriteUninstaller $INSTDIR\uninstall.exe
    
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayName "$(^Name)"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayVersion "${VERSION}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" Publisher "${COMPANY}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" URLInfoAbout "${URL}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayIcon $INSTDIR\uninstall.exe
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" UninstallString $INSTDIR\uninstall.exe
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoModify 1
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoRepair 1
SectionEnd

# Macro for selecting uninstaller sections
!macro SELECT_UNSECTION SECTION_NAME UNSECTION_ID
    Push $R0
    ReadRegStr $R0 HKLM "${REGKEY}\Components" "${SECTION_NAME}"
    StrCmp $R0 1 0 next${UNSECTION_ID}
    !insertmacro SelectSection "${UNSECTION_ID}"
    Goto done${UNSECTION_ID}
next${UNSECTION_ID}:
    !insertmacro UnselectSection "${UNSECTION_ID}"
done${UNSECTION_ID}:
    Pop $R0
!macroend

# Uninstaller sections
Section /o un.Main UNSEC0000
    RMDir /r /REBOOTOK $INSTDIR\xwikionjetty
    DeleteRegValue HKLM "${REGKEY}\Components" Main
SectionEnd

Section /o un.Shortcuts UNSEC0001
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\$(^UninstallLink).lnk"
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\$(^StopXWikiLink).lnk"
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\$(^StartXWikiLink).lnk"

SectionEnd

Section /o un.DesktopShortcuts UNSEC0002
    Delete /REBOOTOK "$DESKTOP\$(^StopXWikiLink).lnk"
    Delete /REBOOTOK "$DESKTOP\$(^StartXWikiLink).lnk"

SectionEnd

Section un.post UNSEC0003
    DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)"
    Delete /REBOOTOK $INSTDIR\uninstall.exe
    DeleteRegValue HKLM "${REGKEY}" StartMenuGroup
    DeleteRegValue HKLM "${REGKEY}" Path
    DeleteRegKey /ifempty HKLM "${REGKEY}\Components"
    DeleteRegKey /ifempty HKLM "${REGKEY}"
    RMDir /REBOOTOK $SMPROGRAMS\$StartMenuGroup
    RMDir /REBOOTOK $INSTDIR
SectionEnd

# Installer functions
# Installer component descriptions
   !insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
     !insertmacro MUI_DESCRIPTION_TEXT ${SEC_MAIN} $(^XWikiFilesDescription)
     !insertmacro MUI_DESCRIPTION_TEXT ${SEC_DESKTOPSHORTCUTS} $(^DesktopShortcutsDescription)
     !insertmacro MUI_DESCRIPTION_TEXT ${SEC_DATABASEBOOTSTRAP} $(^DatabaseBootstrapDescription)
     
   !insertmacro MUI_FUNCTION_DESCRIPTION_END


Function .onInit
    InitPluginsDir
    
    Call LocateJVM
    StrCmp "" $JAVA_INSTALLATION_MSG Success OpenBrowserToGetJava
 
    Success:
        ;Call SetEnv
        Goto Done
 
    OpenBrowserToGetJava:
        MessageBox MB_OK $JAVA_INSTALLATION_MSG
        ExecShell "open" "${GET_JAVA_URL}"    
        Abort    
    Done:
    
    !insertmacro MUI_LANGDLL_DISPLAY
    !insertmacro MUI_INSTALLOPTIONS_EXTRACT_AS "config_port_page.ini" "ServerPort"
    
    StrCpy $StartStopFilesCreated "0"
FunctionEnd

# Uninstaller functions
Function un.onInit
    !insertmacro MUI_UNGETLANGUAGE
    ReadRegStr $INSTDIR HKLM "${REGKEY}" Path
    ReadRegStr $StartMenuGroup HKLM "${REGKEY}" StartMenuGroup
    !insertmacro SELECT_UNSECTION Main ${UNSEC0000}
    !insertmacro SELECT_UNSECTION Shortcuts ${UNSEC0001}
    !insertmacro SELECT_UNSECTION DesktopShortcuts ${UNSEC0002}
FunctionEnd

# CUSTOM PAGE.
# =========================================================================
# Get server port number to be used by jetty.
Function ServerPortPage

   !insertmacro MUI_HEADER_TEXT "$(^ServerPortTitle)" "$(^ServerPortSubtitle)"

   # insert localized labels
   !insertmacro MUI_INSTALLOPTIONS_WRITE "ServerPort" "Field 1" "Text" $(^ConfigPortPrompt)
   !insertmacro MUI_INSTALLOPTIONS_WRITE "ServerPort" "Field 2" "Text" $(^ConfigPortPort)
   
   # Display the page.
   !insertmacro MUI_INSTALLOPTIONS_DISPLAY "ServerPort"

   # Get the user entered values.
   !insertmacro MUI_INSTALLOPTIONS_READ $ServerPort "ServerPort" "Field 3" "State"
   ;!insertmacro MUI_INSTALLOPTIONS_READ $Password "UserPass" "Field 4" "State"

FunctionEnd

Function ServerPortPageLeave

    Push $0
    !insertmacro MUI_INSTALLOPTIONS_READ $0 "ServerPort" "Field 3" "State"
    IntCmpU $0 1024 0 0 checkMax
    MessageBox MB_OK|MB_ICONSTOP $(^BadPortNumberTooLow)
    Abort
    
    checkMax:
    IntCmpU $0 65535 0 portOK 0
    MessageBox MB_OK|MB_ICONSTOP $(^BadPortNumberTooHigh)
    Abort
    
    portOk:
    
    Pop $0
         
FunctionEnd

Function PreComponentPage

     SectionSetText ${SEC_MAIN} $(^XWikiComponent)
     ;SectionSetText ${SEC_SHORTCUTS} $(^ShortcutsComponent)
     SectionSetText ${SEC_DESKTOPSHORTCUTS} $(^DesktopShortcutsComponent)
     SectionSetText ${SEC_DATABASEBOOTSTRAP} $(^DatabaseBootstrapComponent)
     

FunctionEnd


;--------------------------------
Function LocateJVM
    ;Check for Java version and location
    Push $0
    Push $1
    
    ReadRegStr $JAVA_VER HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" "CurrentVersion"
    StrCmp "" "$JAVA_VER" DetectTry2
    ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment\$JAVA_VER" "JavaHome"
    StrCmp "" $0 DetectTry2 CheckJavaVer
    
 
    DetectTry2:
    
        ReadRegStr $JAVA_VER HKLM "SOFTWARE\JavaSoft\Java Development Kit" "CurrentVersion"
        StrCmp "" "$JAVA_VER" JavaNotPresent
        ReadRegStr $0 HKLM "SOFTWARE\JavaSoft\Java Development Kit\$JAVA_VER" "JavaHome"
        StrCmp "" $0 JavaNotPresent CheckJavaVer
    
    
    JavaNotPresent:
        StrCpy $JAVA_INSTALLATION_MSG $(^JavaNotFound)
        Goto Done
 
    CheckJavaVer:
        GetFullPathName /SHORT $JAVA_HOME "$0"
        StrCpy $0 $JAVA_VER 1 0
        StrCpy $1 $JAVA_VER 1 2
        StrCpy $JAVA_VER "$0$1"
        IntCmp 14 $JAVA_VER FoundCorrectJavaVer FoundCorrectJavaVer JavaVerNotCorrect
        
    FoundCorrectJavaVer:
        IfFileExists "$JAVA_HOME\bin\javaw.exe" 0 JavaNotPresent
        ;MessageBox MB_OK "Found Java: $JAVA_VER at $JAVA_HOME"
        Goto Done
        
    JavaVerNotCorrect:
        StrCpy $JAVA_INSTALLATION_MSG $(^OldJavaFound)
        
    Done:
        Pop $1
        Pop $0
FunctionEnd

Function CreateStartFile
    Push $3
    Push $4
    
    FileOpen $4 "$INSTDIR\xwikionjetty\start_xwiki.bat" w
    FileWrite $4 "echo off$\r$\n"
    FileWrite $4 "set LANG=fr_FR.ISO8859-1$\r$\n"
    FileWrite $4 "set JETTY_PORT=$ServerPort$\r$\n"
    FileWrite $4 "set JETTY_HOME=.$\r$\n"
    FileWrite $4 "set JAVA_OPTS=-Xmx300m$\r$\n"
    FileWrite $4 "java %JAVA_OPTS% -Djetty.port=%JETTY_PORT% -Djetty.home=%JETTY_HOME% -Dfile.encoding=iso-8859-1 -jar %JETTY_HOME%/start.jar"
    FileClose $4
    
    FileOpen $4 "$INSTDIR\xwikionjetty\start_xwiki.sh" w
    FileWrite $4 "#!/bin/sh$\r$\n"
    FileWrite $4 "export LANG=fr_FR.ISO8859-1$\r$\n"
    FileWrite $4 "JETTY_PORT=$ServerPort$\r$\n"
    FileWrite $4 "JETTY_HOME=.$\r$\n"
    FileWrite $4 "JAVA_OPTS=-Xmx300m$\r$\n"
    FileWrite $4 "java $$JAVA_OPTS -Djetty.port=$$JETTY_PORT -Djetty.home=$$JETTY_HOME -Dfile.encoding=iso-8859-1 -jar $$JETTY_HOME/start.jar"
    FileClose $4
    
    Pop $4
    Pop $3
FunctionEnd

Function CreateStopFile
    Push $3
    Push $4
    
    FileOpen $4 "$INSTDIR\xwikionjetty\stop_xwiki.bat" w
    FileWrite $4 "echo off$\r$\n"
    FileWrite $4 "set JETTY_HOME=.$\r$\n"
    FileWrite $4 "java -Djetty.home=%JETTY_HOME% -jar %JETTY_HOME%/stop.jar$\r$\n"
    FileClose $4
    
    FileOpen $4 "$INSTDIR\xwikionjetty\stop_xwiki.sh" w
    FileWrite $4 "#!/bin/sh$\r$\n"
    FileWrite $4 "JETTY_HOME=.$\r$\n"
    FileWrite $4 "java -Djetty.home=$$JETTY_HOME -jar $$JETTY_HOME/stop.jar$\r$\n"
    FileClose $4
    
    Pop $4
    Pop $3
FunctionEnd

Function RunXWiki
   SetOutPath $INSTDIR\xwikionjetty
   ExecShell "open" "$INSTDIR\xwikionjetty\start_xwiki.bat" "" SW_SHOWMINIMIZED
   ; let jetty server some time to start -- cross fingers it will be enough ...
   Sleep 10000
   ExecShell "open" "http://localhost:$ServerPort/xwiki/bin/view/Main/WebHome"
FunctionEnd


# Installer Language Strings
# TODO Update the Language Strings with the appropriate translations.

LangString ^UninstallLink ${LANG_FRENCH} "Désinstaller $(^Name)"
LangString ^UninstallLink ${LANG_ENGLISH} "Uninstall $(^Name)"
LangString ^StartXWikiLink ${LANG_ENGLISH} "Start $(^Name)"
LangString ^StartXWikiLink ${LANG_FRENCH} "Démarrer $(^Name)"
LangString ^StopXWikiLink ${LANG_ENGLISH} "Stop $(^Name)"
LangString ^StopXWikiLink ${LANG_FRENCH} "Arrêter $(^Name)"
LangString ^XWikiStopDescription ${LANG_ENGLISH} "Stop $(^Name) application"
LangString ^XWikiStopDescription ${LANG_FRENCH} "Arrêter l'application $(^Name)"
LangString ^XWikiStartDescription ${LANG_ENGLISH} "Start $(^Name) application"
LangString ^XWikiStartDescription ${LANG_FRENCH} "Démarrer l'application $(^Name)"
LangString ^JavaNotFound ${LANG_ENGLISH} "Java Runtime Environment is not installed on your computer. You need version 1.4 or newer to run (^Name). You are going to be redirected to the Java Sun web site."
LangString ^JavaNotFound ${LANG_FRENCH} "L'environnement d'exécution Java n'a pas été trouvé. Vous devez installer la version 1.4 ou supérieure pour faire fonctionner $(^Name). Vous allez être redirigé sur le site Java de Sun."
LangString ^OldJavaFound ${LANG_ENGLISH} "The version of Java Runtime Environment installed on your computer is too old. Version 1.4 or newer is required to run $(^Name)."
LangString ^OldJavaFound ${LANG_FRENCH} "La version de l'environnement d'exécution Java présente sur votre systême est trop ancienne. Il faut installer la version 1.4 ou supérieure pour faire fonctionner $(^Name). Vous allez être redirigé sur le site Java de Sun."
LangString ^ServerPortTitle ${LANG_ENGLISH} "Server port configuration"
LangString ^ServerPortTitle ${LANG_FRENCH} "Configuration du port pour le serveur"
LangString ^ServerPortSubtitle ${LANG_ENGLISH} "Choose a port number for $(^Name). It must be greater than 1024."
LangString ^ServerPortSubtitle ${LANG_FRENCH} "Choisissez un numéro de port pour $(^Name). Celui-ci doit être supérieur à 1024."
LangString ^TypicalInstType ${LANG_ENGLISH} "Typical"
LangString ^TypicalInstType ${LANG_FRENCH} "Standard"
LangString ^CustomInstType ${LANG_ENGLISH} "Custom"
LangString ^CustomInstType ${LANG_FRENCH} "Personnalisée"
LangString ^UpdateInstType ${LANG_ENGLISH} "Update"
LangString ^UpdateInstType ${LANG_FRENCH} "Mise à jour"
LangString ^ComponentsIntro ${LANG_ENGLISH} "Choose the installation type you want."
LangString ^ComponentsIntro ${LANG_FRENCH} "Choisissez le type d'installation que vous souhaitez effectuer."
LangString ^ComponentsInstType ${LANG_ENGLISH} "Installation"
LangString ^ComponentsInstType ${LANG_FRENCH} "Installation"
LangString ^ComponentsComponent ${LANG_ENGLISH} "Options"
LangString ^ComponentsComponent ${LANG_FRENCH} "Options"
LangString ^DesktopShortcutsDescription ${LANG_ENGLISH} "Installs start/stop application shortcuts on your desktop."
LangString ^DesktopShortcutsDescription ${LANG_FRENCH} "Installer sur votre bureau des raccourcis pour démarrer/arrêter l'application."
LangString ^XWikiFilesDescription ${LANG_ENGLISH} "Copy all the files needed by $(^Name)."
LangString ^XWikiFilesDescription ${LANG_FRENCH} "Copier les fichiers nécessaires à $(^Name)."
LangString ^DatabaseBootstrapDescription ${LANG_ENGLISH} "Copy the wiki database. Required for the first installation.$\r$\nWARNING: will remove your data if you install over a previous installation."
LangString ^DatabaseBootstrapDescription ${LANG_FRENCH} "Copier la base de données du wiki. Nécessaire en cas de première installation.$\r$\nATTENTION: efface les données actuelles en cas de mise à jour d'une installation précédente."
LangString ^ShortcutsDescription ${LANG_ENGLISH} "Installs start/stop shortcuts in the Start menu"
LangString ^ShortcutsDescription ${LANG_FRENCH} "Installer les raccourcis pour démarrer/arrêter l'application dans le menu Démarrer"
LangString ^XWikiComponent ${LANG_ENGLISH} "$(^Name) files"
LangString ^XWikiComponent ${LANG_FRENCH} "Fichiers $(^Name)"
LangString ^ShortcutsComponent ${LANG_ENGLISH} "Start menu shortcuts"
LangString ^ShortcutsComponent ${LANG_FRENCH} "Raccourcis du menu Démarrer"
LangString ^DesktopShortcutsComponent ${LANG_ENGLISH} "Desktop shortcuts"
LangString ^DesktopShortcutsComponent ${LANG_FRENCH} "Raccourcis sur le bureau"
LangString ^DatabaseBootstrapComponent ${LANG_ENGLISH} "Wiki database"
LangString ^DatabaseBootstrapComponent ${LANG_FRENCH} "Base de données wiki"
LangString ^BadPortNumberTooLow ${LANG_ENGLISH} "Port number must be greater than 1024"
LangString ^BadPortNumberTooLow ${LANG_FRENCH} "Le port doit être supérieur à 1024"
LangString ^BadPortNumberTooHigh ${LANG_ENGLISH} "Port number must be less than 65535"
LangString ^BadPortNumberTooHigh ${LANG_FRENCH} "Le port doit être inférieur à 65535"
LangString ^UninstallWarning ${LANG_ENGLISH} "This will uninstall $(^Name).$\r$\nWARNING: All $(^Name) data will be erased also. To backup your data copy all the files in the 'db' subdirectory to a safe place before clicking on 'Uninstall'."
LangString ^UninstallWarning ${LANG_FRENCH} "Vous êtes sur le point de désinstaller $(^Name).$\r$\nATTENTION: Toutes les données du wiki vont être détruites également. Pour sauvegarder vos données, copiez les fichiers du répertoire 'db' en lieu sûr, avant de procéder à la désintallation."
LangString ^ConfigPortPrompt ${LANG_ENGLISH} "By default, $(^Name) will use the port 8080. If needed you can change this port number."
LangString ^ConfigPortPrompt ${LANG_FRENCH} "Par défaut, $(^Name) va utiliser le port 8080 pour fonctionner. Si cette valeur ne vous convient pas, merci d'en préciser une autre."
LangString ^ConfigPortPort ${LANG_ENGLISH} "Port number:"
LangString ^ConfigPortPort ${LANG_FRENCH} "Port à utiliser:"
LangString ^RunText ${LANG_ENGLISH} "Run $(^Name)"
LangString ^RunText ${LANG_FRENCH} "Lancer $(^Name)"
LangString ^FinishText ${LANG_FRENCH} "Pour utiliser $(^Name), il faut démarrer le serveur en utilisant soit les raccourcis créés durant l'installation, soit le script $INSTDIR\xwikionjetty\start_xwiki.bat, puis en allant à l'adresse suivante avec votre navigateur:\r\n\r\nhttp://localhost:$ServerPort/xwiki/bin/view/Main/WebHome\r\n\r\nCliquez sur 'Fermer' pour quitter l'installation."
LangString ^FinishText ${LANG_ENGLISH} "In order to use $(^Name), you need to start the server using the shortcuts created during the installation or by running $INSTDIR\xwikionjetty\start_xwiki.bat, then you have to point your Web browser to:\r\n\r\nhttp://localhost:$ServerPort/xwiki/view/Main/WebHome\r\n\r\nClick on 'Finish' to quit the installation."
LangString ^XWikiMoreInformation ${LANG_ENGLISH} "More information about using/install $(^Name)"
LangString ^XWikiMoreInformation ${LANG_FRENCH} "Plus d'information sur l'utilisation/l'installation de $(^Name)"
LangString ^FinishTitle ${LANG_ENGLISH} "$(^Name) has been installed"
LangString ^FinishTitle ${LANG_FRENCH} "$(^Name) est installé"
LangString ^SelectComponent ${LANG_ENGLISH} "Select the type of install :"
LangString ^SelectComponent ${LANG_FRENCH} "Choisissez une installation :"








