package com.bscan.ui.components

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.*

/**
 * Triangle shape for ABS materials
 */
class TriangleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val centerX = size.width / 2f
            val radius = minOf(size.width, size.height) / 2f
            
            // Create equilateral triangle pointing up
            moveTo(centerX, size.height / 2f - radius)
            lineTo(centerX - radius * 0.866f, size.height / 2f + radius * 0.5f)
            lineTo(centerX + radius * 0.866f, size.height / 2f + radius * 0.5f)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Inverted triangle shape for ASA materials
 */
class InvertedTriangleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val centerX = size.width / 2f
            val radius = minOf(size.width, size.height) / 2f
            
            // Create equilateral triangle pointing down
            moveTo(centerX, size.height / 2f + radius)
            lineTo(centerX - radius * 0.866f, size.height / 2f - radius * 0.5f)
            lineTo(centerX + radius * 0.866f, size.height / 2f - radius * 0.5f)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Hexagon shape for PETG materials
 */
class HexagonShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = minOf(size.width, size.height) / 2f
            
            // Create regular hexagon
            for (i in 0..5) {
                val angle = PI / 3 * i
                val x = centerX + radius * cos(angle).toFloat()
                val y = centerY + radius * sin(angle).toFloat()
                if (i == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Octagon shape for PC (Polycarbonate) materials
 */
class OctagonShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = minOf(size.width, size.height) / 2f
            
            // Create regular octagon
            for (i in 0..7) {
                val angle = PI / 4 * i
                val x = centerX + radius * cos(angle).toFloat()
                val y = centerY + radius * sin(angle).toFloat()
                if (i == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Diamond/Rhombus shape for PA/Nylon materials
 */
class DiamondShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = minOf(size.width, size.height) / 2f
            
            // Create diamond (rotated square)
            moveTo(centerX, centerY - radius)
            lineTo(centerX + radius * 0.7f, centerY)
            lineTo(centerX, centerY + radius)
            lineTo(centerX - radius * 0.7f, centerY)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Teardrop shape for PVA water-soluble materials
 */
class TeardropShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = minOf(size.width, size.height) / 2f
            
            // Create teardrop with pointed top and round bottom
            moveTo(centerX, centerY - radius)
            
            // Right curve
            cubicTo(
                centerX + radius * 0.5f, centerY - radius * 0.5f,
                centerX + radius, centerY + radius * 0.2f,
                centerX, centerY + radius
            )
            
            // Left curve
            cubicTo(
                centerX - radius, centerY + radius * 0.2f,
                centerX - radius * 0.5f, centerY - radius * 0.5f,
                centerX, centerY - radius
            )
            
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Vertical lines pattern shape for Support materials
 */
class StripedShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        // For striped pattern, we'll use a circular outline
        // The stripes will be drawn as a pattern overlay
        val path = Path().apply {
            addOval(Rect(Offset.Zero, size))
        }
        return Outline.Generic(path)
    }
}

/**
 * Dodecagon (12-sided) shape for Unknown materials
 */
class DodecagonShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val radius = minOf(size.width, size.height) / 2f
            
            // Create regular dodecagon (12 sides)
            for (i in 0..11) {
                val angle = PI / 6 * i
                val x = centerX + radius * cos(angle).toFloat()
                val y = centerY + radius * sin(angle).toFloat()
                if (i == 0) {
                    moveTo(x, y)
                } else {
                    lineTo(x, y)
                }
            }
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Get the appropriate shape for a material type
 */
fun getMaterialShape(materialType: MaterialType): Shape {
    return when (materialType) {
        MaterialType.PLA -> CircleShape
        MaterialType.ABS -> TriangleShape()
        MaterialType.ASA -> InvertedTriangleShape()
        MaterialType.PETG -> HexagonShape()
        MaterialType.TPU -> RoundedCornerShape(35) // Very rounded square
        MaterialType.PC -> OctagonShape()
        MaterialType.PA -> DiamondShape()
        MaterialType.PVA -> TeardropShape()
        MaterialType.SUPPORT -> StripedShape()
        MaterialType.UNKNOWN -> DodecagonShape()
    }
}