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
