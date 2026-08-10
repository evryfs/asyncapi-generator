# Contribute to the documentation

Documentation source is stored in `docs/` and published to GitHub Pages after
changes are merged into `main`. The generated `site/` directory is local build
output and must not be committed.

## Preview changes locally

Create a temporary Python environment and install the pinned documentation
dependency from the repository root:

```shell
python3 -m venv /tmp/asyncapi-generator-docs && \
  /tmp/asyncapi-generator-docs/bin/python -m pip install \
    --requirement requirements-docs.txt
```

Start the local server:

```shell
/tmp/asyncapi-generator-docs/bin/python -m mkdocs serve
```

Open `http://127.0.0.1:8000/`. MkDocs rebuilds the site when its Markdown or
configuration changes.

## Verify changes

Run the same strict build used by the build pipeline:

```shell
/tmp/asyncapi-generator-docs/bin/python -m mkdocs build --strict
```

Warnings fail the build. Resolve broken internal links and navigation entries
before opening a pull request.

## Place content by purpose

Keep each page focused on the reader's need:

- Tutorials teach a supported workflow from beginning to end.
- How-to guides solve a specific task.
- Reference pages define stable configuration or behavior.
- Explanation pages describe architecture and design decisions.

Add new pages to the explicit navigation in `mkdocs.yml`. Link to repository
files outside `docs/` with an absolute GitHub URL because those files are not
part of the generated site.

## Publication

Pull requests build the documentation without deploying it. After a change is
merged into `main`, the build pipeline uploads the generated site and publishes
it through the `github-pages` environment. Documentation is deployed only when
the complete project build and the strict documentation build both pass.
