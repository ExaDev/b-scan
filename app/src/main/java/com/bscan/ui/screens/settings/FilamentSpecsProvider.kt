package com.bscan.ui.screens.settings

object FilamentSpecsProvider {
    
    fun getFilamentSpecs(): List<Triple<String, String, Pair<String, String>>> {
        return listOf(
            // PLA Basic Series
            Triple("Bambu White", "#FFFFFF", "PLA" to "PLA Basic"),
            Triple("Bambu Black", "#2C2C2C", "PLA" to "PLA Basic"),
            Triple("Bambu Red", "#DC3545", "PLA" to "PLA Basic"),
            Triple("Bambu Blue", "#2196F3", "PLA" to "PLA Basic"),
            Triple("Bambu Green", "#28A745", "PLA" to "PLA Basic"),
            Triple("Bambu Yellow", "#FFC107", "PLA" to "PLA Basic"),
            Triple("Bambu Orange", "#FF6B35", "PLA" to "PLA Basic"),
            Triple("Bambu Purple", "#6F42C1", "PLA" to "PLA Basic"),
            
            // PLA Matte Series
            Triple("Matte Ivory White", "#F8F8FF", "PLA" to "PLA Matte"),
            Triple("Matte Charcoal", "#36454F", "PLA" to "PLA Matte"),
            Triple("Matte Misty Blue", "#6F8FAF", "PLA" to "PLA Matte"),
            Triple("Matte Sakura Pink", "#FFB7C5", "PLA" to "PLA Matte"),
            
            // PLA Silk+ Series
            Triple("Silk Gold", "#FFD700", "PLA" to "PLA Silk+"),
            Triple("Silk Silver", "#C0C0C0", "PLA" to "PLA Silk+"),
            Triple("Silk Copper", "#B87333", "PLA" to "PLA Silk+"),
            Triple("Silk Pearl White", "#F8F6FF", "PLA" to "PLA Silk+"),
            
            // PLA Translucent Series
            Triple("Translucent Teal", "#008080", "PLA" to "PLA Translucent"),
            Triple("Translucent Purple", "#8B008B", "PLA" to "PLA Translucent"),
            Triple("Translucent Blue", "#4169E1", "PLA" to "PLA Translucent"),
            Triple("Translucent Green", "#32CD32", "PLA" to "PLA Translucent"),
            
            // PLA Special Series
            Triple("Galaxy Purple", "#4B0082", "PLA" to "PLA Galaxy"),
            Triple("Sparkle Silver", "#E5E5E5", "PLA" to "PLA Sparkle"),
            Triple("Marble Gray", "#808080", "PLA" to "PLA Marble"),
            Triple("Wood Brown", "#8B4513", "PLA" to "PLA Wood"),
            Triple("Glow Green", "#ADFF2F", "PLA" to "PLA Glow"),
            
            // PLA Gradient Series
            Triple("Dawn Radiance", "#FF69B4", "PLA" to "PLA Silk Multi-Color"),
            Triple("Blue Bubble", "#87CEEB", "PLA" to "PLA Basic Gradient"),
            
            // PLA Carbon Fiber
            Triple("Carbon Black", "#1C1C1C", "PLA" to "PLA-CF"),
            
            // PLA Tough+
            Triple("Tough White", "#FFFFFE", "PLA" to "PLA Tough+"),
            Triple("Tough Black", "#2B2B2B", "PLA" to "PLA Tough+"),
            
            // PETG Series
            Triple("PETG Natural", "#F5F5DC", "PETG" to "PETG Basic"),
            Triple("PETG Clear", "#E0E0E0", "PETG" to "PETG Translucent"),
            Triple("PETG White", "#FFFFFF", "PETG" to "PETG Basic"),
            Triple("PETG Black", "#2C2C2C", "PETG" to "PETG Basic"),
            Triple("PETG Red", "#DC143C", "PETG" to "PETG Basic"),
            Triple("PETG Blue", "#4682B4", "PETG" to "PETG Basic"),
            
            // PETG Carbon Fiber
            Triple("PETG Carbon Black", "#1A1A1A", "PETG" to "PETG-CF"),
            
            // ABS Series
            Triple("ABS White", "#FFFFFF", "ABS" to "ABS"),
            Triple("ABS Black", "#2C2C2C", "ABS" to "ABS"),
            Triple("ABS Red", "#B22222", "ABS" to "ABS"),
            Triple("ABS Blue", "#191970", "ABS" to "ABS"),
            
            // ASA Series
            Triple("ASA White", "#FFFFFF", "ASA" to "ASA"),
            Triple("ASA Black", "#2C2C2C", "ASA" to "ASA"),
            
            // TPU Series
            Triple("TPU Clear", "#E8E8E8", "TPU" to "TPU 95A HF"),
            Triple("TPU Black", "#2F2F2F", "TPU" to "TPU 95A"),
            Triple("TPU White", "#FFFFFF", "TPU" to "TPU 85A"),
            Triple("TPU Frozen", "#B0E0E6", "TPU" to "TPU 90A"),
            Triple("TPU Blaze", "#FF4500", "TPU" to "TPU 90A"),
            
            // Engineering Materials
            Triple("PC Natural", "#F0F8FF", "PC" to "PC"),
            Triple("PC Black", "#2F2F2F", "PC" to "PC"),
            Triple("PA Natural", "#F5F5DC", "PA" to "PA"),
            Triple("PAHT-CF Black", "#1A1A1A", "PA" to "PAHT-CF"),
            
            // Support Materials
            Triple("PVA Natural", "#FFFEF7", "PVA" to "PVA Support"),
            Triple("PLA Support White", "#FFFFFF", "PLA" to "PLA Support")
        )
    }
}