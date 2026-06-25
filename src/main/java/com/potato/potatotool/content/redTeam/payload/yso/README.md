# Built-in Yso Adapter

This package is the PotatoTool-controlled adapter for yso-style payload generation.

The compared Woodpecker helper historically invokes
`ysoserial-for-woodpecker-0.5.2.jar` from the host runtime classpath through
reflection. PotatoTool does not require a user-supplied jar and does not depend
on a remote Maven coordinate for that artifact. Instead, the payload toolbox
ships a package-isolated source snapshot under `thirdparty/woodpecker`.

Current built-in gadget set:

- `URLDNS`
- `CommonsBeanutils1`
- `CommonsCollections1`
- `CommonsCollections2`
- `CommonsCollections3`
- `CommonsCollections4`
- `CommonsCollections5`
- `CommonsCollections6`
- `CommonsCollections7`
- `CommonsBeanutils2`
- `CommonsBeanutils3`
- `CommonsBeanutils1_183`
- `CommonsBeanutils2_183`
- `CommonsBeanutils3_183`
- `CommonsCollections8`
- `CommonsCollections9`
- `CommonsCollections10`
- `CommonsCollections11`
- `BeanShell1`
- `Jython1`
- `MozillaRhino1`
- `MozillaRhino2`
- `AspectJWeaver`
- `JavassistWeld1`
- `JBossInterceptors1`
- `Myfaces1`
- `Myfaces2`
- `FileUpload1`
- `C3P0`
- `Groovy1`
- `Clojure`
- `Hibernate1`
- `Hibernate2`
- `Spring1`
- `Spring2`
- `Spring3`
- `ROME`
- `JSON1`
- `Click1`
- `Wicket1`
- `Vaadin1`
- `CommonsCollectionsK1`
- `CommonsCollectionsK2`
- `Jdk7u21`
- `Jdk8u20`
- `ClassFileWrapper`

`ClassFileWrapper` is a PotatoTool-local helper that serializes validated
`class_file:` bytes in `ClassFilePayload`. The other gadgets are backed by the
isolated Woodpecker-derived snapshot.

`CommonsBeanutils3` is implemented as a compatibility form on the repository's
current `commons-beanutils:1.9.2` classpath; it does not bundle a separate
`1.8.3` runtime. The `*_183` variants remain compatibility-oriented payload
flavors rather than an embedded second beanutils runtime.

Registration model:

- `YsoPayloadMetadata` is the source of truth for UI-visible gadgets.
- Each gadget declares category, input type, `class_file` support, dependency
  hint, and offline verification hint.
- `PanePayloadToolbox` filters gadgets by metadata instead of hard-coded
  gadget-name checks.
- New upstream gadgets should be marked UI-visible only after their source,
  dependencies, and non-deployment generation tests are in this repository.
