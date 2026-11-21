## ClashNeo

A Graphical user interface of [Clash.Meta](https://github.com/MetaCubeX/Clash.Meta) (mihomo) for Android.

ClashNeo is a modified fork of [MetaCubeX/ClashMetaForAndroid](https://github.com/MetaCubeX/ClashMetaForAndroid),
renamed to an independent application. It is distributed under the terms of the
[GPL-3.0](https://www.gnu.org/licenses/gpl-3.0.html); see `LICENSE` and `NOTICE` for the original
attribution to MetaCubeX, CGQAQ and kr328.

### Feature

Feature of [Clash.Meta](https://github.com/MetaCubeX/Clash.Meta)

### Requirement

- Android 5.0+ (minimum)
- Android 7.0+ (recommend)
- `armeabi-v7a` , `arm64-v8a`, `x86` or `x86_64` Architecture

### Build

1. Update submodules

   ```bash
   git submodule update --init --recursive
   ```

2. Install **OpenJDK 21**, **Android SDK**, **CMake** and **Golang**

3. Create `local.properties` in project root with

   ```properties
   sdk.dir=/path/to/android-sdk
   ```

4. (Optional) Custom app package name. Add the following configuration to `local.properties`.

   ```properties
   # config your own applicationId, or it will be 'io.github.neo.clash'
   custom.application.id=com.my.compile.clash
   # remove application id suffix, or the application id will be 'io.github.neo.clash.alpha'
   remove.suffix=true
   ```

5. Create `signing.properties` in project root with

   ```properties
   keystore.path=/path/to/keystore/file
   keystore.password=<key store password>
   key.alias=<key alias>
   key.password=<key password>
   ```

6. Build

   ```bash
   ./gradlew app:assembleAlphaRelease
   ```

### Automation

APP package name is `io.github.neo.clash`. The `alpha` and `meta` flavors append an application id
suffix, so their package names are `io.github.neo.clash.alpha` and `io.github.neo.clash.meta`.

The component names and intent actions below do not depend on the flavor, because the Kotlin package
is `io.github.neo.clash` for every flavor.

- Toggle ClashNeo service status
  - Send intent to activity `io.github.neo.clash.ExternalControlActivity` with action `io.github.neo.clash.action.TOGGLE_CLASH`
- Start ClashNeo service
  - Send intent to activity `io.github.neo.clash.ExternalControlActivity` with action `io.github.neo.clash.action.START_CLASH`
- Stop ClashNeo service
  - Send intent to activity `io.github.neo.clash.ExternalControlActivity` with action `io.github.neo.clash.action.STOP_CLASH`
- Import a profile
  - URL Scheme `clashneo://install-config?url=<encoded URI>`

The legacy `clash://` and `clashmeta://` URL schemes and the legacy
`com.github.metacubex.clash.meta.action.*` intent actions are **not** supported.

### Contribution and Project Maintenance

#### Meta Kernel

- ClashNeo uses the kernel from `android-real` branch under `MetaCubeX/Clash.Meta`, which is a merge of the main `Alpha` branch and `android-open`.
  - If you want to contribute to the kernel, make PRs to `Alpha` branch of the Meta kernel repository.
  - If you want to contribute Android-specific patches to the kernel, make PRs to  `android-open` branch of the Meta kernel repository.

#### Maintenance

- When `MetaCubeX/Clash.Meta` kernel is updated to a new version, the `Update Dependencies` actions in this repo will be triggered automatically.
  - It will pull the new version of the meta kernel, update all the golang dependencies, and create a PR without manual intervention.
  - If there is any compile error in PR, you need to fix it before merging. Alternatively, you may merge the PR directly.
- Manually triggering `Build Pre-Release` actions will compile and publish a `PreRelease` version.
- Manually triggering `Build Release` actions will compile, tag and publish a `Release` version.
  - You must fill the blank `Release Tag` with the tag you want to release in the format of `v1.2.3`.
  - `versionName` and `versionCode` in `build.gradle.kts` will be automatically bumped to the tag you filled above.
