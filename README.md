# lib-compile

"lib-compile" is a RichTea library which provides access to the RichTea compiler from within a RichTea program

The library exports two RichTea functions:

* `Compile` - Compiles a RichTea program from a source string.  Returns a `CompilationResult` instance.
* `BuildDistributable` - Builds an standalone, executable Jar file, from a `CompilationResult` instance.  Returns the `ByteArrayOutputStream` of the jar.
