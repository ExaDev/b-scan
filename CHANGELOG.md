# [3.3.0](https://github.com/Mearman/b-scan/compare/v3.2.0...v3.3.0) (2025-08-23)


### Features

* **debug:** enhance data collection with complete raw and decrypted data capture ([ebfe2ed](https://github.com/Mearman/b-scan/commit/ebfe2edcf826aab8643b9c2994559e62cf2c6f50))
* **navigation:** add navigation callbacks to HomeScreen ([69557d4](https://github.com/Mearman/b-scan/commit/69557d490599f60cdafb42785e3ecc4db602c099))
* **navigation:** wrap NavHost in background Box and connect navigation actions ([4980e32](https://github.com/Mearman/b-scan/commit/4980e32e814428a0495480d9f8fff38954e14e1b))
* **nfc:** improve block data logging and complete raw data capture ([c6d60ce](https://github.com/Mearman/b-scan/commit/c6d60ce978963bcdebee6738c9958823eebdf935))
* **permissions:** add storage permissions for debug data export ([9d42884](https://github.com/Mearman/b-scan/commit/9d42884be7dbc98cf20574805a925f296efb8b6b))
* **scan:** add system bar padding to ScanPromptScreen ([f1f8b83](https://github.com/Mearman/b-scan/commit/f1f8b834accd8a1db9145b4dbbbc0df2c5b553a6))
* **theme:** implement transparent system bars for edge-to-edge design ([569d91a](https://github.com/Mearman/b-scan/commit/569d91ad5bf734d66a25b5d3af7b3eadb9b38730))
* **ui:** add raw data display and export functionality to debug card ([599fa4e](https://github.com/Mearman/b-scan/commit/599fa4e2e55fc4c10b7c1c27d6adde711cefb551))
* **ui:** add TopAppBar with navigation actions to DataBrowserScreen ([ca979b0](https://github.com/Mearman/b-scan/commit/ca979b060e3261526b79c9ebb9944a7c47c2aee3))
* **ui:** enable edge-to-edge display in MainActivity ([e7b52ee](https://github.com/Mearman/b-scan/commit/e7b52ee8e49f669fe6ad21111c2ce5cb03176130))

# [3.2.0](https://github.com/Mearman/b-scan/compare/v3.1.0...v3.2.0) (2025-08-23)


### Bug Fixes

* **decoder:** correct filament diameter parsing to 8-byte float ([bb7c556](https://github.com/Mearman/b-scan/commit/bb7c55622b473dd2e4cf5320cf9f048c6d628446))


### Features

* **decoder:** enhance Block 13 analysis for unknown format ([9a0eb8e](https://github.com/Mearman/b-scan/commit/9a0eb8e93b04175bfb8841b4549b7e8d203e9dc7))
* **decoder:** expand Block 17 data capture for research ([2db21c2](https://github.com/Mearman/b-scan/commit/2db21c2449be37e0578f42e7acf78277c3d24e12))
* **model:** add research fields for unknown RFID data blocks ([00e4c72](https://github.com/Mearman/b-scan/commit/00e4c72b91ad15a7a9b81bef9269b128f2f2810d))

# [3.1.0](https://github.com/Mearman/b-scan/compare/v3.0.0...v3.1.0) (2025-08-22)


### Bug Fixes

* Resolve HomeScreen.kt compilation errors and add filter system ([63b179a](https://github.com/Mearman/b-scan/commit/63b179ab2dcc6bafbbd810595ede07eae125e92c))
* **ui:** correct launcher icon scaling and positioning ([92abb3a](https://github.com/Mearman/b-scan/commit/92abb3a646ab7138ad3784d0aedde9509c3bc283))
* **ui:** eliminate list jumping by removing elastic scroll effects ([520b9b9](https://github.com/Mearman/b-scan/commit/520b9b9222d17581e7a37d5d05ddb850492abf18))
* **ui:** make overscroll-to-reveal work with non-scrollable content ([b4854ec](https://github.com/Mearman/b-scan/commit/b4854ec7c01d5b181184155893962109cabf9e47))
* **ui:** make scrolling behavior more realistic and natural ([058ba46](https://github.com/Mearman/b-scan/commit/058ba464eb81cf9c5f4302f5c59357ff392105e4))
* **ui:** make spool items move down when scan prompt is revealed ([0747240](https://github.com/Mearman/b-scan/commit/07472407c34834f510582860a5bacff5970e207c))
* **ui:** update FilamentDetailsScreen to use tagUid property ([71cc8a3](https://github.com/Mearman/b-scan/commit/71cc8a32ab1ba6ad1acdc10bb13cd5fdc26f5770))


### Features

* Add back navigation handling to FilamentDetailsScreen ([e21843e](https://github.com/Mearman/b-scan/commit/e21843ec0094d173ddc6ce44dddc567b7e626b8b))
* add generated data management methods to repository ([8fc7317](https://github.com/Mearman/b-scan/commit/8fc7317eca33cebe7a907a902e383a18e08ae7ea))
* Add progress callbacks to NFC tag reading ([5d80f48](https://github.com/Mearman/b-scan/commit/5d80f48de68aad66b060eeefa59b19894ec6df22))
* Add scan progress tracking and simulation to MainViewModel ([028a214](https://github.com/Mearman/b-scan/commit/028a2143a2790de7f49be8bff75a834d049d3275))
* Add ScanProgress model for tracking NFC scan progress ([4867d58](https://github.com/Mearman/b-scan/commit/4867d58a8b922db59693570ef4158a2dfccac299))
* add settings icon and navigation to MainActivity ([add10ae](https://github.com/Mearman/b-scan/commit/add10aeafe80265908667139af50765729919352))
* add swipe navigation and SKUs tab for inventory management ([bf670d0](https://github.com/Mearman/b-scan/commit/bf670d0052dc11941cfa8cff3f7bfc60467337ca))
* create comprehensive settings screen with configurable sample data ([0292f3d](https://github.com/Mearman/b-scan/commit/0292f3df2d112a579ef5c569664e971ce0fd4d9f))
* **data:** update sample data with realistic Bambu Lab filament SKUs ([5d5644a](https://github.com/Mearman/b-scan/commit/5d5644a97e415d1b5676901ad19e3e92965c4b6c))
* implement grouping dropdown in controls row ([73c65b9](https://github.com/Mearman/b-scan/commit/73c65b9b12522e8f64f2e9cfaedfd8074998b890))
* Integrate scan progress into MainActivity UI flow ([500272c](https://github.com/Mearman/b-scan/commit/500272c36325b611b22093f19829b2b3c23a7e15))
* **main:** integrate HomeScreen as new landing page ([c001fc1](https://github.com/Mearman/b-scan/commit/c001fc1be7dc25a2875416ab48b9056174a8f0be))
* redesign HomeScreen as comprehensive data browser ([9909f3b](https://github.com/Mearman/b-scan/commit/9909f3b795beefed7a4fb6781982156547a40f02))
* **ui:** add comprehensive filter system to HomeScreen data browser ([c06d6d7](https://github.com/Mearman/b-scan/commit/c06d6d7557e36b10070c5fdfcc1da1cab7adef60))
* **ui:** add elastic scrolling for better touch interaction ([22b3f9c](https://github.com/Mearman/b-scan/commit/22b3f9c51b7ab360b28ce90e4fa12558367df15e))
* **ui:** create new HomeScreen with combined scan prompt and recent spools ([44748b1](https://github.com/Mearman/b-scan/commit/44748b1338cad8b1f6b3a428812aeea32a3a3c55))
* **ui:** implement overscroll-to-reveal scan prompt behavior ([245dada](https://github.com/Mearman/b-scan/commit/245dada49e487171f184045573b6c8ccd4909a96))
* **ui:** show scan prompt by default, hide/reveal with scroll ([2e5c6c2](https://github.com/Mearman/b-scan/commit/2e5c6c20ace68f8afb607f47b1c05a84caa7b69b))

# [3.0.0](https://github.com/Mearman/b-scan/compare/v2.0.2...v3.0.0) (2025-08-22)


### Bug Fixes

* **update:** implement real-time download progress monitoring ([b8ab203](https://github.com/Mearman/b-scan/commit/b8ab20336eb9bdb820e73733260cbf3afa03911e))


### BREAKING CHANGES

* **update:** None - maintains existing UpdateDownloadService API

## [2.0.2](https://github.com/Mearman/b-scan/compare/v2.0.1...v2.0.2) (2025-08-22)


### Bug Fixes

* **ci:** remove custom release body template ([566460c](https://github.com/Mearman/b-scan/commit/566460ce5099c672aa1c3ed449488797df05a40a))

## [2.0.1](https://github.com/Mearman/b-scan/compare/v2.0.0...v2.0.1) (2025-08-22)


### Performance Improvements

* **ci:** optimize semantic-release build performance ([9d42eaf](https://github.com/Mearman/b-scan/commit/9d42eaf1cf0b67541ed87cee43bfd88d71375d95))

# [2.0.0](https://github.com/Mearman/b-scan/compare/v1.2.0...v2.0.0) (2025-08-22)


### Bug Fixes

* **ci:** remove npm cache from semantic-release workflow ([11b6c90](https://github.com/Mearman/b-scan/commit/11b6c90955a04d80daa784ce0b9f23ab6c26be3e))


### Features

* replace custom bash solution with semantic-release ([48aaea1](https://github.com/Mearman/b-scan/commit/48aaea15dee624fb8129cb0baca3cf654b45684a))


### BREAKING CHANGES

* Release workflow now uses semantic-release instead of custom bash solution. This provides better reliability and professional changelog formatting.

## [1.2.0](https://github.com/Mearman/b-scan/compare/v1.1.1...v1.2.0) (2025-08-22)

### Bug Fixes

* exclude version bump commits from changelog generation ([f7a8e65](https://github.com/Mearman/b-scan/commit/f7a8e6554dc4eb9cf09747e9f92b4dff80950d84))

### Features

* add version numbers to APK and AAB filenames in releases ([f46f5d0](https://github.com/Mearman/b-scan/commit/f46f5d0931646e483e7e61c3a774b5ee06a71f61))

## [1.1.1](https://github.com/Mearman/b-scan/compare/v1.1.0...v1.1.1) (2025-08-22)

### Bug Fixes

* improve release notes formatting and remove redundant information ([560cb2d](https://github.com/Mearman/b-scan/commit/560cb2d1cd0132b8bfc0abc522bd55dddb5d8395))

# [1.1.0](https://github.com/Mearman/b-scan/compare/v1.0.156...v1.1.0) (2025-08-22)

### Bug Fixes

* correct bash regex patterns in auto-release workflow ([a62cf73](https://github.com/Mearman/b-scan/commit/a62cf73026fc3c5bf196d3bf8d4bacf5d5deccbe))

### Features

* implement direct release workflow for every push to main ([e3a74bf](https://github.com/Mearman/b-scan/commit/e3a74bf072767159920b0a84bb7023e92be203ea))
* integrate Release Please for automated changelog and releases ([28f0104](https://github.com/Mearman/b-scan/commit/28f01047435dfd03aa39b21019fab5689dc8a888))
* **release:** generate structured changelog from conventional commits ([3537f10](https://github.com/Mearman/b-scan/commit/3537f10a23f999c0f6501ad48d8aa08152d8ddfa))

## [1.0.156](https://github.com/Mearman/b-scan/compare/v1.0.155...v1.0.156) (2025-08-22)

### Features

* **release:** hybrid approach using .github/release.yml template with build information ([80954bd](https://github.com/Mearman/b-scan/commit/80954bdd9bdb059d00c37aa6138b26c56449428f))

# [1.13](https://github.com/Mearman/b-scan/compare/v1.10...v1.13) (2025-08-14)

### Bug Fixes

* correct artifact paths for release file attachments ([2aaa804](https://github.com/Mearman/b-scan/commit/2aaa804824275dabe24d9f29b5f6016684a197bb))
* grant CI workflow write permissions for version commits ([789f188](https://github.com/Mearman/b-scan/commit/789f1880cc1507cb85006212393d4fd366b9cd79))
* resolve CI version management and compatibility issues ([883e97b](https://github.com/Mearman/b-scan/commit/883e97b7d720113a5c201f0a4947dcf0793135b6))

### Features

* implement deterministic versioning without repository modifications ([364f3ee](https://github.com/Mearman/b-scan/commit/364f3eaedff8092d4aec550fb39810b43623995c))

# [1.10](https://github.com/Mearman/b-scan/compare/v1.0.0-clean...v1.10) (2025-08-13)

### Major Features (consolidated from v1.0.0-clean to v1.10)

* **authentication:** implement comprehensive RFID authentication with fallback keys ([40524ab](https://github.com/Mearman/b-scan/commit/40524ab5ae493414b13be5f51a97ef0a9b680cb9))
* **caching:** implement derived key caching system for performance ([4848bba](https://github.com/Mearman/b-scan/commit/4848bba90377eb3d50b06838b9577b049a951f99))
* **debugging:** add comprehensive debug information collection and display ([22f9ebe](https://github.com/Mearman/b-scan/commit/22f9ebee716e7aeed99358612d5ef68fd22621d1))
* **decoder:** implement comprehensive RFID tag decoding per RFID-Tag-Guide ([22222398](https://github.com/Mearman/b-scan/commit/22222398661e42438ee29b0133dcdd2f821a56ab))
* **navigation:** migrate to Navigation Compose architecture ([10920d4](https://github.com/Mearman/b-scan/commit/109dd196917b74fd7433d0a67a40b08e89a24963))
* **repository:** implement scan history persistence and spool management ([d6577ce](https://github.com/Mearman/b-scan/commit/d6577ce91307c5b8cf9fda513041985a7065daa2))
* **testing:** comprehensive testing infrastructure with CI/CD optimisation ([86a527a](https://github.com/Mearman/b-scan/commit/86a527ab5adc578285b18a0b27fb9998ab0229c7))
* **tray-tracking:** comprehensive tray UID tracking system ([5ec092b](https://github.com/Mearman/b-scan/commit/5ec092bd892cbc1aa438c78cad548cec1d5083a0))
* **ui:** SpoolListScreen with collection statistics and filtering ([d6577ce](https://github.com/Mearman/b-scan/commit/d6577ce91307c5b8cf9fda513041985a7065daa2))

### Bug Fixes

* improve NFC authentication error logging ([1cebc1c](https://github.com/Mearman/b-scan/commit/1cebc1c54d85a501a4917a1487f78533519667c1))
* resolve colour preview card issues ([4c217977](https://github.com/Mearman/b-scan/commit/4c217977ec43e836e28cb90251c4b76a3c72693c))
* resolve transparent filament identification ([8f777b5](https://github.com/Mearman/b-scan/commit/8f777b51d9d3afef5ad7b1cafc1b79b8b3647c5f))

### Performance Improvements

* enable R8 full mode and incremental build optimisations ([ccd1ba7](https://github.com/Mearman/b-scan/commit/ccd1ba76257e7301e100cc42856c90b8d2bd0c2b))
* optimise NFC sector authentication with smart key ordering ([4c7c0b9](https://github.com/Mearman/b-scan/commit/4c7c0b997c0ab2756413025e57e3c11cd05f70a6))
