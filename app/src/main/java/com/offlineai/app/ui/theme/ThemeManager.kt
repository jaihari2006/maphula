package com.offlineai.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.Window
import androidx.annotation.ColorInt
import androidx.cardview.widget.CardView
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.offlineai.app.R
import com.offlineai.app.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class LuminanceMode(val id: String, val title: String, val icon: String) {
    DARK("dark", "Dark", "🌙"),
    SEMI_DARK("semidark", "Semi-Dark", "🌘"),
    LIGHT("light", "Light", "☀️");

    companion object {
        fun fromId(id: String?): LuminanceMode =
            values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: DARK
    }
}

enum class AestheticStyle(val id: String, val title: String, val icon: String) {
    IOS("ios", "Apple Liquid Glass", "🍎"),
    MATTE("matte", "Matte Finish", "🖤"),
    SOLID("solid", "Solid Color", "⬛"),
    GLASSY("glassy", "Glassy", "💎");

    companion object {
        fun fromId(id: String?): AestheticStyle =
            values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: IOS
    }
}

enum class AccentPalette(
    val id: String,
    val title: String,
    val emoji: String,
    @ColorInt val primaryColor: Int,
    @ColorInt val secondaryColor: Int,
    @ColorInt val auraColor: Int,
    val isBright: Boolean = false
) {
    // 9 Traditional Deity Palettes
    COSMIC_VIOLET("cosmic_violet", "Cosmic Violet", "🦚", 0xFF7C3AED.toInt(), 0xFFC084FC.toInt(), 0x337C3AED, false),
    KAILASH_CYAN("kailash_cyan", "Kailash Cyan", "🔱", 0xFF06B6D4.toInt(), 0xFF38BDF8.toInt(), 0x3306B6D4, false),
    VEDIC_SAFFRON("vedic_saffron", "Vedic Saffron", "🪷", 0xFFF59E0B.toInt(), 0xFFFBBF24.toInt(), 0x33F59E0B, false),
    LOTUS_ROSE("lotus_rose", "Lotus Rose", "🌸", 0xFFDB2777.toInt(), 0xFFF472B6.toInt(), 0x33DB2777, false),
    HARMONY_EMERALD("harmony_emerald", "Harmony Emerald", "🏔️", 0xFF059669.toInt(), 0xFF34D399.toInt(), 0x33059669, false),
    SACRED_RUBY("sacred_ruby", "Sacred Ruby", "⚡", 0xFFE11D48.toInt(), 0xFFFB7185.toInt(), 0x33E11D48, false),
    SOLAR_GOLD("solar_gold", "Solar Gold", "🦁", 0xFFD97706.toInt(), 0xFFF59E0B.toInt(), 0x33D97706, false),
    ROYAL_AZURE("royal_azure", "Royal Azure", "🛡️", 0xFF2563EB.toInt(), 0xFF60A5FA.toInt(), 0x332563EB, false),
    SHANI_TITANIUM("shani_titanium", "Shani Titanium", "⚖️", 0xFF64748B.toInt(), 0xFF94A3B8.toInt(), 0x3364748B, false),

    // 7 Bright UI Palettes (Optimized for Solid Colour & Matte Finish)
    BRIGHT_CYBER_YELLOW("bright_cyber_yellow", "Cyber Yellow", "⚡", 0xFFFFD600.toInt(), 0xFFFFF176.toInt(), 0x4DFFD600, true),
    BRIGHT_ELECTRIC_CYAN("bright_electric_cyan", "Electric Cyan", "🐬", 0xFF00E5FF.toInt(), 0xFF80F2FF.toInt(), 0x4D00E5FF, true),
    BRIGHT_HOT_MAGENTA("bright_hot_magenta", "Hot Magenta", "🌺", 0xFFFF007F.toInt(), 0xFFFF66B2.toInt(), 0x4DFF007F, true),
    BRIGHT_RADIANT_LIME("bright_radiant_lime", "Radiant Lime", "🍏", 0xFF00E676.toInt(), 0xFF69F0AE.toInt(), 0x4D00E676, true),
    BRIGHT_BLAZING_ORANGE("bright_blazing_orange", "Blazing Orange", "🔥", 0xFFFF6D00.toInt(), 0xFFFFA040.toInt(), 0x4DFF6D00, true),
    BRIGHT_ELECTRIC_BLUE("bright_electric_blue", "Electric Blue", "🔷", 0xFF0066FF.toInt(), 0xFF66A3FF.toInt(), 0x4D0066FF, true),
    BRIGHT_ULTRA_VIOLET("bright_ultra_violet", "Ultra Violet", "💜", 0xFFA855F7.toInt(), 0xFFD8B4FE.toInt(), 0x4DA855F7, true);

    companion object {
        fun deityPalettes(): List<AccentPalette> = values().filter { !it.isBright }
        fun brightPalettes(): List<AccentPalette> = values().filter { it.isBright }
        fun fromId(id: String?): AccentPalette =
            values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: COSMIC_VIOLET
    }
}

data class ResolvedTheme(
    val style: AestheticStyle,
    val mode: LuminanceMode,
    val palette: AccentPalette = AccentPalette.COSMIC_VIOLET,
    val title: String,
    val icon: String,
    val isLight: Boolean,
    val isGlassy: Boolean,
    @ColorInt val bgColor: Int,
    @ColorInt val surfaceColor: Int,
    @ColorInt val surfaceVariant: Int,
    @ColorInt val cardColor: Int,
    @ColorInt val cardStrokeColor: Int,
    val cardStrokeWidthDp: Float,
    @ColorInt val accentColor: Int,
    @ColorInt val accentSecondary: Int,
    @ColorInt val auraGlow: Int,
    @ColorInt val textPrimary: Int,
    @ColorInt val textSecondary: Int,
    @ColorInt val textMuted: Int,
    @ColorInt val chatUserBg: Int,
    @ColorInt val chatAiBg: Int,
    @ColorInt val chatAiStroke: Int,
    @ColorInt val chipBg: Int,
    @ColorInt val chipStroke: Int,
    val cornerRadiusDp: Float
)

object ThemeManager {

    private const val PREFS_NAME = "maphula_theme_prefs"
    private const val KEY_STYLE = "selected_style"
    private const val KEY_MODE = "selected_luminance_mode"
    private const val KEY_PALETTE = "selected_accent_palette"

    private var currentStyle = AestheticStyle.IOS
    private var currentMode = LuminanceMode.DARK
    private var currentPalette = AccentPalette.COSMIC_VIOLET

    private val _currentTheme = MutableStateFlow(resolveTheme(AestheticStyle.IOS, LuminanceMode.DARK, AccentPalette.COSMIC_VIOLET))
    val currentTheme: StateFlow<ResolvedTheme> = _currentTheme.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentStyle = AestheticStyle.fromId(prefs.getString(KEY_STYLE, AestheticStyle.IOS.id))
        currentMode = LuminanceMode.fromId(prefs.getString(KEY_MODE, LuminanceMode.DARK.id))
        currentPalette = AccentPalette.fromId(prefs.getString(KEY_PALETTE, AccentPalette.COSMIC_VIOLET.id))
        _currentTheme.value = resolveTheme(currentStyle, currentMode, currentPalette)
    }

    fun getActiveStyle(): AestheticStyle = currentStyle
    fun getActiveMode(): LuminanceMode = currentMode
    fun getActivePalette(): AccentPalette = currentPalette

    fun setStyle(context: Context, style: AestheticStyle) {
        currentStyle = style
        _currentTheme.value = resolveTheme(currentStyle, currentMode, currentPalette)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_STYLE, style.id).apply()
    }

    fun setMode(context: Context, mode: LuminanceMode) {
        currentMode = mode
        _currentTheme.value = resolveTheme(currentStyle, currentMode, currentPalette)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MODE, mode.id).apply()
    }

    fun setAccentPalette(context: Context, palette: AccentPalette) {
        currentPalette = palette
        _currentTheme.value = resolveTheme(currentStyle, currentMode, currentPalette)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PALETTE, palette.id).apply()
    }

    fun setTheme(context: Context, style: AestheticStyle, mode: LuminanceMode, palette: AccentPalette = currentPalette) {
        currentStyle = style
        currentMode = mode
        currentPalette = palette
        _currentTheme.value = resolveTheme(currentStyle, currentMode, currentPalette)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_STYLE, style.id)
            .putString(KEY_MODE, mode.id)
            .putString(KEY_PALETTE, palette.id)
            .apply()
    }

    fun resolveTheme(
        style: AestheticStyle,
        mode: LuminanceMode,
        palette: AccentPalette = currentPalette
    ): ResolvedTheme {
        val title = "${style.title} (${mode.title})"
        val icon = "${style.icon} ${mode.icon}"

        val accentPrim = palette.primaryColor
        val accentSec = palette.secondaryColor
        val aura = palette.auraColor

        return when (mode) {
            // ================= DARK MODE =================
            LuminanceMode.DARK -> when (style) {
                AestheticStyle.IOS -> ResolvedTheme(
                    style = style,
                    mode = mode,
                    palette = palette,
                    title = "Apple Liquid Glass (Dark)",
                    icon = "🍎 🌙",
                    isLight = false,
                    isGlassy = true,
                    bgColor = Color.parseColor("#070913"), // Deep obsidian liquid space
                    surfaceColor = Color.parseColor("#E60E1322"), // Translucent Liquid Glass Surface
                    surfaceVariant = Color.parseColor("#801D273E"), // Frosted glass variant
                    cardColor = Color.parseColor("#33182236"), // Translucent liquid glass card
                    cardStrokeColor = Color.parseColor("#38FFFFFF"), // Specular luminous glass rim
                    cardStrokeWidthDp = 1.0f,
                    accentColor = palette.primaryColor,
                    accentSecondary = palette.secondaryColor,
                    auraGlow = palette.auraColor,
                    textPrimary = Color.parseColor("#FFFFFF"),
                    textSecondary = Color.parseColor("#9DB0C6"),
                    textMuted = Color.parseColor("#64748B"),
                    chatUserBg = Color.parseColor("#0A84FF"), // Apple Liquid Azure Blue
                    chatAiBg = Color.parseColor("#331E283C"), // Translucent incoming liquid bubble
                    chatAiStroke = Color.parseColor("#38FFFFFF"), // Specular rim on incoming bubble
                    chipBg = Color.parseColor("#33243048"), // Liquid glass chip
                    chipStroke = Color.parseColor("#2EFFFFFF"), // Specular chip rim
                    cornerRadiusDp = 18f
                )
                AestheticStyle.MATTE -> ResolvedTheme(
                    style = style,
                    mode = mode,
                    palette = palette,
                    title = title,
                    icon = icon,
                    isLight = false,
                    isGlassy = false,
                    bgColor = Color.parseColor("#121318"),
                    surfaceColor = Color.parseColor("#1A1C24"),
                    surfaceVariant = Color.parseColor("#252834"),
                    cardColor = Color.parseColor("#1E202B"),
                    cardStrokeColor = accentSec, // Velvety satin matte micro-border tinted with bright accent
                    cardStrokeWidthDp = 1.2f,
                    accentColor = accentPrim,
                    accentSecondary = accentSec,
                    auraGlow = aura,
                    textPrimary = Color.parseColor("#F1F5F9"),
                    textSecondary = Color.parseColor("#94A3B8"),
                    textMuted = Color.parseColor("#64748B"),
                    chatUserBg = accentPrim,
                    chatAiBg = Color.parseColor("#1E202B"),
                    chatAiStroke = accentSec,
                    chipBg = Color.parseColor("#242733"),
                    chipStroke = accentSec,
                    cornerRadiusDp = 14f
                )
                AestheticStyle.SOLID -> ResolvedTheme(
                    style = style,
                    mode = mode,
                    palette = palette,
                    title = title,
                    icon = icon,
                    isLight = false,
                    isGlassy = false,
                    bgColor = Color.parseColor("#0A0A0D"), // Pure jet onyx
                    surfaceColor = Color.parseColor("#14141A"),
                    surfaceVariant = Color.parseColor("#1F1F28"),
                    cardColor = Color.parseColor("#181822"),
                    cardStrokeColor = accentPrim, // Bold solid bright color outline!
                    cardStrokeWidthDp = 2f,
                    accentColor = accentPrim,
                    accentSecondary = accentSec,
                    auraGlow = aura,
                    textPrimary = Color.parseColor("#FFFFFF"),
                    textSecondary = Color.parseColor("#CBD5E1"),
                    textMuted = Color.parseColor("#94A3B8"),
                    chatUserBg = accentPrim,
                    chatAiBg = Color.parseColor("#181822"),
                    chatAiStroke = accentSec,
                    chipBg = Color.parseColor("#1F1F28"),
                    chipStroke = accentPrim,
                    cornerRadiusDp = 8f
                )
                AestheticStyle.GLASSY -> ResolvedTheme(
                    style = style,
                    mode = mode,
                    palette = palette,
                    title = title,
                    icon = icon,
                    isLight = false,
                    isGlassy = true,
                    bgColor = Color.parseColor("#0A0D1A"), // Cosmic midnight void
                    surfaceColor = Color.parseColor("#D912182D"), // Translucent dark acrylic
                    surfaceVariant = Color.parseColor("#801A233F"),
                    cardColor = Color.parseColor("#5916203B"), // Frosted glass
                    cardStrokeColor = Color.argb(0x80, Color.red(accentSec), Color.green(accentSec), Color.blue(accentSec)),
                    cardStrokeWidthDp = 1.8f,
                    accentColor = accentPrim,
                    accentSecondary = accentSec,
                    auraGlow = aura,
                    textPrimary = Color.parseColor("#F8FAFC"),
                    textSecondary = Color.parseColor("#CBD5E1"),
                    textMuted = Color.parseColor("#94A3B8"),
                    chatUserBg = Color.argb(0x99, Color.red(accentPrim), Color.green(accentPrim), Color.blue(accentPrim)),
                    chatAiBg = Color.parseColor("#4D16203B"),
                    chatAiStroke = Color.argb(0x66, Color.red(accentSec), Color.green(accentSec), Color.blue(accentSec)),
                    chipBg = Color.parseColor("#4D1A233F"),
                    chipStroke = Color.argb(0x80, Color.red(accentSec), Color.green(accentSec), Color.blue(accentSec)),
                    cornerRadiusDp = 20f
                )
            }

            // ================= SEMI-DARK MODE =================
            LuminanceMode.SEMI_DARK -> when (style) {
                AestheticStyle.IOS -> ResolvedTheme(
                    style = style,
                    mode = mode,
                    palette = palette,
                    title = "Apple Liquid Glass (Semi-Dark)",
                    icon = "🍎 🌘",
                    isLight = false,
                    isGlassy = true,
                    bgColor = Color.parseColor("#0F1422"),
                    surfaceColor = Color.parseColor("#E6161F32"),
                    surfaceVariant = Color.parseColor("#8025324E"),
                    cardColor = Color.parseColor("#4D202C44"),
                    cardStrokeColor = Color.parseColor("#38FFFFFF"),
                    cardStrokeWidthDp = 1.0f,
                    accentColor = palette.primaryColor,
                    accentSecondary = palette.secondaryColor,
                    auraGlow = palette.auraColor,
                    textPrimary = Color.parseColor("#F8FAFC"),
                    textSecondary = Color.parseColor("#CBD5E1"),
                    textMuted = Color.parseColor("#8392A5"),
                    chatUserBg = Color.parseColor("#0A84FF"),
                    chatAiBg = Color.parseColor("#4D222E48"),
                    chatAiStroke = Color.parseColor("#38FFFFFF"),
                    chipBg = Color.parseColor("#40283754"),
                    chipStroke = Color.parseColor("#2EFFFFFF"),
                    cornerRadiusDp = 18f
                )
                AestheticStyle.MATTE -> ResolvedTheme(
                    style = style,
                    mode = mode,
                    palette = palette,
                    title = title,
                    icon = icon,
                    isLight = false,
                    isGlassy = false,
                    bgColor = Color.parseColor("#1E2028"), // Twilight slate
                    surfaceColor = Color.parseColor("#272A35"),
                    surfaceVariant = Color.parseColor("#333745"),
                    cardColor = Color.parseColor("#2B2E3A"),
                    cardStrokeColor = accentSec, // Velvety satin matte stroke
                    cardStrokeWidthDp = 1.2f,
                    accentColor = accentPrim,
                    accentSecondary = accentSec,
                    auraGlow = aura,
                    textPrimary = Color.parseColor("#F1F5F9"),
                    textSecondary = Color.parseColor("#A0AEC0"),
                    textMuted = Color.parseColor("#718096"),
                    chatUserBg = accentPrim,
                    chatAiBg = Color.parseColor("#2B2E3A"),
                    chatAiStroke = accentSec,
                    chipBg = Color.parseColor("#333745"),
                    chipStroke = accentSec,
                    cornerRadiusDp = 14f
                )
                AestheticStyle.SOLID -> ResolvedTheme(
                    style = style,
                    mode = mode,
                    palette = palette,
                    title = title,
                    icon = icon,
                    isLight = false,
                    isGlassy = false,
                    bgColor = Color.parseColor("#181B24"), // Slate midnight
                    surfaceColor = Color.parseColor("#202430"),
                    surfaceVariant = Color.parseColor("#2B3040"),
                    cardColor = Color.parseColor("#242836"),
                    cardStrokeColor = accentPrim, // Bold solid bright outline!
                    cardStrokeWidthDp = 2f,
                    accentColor = accentPrim,
                    accentSecondary = accentSec,
                    auraGlow = aura,
                    textPrimary = Color.parseColor("#FFFFFF"),
                    textSecondary = Color.parseColor("#CBD5E1"),
                    textMuted = Color.parseColor("#94A3B8"),
                    chatUserBg = accentPrim,
                    chatAiBg = Color.parseColor("#242836"),
                    chatAiStroke = accentSec,
                    chipBg = Color.parseColor("#2B3040"),
                    chipStroke = accentPrim,
                    cornerRadiusDp = 8f
                )
                AestheticStyle.GLASSY -> ResolvedTheme(
                    style = style,
                    mode = mode,
                    palette = palette,
                    title = title,
                    icon = icon,
                    isLight = false,
                    isGlassy = true,
                    bgColor = Color.parseColor("#141829"), // Twilight cosmic blue
                    surfaceColor = Color.parseColor("#D920263D"),
                    surfaceVariant = Color.parseColor("#802B3352"),
                    cardColor = Color.parseColor("#66242D47"),
                    cardStrokeColor = Color.argb(0x80, Color.red(accentSec), Color.green(accentSec), Color.blue(accentSec)),
                    cardStrokeWidthDp = 1.8f,
                    accentColor = accentPrim,
                    accentSecondary = accentSec,
                    auraGlow = aura,
                    textPrimary = Color.parseColor("#F8FAFC"),
                    textSecondary = Color.parseColor("#CBD5E1"),
                    textMuted = Color.parseColor("#94A3B8"),
                    chatUserBg = Color.argb(0x99, Color.red(accentPrim), Color.green(accentPrim), Color.blue(accentPrim)),
                    chatAiBg = Color.parseColor("#59242D47"),
                    chatAiStroke = Color.argb(0x66, Color.red(accentSec), Color.green(accentSec), Color.blue(accentSec)),
                    chipBg = Color.parseColor("#4D2B3352"),
                    chipStroke = Color.argb(0x80, Color.red(accentSec), Color.green(accentSec), Color.blue(accentSec)),
                    cornerRadiusDp = 20f
                )
            }

            // ================= LIGHT MODE =================
            LuminanceMode.LIGHT -> when (style) {
                AestheticStyle.IOS -> ResolvedTheme(
                    style = style,
                    mode = mode,
                    palette = palette,
                    title = "Apple Liquid Glass (Light)",
                    icon = "🍎 ☀️",
                    isLight = true,
                    isGlassy = true,
                    bgColor = Color.parseColor("#F0F4FA"), // Apple Daylight Liquid Glass
                    surfaceColor = Color.parseColor("#E6FFFFFF"), // Translucent Pearl Liquid Glass
                    surfaceVariant = Color.parseColor("#B3E2EDF8"),
                    cardColor = Color.parseColor("#B8FFFFFF"), // Frosted glass card
                    cardStrokeColor = Color.parseColor("#66FFFFFF"), // Specular light rim
                    cardStrokeWidthDp = 1.0f,
                    accentColor = palette.primaryColor,
                    accentSecondary = palette.secondaryColor,
                    auraGlow = palette.auraColor,
                    textPrimary = Color.parseColor("#0A101D"),
                    textSecondary = Color.parseColor("#4A5568"),
                    textMuted = Color.parseColor("#7A8B9E"),
                    chatUserBg = Color.parseColor("#007AFF"),
                    chatAiBg = Color.parseColor("#E0FFFFFF"),
                    chatAiStroke = Color.parseColor("#60FFFFFF"),
                    chipBg = Color.parseColor("#CCEDF4FC"),
                    chipStroke = Color.parseColor("#60FFFFFF"),
                    cornerRadiusDp = 18f
                )
                AestheticStyle.MATTE -> ResolvedTheme(
                    style = style,
                    mode = mode,
                    palette = palette,
                    title = title,
                    icon = icon,
                    isLight = true,
                    isGlassy = false,
                    bgColor = Color.parseColor("#F3F4F6"), // Warm porcelain gray
                    surfaceColor = Color.parseColor("#FFFFFF"),
                    surfaceVariant = Color.parseColor("#E5E7EB"),
                    cardColor = Color.parseColor("#FFFFFF"),
                    cardStrokeColor = accentSec, // Velvety satin matte stroke!
                    cardStrokeWidthDp = 1.2f,
                    accentColor = accentPrim,
                    accentSecondary = accentSec,
                    auraGlow = aura,
                    textPrimary = Color.parseColor("#0F172A"),
                    textSecondary = Color.parseColor("#475569"),
                    textMuted = Color.parseColor("#64748B"),
                    chatUserBg = accentPrim,
                    chatAiBg = Color.parseColor("#FFFFFF"),
                    chatAiStroke = accentSec,
                    chipBg = Color.parseColor("#E2E8F0"),
                    chipStroke = accentSec,
                    cornerRadiusDp = 14f
                )
                AestheticStyle.SOLID -> ResolvedTheme(
                    style = style,
                    mode = mode,
                    palette = palette,
                    title = title,
                    icon = icon,
                    isLight = true,
                    isGlassy = false,
                    bgColor = Color.parseColor("#FFFFFF"), // Pure solid white
                    surfaceColor = Color.parseColor("#F8FAFC"),
                    surfaceVariant = Color.parseColor("#F1F5F9"),
                    cardColor = Color.parseColor("#FFFFFF"),
                    cardStrokeColor = accentPrim, // Bold solid bright outline!
                    cardStrokeWidthDp = 2.2f,
                    accentColor = accentPrim,
                    accentSecondary = accentSec,
                    auraGlow = aura,
                    textPrimary = Color.parseColor("#000000"),
                    textSecondary = Color.parseColor("#374151"),
                    textMuted = Color.parseColor("#6B7280"),
                    chatUserBg = accentPrim,
                    chatAiBg = Color.parseColor("#F8FAFC"),
                    chatAiStroke = accentSec,
                    chipBg = Color.parseColor("#F1F5F9"),
                    chipStroke = accentPrim,
                    cornerRadiusDp = 8f
                )
                AestheticStyle.GLASSY -> ResolvedTheme(
                    style = style,
                    mode = mode,
                    palette = palette,
                    title = title,
                    icon = icon,
                    isLight = true,
                    isGlassy = true,
                    bgColor = Color.parseColor("#EBF3FC"), // Frosted daylight sky
                    surfaceColor = Color.parseColor("#E6FFFFFF"), // Translucent pearl glass
                    surfaceVariant = Color.parseColor("#B3E2E8F0"),
                    cardColor = Color.parseColor("#B3FFFFFF"),
                    cardStrokeColor = Color.argb(0x80, Color.red(accentSec), Color.green(accentSec), Color.blue(accentSec)),
                    cardStrokeWidthDp = 1.8f,
                    accentColor = accentPrim,
                    accentSecondary = accentSec,
                    auraGlow = aura,
                    textPrimary = Color.parseColor("#0B1329"),
                    textSecondary = Color.parseColor("#334155"),
                    textMuted = Color.parseColor("#64748B"),
                    chatUserBg = Color.argb(0xB3, Color.red(accentPrim), Color.green(accentPrim), Color.blue(accentPrim)),
                    chatAiBg = Color.parseColor("#B3FFFFFF"),
                    chatAiStroke = Color.argb(0x80, Color.red(accentSec), Color.green(accentSec), Color.blue(accentSec)),
                    chipBg = Color.parseColor("#B3E0F2FE"),
                    chipStroke = Color.argb(0x80, Color.red(accentSec), Color.green(accentSec), Color.blue(accentSec)),
                    cornerRadiusDp = 20f
                )
            }
        }
    }

    // ================= DYNAMIC DRAWABLE GENERATORS =================

    fun cardBackground(context: Context, theme: ResolvedTheme = currentTheme.value): GradientDrawable {
        val density = context.resources.displayMetrics.density
        return if (theme.style == AestheticStyle.IOS) {
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                if (theme.isLight) {
                    intArrayOf(Color.parseColor("#E6FFFFFF"), Color.parseColor("#CCF0F6FC"))
                } else {
                    intArrayOf(Color.parseColor("#3D222E44"), Color.parseColor("#24141E30"))
                }
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = theme.cornerRadiusDp * density
                setStroke((1f * density).toInt(), if (theme.isLight) Color.parseColor("#80FFFFFF") else Color.parseColor("#38FFFFFF"))
            }
        } else {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = theme.cornerRadiusDp * density
                setColor(theme.cardColor)
                setStroke((theme.cardStrokeWidthDp * density).toInt(), theme.cardStrokeColor)
            }
        }
    }

    fun chipBackground(context: Context, theme: ResolvedTheme = currentTheme.value): GradientDrawable {
        val density = context.resources.displayMetrics.density
        return if (theme.style == AestheticStyle.IOS) {
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                if (theme.isLight) {
                    intArrayOf(Color.parseColor("#E0FFFFFF"), Color.parseColor("#C4E3EFFB"))
                } else {
                    intArrayOf(Color.parseColor("#3D2A3A56"), Color.parseColor("#261B253B"))
                }
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 16f * density
                setStroke((0.9f * density).toInt(), if (theme.isLight) Color.parseColor("#80FFFFFF") else Color.parseColor("#30FFFFFF"))
            }
        } else {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = if (theme.style == AestheticStyle.SOLID) 8f * density else 16f * density
                setColor(theme.chipBg)
                setStroke((theme.cardStrokeWidthDp * density).toInt(), theme.chipStroke)
            }
        }
    }

    fun inputBackground(context: Context, theme: ResolvedTheme = currentTheme.value): GradientDrawable {
        val density = context.resources.displayMetrics.density
        return if (theme.style == AestheticStyle.IOS) {
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                if (theme.isLight) {
                    intArrayOf(Color.parseColor("#F0FFFFFF"), Color.parseColor("#DCEAF7"))
                } else {
                    intArrayOf(Color.parseColor("#38233049"), Color.parseColor("#20152033"))
                }
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 22f * density
                setStroke((1f * density).toInt(), if (theme.isLight) Color.parseColor("#99FFFFFF") else Color.parseColor("#38FFFFFF"))
            }
        } else {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = if (theme.style == AestheticStyle.SOLID) 8f * density else 22f * density
                setColor(if (theme.isGlassy) theme.chipBg else theme.cardColor)
                setStroke((theme.cardStrokeWidthDp * density).toInt(), theme.cardStrokeColor)
            }
        }
    }

    fun buttonGradient(context: Context, theme: ResolvedTheme = currentTheme.value): GradientDrawable {
        val density = context.resources.displayMetrics.density
        return if (theme.style == AestheticStyle.IOS) {
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#1EA0FF"), Color.parseColor("#0062FF"))
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20f * density
                setStroke((1.2f * density).toInt(), Color.parseColor("#66FFFFFF"))
            }
        } else if (theme.style == AestheticStyle.SOLID) {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * density
                setColor(theme.accentColor)
                setStroke((1.5f * density).toInt(), theme.accentSecondary)
            }
        } else if (theme.style == AestheticStyle.MATTE) {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 14f * density
                setColor(theme.accentColor)
                setStroke((1f * density).toInt(), theme.accentSecondary)
            }
        } else {
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(theme.accentColor, theme.accentSecondary)
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = if (theme.style == AestheticStyle.SOLID) 8f * density else 20f * density
                if (theme.isGlassy) {
                    setStroke((1.5f * density).toInt(), Color.argb(0xB3, 255, 255, 255))
                }
            }
        }
    }

    fun chatUserBackground(context: Context, theme: ResolvedTheme = currentTheme.value): GradientDrawable {
        val density = context.resources.displayMetrics.density
        val r = theme.cornerRadiusDp * density
        val smallR = if (theme.style == AestheticStyle.SOLID) 3f * density else 4f * density
        // Authentic Apple iMessage bubble: smooth all around, anchored tail corner on bottom-right (user side)
        val radii = floatArrayOf(
            r, r,           // top-left
            r, r,           // top-right
            smallR, smallR, // bottom-right (anchors message tail to user edge)
            r, r            // bottom-left
        )
        return if (theme.style == AestheticStyle.IOS) {
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    Color.parseColor("#1EA0FF"), // Glowing specular top reflection
                    Color.parseColor("#0066FF")  // Deep Apple liquid blue bottom
                )
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = radii
                setStroke((1f * density).toInt(), Color.parseColor("#59FFFFFF"))
            }
        } else {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = radii
                setColor(theme.chatUserBg)
                if (theme.isGlassy) {
                    setStroke((1.5f * density).toInt(), if (theme.isLight) Color.parseColor("#818CF8") else Color.parseColor("#A78BFA"))
                } else if (theme.style == AestheticStyle.SOLID) {
                    setStroke((1.5f * density).toInt(), theme.accentSecondary)
                } else if (theme.style == AestheticStyle.MATTE) {
                    setStroke((1f * density).toInt(), theme.accentSecondary)
                }
            }
        }
    }

    fun chatAiBackground(context: Context, theme: ResolvedTheme = currentTheme.value): GradientDrawable {
        val density = context.resources.displayMetrics.density
        val r = theme.cornerRadiusDp * density
        val smallR = if (theme.style == AestheticStyle.SOLID) 3f * density else 4f * density
        // Authentic Apple iMessage bubble: smooth all around, anchored tail corner on bottom-left (AI side)
        val radii = floatArrayOf(
            r, r,           // top-left
            r, r,           // top-right
            r, r,           // bottom-right
            smallR, smallR  // bottom-left (anchors message tail to incoming edge)
        )
        return if (theme.style == AestheticStyle.IOS) {
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                if (theme.isLight) {
                    intArrayOf(Color.parseColor("#F5FFFFFF"), Color.parseColor("#D9EEF5FC"))
                } else {
                    intArrayOf(Color.parseColor("#40283652"), Color.parseColor("#26182236"))
                }
            ).apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = radii
                setStroke((1f * density).toInt(), if (theme.isLight) Color.parseColor("#80FFFFFF") else Color.parseColor("#38FFFFFF"))
            }
        } else {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadii = radii
                setColor(theme.chatAiBg)
                setStroke((theme.cardStrokeWidthDp * density).toInt(), theme.chatAiStroke)
            }
        }
    }

    fun surfaceBackground(context: Context, theme: ResolvedTheme = currentTheme.value): GradientDrawable {
        val density = context.resources.displayMetrics.density
        return if (theme.style == AestheticStyle.IOS) {
            GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                if (theme.isLight) {
                    intArrayOf(Color.parseColor("#F5FFFFFF"), Color.parseColor("#E6EDF5FD"))
                } else {
                    intArrayOf(Color.parseColor("#E6161D2B"), Color.parseColor("#D90E131E"))
                }
            ).apply {
                shape = GradientDrawable.RECTANGLE
                setStroke((0.8f * density).toInt(), if (theme.isLight) Color.parseColor("#4DFFFFFF") else Color.parseColor("#26FFFFFF"))
            }
        } else {
            GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                setColor(theme.surfaceColor)
                if (theme.isGlassy) {
                    setStroke((1.5f * density).toInt(), theme.cardStrokeColor)
                }
            }
        }
    }

    // ================= VIEW STYLING HELPERS =================

    fun setupSmoothBottomSheet(dialog: BottomSheetDialog) {
        dialog.window?.attributes?.windowAnimations = R.style.Animation_OfflineAI_BottomSheet
    }

    fun applyThemeToMain(binding: ActivityMainBinding, window: Window, theme: ResolvedTheme = currentTheme.value) {
        val context = binding.root.context

        // Window status bar & navigation bar + light/dark icon controller
        window.statusBarColor = theme.bgColor
        window.navigationBarColor = theme.surfaceColor

        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = theme.isLight
        insetsController.isAppearanceLightNavigationBars = theme.isLight

        // Root background
        binding.root.setBackgroundColor(theme.bgColor)

        // Apple iOS Status Bar & Dynamic Island
        binding.layoutIosStatusBar.background = surfaceBackground(context, theme)
        binding.tvIosStatusTime.setTextColor(theme.textPrimary)
        binding.tvIosSignal.setTextColor(theme.textSecondary)
        binding.tvIosBattery.setTextColor(theme.textPrimary)

        // Apple iPhone Home Indicator Gesture Bar
        binding.layoutHomeIndicatorContainer.background = surfaceBackground(context, theme)
        binding.viewBottomNavDivider.setBackgroundColor(theme.cardStrokeColor)

        // Top Header
        binding.layoutTopHeader.background = surfaceBackground(context, theme)
        binding.tvAppTitle.setTextColor(theme.textPrimary)
        binding.tvDeityAuraSubtitle.setTextColor(theme.accentColor)
        binding.scrollQuickDeck.background = surfaceBackground(context, theme)

        // Calendar Tab (Top Left Corner)
        binding.layoutCalendarTab.background = chipBackground(context, theme)
        binding.tvCalendarYearTab.setTextColor(theme.textPrimary)

        // Scriptures & Voice Assistant Header Tabs
        binding.btnScripturesTab.background = chipBackground(context, theme)
        binding.btnVoiceAssistantTab.background = chipBackground(context, theme)
        binding.btnSkillsTab.background = chipBackground(context, theme)
        binding.tvSkillsTabTitle.setTextColor(theme.accentColor)
        binding.navSkillsDrawer.background = surfaceBackground(context, theme)

        // Karma & Streak Badge
        binding.layoutKarmaBadge.background = chipBackground(context, theme)

        // Theme Switcher Badge
        binding.layoutThemeBadge.background = chipBackground(context, theme)
        binding.tvThemeIcon.text = "${theme.palette.emoji} ${theme.mode.icon}"
        binding.tvThemeName.text = "${theme.palette.title.split(" ").first()} • ${theme.mode.title}"
        binding.tvThemeName.setTextColor(theme.textPrimary)

        // Network Status Badge
        binding.layoutNetworkBadge.background = chipBackground(context, theme)

        // Settings Button
        binding.btnSettings.background = chipBackground(context, theme)
        binding.btnSettings.setColorFilter(theme.textPrimary)

        // Bottom Navigation Bar
        if (theme.style == AestheticStyle.IOS) {
            binding.bottomNav.background = surfaceBackground(context, theme)
            binding.viewBottomNavDivider.setBackgroundColor(if (theme.isLight) Color.parseColor("#4DFFFFFF") else Color.parseColor("#26FFFFFF"))
        } else {
            binding.bottomNav.setBackgroundColor(theme.surfaceColor)
            binding.viewBottomNavDivider.setBackgroundColor(theme.cardStrokeColor)
        }
        val navStates = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val navColors = intArrayOf(
            theme.accentColor,
            theme.textMuted
        )
        val colorStateList = ColorStateList(navStates, navColors)
        binding.bottomNav.itemIconTintList = colorStateList
        binding.bottomNav.itemTextColor = colorStateList
    }

    fun applyCardStyle(card: CardView, theme: ResolvedTheme = currentTheme.value) {
        val density = card.context.resources.displayMetrics.density
        card.setCardBackgroundColor(theme.cardColor)
        card.radius = theme.cornerRadiusDp * density
        card.cardElevation = when (theme.style) {
            AestheticStyle.IOS -> 4f * density
            AestheticStyle.SOLID -> 0f
            AestheticStyle.GLASSY -> 6f * density
            AestheticStyle.MATTE -> 2f * density
        }
    }
}
