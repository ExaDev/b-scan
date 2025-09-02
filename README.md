# B-Scan - Universal Inventory Management

**Track anything, anywhere, with any identifier.**

B-Scan is a modern Android app that helps you organise your inventory using whatever identification method works best for you. Whether you're scanning RFID tags on 3D printer filament, reading QR codes on equipment, or simply typing in serial numbers, B-Scan adapts to your workflow.

Originally designed for Bambu Lab 3D printer filament management, B-Scan has grown into a universal inventory system that works with any component type and identification method.

## What Can You Track?

B-Scan works with any inventory you need to manage:

- **3D Printing Supplies**: Filament reels, nozzles, build plates, tools
- **Workshop Equipment**: Power tools, hand tools, consumables
- **Electronic Components**: Parts, assemblies, test equipment
- **Office Supplies**: Equipment, consumables, assets
- **Personal Collections**: Books, games, models, memorabilia
- **Any Custom Category**: You define what components matter to you

## How Do You Identify Items?

Use whatever works best for your situation:

- **📱 NFC/RFID Scanning**: Just tap your phone to any NFC tag
- **🔄 Future Expansion**: QR codes and barcodes (planned)
- **✏️ Manual Entry**: Type in serial numbers, part numbers, or custom IDs
- **🏷️ Batch Tracking**: Manage production lots and quality control
- **📦 SKU Lookup**: Connect to your existing catalog system

No special hardware required - use your phone's built-in NFC and camera, or enter everything manually.

## Key Features

✨ **Works Offline**: No internet required - all your data stays on your device  
🔄 **Flexible Organization**: Create hierarchical relationships between components  
🎨 **Visual Design**: Beautiful Material Design interface with colour-coded categories  
📊 **Smart Inventory**: Automatically aggregate stock levels across multiple instances  
⚖️ **Weight Tracking**: Connect Bluetooth scales for consumption monitoring  
💾 **Backup & Restore**: Export/import your complete inventory as JSON files  
🔒 **Privacy First**: Your data never leaves your device

## Getting Started

### Requirements

- **Android 10 (API 29) or newer** - Most phones from 2019 onwards
- **Optional features** (app works fine without these):
  - NFC for tap-to-scan identification
  - Camera for future QR code and barcode support
  - Bluetooth for connecting scales

### Installation

1. **Download** the latest APK from the [GitHub releases page](https://github.com/JoeMearman/b-scan/releases)
2. **Install** the APK (you may need to enable "Install from unknown sources" in Android settings)
3. **Launch** the app and grant permissions when prompted

### Quick Start

1. **Open B-Scan** on your Android device
2. **Choose your method**:
   - **NFC/RFID**: Tap your phone to any NFC tag
   - **Future**: QR codes and barcodes (in development)  
   - **Manual**: Tap "Add Item" to type in details
3. **Organise**: Create relationships between components, set up categories
4. **Track**: Monitor quantities, locations, and usage over time

The app automatically recognises different identification formats and presents information in a clean, easy-to-understand interface.

### Supported Identification Methods

**NFC/RFID Tags**:
- **Bambu Lab**: Complete support for all filament types with full specifications
- **Creality**: Basic support for ASCII-encoded filament data
- **Generic NFC**: Any NFC tag can store custom component information

**Visual Scanning**:
- **QR Codes**: Planned for future release
- **Barcodes**: Planned for future release

**Manual Methods**:
- **Serial Numbers**: Perfect for equipment and tools
- **Part Numbers**: Standard inventory identification
- **Custom IDs**: Whatever system works for your needs

## Privacy & Security

🔒 **Your data stays private**: B-Scan processes everything locally on your device  
🚫 **No internet required**: Works completely offline by design  
📱 **Local storage only**: All inventory data is stored on your device  
👁️ **Read-only access**: App can read identification sources but cannot modify them  
💾 **You control backups**: Export your data anytime as JSON files

## Current Limitations

**Scanning Features**:
- NFC requires close physical proximity (~4cm)
- Camera scanning needs good lighting and clear view
- Cannot write to or modify identification sources (read-only)

**By Design**:
- Works offline only (for privacy)
- Manual backup required for device transfers
- Local storage only (no cloud sync)

## Related Projects

This app is part of a larger ecosystem of 3D printing and inventory management tools:

- **OpenSpool**: ESP32 hardware for automatic filament detection
- **BambuSpoolPal**: Full-featured app with Spoolman integration  
- **RFID-Tag-Guide**: Research documentation for Bambu Lab RFID protocols

*Note: These are related projects that may have separate repositories or be part of private development.*

## Documentation

📖 **For Users**: This README covers everything you need to get started  
🛠️ **For Developers**: Technical specifications, architecture details, and development setup are in the [project wiki](../../wiki)  
📚 **For Contributors**: See the [Contributing](#contributing) section below

## Screenshots

*Screenshots will be added as the app develops*

## Contributing

We welcome contributions! Whether you want to:

- **Report bugs** or request features
- **Improve documentation** or translations
- **Add support** for new identification methods
- **Enhance the UI** or user experience

Please feel free to open an issue or submit a pull request.

### For Developers

If you're interested in contributing code:

1. **Fork** the repository
2. **Set up** your development environment (see the [project wiki](../../wiki) for build instructions)
3. **Test thoroughly** with real identification sources
4. **Submit** a pull request with clear description of changes

**Technical Details**: Architecture documentation, RFID specifications, and development guides are maintained in the [project wiki](../../wiki).

## Community

### Related Projects

B-Scan is part of a larger ecosystem of inventory and 3D printing management tools:

- **OpenSpool**: ESP32 hardware for automatic filament detection
- **BambuSpoolPal**: Full-featured app with Spoolman integration  
- **RFID-Tag-Guide**: Research documentation for Bambu Lab RFID protocols

*Note: These are related projects that may have separate repositories or be part of private development.*

### Third-Party Tools

- **[Spoolease](https://www.spoolease.io/)**: Web-based filament spool management platform
- **[Filaman](https://www.filaman.app/)**: Filament tracking and management application  
- **[MyFilametrics](https://www.myfilametrics.com/)**: Advanced filament analytics and monitoring platform

## License

This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE) file for details.

## Disclaimer

This is an unofficial third-party application. It is not affiliated with or endorsed by any manufacturer whose products it supports. Use at your own risk.