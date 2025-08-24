# [3.10.0](https://github.com/Mearman/b-scan/compare/v3.9.0...v3.10.0) (2025-08-24)


### Bug Fixes

* **build:** correct jacoco exclude syntax for gradle compatibility ([91e780d](https://github.com/Mearman/b-scan/commit/91e780d43b2405c9f6068f10122fc47c2563ce8c))


### Features

* **ui:** add overscroll behaviour to EnhancedSkusList ([944ac2d](https://github.com/Mearman/b-scan/commit/944ac2d3808bd8a5ac7549e5bd971a0a1e69e1ac))
* **ui:** enable overscroll scan prompts across all tabs ([42126e9](https://github.com/Mearman/b-scan/commit/42126e9c1b0f1c5a9bc804b7e6669dc4189362f5))

# [3.9.0](https://github.com/Mearman/b-scan/compare/v3.8.0...v3.9.0) (2025-08-24)


### Bug Fixes

* **interpreter:** handle UNKNOWN tag format in InterpreterFactory ([f986e5c](https://github.com/Mearman/b-scan/commit/f986e5c17df3a86b9b7c3666da0887a1ac933e09))
* **nfc:** resolve authentication failures and caching issues ([1e7768a](https://github.com/Mearman/b-scan/commit/1e7768ae3b83765fd68fbc452d1402e312cb9ecd))
* restore missing imports in DataBrowserScreen ([ac3da32](https://github.com/Mearman/b-scan/commit/ac3da32e34d6cbd4a029daee90ff23779587928c))
* **test:** add explicit MockitoAnnotations.openMocks() calls ([f2868b1](https://github.com/Mearman/b-scan/commit/f2868b18becf8e82249008db3b9601f8e74b35fa))
* **ui:** migrate Help icon to AutoMirrored variant ([4a84fdc](https://github.com/Mearman/b-scan/commit/4a84fdcabd7ed1810a77ff7c1c5d2ab2886cbcaa))
* **ui:** update NestedScrollSource usage for API compatibility ([325dcac](https://github.com/Mearman/b-scan/commit/325dcac27fb1308a5d4f950fc987d4229f050c75))


### Features

* add mappings repository for updateable filament mappings ([9b7de46](https://github.com/Mearman/b-scan/commit/9b7de4619319322e1ed5711d45b8498e4a04fa7c))
* add runtime filament interpretation system ([b3383d7](https://github.com/Mearman/b-scan/commit/b3383d7f88f1896acd955fcde135b4821168f503))
* **data:** add BambuProductDatabase with comprehensive product catalogue ([3d5a17d](https://github.com/Mearman/b-scan/commit/3d5a17d5d63188e0b4a016e4984a3ff7ca19d616))
* **debug:** add support for creating new scan data models ([10bc651](https://github.com/Mearman/b-scan/commit/10bc651e3d4b5dd94534fbe9104bd001784a49ec))
* **decoder:** integrate product lookup in tag decoding process ([b072dc6](https://github.com/Mearman/b-scan/commit/b072dc655320cb4822c9e903cf489480bea87f7d))
* **detector:** add tag format detection infrastructure ([52f0909](https://github.com/Mearman/b-scan/commit/52f0909cc2cf427ecc2d6ccaec20056fe4a1df26))
* implement standard pull-to-refresh with scan prompts ([f8aa5d2](https://github.com/Mearman/b-scan/commit/f8aa5d2b04d00e99f67ab4f449f47c3697e390df))
* **interpreter:** enhance BambuFormatInterpreter to accept UNKNOWN format ([fa83629](https://github.com/Mearman/b-scan/commit/fa83629e6dfd93a46fa30de2451ace2e9ec5cc0e))
* **model:** add BambuProduct data model for purchase links ([16aa130](https://github.com/Mearman/b-scan/commit/16aa1303b4055e4f23af799463371fa85fa85c08))
* **model:** add BambuProduct field to FilamentInfo ([ca8762c](https://github.com/Mearman/b-scan/commit/ca8762ccddf02421f125e168da70c68c9e0ba214))
* **model:** add format detection fields to data models ([8117e61](https://github.com/Mearman/b-scan/commit/8117e619ef6fd06eb14413f3450fc4ca07b2ff75))
* **repository:** add DataExportManager for scan data export/import ([7b7730b](https://github.com/Mearman/b-scan/commit/7b7730b0f93347e0ebb27095d06ee0ba0b647007))
* **repository:** add SkuRepository for product lookup services ([45e9276](https://github.com/Mearman/b-scan/commit/45e92763ff39f1d36f9fb694e8d9143923848aa4))
* **settings:** integrate export/import functionality in settings screen ([63a8e8a](https://github.com/Mearman/b-scan/commit/63a8e8a7219935c2140c784bdacfe6dad4d30212))
* **test:** enhance SampleDataGenerator with product integration ([757366a](https://github.com/Mearman/b-scan/commit/757366aac54176e674ab987882eda009fc99ceb1))
* **test:** enhance SampleDataGenerator with realistic Bambu format data ([6c5f63d](https://github.com/Mearman/b-scan/commit/6c5f63d8dffc5e68c20b2d3c1b39cd5ff49461b6))
* **ui:** add detail screen for individual scan viewing ([237ee95](https://github.com/Mearman/b-scan/commit/237ee9561ff2052e694bb329f977d814c8f0e296))
* **ui:** add EnhancedSkusList component for advanced product browsing ([3317c73](https://github.com/Mearman/b-scan/commit/3317c730380e5842b8350e3a908b81a00cdc92a3))
* **ui:** add ExportImportCard component for data management ([10e67d5](https://github.com/Mearman/b-scan/commit/10e67d53dcef546f7f9cdc3ba1ec7d161cf68cb0))
* **ui:** add PurchaseLinksCard component for e-commerce integration ([a5b49ca](https://github.com/Mearman/b-scan/commit/a5b49ca9fcd63c2405924306a5576e6638c8464e))
* **ui:** enhance home screen components with improved filtering and display ([cb477fc](https://github.com/Mearman/b-scan/commit/cb477fcdb1af8a1385b82884f1093b597f757df9))
* **ui:** implement full-page scan prompt with enhanced overscroll behavior ([dce9c1b](https://github.com/Mearman/b-scan/commit/dce9c1bd36b2fb2b3ba6e7ea2e4a73f1666b23d6))
* **ui:** integrate purchase links in FilamentDetailsScreen ([c80c349](https://github.com/Mearman/b-scan/commit/c80c349570dc46a65183c4dacbe07b32a101a263))

# [3.8.0](https://github.com/Mearman/b-scan/compare/v3.7.0...v3.8.0) (2025-08-23)


### Bug Fixes

* **ui:** refactor TagsList to display individual tag UIDs with navigation ([6459386](https://github.com/Mearman/b-scan/commit/645938696e2da681106f777678e709cd9ed4ff8b))


### Features

* **ui:** add individualTags parameter to DataBrowserScreen ([5b8bde0](https://github.com/Mearman/b-scan/commit/5b8bde02643ed5837bce503742e35a972727f95f))
* **ui:** load separate datasets for spools and individual tags ([ccc1e08](https://github.com/Mearman/b-scan/commit/ccc1e084cde4133f7aeaf5fab18d2482e8cc32fa))

# [3.7.0](https://github.com/Mearman/b-scan/compare/v3.6.0...v3.7.0) (2025-08-23)


### Features

* **data:** add tray-based spool grouping and details retrieval ([705a4a1](https://github.com/Mearman/b-scan/commit/705a4a1acfb56653221c3db5ff2c83ec20d4853e))
* **navigation:** add spool details navigation and auto-navigation after scan ([fbbbc96](https://github.com/Mearman/b-scan/commit/fbbbc96f26bb14617402d1c21862f23720ec2a46))
* **repository:** add backward compatibility for tag UID lookups ([324a9b2](https://github.com/Mearman/b-scan/commit/324a9b2aef788b53c1bc7e6382df4b7c79420841))
* **ui:** add click functionality to spool cards ([64f4c02](https://github.com/Mearman/b-scan/commit/64f4c02d2bc8b44b7760e19a233dcc29df080bb6))
* **ui:** add navigation from scan history to spool details ([a740f8c](https://github.com/Mearman/b-scan/commit/a740f8ca8e1a6a19d8fd5198632947d41bc89353))
* **ui:** add spool details screen and components ([796e4c1](https://github.com/Mearman/b-scan/commit/796e4c1e8ac7a68a368264c10ea93b5ca51d75f4))
* **ui:** integrate details navigation in home screens ([1b7131a](https://github.com/Mearman/b-scan/commit/1b7131ad36c614c4ad2687e7c20dfbb7739184c2))
* **ui:** switch home screen to tray-based spool grouping ([5950ec5](https://github.com/Mearman/b-scan/commit/5950ec50fd2a5a994e98262d3f86302d116c36c6))
* **ui:** update spool list to use tray-based grouping and navigation ([f6fc320](https://github.com/Mearman/b-scan/commit/f6fc320c73307d3f947688a9465b2d97f297ae5d))

# [3.6.0](https://github.com/Mearman/b-scan/compare/v3.5.0...v3.6.0) (2025-08-23)


### Bug Fixes

* **ci:** resolve YAML syntax error in commit message ([08d678c](https://github.com/Mearman/b-scan/commit/08d678c951e11332b7c215364d4c916394168f41))
* **ci:** resolve YAML syntax error in workflow commit message ([d6bb663](https://github.com/Mearman/b-scan/commit/d6bb663a9d6fe33fa1ee66d53162e521a7d9c5f6))
* **ci:** simplify commit message to resolve YAML syntax error ([2e08c6b](https://github.com/Mearman/b-scan/commit/2e08c6bd857eda9e320882cf3089b55e42828b0b))
* **ci:** update job dependencies and references ([fb2b174](https://github.com/Mearman/b-scan/commit/fb2b174821e1dcde10f54094853f756e9f8370cf))
* **ci:** use single-line commit message to resolve YAML parsing ([f84ed57](https://github.com/Mearman/b-scan/commit/f84ed57be37f79ecb7dab9fdbd44f31fbca48f45))


### Features

* **ci:** add semantic-release execution and post-release commit ([64c8ceb](https://github.com/Mearman/b-scan/commit/64c8ceb229bf560f1ade246a0108571e87b7223d))
* **ci:** add semantic-release setup to build job ([ed7b4a7](https://github.com/Mearman/b-scan/commit/ed7b4a77f506c31c54881defd2be05b735f1f31a))
* **ci:** add version file updates before build ([321b160](https://github.com/Mearman/b-scan/commit/321b1603d889df7ba7d46d4fe83b2f9d5c9d6a41))
* **ci:** replace static version with semantic-release dry-run ([e6bed68](https://github.com/Mearman/b-scan/commit/e6bed689cf8bb678cec55587eafc618a2f735999))
* **ui:** implement vertical mirror for sort direction icon ([c978f6c](https://github.com/Mearman/b-scan/commit/c978f6cf5627590e05393f3ab37540240453c0a7))

# [3.5.0](https://github.com/Mearman/b-scan/compare/v3.4.0...v3.5.0) (2025-08-23)


### Bug Fixes

* **ui:** make filter dialog scrollable ([71ccbdb](https://github.com/Mearman/b-scan/commit/71ccbdba5c5c96fb1d543392d317ea778a209d6b))


### Features

* **ui:** add separate sort direction toggle button with property labels ([723ffb6](https://github.com/Mearman/b-scan/commit/723ffb6e37e8187a7cae8f2fe8ce42e2294d51e7))
* **ui:** implement directional sorting logic across all list components ([452c710](https://github.com/Mearman/b-scan/commit/452c710e3d9600eee59a1855901eae1cb5f9cd6f))
* **ui:** implement separate sort property and direction state management ([a302591](https://github.com/Mearman/b-scan/commit/a3025916a6fcfe59778f6b25df2824366769412f))

# [3.4.0](https://github.com/Mearman/b-scan/compare/v3.3.0...v3.4.0) (2025-08-23)

### Features

* **ui:** improve controls layout with compact buttons and fix dropdown positioning ([3bf539f](https://github.com/Mearman/b-scan/commit/3bf539f5f6fdc2aa1ce3c17ee44b3fa17630fba6))

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

* **ci:** optimise semantic-release build performance ([9d42eaf](https://github.com/Mearman/b-scan/commit/9d42eaf1cf0b67541ed87cee43bfd88d71375d95))

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

## [1.0.155](https://github.com/Mearman/b-scan/compare/v1.0.154...v1.0.155) (2025-08-22)

### Features

* **release:** use .github/release.yml template for automated changelog categorisation ([df0674b](https://github.com/Mearman/b-scan/commit/df0674b6a4b6d96e3e5b300f0273f09f7be55b0b))

## [1.0.154](https://github.com/Mearman/b-scan/compare/v1.0.131...v1.0.154) (2025-08-22)

### Bug Fixes

* **ci:** add artifact validation and improved error handling for release creation ([5eb3c85](https://github.com/Mearman/b-scan/commit/5eb3c8576c044bef0ed92599e41a9462a95593fb))
* **ci:** correct artifact paths in release creation ([d71d981](https://github.com/Mearman/b-scan/commit/d71d98190b5e2508a0e4048dd637c0eb268e9858))
* **ci:** migrate from deprecated gradle-build-action to setup-gradle ([da56a59](https://github.com/Mearman/b-scan/commit/da56a592ea8e45b8b3bf00d08d1775286f92b21f))
* **ci:** resolve build scan terms and lint reports path issues ([a1a7898](https://github.com/Mearman/b-scan/commit/a1a789808105a052cb196fcb6b0ea4f33213e680))
* **ci:** update deprecated gradle-home-cache-cleanup to cache-cleanup ([214a290](https://github.com/Mearman/b-scan/commit/214a290a6294eb6a377ad0d656b53fcb2bd7fdcd))
* **ci:** use correct full artifact paths for release ([468e7ed](https://github.com/Mearman/b-scan/commit/468e7ed3c3d9ea2d03353cfb5fa544f1cf1f8ae5))
* improve tray UID display formatting to prevent garbled text ([68b9b25](https://github.com/Mearman/b-scan/commit/68b9b255b0f8fe256ddf39e0897a08a704d8e126))
* resolve compilation errors in tray tracking implementation ([0bf93ce](https://github.com/Mearman/b-scan/commit/0bf93cec3a138e849a89d0b57f4a2430b618fd3a))

### Features

* add reactive data refresh to tray tracking screen ([de78578](https://github.com/Mearman/b-scan/commit/de78578afe92977f8f4e6b47ed047538c3678365))
* add comprehensive tray UID tracking system ([5ec092b](https://github.com/Mearman/b-scan/commit/5ec092bd892cbc1aa438c78cad548cec1d5083a0))
* **ci:** enhance gradle-build-action with build scans and job summaries ([dad4813](https://github.com/Mearman/b-scan/commit/dad48134fa7316e16b5c936be4b3d0b6021b5cde))

### Performance Improvements

* **android:** enable R8 full mode and incremental build optimisations ([ccd1ba7](https://github.com/Mearman/b-scan/commit/ccd1ba76257e7301e100cc42856c90b8d2bd0c2b))
* **cache:** enable remote build cache for cross-run persistence ([d94d3bf](https://github.com/Mearman/b-scan/commit/d94d3bf20eb63a0d2c62488e8825989fa011fd0b))
* **ci:** add multi-tier build output caching for incremental builds ([8a5e0eb](https://github.com/Mearman/b-scan/commit/8a5e0eb92e38a5a137adda9c2ee65b60be065cde))
* **ci:** optimise Gradle environment variables for performance ([2898b21](https://github.com/Mearman/b-scan/commit/2898b21bc1fafef30ccafd67a380548c982366db))
* **ci:** remove unnecessary job dependencies for parallel execution ([9f28079](https://github.com/Mearman/b-scan/commit/9f28079c2b500ee115fc992216c0ba360c41be98))
* **ci:** upgrade Android emulator configuration for better performance ([ab61f99](https://github.com/Mearman/b-scan/commit/ab61f999e4597f1e304bd2cbd9365d5767274f64))
* enable stable configuration cache and Kotlin incremental compilation ([e774f71](https://github.com/Mearman/b-scan/commit/e774f71111d89022087e15e09cff9c6370dfd280))

## [1.0.131](https://github.com/Mearman/b-scan/compare/v1.0.129...v1.0.131) (2025-08-22)

### Bug Fixes

* **build:** configure explicit debug keystore for consistent CI/local signing ([2533d6e](https://github.com/Mearman/b-scan/commit/2533d6e6fe38504ae1f24ec2d715e8b6dbf2abc0))

### Features

* **ci:** add keystore verification logging to build action ([96bd847](https://github.com/Mearman/b-scan/commit/96bd847dbe77aa2bbd885359785e6161b9ce60de))

## [1.0.129](https://github.com/Mearman/b-scan/compare/v1.0.120...v1.0.129) (2025-08-22)

### Features

* migrate to Navigation Compose from conditional rendering ([109dd19](https://github.com/Mearman/b-scan/commit/109dd196917b74fd7433d0a67a40b08e89a24963))

## [1.0.120](https://github.com/Mearman/b-scan/compare/v1.0.118...v1.0.120) (2025-08-22)

### Bug Fixes

* **ci:** allow merge commits to bypass conventional commit validation ([f99f2d7](https://github.com/Mearman/b-scan/commit/f99f2d70fdf68ecf4e3aed599be0ef35f008d95a))
* **ci:** resolve shell command substitution in release messages ([2119db7](https://github.com/Mearman/b-scan/commit/2119db7ba71b5c2dbd70c4595dd62b8b2f1e2b23))
* resolve ArrowBack icon deprecation in SpoolListScreen ([d81cf03](https://github.com/Mearman/b-scan/commit/d81cf0391609eb3fc527a36116ff84fac08e77db))
* resolve deprecation warnings in MainActivity ([e250bd5](https://github.com/Mearman/b-scan/commit/e250bd518943f7e12d0f5283abf81ba8ca64182e))
* resolve getParcelableExtra deprecation in NfcManager ([9ecc8d6](https://github.com/Mearman/b-scan/commit/9ecc8d65737a8ea2173df928d61422c29fc0f79f))

### Features

* optimise NFC sector authentication with smart key ordering (#7) ([4c7c0b9](https://github.com/Mearman/b-scan/commit/4c7c0b997c0ab2756413025e57e3c11cd05f70a6))

## [1.0.118](https://github.com/Mearman/b-scan/compare/v1.0.106...v1.0.118) (2025-08-22)

### Features

* **build:** add automatic git hooks installation ([effde50](https://github.com/Mearman/b-scan/commit/effde50d0221b86fb6f6e0f9a50b300ae5821017))
* **build:** add git commit template for conventional commits ([f75ed51](https://github.com/Mearman/b-scan/commit/f75ed516c01a25249feaaf237be088fe73676703))
* **ci:** add automatic changelog generation from conventional commits ([3163bfd](https://github.com/Mearman/b-scan/commit/3163bfd20574e2ae6a788e1a4057429d963b14dc))
* **debug:** add bed temperature debugging helper ([4bf3695](https://github.com/Mearman/b-scan/commit/4bf3695384dbd9bb63d694f4c08928ebf103d8cc))
* **decoder:** add comprehensive bed temperature debugging ([6331f03](https://github.com/Mearman/b-scan/commit/6331f0321220d6ba18fac4cb7c46c6a188cfc676))
* **nfc:** enable fallback keys and improve sector 1 debugging ([a3d44c2](https://github.com/Mearman/b-scan/commit/a3d44c2c5a26d8da2256d34140ed2adcef279492))
* **repository:** add unique spool aggregation methods ([a838047](https://github.com/Mearman/b-scan/commit/a8380471a5fcc7ea11416b3fc5a4a4d1f96f1ae6))
* **ui:** add SpoolListScreen with collection statistics and filtering ([d6577ce](https://github.com/Mearman/b-scan/commit/d6577ce91307c5b8cf9fda513041985a7065daa2))
* **ui:** integrate spool list navigation in MainActivity ([8be3e8d](https://github.com/Mearman/b-scan/commit/8be3e8da956a9d30a843a2a4ec2a278a689fe63f))
* **viewmodel:** add spool list navigation state management ([529e19e](https://github.com/Mearman/b-scan/commit/529e19e47fc348d2a327e576eb5cac6a053c409a))

### Performance Improvements

* IDE Config & Admin Structure Change (#6) ([342899c](https://github.com/Mearman/b-scan/commit/342899ce4e1906f8daa61c87c72549ec79b6f842))

## [1.0.106](https://github.com/Mearman/b-scan/compare/v1.0.88...v1.0.106) (2025-08-22)

### Bug Fixes

* **test:** replace assertDoesNotThrow with try-catch in cache tests ([b5d15cd](https://github.com/Mearman/b-scan/commit/b5d15cdd493abc54e0eec50e3fa8ac7b455977ac))
* resolve assertion inconsistencies in CachedBambuKeyDerivationTest ([4b34802](https://github.com/Mearman/b-scan/commit/4b348027ba84519647bc12b698be6779de13520b))
* resolve assertion inconsistencies in DerivedKeyCacheTest ([57db558](https://github.com/Mearman/b-scan/commit/57db55807ecb4b888a7fe3e07052a0ed59476dde))

### Features

* test: add ColorPreviewCard parseColor function test coverage ([e28fc6c](https://github.com/Mearman/b-scan/commit/e28fc6caa646a7f0676e5b4bb1cb11d1df2b7234))
* test: add comprehensive NfcManager test coverage ([45f7695](https://github.com/Mearman/b-scan/commit/45f76956784339c05906a07b7d296385061f4164))
* test: add comprehensive tests for key derivation algorithm changes ([9f09ee7](https://github.com/Mearman/b-scan/commit/9f09ee746135a9a27943b0bcdbb0ce53f89596a4))
* test: add comprehensive transparency detection tests ([e1db1c8](https://github.com/Mearman/b-scan/commit/e1db1c83ebc54f4f95ed5bdfefd784f06d0ca2e9))
* test: add transparency handling tests to decoder error handling ([ee74a63](https://github.com/Mearman/b-scan/commit/ee74a633e889651bc8e3aeafeba94dd5235f6d6a))

## [1.0.88](https://github.com/Mearman/b-scan/compare/v1.0.84...v1.0.88) (2025-08-22)

### Features

* Merge pull request #4 from srleach/feature/purge-from-cache ([6ddef11](https://github.com/Mearman/b-scan/commit/6ddef1140082c17b8903d02c45fb6d255ec2b501))
* Merge pull request #3 from srleach/feature/cache-derived-keys ([c1d6263](https://github.com/Mearman/b-scan/commit/c1d626b321690199b175e6faa368f1f9f6dec917))
* Merge pull request #2 from srleach/bugfix/resolve-colour-preview ([14affa7](https://github.com/Mearman/b-scan/commit/14affa7baa314bc3c06883997cdadffd4fab9d60))
* Merge pull request #1 from srleach/bugfix/resolve-bad-read ([341370a](https://github.com/Mearman/b-scan/commit/341370a2418d7c5b096495ef20a653ab5dbb2ae4))
* Add cache invalidation ([4e33ae8](https://github.com/Mearman/b-scan/commit/4e33ae8784e0f2aaac8cc0234b40b75695b96c99))
* Caching implementation ([4848bba](https://github.com/Mearman/b-scan/commit/4848bba90377eb3d50b06838b9577b049a951f99))
* Resolve transparent filament identification ([8f777b5](https://github.com/Mearman/b-scan/commit/8f777b51d9d3afef5ad7b1cafc1b79b8b3647c5f))
* Resolve colour hex name issue ([4c21797](https://github.com/Mearman/b-scan/commit/4c217977ec43e836e28cb90251c4b76a3c72693c))
* Convert RGBA format (from the tag data) to AARRGGBB format ([a261484](https://github.com/Mearman/b-scan/commit/a26148419cda297e829d836d10ec0ddcec9a3f7e))
* Modify key derivation process ([49939d6](https://github.com/Mearman/b-scan/commit/49939d6ed51134b5951e692bab7255c47d8f6f47))

## [1.0.84](https://github.com/Mearman/b-scan/compare/v1.0.82...v1.0.84) (2025-08-22)

Note: Same content as v1.0.88 due to duplicate tagging

## [1.0.82](https://github.com/Mearman/b-scan/compare/v1.0.81...v1.0.82) (2025-08-14)

### Features

* add UID to debug info for NFC authentication troubleshooting ([e24b121](https://github.com/Mearman/b-scan/commit/e24b121492564c282ad46f48d5fba2883bc34fa8))

## [1.0.81](https://github.com/Mearman/b-scan/compare/v1.0.80...v1.0.81) (2025-08-14)

### Bug Fixes

* display actual app version instead of hardcoded "1.0" ([a06ab27](https://github.com/Mearman/b-scan/commit/a06ab276095def6e3afbb0f25be75bd5647b6b44))

## [1.0.80](https://github.com/Mearman/b-scan/compare/v1.0.79...v1.0.80) (2025-08-14)

### Bug Fixes

* improve NFC authentication error logging and debugging ([1cebc1c](https://github.com/Mearman/b-scan/commit/1cebc1c54d85a501a4917a1487f78533519667c1))

## [1.0.79](https://github.com/Mearman/b-scan/compare/v1.0.70...v1.0.79) (2025-08-14)

### Features

* integrate debug information throughout MainActivity UI flow ([2851af2](https://github.com/Mearman/b-scan/commit/2851af26d184898dc659c7265af41dd679c01bac))
* integrate debug information display in FilamentDetailsScreen ([1f6b160](https://github.com/Mearman/b-scan/commit/1f6b160496993cd864f577009e89d5360bbc352e))
* add comprehensive DebugInfoCard with copy-to-clipboard functionality ([0f1b80f](https://github.com/Mearman/b-scan/commit/0f1b80fad322b72f99e3a8b203cf3f122b7ed9ee))
* create ErrorScreen component with debug information display ([22f9ebe](https://github.com/Mearman/b-scan/commit/22f9ebee716e7aeed99358612d5ef68fd22621d1))
* integrate debug information into UI state management ([a0304f8](https://github.com/Mearman/b-scan/commit/a0304f8279bcbe61de82c918be315a1775e4bd76))

## [1.0.70](https://github.com/Mearman/b-scan/compare/v1.0.63...v1.0.70) (2025-08-14)

### Features

* add comprehensive HKDF key derivation tests ([b240613](https://github.com/Mearman/b-scan/commit/b24061360bff38d998b6c91ab19f8c491d83db86))
* enhance RFID authentication with comprehensive fallback keys ([4052404](https://github.com/Mearman/b-scan/commit/40524ab5ae493414b13be5f51a97ef0a9b680cb9))
* add authentication status check to DebugDataCollector ([d9f3da7](https://github.com/Mearman/b-scan/commit/d9f3da7ad28e75501161c0f552370b0f209d56ea))
* enhance HKDF key derivation with validation and logging ([18acd82](https://github.com/Mearman/b-scan/commit/18acd82c6ff5bcdc5e17ed2b88b6ff44bf6ef792))
* add simple RFID decoder validation tests ([dc942e4](https://github.com/Mearman/b-scan/commit/dc942e4cb67f57dc05ad9f7bcf81454139253f3d))
* add integration tests for real-world RFID tag scenarios ([571d3e2](https://github.com/Mearman/b-scan/commit/571d3e2e3f91465854f8d6b16e854069452b998a))
* add comprehensive error handling tests for RFID decoder ([24711041](https://github.com/Mearman/b-scan/commit/24711041546514167fd0d382073cfba4016a9ddc))
* add comprehensive RFID decoder tests with real example data ([22222398](https://github.com/Mearman/b-scan/commit/22222398661e42438ee29b0133dcdd2f821a56ab))
* implement comprehensive RFID tag decoding per RFID-Tag-Guide ([82256fa](https://github.com/Mearman/b-scan/commit/82256fa049b1b9f7b335a1ded6b6c37017d76430))
* extend FilamentInfo model with comprehensive RFID fields ([2c53626](https://github.com/Mearman/b-scan/commit/2c53626f60cf3e4f3c2f09b760fef67808ae45b2))

## [1.0.63](https://github.com/Mearman/b-scan/compare/v1.0.62...v1.0.63) (2025-08-14)

### Bug Fixes

* correct version fallback in build.gradle.kts ([6307faa](https://github.com/Mearman/b-scan/commit/6307faa766addb717dd01f6421f05db429dc747a))

## [1.0.62](https://github.com/Mearman/b-scan/compare/v1.0.61...v1.0.62) (2025-08-14)

### Bug Fixes

* resolve Compose state issue in CI navigation test ([92a657e](https://github.com/Mearman/b-scan/commit/92a657ed9cf05e9dfac532a65bd324f07b9f847a))

## [1.0.61](https://github.com/Mearman/b-scan/compare/v1.0.60...v1.0.61) (2025-08-14)

### Bug Fixes

* resolve compilation error in CI test ([ddd338f](https://github.com/Mearman/b-scan/commit/ddd338fbfe5b32cb9cf9c74f8fd03f936199d7b8))

## [1.0.60](https://github.com/Mearman/b-scan/compare/v1.0.59...v1.0.60) (2025-08-14)

### Bug Fixes

* resolve NFC dependency issues in CI instrumented tests ([84da765](https://github.com/Mearman/b-scan/commit/84da7657ab49ec48138e426b4b56adc533962f15))

## [1.0.59](https://github.com/Mearman/b-scan/compare/v1.0.58...v1.0.59) (2025-08-14)

### Bug Fixes

* resolve CI instrumented test timing issues ([19effd9](https://github.com/Mearman/b-scan/commit/19effd9f65d9af8afd6586c1a62de87ab95f1e7e))

## [1.0.58](https://github.com/Mearman/b-scan/compare/v1.0.55...v1.0.58) (2025-08-14)

### Features

* enhance test runner with CI mode support ([d3aa356](https://github.com/Mearman/b-scan/commit/d3aa356da0cb4fbe57de037c6a8f69ed28069278))
* add CI-optimised instrumented tests ([dd29b94](https://github.com/Mearman/b-scan/commit/dd29b948a20168d57c7ed2ec618d26ad37afa2ac))

## [1.0.55](https://github.com/Mearman/b-scan/compare/v1.0.51...v1.0.55) (2025-08-14)

### Bug Fixes

* optimise CI pipeline for reliability and resource constraints ([ec33eb4](https://github.com/Mearman/b-scan/commit/ec33eb4b68d8c528dcd9f7b22eb9a198aa95a3e6))
* resolve CI linting task failures ([169439d](https://github.com/Mearman/b-scan/commit/169439d45591926b8246a3bf5adb7557be8a3ed0))

### Features

* comprehensive e2e testing infrastructure ([4c6f853](https://github.com/Mearman/b-scan/commit/4c6f853a01ab4275fa20c58f1bd83617e0d1a680))

## [1.0.51](https://github.com/Mearman/b-scan/compare/v1.0.50...v1.0.51) (2025-08-14)

### Features

* comprehensive improvements and testing infrastructure ([86a527a](https://github.com/Mearman/b-scan/commit/86a527ab5adc578285b18a0b27fb9998ab0229c7))

## [1.0.50](https://github.com/Mearman/b-scan/compare/v1.0.49...v1.0.50) (2025-08-14)

### Bug Fixes

* resolve history page crash with LocalDateTime serialization ([565c00a](https://github.com/Mearman/b-scan/commit/565c00abe2993edd3f07756e43b9e1917f8a0711))

## [1.0.49](https://github.com/Mearman/b-scan/compare/v1.13...v1.0.49) (2025-08-14)

Note: This release has the same content as v1.13 due to parallel development

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

## [1.0.0-clean](https://github.com/Mearman/b-scan/releases/tag/v1.0.0-clean) (2025-08-13)

Initial clean release with basic NFC tag reading functionality and Android app foundation.

### Development Tags (2025-08-13)

* **v2025.08.13-df3ce16:** fix: use default debug signing config instead of creating duplicate
* **v2025.08.13-8b6f016:** fix: replace deprecated GitHub Actions with modern GitHub CLI
* **v2025.08.13-7f252dc:** fix: add missing FilamentInfo parameters
* **v2025.08.13-6defddd:** feat: significantly improve CI build caching strategy
* **v2025.08.13-5105bac:** fix: add contents write permission to release workflow
* **v2025.08.13-017a76f:** debug: add comprehensive logging to diagnose NFC colour issue
