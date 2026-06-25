# Woodpecker Yso Snapshot

This directory contains a package-isolated source snapshot adapted from the
`ysoserial-for-woodpecker` codebase for PotatoTool's built-in payload toolbox.

Source notes:

- Adapted from the `ysoserial-for-woodpecker` project maintained for the
  Woodpecker framework.
- The current upstream source tree reports version `0.5.3` in its `pom.xml`.
- The compared Woodpecker helper historically invoked
  `ysoserial-for-woodpecker-0.5.2.jar` from the host runtime classpath.
- PotatoTool does not load an external jar from the user environment. It
  compiles the required payload sources directly into the application jar.

Local adaptations:

- The source tree is package-relocated under the PotatoTool namespace.
- CLI-only helpers, exploit launchers, and runtime scanner glue are not shipped.
- Only the payload classes required by the built-in UI are retained.
- Reflection and payload runner helpers are trimmed to the minimum needed for
  in-process payload generation.

Current retained gadget classes:

- `URLDNS`
- `CommonsBeanutils1`
- `CommonsCollectionsK1`
- `CommonsCollectionsK2`
- `Jdk7u21`
- `Jdk8u20`

The upstream project inherits from `ysoserial`; the MIT license text from the
upstream `ysoserial` repository is included here as `LICENSE-ysoserial.txt`.
