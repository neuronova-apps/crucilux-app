package com.neuronovaapps.crucilux.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.pm.PackageInfoCompat
import com.neuronovaapps.crucilux.R
import com.neuronovaapps.crucilux.data.NeuroNovaLinks
import com.neuronovaapps.crucilux.ui.theme.LocalCruciluxHighContrast
import java.util.Calendar

@Composable
fun AboutScreen(
    onVolver: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onVolver)

    val context = LocalContext.current
    val appInfo = remember(context) { loadAppInfo(context) }
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── 1. Encabezado ────────────────────────────────────────────────
            item {
                AboutHeader(
                    onVolver = onVolver,
                    versionName = appInfo.versionName,
                )
            }

            // ── 2. Sobre Crucilux ───────────────────────────────────────────
            item {
                AboutSection("Sobre Crucilux") {
                    AboutBody(
                        "Crucilux es una aplicación desarrollada por NeuroNova Apps para disfrutar " +
                            "y resolver crucigramas mediante desafíos de palabras, vocabulario, " +
                            "comprensión de pistas y razonamiento verbal."
                    )
                }
            }

            // ── 3. Propósito ────────────────────────────────────────────────
            item {
                AboutSection("Propósito") {
                    AboutBody(
                        "Crucilux ha sido desarrollada con fines de entretenimiento y ejercitación " +
                            "mediante desafíos de vocabulario, palabras y razonamiento verbal."
                    )
                }
            }

            // ── 4. Características ──────────────────────────────────────────
            item {
                AboutSection("Características") {
                    listOf(
                        "Colección de crucigramas organizados por categorías temáticas.",
                        "Modalidades de comprobación clásica y asistida.",
                        "Sistema de pistas para apoyar la resolución de palabras.",
                        "Seguimiento del progreso y estadísticas de juego.",
                        "Sistema de niveles y puntos de experiencia (XP).",
                    ).forEach { feature ->
                        BulletItem(feature)
                    }
                }
            }

            // ── 5. Accesibilidad y personalización ───────────────────────────
            item {
                AboutSection("Accesibilidad y personalización") {
                    AboutBody(
                        "Crucilux incorpora opciones de visualización y personalización para adaptar " +
                            "la experiencia a las preferencias del usuario."
                    )
                    BulletItem("Modo día y modo noche.")
                    BulletItem("Ajuste global de tamaño de texto.")
                    BulletItem("Alto contraste para reforzar bordes y visibilidad.")
                    BulletItem("Personalización del nombre del jugador en el perfil local.")
                }
            }

            // ── 6. Privacidad y datos ───────────────────────────────────────
            item {
                AboutSection("Privacidad y datos") {
                    AboutBody(
                        "Consulta la información sobre privacidad y tratamiento de datos aplicable " +
                            "a Crucilux y a los servicios utilizados por la aplicación."
                    )
                    ExternalLink("Política de privacidad", NeuroNovaLinks.PRIVACY_POLICY_URL)
                }
            }

            // ── 7. Términos y condiciones ───────────────────────────────────
            item {
                AboutSection("Términos y condiciones") {
                    ExternalLink("Términos y condiciones", NeuroNovaLinks.TERMS_URL)
                }
            }

            // ── 8. Licencias y atribuciones ─────────────────────────────────
            item {
                AboutSection("Licencias y atribuciones") {
                    AboutBody(
                        "Consulta las licencias, atribuciones y recursos de terceros utilizados por " +
                            "las aplicaciones de NeuroNova Apps."
                    )
                    ExternalLink("Licencias y atribuciones", NeuroNovaLinks.LICENSES_URL)
                }
            }

            // ── 9. Soporte ──────────────────────────────────────────────────
            item {
                AboutSection("Soporte") {
                    AboutBody(
                        "¿Encontraste un problema o tienes alguna sugerencia? Puedes comunicarte " +
                            "con NeuroNova Apps."
                    )
                    ExternalLink("Contactar con soporte", NeuroNovaLinks.SUPPORT_URL)
                    ExternalLink("Reportar un problema", NeuroNovaLinks.REPORT_ISSUE_URL)
                }
            }

            // ── 10. NeuroNova Apps ──────────────────────────────────────────
            item {
                AboutSection("NeuroNova Apps") {
                    AboutBody(
                        "Crucilux forma parte del ecosistema digital de NeuroNova Apps."
                    )
                    ExternalLink("Más aplicaciones de NeuroNova Apps", NeuroNovaLinks.MORE_APPS_URL)
                    ExternalLink("Sitio oficial de NeuroNova Apps", NeuroNovaLinks.NEURONOVA_APPS_URL)
                }
            }

            // ── 11. Presencia online de Crucilux ────────────────────────────
            item {
                AboutSection("Crucilux en línea") {
                    ExternalLink("Sitio oficial de Crucilux", NeuroNovaLinks.CRUCILUX_OFFICIAL_URL)
                    ExternalLink("Repositorio en GitHub", NeuroNovaLinks.REPOSITORY_URL)
                }
            }

            // ── 12. Información de la aplicación ────────────────────────────
            item {
                AboutSection("Información de la aplicación") {
                    AppInfoRow("Versión", appInfo.versionName)
                    AppInfoRow("Compilación", appInfo.versionCode)
                    AppInfoRow("Desarrollador", "NeuroNova Apps")
                }
            }

            // ── 13. Créditos ────────────────────────────────────────────────
            item {
                AboutSection("Créditos") {
                    AboutBody("Desarrollado por NeuroNova Apps.")
                    AboutBody("Creado por Gabriel Berrospi.")
                }
            }

            // ── 14. Copyright ───────────────────────────────────────────────
            item {
                Text(
                    text = "© $currentYear NeuroNova Apps. Todos los derechos reservados.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun AboutHeader(
    onVolver: () -> Unit,
    versionName: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp),
            contentAlignment = Alignment.Center,
        ) {
            val isHighContrast = LocalCruciluxHighContrast.current
            Surface(
                onClick = onVolver,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(48.dp)
                    .semantics { contentDescription = "Volver a Configuración" },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    width = if (isHighContrast) 1.5.dp else 1.dp,
                    color = if (isHighContrast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Text(
                text = "Acerca de",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Spacer(Modifier.height(18.dp))

        Image(
            painter = painterResource(R.drawable.crucilux_intro_icon),
            contentDescription = "Logotipo de Crucilux",
            modifier = Modifier.size(104.dp),
            contentScale = ContentScale.Fit,
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "Crucilux",
            fontSize = 27.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Text(
            text = "Crucigramas y desafíos de palabras",
            modifier = Modifier.padding(top = 4.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "Versión $versionName",
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )

        Text(
            text = "NeuroNova Apps",
            modifier = Modifier.padding(top = 3.dp),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AboutSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isHighContrast = LocalCruciluxHighContrast.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (isHighContrast) 1.5.dp else 1.dp,
            color = if (isHighContrast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            content()
        }
    }
}

@Composable
private fun AboutBody(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    )
}

@Composable
private fun BulletItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "•",
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Black,
        )
        Text(
            text = text,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
    }
}

@Composable
private fun ExternalLink(label: String, url: String) {
    val context = LocalContext.current
    val isHighContrast = LocalCruciluxHighContrast.current

    Surface(
        onClick = { openExternalUrl(context, url) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .semantics {
                contentDescription = "$label. Abre un enlace externo"
                role = Role.Button
            },
        shape = RoundedCornerShape(14.dp),
        color = if (isHighContrast) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(
            width = if (isHighContrast) 1.5.dp else 1.dp,
            color = if (isHighContrast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                lineHeight = 20.sp,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(start = 10.dp)
                    .size(20.dp),
            )
        }
    }
}

@Composable
private fun AppInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = "$label:",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

private fun openExternalUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }

    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        showLinkError(context)
    } catch (_: SecurityException) {
        showLinkError(context)
    }
}

private fun showLinkError(context: Context) {
    Toast.makeText(
        context,
        "No se encontró una aplicación para abrir este enlace.",
        Toast.LENGTH_SHORT,
    ).show()
}

private fun loadAppInfo(context: Context): AppInfo {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        AppInfo(
            versionName = packageInfo.versionName.orEmpty().ifBlank { "—" },
            versionCode = PackageInfoCompat.getLongVersionCode(packageInfo).toString(),
        )
    } catch (_: PackageManager.NameNotFoundException) {
        AppInfo(versionName = "—", versionCode = "—")
    }
}

private data class AppInfo(
    val versionName: String,
    val versionCode: String,
)
