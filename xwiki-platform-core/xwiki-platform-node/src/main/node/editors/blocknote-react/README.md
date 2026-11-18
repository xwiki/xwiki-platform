# BlockNote editor (React)

The BlockNote editor shared by XWiki (`xwiki-platform-blocknote-webjar`) and Cristal, together with its generic editing
features. This file describes the architecture the editor and its features follow: new features are written against
it, and the existing ones are checked against it.

## Terms

- **Editor**: the BlockNote editor instance and the code creating it (`BlockNoteViewWrapper`), which knows nothing
  about the editing features.
- **Editing feature**: something the user can do while editing (create a link to a wiki page, upload a file, insert a
  macro call). A feature is either **vertical**, i.e. user-facing (links, images, macros), or **horizontal**, i.e. used
  by other features (file upload).
- **Service**: an implementation bound in the Inversify service container, which a feature calls (`LinkEditor`,
  `FileUploader`, `MacroEditor`).
- **Editor instance**: `EditorInstance`, i.e. `{ editor, options, metadata, serviceContainer }`, one per mounted
  editor.

## Principles

### Features

1. **Editing features are decoupled from the editor.** The editor collects what the features contribute and applies
   it; it doesn't name or special-case any feature.
2. **A feature contributes, it doesn't patch.** A feature can contribute toolbar entries, slash menu entries,
   suggestion menus, BlockNote extensions (ProseMirror plugins, paste and drop handling, keyboard shortcuts, input
   rules), BlockNote editor options (e.g. `uploadFile`) and default values for its own options. The editor merges the
   contributions and reports a conflict (e.g. two features setting the same BlockNote option) rather than letting the
   last one win.
3. **Horizontal features are used through their service, never through the feature.** Links and images call the
   `FileUploader`; they don't depend on the feature handling the files pasted or dropped in the editor, which can be
   disabled on its own.
4. **A feature without its service hides its actions** (e.g. no action to create or edit a link when no `LinkEditor`
   is bound). It can fall back on a built-in implementation when there's one generic enough (e.g. the built-in image
   UI).
5. **The data that the BlockNote schema can't hold** (e.g. resource references, generated link labels, macro output)
   goes through the metadata store of the editor instance and the `*MetadataHandler` services (`beforeEdit`,
   `beforeUpdate`, `afterUpdate`). Features don't write integration-specific metadata themselves.

### Configuration

6. **The options hold values, not behavior.** Behavior is injected through the service container. Callbacks are used
   only at a framework boundary or for the editor's own events (`onChange`, `refs`).
7. **Where an option goes** (top level or `features.<id>`, declared by which layer, named how) is described in the TSDoc
   of `EditorOptions`. `features.<id>: false` disables a feature.

### Services and state

8. **Services are stateless singletons** bound in the shared service container. Each call takes the editor instance
   as first argument. The bindings specific to an editor instance go in the child container of that instance.
9. **The state of an editor instance lives with the instance** (its metadata store, the store of a BlockNote extension,
   its child container), never in a service, in a closure captured when the editor is created, or in a module global.
10. **Several editors on the same page don't interfere.** DOM listeners are scoped to the editor (best done through a
    BlockNote extension), state is kept per instance, and realtime content is identified by the editor `name`.

### Layers

11. **`xwiki-platform-blocknote-webjar`**: everything specific to editing in XWiki (macro and image wizards, temporary
    uploads, XWiki metadata handlers, form integration).
12. **`blocknote-headless`**: the Vue integration only (the Vue component, Vue UI such as the link modal, the host Vue
    app context). Nothing framework-agnostic.
13. **`blocknote-react`**: the generic editing implementation, serving both XWiki and Cristal: the editor, the editing
    features, the service interfaces and their generic implementations.
14. **The other `xwiki-platform-node` packages**: what isn't specific to editing and can be used outside BlockNote
    (e.g. attachments, link suggestions, model references, document service).
15. **Dependencies point one way**: webjar → headless → react → other packages.

### Quality

16. **Each feature is testable alone**: a Playwright component test story binds only the services the feature needs.
17. **The public API** is tagged with `@since` and `@beta`, and its API report (`etc/*.api.md`) is kept up to date.

## Known deviations

| Code | Deviation | Principles |
|---|---|---|
| Links, images, macros, code block | wired directly by the editor, which also checks the syntax features (only the file transfer contributes through `mergeContributions`) | 1, 2 |
| `lang` option | changes a module-global translation instance | 9, 10 |
| Realtime metadata | references and generated labels are only in the metadata store of the inserting client | 5, 10 |
