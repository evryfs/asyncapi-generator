# Validator documentation

The validator documentation is separated by purpose:

- [Architecture](architecture.md) explains why validation has its current boundary and data flow.
- [Rule inventory](rules.md) is the reference for stable rule codes, ownership, authority, and audit status.
- [Adding or changing a rule](adding-validation-rule.md) is the contributor procedure.

The current implementation profile is AsyncAPI 3.0. The parsed document selects that profile once at the public validator entry point.
