# Bambu Lab Catalog Updater

Simple Kotlin script to scrape current Bambu Lab product data and update the mappings files used by B-Scan.

## Usage

```bash
# Run the updater
cd developer-tools/catalog-updater
./gradlew run

# Or build and run JAR
./gradlew jar
java -jar build/libs/catalog-updater-1.0.0.jar
```

## What it does

1. **Scrapes Bambu Lab store** for current product SKUs
2. **Extracts product data** (names, colors, prices, availability)  
3. **Generates** `bambu_filament_mappings.json` - B-Scan compatible mappings format

## Output File

### bambu_filament_mappings.json
Compatible with B-Scan's `FilamentMappings` format:
```json
{
  "version": 1703123456,
  "lastUpdated": "2024-01-01T12:34:56",
  "colorMappings": { "#FF0000": "Red", ... },
  "materialMappings": { "PLA_BASIC": "PLA Basic", ... },
  "productCatalog": [
    {
      "variantId": "40206189035580",
      "productHandle": "pla-basic-filament",
      "productName": "PLA Basic",
      "colorName": "Jade White",
      "colorHex": "#F5F5DC",
      "colorCode": "10100",
      "price": 24.99,
      "available": true,
      "url": "https://uk.store.bambulab.com/products/pla-basic-filament",
      "manufacturer": "Bambu Lab",
      "materialType": "PLA_BASIC",
      "internalCode": "GFL00"
    }
  ]
}
```

## Git Tracking

Add the file to git for tracking:
```bash
git add bambu_filament_mappings.json
git commit -m "update: refresh Bambu Lab product catalog"
```

## Error Handling

The script will:
- Retry failed requests
- Continue on individual product failures  
- Output detailed progress information
- Exit with error code if critical failure occurs

## Dependencies

- Kotlin 1.9.22
- Coroutines for async scraping
- Jsoup for HTML parsing  
- Gson for JSON generation

No CLI framework needed - just runs and updates the files.