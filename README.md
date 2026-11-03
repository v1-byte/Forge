# Forge - APK AI Editor

Forge is an Android-focused APK editor project built on top of open-source engines. Upstream source is vendored as ordinary files from GitHub ZIP snapshots (not Git submodules).

## Upstream engines

- **Editor Core:** [coder/code-server](https://github.com/coder/code-server) — MIT. The login-page title is customized to “Forge Editor”; the upstream license and copyright notices are retained.
- **Inference Engine:** [ggml-org/llama.cpp](https://github.com/ggml-org/llama.cpp) — MIT. Vendored under `forge-engine/forge-inference/`.
- **Compiler:** [mlc-ai/mlc-llm](https://github.com/mlc-ai/mlc-llm) — Apache-2.0. Its upstream `LICENSE` and `NOTICE` are retained.
- **Trainer:** [unslothai/unsloth](https://github.com/unslothai/unsloth) — mixed licensing as described by the upstream `LICENSE`: Apache-2.0 applies to `unsloth/`, `tests/`, and `scripts/`; AGPLv3 applies to `studio/` and `unsloth_cli/`. Review the applicable license before building, modifying, or distributing those components.

The source snapshots are retained with their upstream license, NOTICE, COPYING, and nested third-party license files. Convenience copies are collected under [`LICENSES/`](LICENSES/); the original files remain in each engine directory.

## Assistant

Open WebUI is excluded. A Forge-native assistant UI is planned and has not yet been implemented.

## Source ownership and modifications

Upstream code remains subject to its original copyrights and licenses. Forge-specific changes are limited to the project’s own additions and clearly identified modifications; this repository does not claim ownership of upstream source.

## Repository layout

```text
forge-engine/
  forge-editor/
  forge-inference/
  forge-compiler/
  forge-trainer/
LICENSES/
README.md
```
