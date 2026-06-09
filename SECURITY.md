# Security Policy

[中文文档](./SECURITY.zh-CN.md)

## Supported Versions

ComposeRichEditor is preparing for its first alpha release. Until a stable release is available, security fixes will target the latest development branch and the latest published alpha when applicable.

| Version | Supported |
|---|---|
| `0.1.x-alpha` | Yes, after publication |
| Older versions | No |

## Reporting a Vulnerability

Please do not open a public issue for security vulnerabilities.

Report security issues privately to the project maintainers. If a dedicated security contact is not available yet, use the repository owner's private GitHub contact method or open a minimal public issue asking for a private contact channel without disclosing details.

Please include:

- Affected version or commit.
- Platform and environment.
- Vulnerability description.
- Reproduction steps or proof of concept if available.
- Potential impact.
- Suggested fix or mitigation if known.

## Scope

Security-relevant areas include:

- Rich text import/export and parsing.
- HTML/Markdown/plain text paste handling.
- Link handling.
- Clipboard behavior.
- Serialization/deserialization.
- Any behavior that may cause data loss, unexpected content execution, or sensitive data exposure.

ComposeRichEditor does not intentionally execute user-provided HTML, JavaScript, or external content. Please report any behavior that appears to violate this expectation.
