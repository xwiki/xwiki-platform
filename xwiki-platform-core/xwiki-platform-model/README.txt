Design Rules Implemented
========================

* Typed API
* Entities are represented by Interfaces for the following reasons:
** We don't expose any implementation details. If we had a class we would have to share it between implementation and
   introduce a Storage API. We don't need to do that with interfaces
** Makes the Model to be easily unit-testable
** Easy to have an in-memory implementation for functional testing (makes test real fast!)
* Entity getters will return a new Entity if the asked Entity doesn't exist in the store
* All APIs that call the Store will throw a checked ModelException to signify an error (rather than ignoring the error
  and returning an empty Entity)
* Additions/modifications/deletes are only committed to the Store in Persistable.save() which allows for stacking
  operations and doing them all at once (allows optimizations and support of Stores that work this way, like JCR,
  SCMs). Uncommitted changes are committed as part of the same transaction (all or nothing) and need to have a
  Session opened before being able to be committed. The idea is that the Session is started for each User request
  and closed at the end of the request, discarding changes that haven't been saved.
* All Entities (except Object, ObjectDefinition and below) are Extensible which means they can have Objects to add
  metadata to them.
* All Entities are Referenceable which means they can be pointed to directly with a reference.
* Attachments are implemented as Objects (which means we need a BLOLB Object Definition Type).