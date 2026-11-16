# Animation tracks v2

**Implemented on the development branch (`1.21.1`).**

Named `animations` arrays, multiple concurrent tracks per trigger,
independent watches, millisecond `delay`, simultaneous transform composition
and the dynamic F8 animation list are now implemented.

The old `animation` object was intentionally removed before any release.
For the actual supported schema, complete examples and editor instructions,
see [Declarative animations](ANIMATIONS.md).

Further extensions, such as looping, ping-pong, arbitrary custom event
triggers and timeline sequences, remain future work. They are not part
of the currently implemented contract.
