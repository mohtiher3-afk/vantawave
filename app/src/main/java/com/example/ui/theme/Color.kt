package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Studio Matte Obsidian & Slate Foundation
val VantaBackground = Color(0xFF0C0D12)       // Deep Matte Obsidian
val VantaSurface = Color(0xFF14161F)          // Matte Studio Surface 1
val VantaSurfaceVariant = Color(0xFF1B1E2B)   // Matte Studio Surface 2
val VantaSurfaceElevated = Color(0xFF232738)  // Matte Studio Card / Raised
val VantaGlassSurface = Color(0x40161824)     // Translucent Matte Glass

// Studio Precision Accent Palette
val VantaPrimary = Color(0xFF8B5CF6)          // Precision Electric Violet
val VantaSecondary = Color(0xFF00D2EE)        // Studio High-Res Cyan
val VantaTertiary = Color(0xFFFF9500)         // Warm Studio Amber / Cue

// High-Contrast Refined Typography Colors
val VantaTextPrimary = Color(0xFFF9FAFB)      // High-contrast clean white
val VantaTextSecondary = Color(0xFFA1A7B8)    // Soft studio silver
val VantaTextMuted = Color(0xFF6B7280)        // Neutral muted gray

// Hairline Borders & Restrained Ambient Glow
val VantaCardBorder = Color(0x40374151)       // Hairline slate border
val VantaGlow = Color(0x338B5CF6)             // Soft focused violet glow

// Accent Presets
val VantaAccent = Color(0xFF007AFF) // Placeholder for User's Hex
val AccentNeonCyan = Color(0xFF00D2EE)
val AccentElectricLime = Color(0xFF10B981)    // Studio Emerald Mint
val AccentSunsetOrange = Color(0xFFFF9500)    // Warm Amber

val NeonPurpleGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF8B5CF6), Color(0xFF00D2EE))
)

val DarkVoidGradient = Brush.verticalGradient(
    colors = listOf(Color(0xFF14161F), Color(0xFF0C0D12))
)
