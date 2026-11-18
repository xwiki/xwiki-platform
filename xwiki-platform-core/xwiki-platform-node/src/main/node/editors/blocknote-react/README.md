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

1. **Editing features are decoupled from the editor.** A feature is an `EditorFeature` bound in the service container
   under `editorFeatureName`, for all the editor instances or for one of them. The editor has no feature of its own:
   the generic ones are bound by the `ComponentInit` of this package, which the integrations call (an integration that
   doesn't gets an editor without them, silently). The generic features are also exported, so that an integration can
   bind only some of them, in another order, or replace one of them with its own. The editor collects what the features contribute and applies it;
   it doesn't name or special-case any feature.
   `features.<id>: false` disables a feature, and a feature contributes nothing when the syntax of the edited content
   can't save what it adds.
2. **A feature contributes, it doesn't patch.** A feature can contribute block and inline content types to the schema
   (their types erased), slash menu items (an item with the key of a BlockNote default item replaces it, in place),
   formatting toolbar items (including the toolbar shown for a block type), components rendered with the editor (e.g.
   its menus and toolbars), BlockNote extensions (ProseMirror plugins, paste and drop handling, keyboard shortcuts,
   input rules) and BlockNote editor options (e.g. `uploadFile`). The editor merges the contributions and reports a
   conflict (e.g. two features setting the same BlockNote option, adding the same block type or replacing the same
   slash menu item) rather than letting the last one win. The editor itself only removes the BlockNote default items
   that the syntax of the edited content can't save.
3. **Horizontal features are used through their service, never through the feature.** Links and images call the
   `FileUploader`; they don't depend on the feature handling the files pasted or dropped in the editor, which can be
   disabled on its own.
4. **A feature without its service hides its actions** (e.g. no action to create or edit a link when no `LinkEditor`
   is bound). It can fall back on a built-in implementation when there's one generic enough (e.g. the built-in image
   UI).
5. **A feature's code lives in its own folder.** `src/features/<id>/` holds what only that feature uses (its BlockNote
   extensions, components, menu items) and exports the feature. What several features use (e.g. the `FileUploader`,
   the metadata store, the editor services in `src/services/`) lives outside `src/features/`. A feature folder never
   imports from another feature folder.
6. **The data that the BlockNote schema can't hold** (e.g. resource references, generated link labels, macro output)
   goes through the metadata store of the editor instance and the `*MetadataHandler` services (`beforeEdit`,
   `beforeUpdate`, `afterUpdate`). Features don't write integration-specific metadata themselves.

### Configuration

7. **The options hold values, not behavior.** Behavior is injected through the service container. Callbacks are used
   only at a framework boundary or for the editor's own events (`onChange`, `refs`).
8. **Where an option goes** (top level or `features.<id>`, declared by which layer, named how) is described in the TSDoc
   of `EditorOptions`. `features.<id>: false` disables a feature.

### Services and state

9. **Services are stateless singletons** bound in the shared service container. Each call takes the editor instance
   as first argument. The bindings specific to an editor instance go in the child container of that instance.
10. **The state of an editor instance lives with the instance** (its metadata store, the store of a BlockNote extension,
    its child container), never in a service, in a closure captured when the editor is created, or in a module global.
11. **Several editors on the same page don't interfere.** DOM listeners are scoped to the editor (best done through a
    BlockNote extension), state is kept per instance, and realtime content is identified by the editor `name`.

### Layers

12. **`xwiki-platform-blocknote-webjar`**: everything specific to editing in XWiki (macro and image wizards, temporary
    uploads, XWiki metadata handlers, form integration).
13. **`blocknote-headless`**: the Vue integration only (the Vue component, Vue UI such as the link modal, the host Vue
    app context). Nothing framework-agnostic.
14. **`blocknote-react`**: the generic editing implementation, serving both XWiki and Cristal: the editor, the editing
    features, the service interfaces and their generic implementations.
15. **The other `xwiki-platform-node` packages**: what isn't specific to editing and can be used outside BlockNote
    (e.g. attachments, link suggestions, model references, document service).
16. **Dependencies point one way**: webjar → headless → react → other packages.

### Quality

17. **Each feature is testable alone**: a Playwright component test story binds only the services the feature needs.
18. **The public API** is tagged with `@since` and `@beta`, and its API report (`etc/*.api.md`) is kept up to date.

## Known deviations

| Code | Deviation | Principles |
|---|---|---|
| Image, code block and server-rendered macro specs | part of the static schema of the editor, which imports them from `src/features/images`, `src/features/codeBlocks` and `src/features/macros` (the typed API derives from the schema, and the content holding macro calls must load whatever the features), so the Ctrl+Alt+C shortcut of the code blocks (registered by BlockNote) works even when the syntax can't save code blocks | 1 |
| `lang` option | changes a module-global translation instance | 10, 11 |
| Realtime metadata | references and generated labels are only in the metadata store of the inserting client | 6, 11 |
