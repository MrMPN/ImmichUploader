# Immich Power-User Uploader Ideas

Based on Immich API docs in Context7, these are high-value features to surface in a power-user web uploader.

## Prioritized Features

1. Pre-upload dedupe check
- Use `POST /assets/bulk-upload-check` with checksums to skip uploads before transfer.
- Source: https://api.immich.app/endpoints/assets

2. Duplicate review queue
- Surface duplicates via `GET /duplicates` and allow cleanup actions.
- Source: https://api.immich.app/endpoints

3. Rich upload form controls
- Expose advanced upload fields on `POST /assets`: `sidecarData`, `livePhotoVideoId`, `isFavorite`, `visibility`, `duration`.
- Source: https://api.immich.app/endpoints/assets/uploadAsset

4. Bulk album + tag pipelines
- Apply album/tag operations to large batches with result visibility (`PUT /albums/assets`, `PUT /tags/{id}/assets`, `DELETE /tags/{id}/assets`).
- Source: https://api.immich.app/endpoints/albums/addAssetsToAlbums
- Source: https://api.immich.app/endpoints/tags/tagAssets
- Source: https://api.immich.app/endpoints/tags/untagAssets

5. Post-upload job runner
- Add a “Run processing now” panel via `POST /assets/jobs` (thumbnail/metadata/OCR workflows).
- Source: https://api.immich.app/endpoints/assets

6. Advanced QA search after upload
- Add filters against `POST /search/metadata` (date, checksum, camera, location, tags, “not in album”, etc.) to validate imported batches.
- Source: https://api.immich.app/endpoints/search/searchAssets

7. Smart + OCR verification tools
- Add search panels for `POST /search/smart` and OCR-aware metadata filters to quickly find mis-tagged/misdated media.
- Source: https://api.immich.app/endpoints/search

8. Large-file triage
- Use `POST /search/large-assets` to highlight large videos/images and apply archive/organization rules.
- Source: https://api.immich.app/endpoints/search

9. Burst/variant stack controls
- Add stack management (`/stacks`) for burst uploads so users can group/select primary assets quickly.
- Source: https://api.immich.app/endpoints/stacks

10. Face/person cleanup workflows
- Surface detected faces (`/faces`) for manual reassignment and QA after bulk imports.
- Source: https://api.immich.app/endpoints/faces

## Recommended First Slice

For this uploader, the strongest immediate wins are:
- `bulk-upload-check`
- richer `uploadAsset` fields
- post-upload `assets/jobs`
- advanced `search/metadata` validation

These deliver strong power-user value with relatively low UI complexity.
