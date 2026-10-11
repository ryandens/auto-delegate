# Releasing AutoDelegate

The root `gradle.properties` owns `group` and `version`. Gradle applies these
properties to the root and all modules, so publication coordinates, project
dependencies, and Sonatype staging use the same version. Convention plugins own
build behavior; `gradle/libs.versions.toml` owns dependency versions.

## One-time setup

1. Sign in to the [Central Publisher Portal](https://central.sonatype.com/),
   confirm that you own the `com.ryandens` namespace, and generate a
   [Portal user token](https://central.sonatype.org/publish/generate-portal-token/).
   Give the token a clear CI-specific name and an expiration, store it in
   1Password, and rotate the GitHub environment secrets before it expires.
   These are token credentials, never your Sonatype account password.
   Old OSSRH credentials and the old numeric staging profile are no longer used.
2. Create a GitHub Actions environment named `maven-central`. Limit deployments
   to tags matching `[0-9]*.[0-9]*.[0-9]*`. Optionally add a required reviewer. Protect `main` with
   the Validate workflow and use a tag ruleset to restrict creating/updating
   release tags to maintainers.
3. Add these **environment secrets** (no repository variables are required):

   | Secret | Value |
   | --- | --- |
   | `MAVEN_CENTRAL_TOKEN_USERNAME` | Portal token username, not your login email |
   | `MAVEN_CENTRAL_TOKEN_PASSWORD` | Portal token password |
   | `MAVEN_SIGNING_KEY` | Complete ASCII-armored, passphrase-protected PGP private key, with real newlines |
   | `MAVEN_SIGNING_PASSWORD` | Passphrase for that exported private key |

### Signing key in 1Password

The existing Maven signing key is stored in the **Private** vault as
**Maven Central signing key** (item ID `pwnmdwkz7bgkxlqjhhflzjos7u`). Its fingerprint is
`3248202EE69438B28A3F5DBC62731B5459631CF8`, matching the signing key used for
AutoDelegate `0.3.1`, with identity `Ryan Dens <ryandens735@gmail.com>`.

The `private key` field contains the complete ASCII-armored private key encrypted
with AES-256; the `passphrase` field contains its generated export passphrase.
Both are concealed fields. The original Keybase key was retained. The migrated
key was checked by decrypting it and signing/verifying a challenge, and the
1Password contents were read back and compared after saving.

Copy the values directly from 1Password to the GitHub environment without
printing them or putting them in shell history. Keybase is no longer needed for
this setup:

```sh
set -o pipefail
op read 'op://Private/pwnmdwkz7bgkxlqjhhflzjos7u/private key' | gh secret set MAVEN_SIGNING_KEY --env maven-central --repo ryandens/auto-delegate
op read 'op://Private/pwnmdwkz7bgkxlqjhhflzjos7u/passphrase' | gh secret set MAVEN_SIGNING_PASSWORD --env maven-central --repo ryandens/auto-delegate
```

Ensure the public key remains available on a
[keyserver supported by Central](https://central.sonatype.org/publish/requirements/gpg/).
Moving the private key to 1Password does not change the public fingerprint or
require revoking the key.

For the Portal token, substitute your actual 1Password vault, item, and field names:

```sh
set -o pipefail
op read 'op://VAULT/Maven Central/token username' | gh secret set MAVEN_CENTRAL_TOKEN_USERNAME --env maven-central --repo ryandens/auto-delegate
op read 'op://VAULT/Maven Central/token password' | gh secret set MAVEN_CENTRAL_TOKEN_PASSWORD --env maven-central --repo ryandens/auto-delegate
```

## Release a version

1. Set `version=X.Y.Z` in root `gradle.properties`, update the README dependency
   examples, and merge the change into `main` after validation passes. Choose an
   unpublished version. The current project version is `0.4.0`; publishing
   requires pushing the matching `0.4.0` tag.
2. Tag the merged commit and push the tag:

   ```sh
   git switch main
   git pull --ff-only
   git tag -a X.Y.Z -m 'Release X.Y.Z'
   git push origin X.Y.Z
   ```

3. The Release workflow verifies the tag matches the committed version and that
   the commit is on `main`, then runs the full build and build-logic tests without
   publishing credentials. A separate, fresh runner builds and signs both
   published modules, uploads them into one Sonatype staging repository, and
   closes/releases it. The examples module is not published. A final job creates
   a GitHub release with generated notes only after the publishing job succeeds.

Release jobs disable Gradle caches and public build scans. Signing keys are
loaded in memory only in the publishing step; no GPG installation or keyring is
needed in CI. Actions are pinned to commit IDs. Concurrent releases are
serialized and an active publication is never automatically cancelled by a newer
run, since interrupting an upload can leave a partial Sonatype deployment. You
can still cancel a run manually in GitHub Actions; inspect Central before retrying.

The workflow deliberately accepts only stable `MAJOR.MINOR.PATCH` tags.
Snapshot and prerelease publishing are not enabled. You can keep a
`X.Y.Z-SNAPSHOT` version on `main` between releases; change it to a stable version
before tagging.

## Failures and retries

- Validation failure: fix the problem before publishing. Never move a tag whose
  version has already been published.
- Missing credentials: configure the environment secrets, then rerun failed jobs.
- Upload/close/release timeout: inspect the deployment in the Central Portal and
  the staging repository ID in the Actions logs before retrying. A request may
  have succeeded despite the timeout. Finish or drop that deployment first;
  Maven Central versions are immutable, so do not blindly repeat uploads.
- Only the GitHub release job failed: rerun **failed jobs**, which does not rerun
  the successful Maven publication. The GitHub release step tolerates an
  existing release.
- Manual dispatch is available for an existing tag (`gh workflow run release.yml
  --ref X.Y.Z`); it reruns publication, so use it only after checking Central.
  Branch dispatches fail validation.

The existing Nexus publishing plugin uses Sonatype's documented
[Central compatibility endpoint](https://central.sonatype.org/publish/publish-portal-ossrh-staging-api/#getting-started-for-nexus-repository-manager-2-api-plugins).
It handles the staging lifecycle; no additional manual upload API call is needed.

The `ossrh-staging-api.central.sonatype.com/service/local/` URL publishes releases
to Maven Central through the Central Publisher Portal's compatibility service.
The `central.sonatype.com/repository/maven-snapshots/` URL is its separate snapshot
repository; the stable-only release workflow does not use it.

The previous numeric `stagingProfileId` was configured for the retired OSSRH
service. It is omitted so the Nexus plugin can discover the profile for
`packageGroup = com.ryandens` from the new service using the Portal token.
An explicit profile ID is an optional optimization, not a requirement; see the
[plugin's profile discovery documentation](https://github.com/gradle-nexus/publish-plugin#behind-the-scenes).
