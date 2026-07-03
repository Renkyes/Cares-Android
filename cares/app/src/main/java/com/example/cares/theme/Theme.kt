// file: ui/theme/Theme.kt
package com.example.cares.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Tema principale dell'applicazione Cares.
 *
 * Supporta:
 * - **Temi dinamici:** 7 colori primari tra cui scegliere (VERDE, BLU, VIOLA, ARANCIONE, ROSA, ROSSO, TEAL).
 * - **Modalità scura/chiara:** adattamento automatico basato sulle impostazioni di sistema.
 * - **Barra di stato e navigazione:** trasparenti con icone adattate al tema.
 *
 * @param darkTheme Se `true`, applica il tema scuro. Default: `isSystemInDarkTheme()`.
 * @param theme Il tema colore selezionato ([AppTheme]). Default: [AppTheme.GREEN].
 * @param content Contenuto dell'app da avvolgere con il tema.
 *
 * @see AppTheme per i temi disponibili.
 * @see getThemeColors per la mappatura dei colori.
 */
@Composable
fun CaresTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    theme: AppTheme = AppTheme.GREEN,
    content: @Composable () -> Unit
) {
    // Ottiene i colori del tema selezionato in base alla modalità (chiara/scura)
    val colors = getThemeColors(theme, darkTheme)

    // Costruisce il color scheme di Material Design 3
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryLight,
            onPrimaryContainer = colors.primaryDark,
            secondary = colors.secondary,
            onSecondary = colors.onSecondary,
            secondaryContainer = colors.secondaryLight,
            onSecondaryContainer = colors.secondaryDark,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surface,
            onSurface = colors.onSurface,
            error = colors.error,
            onError = Color.White
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            onPrimary = colors.onPrimary,
            primaryContainer = colors.primaryLight,
            onPrimaryContainer = colors.primaryDark,
            secondary = colors.secondary,
            onSecondary = colors.onSecondary,
            secondaryContainer = colors.secondaryLight,
            onSecondaryContainer = colors.secondaryDark,
            background = colors.background,
            onBackground = colors.onBackground,
            surface = colors.surface,
            onSurface = colors.onSurface,
            error = colors.error,
            onError = Color.White
        )
    }

    // Configurazione della barra di stato e navigazione
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Imposta le barre come trasparenti
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            // Adatta il colore delle icone delle barre al tema
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    // Applica il tema Material Design 3
    MaterialTheme(
        colorScheme = colorScheme,
        typography = GlassTypography,
        shapes = Shapes,
        content = content
    )
}

// ================================ HELPER PER I COLORI DEL TEMA ================================

/**
 * Container per i colori di un tema, sia in modalità chiara che scura.
 *
 * I valori vengono estratti dagli oggetti tema definiti in [Color.kt]
 * (es. [GreenTheme], [BlueTheme], ecc.) e organizzati in un'unica struttura
 * per facilitare la costruzione del [androidx.compose.material3.ColorScheme].
 *
 * @property primary Colore primario (brand).
 * @property primaryDark Variante scura del colore primario.
 * @property primaryLight Variante chiara del colore primario.
 * @property secondary Colore secondario per elementi di supporto.
 * @property secondaryDark Variante scura del colore secondario.
 * @property secondaryLight Variante chiara del colore secondario.
 * @property background Colore di sfondo delle schermate.
 * @property surface Colore delle superfici (cards, dialoghi, ecc.).
 * @property error Colore per messaggi di errore e stati critici.
 * @property onPrimary Colore per testo e icone su sfondi primari.
 * @property onSecondary Colore per testo e icone su sfondi secondari.
 * @property onBackground Colore per testo su sfondo.
 * @property onSurface Colore per testo su superfici.
 */
data class ThemeColors(
    val primary: Color,
    val primaryDark: Color,
    val primaryLight: Color,
    val secondary: Color,
    val secondaryDark: Color,
    val secondaryLight: Color,
    val background: Color,
    val surface: Color,
    val error: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onBackground: Color,
    val onSurface: Color
)

/**
 * Restituisce i colori del tema specificato, in base alla modalità scura/chiara.
 *
 * Mappa l'enum [AppTheme] ai colori definiti negli oggetti tema corrispondenti
 * ([GreenTheme], [BlueTheme], ecc.), selezionando la variante chiara o scura
 * in base al parametro `dark`.
 *
 * **Temi disponibili:**
 * - [AppTheme.GREEN]: Tema verde (default).
 * - [AppTheme.BLUE]: Tema blu.
 * - [AppTheme.PURPLE]: Tema viola.
 * - [AppTheme.ORANGE]: Tema arancione.
 * - [AppTheme.PINK]: Tema rosa.
 * - [AppTheme.RED]: Tema rosso.
 * - [AppTheme.TEAL]: Tema verde acqua.
 *
 * @param theme Il tema selezionato dall'utente ([AppTheme]).
 * @param dark Se `true`, restituisce la variante scura del tema, altrimenti la chiara.
 * @return Un oggetto [ThemeColors] contenente tutti i colori del tema.
 */
fun getThemeColors(theme: AppTheme, dark: Boolean): ThemeColors {
    return when (theme) {
        AppTheme.GREEN -> if (dark) {
            ThemeColors(
                primary = GreenTheme.DarkPrimary,
                primaryDark = GreenTheme.DarkPrimaryDark,
                primaryLight = GreenTheme.PrimaryLight,
                secondary = GreenTheme.Secondary,
                secondaryDark = GreenTheme.SecondaryDark,
                secondaryLight = GreenTheme.SecondaryLight,
                background = GreenTheme.DarkBackground,
                surface = GreenTheme.DarkSurface,
                error = GreenTheme.Error,
                onPrimary = GreenTheme.DarkOnPrimary,
                onSecondary = GreenTheme.OnSecondary,
                onBackground = GreenTheme.DarkOnBackground,
                onSurface = GreenTheme.DarkOnSurface
            )
        } else {
            ThemeColors(
                primary = GreenTheme.Primary,
                primaryDark = GreenTheme.PrimaryDark,
                primaryLight = GreenTheme.PrimaryLight,
                secondary = GreenTheme.Secondary,
                secondaryDark = GreenTheme.SecondaryDark,
                secondaryLight = GreenTheme.SecondaryLight,
                background = GreenTheme.Background,
                surface = GreenTheme.Surface,
                error = GreenTheme.Error,
                onPrimary = GreenTheme.OnPrimary,
                onSecondary = GreenTheme.OnSecondary,
                onBackground = GreenTheme.OnBackground,
                onSurface = GreenTheme.OnSurface
            )
        }

        AppTheme.BLUE -> if (dark) {
            ThemeColors(
                primary = BlueTheme.DarkPrimary,
                primaryDark = BlueTheme.DarkPrimaryDark,
                primaryLight = BlueTheme.PrimaryLight,
                secondary = BlueTheme.Secondary,
                secondaryDark = BlueTheme.SecondaryDark,
                secondaryLight = BlueTheme.SecondaryLight,
                background = BlueTheme.DarkBackground,
                surface = BlueTheme.DarkSurface,
                error = BlueTheme.Error,
                onPrimary = BlueTheme.DarkOnPrimary,
                onSecondary = BlueTheme.OnSecondary,
                onBackground = BlueTheme.DarkOnBackground,
                onSurface = BlueTheme.DarkOnSurface
            )
        } else {
            ThemeColors(
                primary = BlueTheme.Primary,
                primaryDark = BlueTheme.PrimaryDark,
                primaryLight = BlueTheme.PrimaryLight,
                secondary = BlueTheme.Secondary,
                secondaryDark = BlueTheme.SecondaryDark,
                secondaryLight = BlueTheme.SecondaryLight,
                background = BlueTheme.Background,
                surface = BlueTheme.Surface,
                error = BlueTheme.Error,
                onPrimary = BlueTheme.OnPrimary,
                onSecondary = BlueTheme.OnSecondary,
                onBackground = BlueTheme.OnBackground,
                onSurface = BlueTheme.OnSurface
            )
        }

        AppTheme.PURPLE -> if (dark) {
            ThemeColors(
                primary = PurpleTheme.DarkPrimary,
                primaryDark = PurpleTheme.DarkPrimaryDark,
                primaryLight = PurpleTheme.PrimaryLight,
                secondary = PurpleTheme.Secondary,
                secondaryDark = PurpleTheme.SecondaryDark,
                secondaryLight = PurpleTheme.SecondaryLight,
                background = PurpleTheme.DarkBackground,
                surface = PurpleTheme.DarkSurface,
                error = PurpleTheme.Error,
                onPrimary = PurpleTheme.DarkOnPrimary,
                onSecondary = PurpleTheme.OnSecondary,
                onBackground = PurpleTheme.DarkOnBackground,
                onSurface = PurpleTheme.DarkOnSurface
            )
        } else {
            ThemeColors(
                primary = PurpleTheme.Primary,
                primaryDark = PurpleTheme.PrimaryDark,
                primaryLight = PurpleTheme.PrimaryLight,
                secondary = PurpleTheme.Secondary,
                secondaryDark = PurpleTheme.SecondaryDark,
                secondaryLight = PurpleTheme.SecondaryLight,
                background = PurpleTheme.Background,
                surface = PurpleTheme.Surface,
                error = PurpleTheme.Error,
                onPrimary = PurpleTheme.OnPrimary,
                onSecondary = PurpleTheme.OnSecondary,
                onBackground = PurpleTheme.OnBackground,
                onSurface = PurpleTheme.OnSurface
            )
        }

        AppTheme.ORANGE -> if (dark) {
            ThemeColors(
                primary = OrangeTheme.DarkPrimary,
                primaryDark = OrangeTheme.DarkPrimaryDark,
                primaryLight = OrangeTheme.PrimaryLight,
                secondary = OrangeTheme.Secondary,
                secondaryDark = OrangeTheme.SecondaryDark,
                secondaryLight = OrangeTheme.SecondaryLight,
                background = OrangeTheme.DarkBackground,
                surface = OrangeTheme.DarkSurface,
                error = OrangeTheme.Error,
                onPrimary = OrangeTheme.DarkOnPrimary,
                onSecondary = OrangeTheme.OnSecondary,
                onBackground = OrangeTheme.DarkOnBackground,
                onSurface = OrangeTheme.DarkOnSurface
            )
        } else {
            ThemeColors(
                primary = OrangeTheme.Primary,
                primaryDark = OrangeTheme.PrimaryDark,
                primaryLight = OrangeTheme.PrimaryLight,
                secondary = OrangeTheme.Secondary,
                secondaryDark = OrangeTheme.SecondaryDark,
                secondaryLight = OrangeTheme.SecondaryLight,
                background = OrangeTheme.Background,
                surface = OrangeTheme.Surface,
                error = OrangeTheme.Error,
                onPrimary = OrangeTheme.OnPrimary,
                onSecondary = OrangeTheme.OnSecondary,
                onBackground = OrangeTheme.OnBackground,
                onSurface = OrangeTheme.OnSurface
            )
        }

        AppTheme.PINK -> if (dark) {
            ThemeColors(
                primary = PinkTheme.DarkPrimary,
                primaryDark = PinkTheme.DarkPrimaryDark,
                primaryLight = PinkTheme.PrimaryLight,
                secondary = PinkTheme.Secondary,
                secondaryDark = PinkTheme.SecondaryDark,
                secondaryLight = PinkTheme.SecondaryLight,
                background = PinkTheme.DarkBackground,
                surface = PinkTheme.DarkSurface,
                error = PinkTheme.Error,
                onPrimary = PinkTheme.DarkOnPrimary,
                onSecondary = PinkTheme.OnSecondary,
                onBackground = PinkTheme.DarkOnBackground,
                onSurface = PinkTheme.DarkOnSurface
            )
        } else {
            ThemeColors(
                primary = PinkTheme.Primary,
                primaryDark = PinkTheme.PrimaryDark,
                primaryLight = PinkTheme.PrimaryLight,
                secondary = PinkTheme.Secondary,
                secondaryDark = PinkTheme.SecondaryDark,
                secondaryLight = PinkTheme.SecondaryLight,
                background = PinkTheme.Background,
                surface = PinkTheme.Surface,
                error = PinkTheme.Error,
                onPrimary = PinkTheme.OnPrimary,
                onSecondary = PinkTheme.OnSecondary,
                onBackground = PinkTheme.OnBackground,
                onSurface = PinkTheme.OnSurface
            )
        }

        AppTheme.RED -> if (dark) {
            ThemeColors(
                primary = RedTheme.DarkPrimary,
                primaryDark = RedTheme.DarkPrimaryDark,
                primaryLight = RedTheme.PrimaryLight,
                secondary = RedTheme.Secondary,
                secondaryDark = RedTheme.SecondaryDark,
                secondaryLight = RedTheme.SecondaryLight,
                background = RedTheme.DarkBackground,
                surface = RedTheme.DarkSurface,
                error = RedTheme.Error,
                onPrimary = RedTheme.DarkOnPrimary,
                onSecondary = RedTheme.OnSecondary,
                onBackground = RedTheme.DarkOnBackground,
                onSurface = RedTheme.DarkOnSurface
            )
        } else {
            ThemeColors(
                primary = RedTheme.Primary,
                primaryDark = RedTheme.PrimaryDark,
                primaryLight = RedTheme.PrimaryLight,
                secondary = RedTheme.Secondary,
                secondaryDark = RedTheme.SecondaryDark,
                secondaryLight = RedTheme.SecondaryLight,
                background = RedTheme.Background,
                surface = RedTheme.Surface,
                error = RedTheme.Error,
                onPrimary = RedTheme.OnPrimary,
                onSecondary = RedTheme.OnSecondary,
                onBackground = RedTheme.OnBackground,
                onSurface = RedTheme.OnSurface
            )
        }

        AppTheme.TEAL -> if (dark) {
            ThemeColors(
                primary = TealTheme.DarkPrimary,
                primaryDark = TealTheme.DarkPrimaryDark,
                primaryLight = TealTheme.PrimaryLight,
                secondary = TealTheme.Secondary,
                secondaryDark = TealTheme.SecondaryDark,
                secondaryLight = TealTheme.SecondaryLight,
                background = TealTheme.DarkBackground,
                surface = TealTheme.DarkSurface,
                error = TealTheme.Error,
                onPrimary = TealTheme.DarkOnPrimary,
                onSecondary = TealTheme.OnSecondary,
                onBackground = TealTheme.DarkOnBackground,
                onSurface = TealTheme.DarkOnSurface
            )
        } else {
            ThemeColors(
                primary = TealTheme.Primary,
                primaryDark = TealTheme.PrimaryDark,
                primaryLight = TealTheme.PrimaryLight,
                secondary = TealTheme.Secondary,
                secondaryDark = TealTheme.SecondaryDark,
                secondaryLight = TealTheme.SecondaryLight,
                background = TealTheme.Background,
                surface = TealTheme.Surface,
                error = TealTheme.Error,
                onPrimary = TealTheme.OnPrimary,
                onSecondary = TealTheme.OnSecondary,
                onBackground = TealTheme.OnBackground,
                onSurface = TealTheme.OnSurface
            )
        }
    }
}