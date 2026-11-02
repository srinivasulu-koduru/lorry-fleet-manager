package com.lorryowner.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lorryowner.app.ui.theme.BhpNotificationDot

@Composable
fun DashboardTruckIcon(
    tint: Color = Color.White,
    color: Color = tint,
    modifier: Modifier = Modifier.size(20.dp)
) {
    val effectiveTint = if (color != tint) color else tint
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Cargo Container Box
        drawRoundRect(
            color = effectiveTint,
            topLeft = Offset(w * 0.08f, h * 0.22f),
            size = Size(w * 0.54f, h * 0.48f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
            style = Stroke(width = w * 0.08f)
        )

        // Cab
        val cabPath = Path().apply {
            moveTo(w * 0.62f, h * 0.35f)
            lineTo(w * 0.78f, h * 0.35f)
            lineTo(w * 0.92f, h * 0.52f)
            lineTo(w * 0.92f, h * 0.70f)
            lineTo(w * 0.62f, h * 0.70f)
            close()
        }
        drawPath(
            path = cabPath,
            color = effectiveTint,
            style = Stroke(width = w * 0.08f)
        )

        // Cab window
        val windowPath = Path().apply {
            moveTo(w * 0.68f, h * 0.42f)
            lineTo(w * 0.76f, h * 0.42f)
            lineTo(w * 0.85f, h * 0.52f)
            lineTo(w * 0.68f, h * 0.52f)
            close()
        }
        drawPath(
            path = windowPath,
            color = effectiveTint,
            style = Fill
        )

        // Left Wheel
        drawCircle(
            color = effectiveTint,
            radius = w * 0.10f,
            center = Offset(w * 0.26f, h * 0.76f),
            style = Fill
        )

        // Right Wheel
        drawCircle(
            color = effectiveTint,
            radius = w * 0.10f,
            center = Offset(w * 0.78f, h * 0.76f),
            style = Fill
        )
    }
}

@Composable
fun NotificationBellIcon(
    tint: Color,
    hasBadge: Boolean = true,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Bell Body
        val bellPath = Path().apply {
            moveTo(w * 0.50f, h * 0.18f)
            cubicTo(w * 0.36f, h * 0.18f, w * 0.30f, h * 0.32f, w * 0.30f, h * 0.54f)
            cubicTo(w * 0.30f, h * 0.68f, w * 0.20f, h * 0.74f, w * 0.20f, h * 0.76f)
            lineTo(w * 0.80f, h * 0.76f)
            cubicTo(w * 0.80f, h * 0.74f, w * 0.70f, h * 0.68f, w * 0.70f, h * 0.54f)
            cubicTo(w * 0.70f, h * 0.32f, w * 0.64f, h * 0.18f, w * 0.50f, h * 0.18f)
            close()
        }
        drawPath(
            path = bellPath,
            color = tint,
            style = Stroke(width = w * 0.08f)
        )

        // Clapper
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = true,
            topLeft = Offset(w * 0.42f, h * 0.76f),
            size = Size(w * 0.16f, h * 0.14f)
        )

        // Optional Notification dot badge
        if (hasBadge) {
            drawCircle(
                color = BhpNotificationDot,
                radius = w * 0.11f,
                center = Offset(w * 0.76f, h * 0.24f)
            )
        }
    }
}

@Composable
fun CheckmarkCircleIcon(
    tint: Color = Color.Unspecified,
    color: Color = tint,
    modifier: Modifier = Modifier.size(20.dp)
) {
    val effectiveTint = if (color != Color.Unspecified) color else if (tint != Color.Unspecified) tint else Color.White
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Circle
        drawCircle(
            color = effectiveTint,
            radius = w * 0.44f,
            center = Offset(w * 0.50f, h * 0.50f),
            style = Stroke(width = w * 0.10f)
        )

        // Checkmark
        val checkPath = Path().apply {
            moveTo(w * 0.30f, h * 0.52f)
            lineTo(w * 0.45f, h * 0.66f)
            lineTo(w * 0.72f, h * 0.36f)
        }
        drawPath(
            path = checkPath,
            color = effectiveTint,
            style = Stroke(width = w * 0.10f)
        )
    }
}

@Composable
fun ClockTimerIcon(
    tint: Color = Color.Unspecified,
    color: Color = tint,
    modifier: Modifier = Modifier.size(20.dp)
) {
    val effectiveTint = if (color != Color.Unspecified) color else if (tint != Color.Unspecified) tint else Color.White
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Circle
        drawCircle(
            color = effectiveTint,
            radius = w * 0.44f,
            center = Offset(w * 0.50f, h * 0.50f),
            style = Stroke(width = w * 0.10f)
        )

        // Hands
        drawLine(
            color = effectiveTint,
            start = Offset(w * 0.50f, h * 0.50f),
            end = Offset(w * 0.50f, h * 0.26f),
            strokeWidth = w * 0.09f
        )
        drawLine(
            color = effectiveTint,
            start = Offset(w * 0.50f, h * 0.50f),
            end = Offset(w * 0.68f, h * 0.50f),
            strokeWidth = w * 0.09f
        )
    }
}

@Composable
fun FormatListIcon(
    tint: Color = Color.Unspecified,
    color: Color = tint,
    modifier: Modifier = Modifier.size(20.dp)
) {
    val effectiveTint = if (color != Color.Unspecified) color else if (tint != Color.Unspecified) tint else Color.White
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val yCoords = listOf(0.28f, 0.50f, 0.72f)
        for (y in yCoords) {
            // Bullet
            drawCircle(
                color = effectiveTint,
                radius = w * 0.05f,
                center = Offset(w * 0.22f, h * y)
            )
            // Line
            drawLine(
                color = effectiveTint,
                start = Offset(w * 0.36f, h * y),
                end = Offset(w * 0.82f, h * y),
                strokeWidth = w * 0.09f
            )
        }
    }
}

@Composable
fun LocationPinIcon(
    tint: Color = Color.Unspecified,
    color: Color = tint,
    modifier: Modifier = Modifier.size(24.dp)
) {
    val effectiveTint = if (color != Color.Unspecified) color else if (tint != Color.Unspecified) tint else Color.White
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val pinPath = Path().apply {
            moveTo(w * 0.50f, h * 0.88f)
            cubicTo(w * 0.28f, h * 0.62f, w * 0.20f, h * 0.46f, w * 0.20f, h * 0.36f)
            cubicTo(w * 0.20f, h * 0.18f, w * 0.33f, h * 0.10f, w * 0.50f, h * 0.10f)
            cubicTo(w * 0.67f, h * 0.10f, w * 0.80f, h * 0.18f, w * 0.80f, h * 0.36f)
            cubicTo(w * 0.80f, h * 0.46f, w * 0.72f, h * 0.62f, w * 0.50f, h * 0.88f)
            close()
        }
        drawPath(
            path = pinPath,
            color = effectiveTint,
            style = Stroke(width = w * 0.08f)
        )

        // Center dot
        drawCircle(
            color = effectiveTint,
            radius = w * 0.12f,
            center = Offset(w * 0.50f, h * 0.36f)
        )
    }
}

@Composable
fun WrenchToolIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Handle from (w * 0.22f, h * 0.78f) to (w * 0.58f, h * 0.42f)
        drawLine(
            color = tint,
            start = Offset(w * 0.22f, h * 0.78f),
            end = Offset(w * 0.58f, h * 0.42f),
            strokeWidth = w * 0.13f,
            cap = StrokeCap.Round
        )

        // Jaw base connecting prongs
        drawLine(
            color = tint,
            start = Offset(w * 0.52f, h * 0.36f),
            end = Offset(w * 0.64f, h * 0.48f),
            strokeWidth = w * 0.14f
        )

        // Upper prong
        drawLine(
            color = tint,
            start = Offset(w * 0.52f, h * 0.36f),
            end = Offset(w * 0.68f, h * 0.20f),
            strokeWidth = w * 0.12f,
            cap = StrokeCap.Round
        )

        // Lower prong
        drawLine(
            color = tint,
            start = Offset(w * 0.64f, h * 0.48f),
            end = Offset(w * 0.80f, h * 0.32f),
            strokeWidth = w * 0.12f,
            cap = StrokeCap.Round
        )

        // Hole at bottom-left handle
        drawCircle(
            color = Color.White,
            radius = w * 0.045f,
            center = Offset(w * 0.22f, h * 0.78f)
        )
    }
}

@Composable
fun GasPumpIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Pump Body
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.20f, h * 0.18f),
            size = Size(w * 0.46f, h * 0.68f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
            style = Stroke(width = w * 0.08f)
        )

        // Screen area
        drawRect(
            color = tint,
            topLeft = Offset(w * 0.30f, h * 0.28f),
            size = Size(w * 0.26f, h * 0.18f),
            style = Stroke(width = w * 0.06f)
        )

        // Hose & Nozzle
        val hosePath = Path().apply {
            moveTo(w * 0.66f, h * 0.36f)
            lineTo(w * 0.80f, h * 0.36f)
            cubicTo(w * 0.88f, h * 0.36f, w * 0.88f, h * 0.68f, w * 0.80f, h * 0.68f)
            lineTo(w * 0.76f, h * 0.68f)
            lineTo(w * 0.76f, h * 0.56f)
        }
        drawPath(
            path = hosePath,
            color = tint,
            style = Stroke(width = w * 0.07f)
        )
    }
}

@Composable
fun ChevronRightIcon(
    tint: Color = Color.Unspecified,
    color: Color = tint,
    modifier: Modifier = Modifier.size(16.dp)
) {
    val effectiveTint = if (color != Color.Unspecified) color else if (tint != Color.Unspecified) tint else Color.White
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.35f, h * 0.20f)
            lineTo(w * 0.65f, h * 0.50f)
            lineTo(w * 0.35f, h * 0.80f)
        }
        drawPath(
            path = path,
            color = effectiveTint,
            style = Stroke(width = w * 0.12f)
        )
    }
}

@Composable
fun HomeNavIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(22.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Roof
        val roofPath = Path().apply {
            moveTo(w * 0.15f, h * 0.48f)
            lineTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.85f, h * 0.48f)
        }
        drawPath(
            path = roofPath,
            color = tint,
            style = Stroke(width = w * 0.09f)
        )

        // House walls
        val wallsPath = Path().apply {
            moveTo(w * 0.25f, h * 0.44f)
            lineTo(w * 0.25f, h * 0.84f)
            lineTo(w * 0.75f, h * 0.84f)
            lineTo(w * 0.75f, h * 0.44f)
        }
        drawPath(
            path = wallsPath,
            color = tint,
            style = Stroke(width = w * 0.09f)
        )
    }
}

@Composable
fun MoreDotsIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val xs = listOf(0.24f, 0.50f, 0.76f)
        for (x in xs) {
            drawCircle(
                color = tint,
                radius = w * 0.08f,
                center = Offset(w * x, h * 0.50f)
            )
        }
    }
}

/**
 * Graphic illustration of the blue truck and orange cab shown in the greeting card.
 */
@Composable
fun GreetingTruckGraphic(
    modifier: Modifier = Modifier.size(width = 96.dp, height = 48.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Cargo Container (Blue)
        val containerColor = Color(0xFF386A94)
        drawRoundRect(
            color = containerColor,
            topLeft = Offset(w * 0.04f, h * 0.12f),
            size = Size(w * 0.56f, h * 0.60f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )

        // Container horizontal line accent
        drawLine(
            color = Color(0xFF2C567B),
            start = Offset(w * 0.06f, h * 0.42f),
            end = Offset(w * 0.58f, h * 0.42f),
            strokeWidth = h * 0.03f
        )

        // 2. Cab (Orange / Amber)
        val cabColor = Color(0xFFF58220)
        val cabPath = Path().apply {
            moveTo(w * 0.60f, h * 0.24f)
            lineTo(w * 0.76f, h * 0.24f)
            lineTo(w * 0.88f, h * 0.44f)
            lineTo(w * 0.88f, h * 0.72f)
            lineTo(w * 0.60f, h * 0.72f)
            close()
        }
        drawPath(path = cabPath, color = cabColor)

        // 3. Windshield (Light blue-white)
        val windowPath = Path().apply {
            moveTo(w * 0.64f, h * 0.30f)
            lineTo(w * 0.74f, h * 0.30f)
            lineTo(w * 0.82f, h * 0.44f)
            lineTo(w * 0.64f, h * 0.44f)
            close()
        }
        drawPath(path = windowPath, color = Color(0xFFE2E8F0))

        // 4. Wheels Chassis bar
        drawLine(
            color = Color(0xFF1E293B),
            start = Offset(w * 0.10f, h * 0.72f),
            end = Offset(w * 0.84f, h * 0.72f),
            strokeWidth = h * 0.06f
        )

        // 5. Wheels (Outer dark tire, inner rim)
        val wheelCenters = listOf(
            Offset(w * 0.20f, h * 0.74f),
            Offset(w * 0.44f, h * 0.74f),
            Offset(w * 0.78f, h * 0.74f)
        )

        val tireColor = Color(0xFF1E293B)
        val rimColor = Color(0xFF94A3B8)
        val hubColor = Color(0xFFF1F5F9)

        for (center in wheelCenters) {
            // Tire
            drawCircle(
                color = tireColor,
                radius = h * 0.18f,
                center = center
            )
            // Rim
            drawCircle(
                color = rimColor,
                radius = h * 0.10f,
                center = center
            )
            // Hub
            drawCircle(
                color = hubColor,
                radius = h * 0.04f,
                center = center
            )
        }
    }
}

/**
 * Driver / Chauffeur Cap Icon used in Manage Drivers top card and Quick Actions.
 */
@Composable
fun DriverCapIcon(
    tint: Color = Color.Unspecified,
    color: Color = tint,
    modifier: Modifier = Modifier.size(24.dp)
) {
    val effectiveTint = if (color != Color.Unspecified) color else if (tint != Color.Unspecified) tint else Color.White
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Cap Crown (Dome)
        val capPath = Path().apply {
            moveTo(w * 0.22f, h * 0.32f)
            cubicTo(w * 0.22f, h * 0.16f, w * 0.78f, h * 0.16f, w * 0.78f, h * 0.32f)
            close()
        }
        drawPath(path = capPath, color = effectiveTint)

        // Cap Visor / Peak
        val visorPath = Path().apply {
            moveTo(w * 0.18f, h * 0.34f)
            lineTo(w * 0.82f, h * 0.34f)
            cubicTo(w * 0.76f, h * 0.44f, w * 0.24f, h * 0.44f, w * 0.18f, h * 0.34f)
            close()
        }
        drawPath(path = visorPath, color = effectiveTint)

        // Driver Head / Face
        drawCircle(
            color = effectiveTint,
            radius = w * 0.18f,
            center = Offset(w * 0.50f, h * 0.48f)
        )

        // Shoulders / Torso
        val bodyPath = Path().apply {
            moveTo(w * 0.18f, h * 0.88f)
            cubicTo(w * 0.24f, h * 0.68f, w * 0.76f, h * 0.68f, w * 0.82f, h * 0.88f)
            close()
        }
        drawPath(path = bodyPath, color = effectiveTint)
    }
}

/**
 * Checkbox square with checkmark inside, matching "Active Drivers" card.
 */
@Composable
fun CheckSquareIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Square container
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.12f, h * 0.12f),
            size = Size(w * 0.76f, h * 0.76f),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
        )

        // Checkmark inside (white)
        val checkPath = Path().apply {
            moveTo(w * 0.30f, h * 0.50f)
            lineTo(w * 0.44f, h * 0.64f)
            lineTo(w * 0.70f, h * 0.36f)
        }
        drawPath(
            path = checkPath,
            color = Color.White,
            style = Stroke(width = w * 0.10f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Trip Route Icon (Path with pin), matching "Completed Trips" card.
 */
@Composable
fun TripRouteIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Origin dot
        drawCircle(
            color = tint,
            radius = w * 0.09f,
            center = Offset(w * 0.28f, h * 0.74f)
        )

        // Curved Route Path
        val path = Path().apply {
            moveTo(w * 0.34f, h * 0.74f)
            cubicTo(w * 0.60f, h * 0.74f, w * 0.30f, h * 0.44f, w * 0.68f, h * 0.40f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = w * 0.08f, cap = StrokeCap.Round)
        )

        // Destination Pin
        val pinPath = Path().apply {
            moveTo(w * 0.72f, h * 0.50f)
            cubicTo(w * 0.60f, h * 0.38f, w * 0.56f, h * 0.30f, w * 0.56f, h * 0.24f)
            cubicTo(w * 0.56f, h * 0.14f, w * 0.64f, h * 0.08f, w * 0.72f, h * 0.08f)
            cubicTo(w * 0.80f, h * 0.08f, w * 0.88f, h * 0.14f, w * 0.88f, h * 0.24f)
            cubicTo(w * 0.88f, h * 0.30f, w * 0.84f, h * 0.38f, w * 0.72f, h * 0.50f)
            close()
        }
        drawPath(path = pinPath, color = tint)

        drawCircle(
            color = Color.White,
            radius = w * 0.05f,
            center = Offset(w * 0.72f, h * 0.24f)
        )
    }
}

/**
 * Distance Pin Icon, matching "Total Distance" card.
 */
@Composable
fun DistancePinIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val pinPath = Path().apply {
            moveTo(w * 0.50f, h * 0.88f)
            cubicTo(w * 0.26f, h * 0.62f, w * 0.18f, h * 0.44f, w * 0.18f, h * 0.34f)
            cubicTo(w * 0.18f, h * 0.16f, w * 0.32f, h * 0.08f, w * 0.50f, h * 0.08f)
            cubicTo(w * 0.68f, h * 0.08f, w * 0.82f, h * 0.16f, w * 0.82f, h * 0.34f)
            cubicTo(w * 0.82f, h * 0.44f, w * 0.74f, h * 0.62f, w * 0.50f, h * 0.88f)
            close()
        }
        drawPath(path = pinPath, color = tint)

        drawCircle(
            color = Color.White,
            radius = w * 0.11f,
            center = Offset(w * 0.50f, h * 0.34f)
        )
    }
}

/**
 * Standard back arrow icon for top navigation bar.
 */
@Composable
fun BackArrowIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Horizontal Stem
        drawLine(
            color = tint,
            start = Offset(w * 0.80f, h * 0.50f),
            end = Offset(w * 0.22f, h * 0.50f),
            strokeWidth = w * 0.10f,
            cap = StrokeCap.Round
        )

        // Arrow head chevron
        val arrowPath = Path().apply {
            moveTo(w * 0.46f, h * 0.26f)
            lineTo(w * 0.22f, h * 0.50f)
            lineTo(w * 0.46f, h * 0.74f)
        }
        drawPath(
            path = arrowPath,
            color = tint,
            style = Stroke(width = w * 0.10f, cap = StrokeCap.Round)
        )
    }
}

/**
 * Driver Avatar illustration matching the circular driver avatars in the reference design.
 */
@Composable
fun DriverAvatarGraphic(
    avatarIndex: Int = 0,
    modifier: Modifier = Modifier.size(52.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Circle background: Light blue tint
        drawCircle(
            color = Color(0xFFDCEAF9),
            radius = w * 0.50f,
            center = Offset(w * 0.50f, h * 0.50f)
        )

        val capColor = when (avatarIndex % 3) {
            0 -> Color(0xFF1976D2) // Blue cap
            1 -> Color(0xFF4A2B11) // Brown hair
            else -> Color(0xFF1E293B) // Dark cap/hair
        }
        val shirtColor = when (avatarIndex % 3) {
            0 -> Color(0xFF2563EB)
            1 -> Color(0xFF1E40AF)
            else -> Color(0xFF3B82F6)
        }
        val skinColor = when (avatarIndex % 4) {
            0 -> Color(0xFFFCD3A1)
            1 -> Color(0xFFFBC687)
            2 -> Color(0xFFE2A166)
            else -> Color(0xFF8D5524)
        }

        // Shirt / Shoulders
        val shirtPath = Path().apply {
            moveTo(w * 0.14f, h * 0.94f)
            cubicTo(w * 0.22f, h * 0.72f, w * 0.78f, h * 0.72f, w * 0.86f, h * 0.94f)
            close()
        }
        drawPath(path = shirtPath, color = shirtColor)

        // Neck
        drawRect(
            color = skinColor,
            topLeft = Offset(w * 0.42f, h * 0.62f),
            size = Size(w * 0.16f, h * 0.16f)
        )

        // Head / Face
        drawCircle(
            color = skinColor,
            radius = w * 0.22f,
            center = Offset(w * 0.50f, h * 0.48f)
        )

        // Cap / Hair (top)
        val hairPath = Path().apply {
            moveTo(w * 0.26f, h * 0.44f)
            cubicTo(w * 0.24f, h * 0.22f, w * 0.76f, h * 0.22f, w * 0.74f, h * 0.44f)
            close()
        }
        drawPath(path = hairPath, color = capColor)

        // Cap Visor (for index 0 and 2)
        if (avatarIndex % 2 == 0) {
            val visor = Path().apply {
                moveTo(w * 0.24f, h * 0.44f)
                lineTo(w * 0.76f, h * 0.44f)
                cubicTo(w * 0.70f, h * 0.50f, w * 0.30f, h * 0.50f, w * 0.24f, h * 0.44f)
                close()
            }
            drawPath(path = visor, color = capColor)
        }
    }
}

@Composable
fun CalendarIcon(
    tint: Color = Color.Unspecified,
    color: Color = tint,
    modifier: Modifier = Modifier.size(20.dp)
) {
    val effectiveTint = if (color != Color.Unspecified) color else if (tint != Color.Unspecified) tint else Color.White
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Calendar body
        drawRoundRect(
            color = effectiveTint,
            topLeft = Offset(w * 0.15f, h * 0.25f),
            size = Size(w * 0.70f, h * 0.65f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = w * 0.08f)
        )

        // Top binder rings
        drawLine(
            color = effectiveTint,
            start = Offset(w * 0.32f, h * 0.12f),
            end = Offset(w * 0.32f, h * 0.28f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = effectiveTint,
            start = Offset(w * 0.68f, h * 0.12f),
            end = Offset(w * 0.68f, h * 0.28f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )

        // Header separator line
        drawLine(
            color = effectiveTint,
            start = Offset(w * 0.15f, h * 0.45f),
            end = Offset(w * 0.85f, h * 0.45f),
            strokeWidth = w * 0.06f
        )

        // Date grid dots
        val xCoords = listOf(0.32f, 0.50f, 0.68f)
        val yCoords = listOf(0.60f, 0.75f)
        for (y in yCoords) {
            for (x in xCoords) {
                drawCircle(
                    color = effectiveTint,
                    radius = w * 0.035f,
                    center = Offset(w * x, h * y)
                )
            }
        }
    }
}

@Composable
fun PackageBoxIcon(
    tint: Color = Color.Unspecified,
    color: Color = tint,
    modifier: Modifier = Modifier.size(20.dp)
) {
    val effectiveTint = if (color != Color.Unspecified) color else if (tint != Color.Unspecified) tint else Color.White
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Outer box
        drawRoundRect(
            color = effectiveTint,
            topLeft = Offset(w * 0.18f, h * 0.20f),
            size = Size(w * 0.64f, h * 0.64f),
            cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
            style = Stroke(width = w * 0.08f)
        )

        // Flap seam (center vertical line)
        drawLine(
            color = effectiveTint,
            start = Offset(w * 0.50f, h * 0.20f),
            end = Offset(w * 0.50f, h * 0.84f),
            strokeWidth = w * 0.06f
        )

        // Tape horizontal stripe
        drawLine(
            color = effectiveTint,
            start = Offset(w * 0.24f, h * 0.48f),
            end = Offset(w * 0.76f, h * 0.48f),
            strokeWidth = w * 0.07f
        )
    }
}

@Composable
fun EditPencilIcon(
    tint: Color = Color.Unspecified,
    color: Color = tint,
    modifier: Modifier = Modifier.size(18.dp)
) {
    val effectiveTint = if (color != Color.Unspecified) color else if (tint != Color.Unspecified) tint else Color.White
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val pencilPath = Path().apply {
            moveTo(w * 0.72f, h * 0.14f)
            lineTo(w * 0.86f, h * 0.28f)
            lineTo(w * 0.38f, h * 0.76f)
            lineTo(w * 0.20f, h * 0.80f)
            lineTo(w * 0.24f, h * 0.62f)
            close()
        }
        drawPath(
            path = pencilPath,
            color = effectiveTint,
            style = Stroke(width = w * 0.09f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun EyeIcon(
    tint: Color = Color.Unspecified,
    color: Color = tint,
    modifier: Modifier = Modifier.size(18.dp)
) {
    val effectiveTint = if (color != Color.Unspecified) color else if (tint != Color.Unspecified) tint else Color.White
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Eye shape
        val eyePath = Path().apply {
            moveTo(w * 0.12f, h * 0.50f)
            cubicTo(w * 0.28f, h * 0.24f, w * 0.72f, h * 0.24f, w * 0.88f, h * 0.50f)
            cubicTo(w * 0.72f, h * 0.76f, w * 0.28f, h * 0.76f, w * 0.12f, h * 0.50f)
            close()
        }
        drawPath(
            path = eyePath,
            color = effectiveTint,
            style = Stroke(width = w * 0.08f)
        )

        // Pupil
        drawCircle(
            color = effectiveTint,
            radius = w * 0.14f,
            center = Offset(w * 0.50f, h * 0.50f)
        )
    }
}

@Composable
fun DocumentTextIcon(
    tint: Color = Color.Unspecified,
    color: Color = tint,
    modifier: Modifier = Modifier.size(20.dp)
) {
    val effectiveTint = if (color != Color.Unspecified) color else if (tint != Color.Unspecified) tint else Color.White
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Paper outline
        val pagePath = Path().apply {
            moveTo(w * 0.22f, h * 0.15f)
            lineTo(w * 0.62f, h * 0.15f)
            lineTo(w * 0.78f, h * 0.31f)
            lineTo(w * 0.78f, h * 0.85f)
            lineTo(w * 0.22f, h * 0.85f)
            close()
        }
        drawPath(
            path = pagePath,
            color = effectiveTint,
            style = Stroke(width = w * 0.08f)
        )

        // Folded corner
        drawLine(
            color = effectiveTint,
            start = Offset(w * 0.62f, h * 0.15f),
            end = Offset(w * 0.62f, h * 0.31f),
            strokeWidth = w * 0.07f
        )
        drawLine(
            color = effectiveTint,
            start = Offset(w * 0.62f, h * 0.31f),
            end = Offset(w * 0.78f, h * 0.31f),
            strokeWidth = w * 0.07f
        )

        // Text lines
        drawLine(
            color = effectiveTint,
            start = Offset(w * 0.34f, h * 0.46f),
            end = Offset(w * 0.66f, h * 0.46f),
            strokeWidth = w * 0.07f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = effectiveTint,
            start = Offset(w * 0.34f, h * 0.60f),
            end = Offset(w * 0.66f, h * 0.60f),
            strokeWidth = w * 0.07f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun CoinsStackIcon(
    tint: Color = Color(0xFF059669),
    modifier: Modifier = Modifier.size(36.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Left coin stack (3 coins)
        for (i in 0..2) {
            val yOffset = h * (0.64f - i * 0.11f)
            drawOval(
                color = tint,
                topLeft = Offset(w * 0.04f, yOffset),
                size = Size(w * 0.44f, h * 0.20f)
            )
            drawOval(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = Offset(w * 0.07f, yOffset + h * 0.02f),
                size = Size(w * 0.38f, h * 0.14f)
            )
        }

        // Right coin stack (4 coins)
        for (i in 0..3) {
            val yOffset = h * (0.60f - i * 0.11f)
            drawOval(
                color = tint,
                topLeft = Offset(w * 0.48f, yOffset),
                size = Size(w * 0.48f, h * 0.20f)
            )
            drawOval(
                color = Color.White.copy(alpha = 0.25f),
                topLeft = Offset(w * 0.51f, yOffset + h * 0.02f),
                size = Size(w * 0.42f, h * 0.14f)
            )
        }
    }
}

@Composable
fun HeroWhiteTruckGraphic(
    modifier: Modifier = Modifier.size(130.dp, 90.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Container / Trailer (Right side, perspective)
        val trailerPath = Path().apply {
            moveTo(w * 0.35f, h * 0.10f)
            lineTo(w * 0.98f, h * 0.22f)
            lineTo(w * 0.98f, h * 0.72f)
            lineTo(w * 0.35f, h * 0.76f)
            close()
        }
        drawPath(
            path = trailerPath,
            color = Color(0xFFF1F5F9), // Clean silver-white
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
        drawPath(
            path = trailerPath,
            color = Color(0xFFCBD5E1),
            style = Stroke(width = w * 0.015f)
        )

        // Cab Body (Left side, angled towards viewer)
        val cabPath = Path().apply {
            moveTo(w * 0.08f, h * 0.38f)
            lineTo(w * 0.36f, h * 0.24f)
            lineTo(w * 0.36f, h * 0.76f)
            lineTo(w * 0.05f, h * 0.76f)
            lineTo(w * 0.05f, h * 0.45f)
            close()
        }
        drawPath(
            path = cabPath,
            color = Color(0xFFE2E8F0),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        // Cab Windshield
        val windshield = Path().apply {
            moveTo(w * 0.09f, h * 0.40f)
            lineTo(w * 0.33f, h * 0.28f)
            lineTo(w * 0.33f, h * 0.48f)
            lineTo(w * 0.08f, h * 0.52f)
            close()
        }
        drawPath(
            path = windshield,
            color = Color(0xFF1E293B), // Dark tinted glass
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        // Front Grille
        drawRoundRect(
            color = Color(0xFF94A3B8),
            topLeft = Offset(w * 0.06f, h * 0.56f),
            size = Size(w * 0.20f, h * 0.16f),
            cornerRadius = CornerRadius(w * 0.02f, w * 0.02f)
        )

        // Green EV / Eco plate on bumper
        drawRoundRect(
            color = Color(0xFF10B981),
            topLeft = Offset(w * 0.10f, h * 0.72f),
            size = Size(w * 0.10f, h * 0.04f),
            cornerRadius = CornerRadius(w * 0.01f, w * 0.01f)
        )

        // Wheels
        drawCircle(
            color = Color(0xFF0F172A),
            radius = w * 0.08f,
            center = Offset(w * 0.20f, h * 0.80f)
        )
        drawCircle(
            color = Color(0xFF94A3B8),
            radius = w * 0.04f,
            center = Offset(w * 0.20f, h * 0.80f)
        )

        drawCircle(
            color = Color(0xFF0F172A),
            radius = w * 0.08f,
            center = Offset(w * 0.65f, h * 0.80f)
        )
        drawCircle(
            color = Color(0xFF94A3B8),
            radius = w * 0.04f,
            center = Offset(w * 0.65f, h * 0.80f)
        )
        drawCircle(
            color = Color(0xFF0F172A),
            radius = w * 0.08f,
            center = Offset(w * 0.82f, h * 0.78f)
        )
        drawCircle(
            color = Color(0xFF94A3B8),
            radius = w * 0.04f,
            center = Offset(w * 0.82f, h * 0.78f)
        )
    }
}

@Composable
fun HamburgerMenuIcon(
    tint: Color = Color(0xFF0F172A),
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.09f
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.28f),
            end = Offset(w * 0.85f, h * 0.28f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.50f),
            end = Offset(w * 0.85f, h * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.72f),
            end = Offset(w * 0.85f, h * 0.72f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun FinanceTrendingIcon(
    tint: Color = Color(0xFF0F172A),
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.10f

        // Trend line with points
        val path = Path().apply {
            moveTo(w * 0.15f, h * 0.75f)
            lineTo(w * 0.42f, h * 0.50f)
            lineTo(w * 0.60f, h * 0.65f)
            lineTo(w * 0.88f, h * 0.25f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Arrow head pointing top-right
        val arrow = Path().apply {
            moveTo(w * 0.68f, h * 0.25f)
            lineTo(w * 0.88f, h * 0.25f)
            lineTo(w * 0.88f, h * 0.45f)
        }
        drawPath(
            path = arrow,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun UpTrendCircleIcon(
    modifier: Modifier = Modifier.size(28.dp),
    backgroundColor: Color = Color(0xFF10B981).copy(alpha = 0.2f),
    arrowColor: Color = Color(0xFF10B981)
) {
    Box(
        modifier = modifier
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.16f

            // Arrow shaft
            drawLine(
                color = arrowColor,
                start = Offset(w * 0.5f, h * 0.85f),
                end = Offset(w * 0.5f, h * 0.15f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            // Arrow head
            drawLine(
                color = arrowColor,
                start = Offset(w * 0.22f, h * 0.42f),
                end = Offset(w * 0.5f, h * 0.15f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = arrowColor,
                start = Offset(w * 0.78f, h * 0.42f),
                end = Offset(w * 0.5f, h * 0.15f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun DownTrendCircleIcon(
    modifier: Modifier = Modifier.size(28.dp),
    backgroundColor: Color = Color(0xFFEF4444).copy(alpha = 0.2f),
    arrowColor: Color = Color(0xFFEF4444)
) {
    Box(
        modifier = modifier
            .background(backgroundColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(14.dp)) {
            val w = size.width
            val h = size.height
            val stroke = w * 0.16f

            // Arrow shaft
            drawLine(
                color = arrowColor,
                start = Offset(w * 0.5f, h * 0.15f),
                end = Offset(w * 0.5f, h * 0.85f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            // Arrow head
            drawLine(
                color = arrowColor,
                start = Offset(w * 0.22f, h * 0.58f),
                end = Offset(w * 0.5f, h * 0.85f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = arrowColor,
                start = Offset(w * 0.78f, h * 0.58f),
                end = Offset(w * 0.5f, h * 0.85f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun TrophyIcon(
    tint: Color = Color(0xFFEAB308),
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Cup body
        val cupPath = Path().apply {
            moveTo(w * 0.25f, h * 0.20f)
            lineTo(w * 0.75f, h * 0.20f)
            cubicTo(w * 0.75f, h * 0.55f, w * 0.65f, h * 0.65f, w * 0.50f, h * 0.65f)
            cubicTo(w * 0.35f, h * 0.65f, w * 0.25f, h * 0.55f, w * 0.25f, h * 0.20f)
            close()
        }
        drawPath(path = cupPath, color = tint)

        // Stem
        drawRect(
            color = tint,
            topLeft = Offset(w * 0.44f, h * 0.65f),
            size = Size(w * 0.12f, h * 0.15f)
        )

        // Base
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.30f, h * 0.78f),
            size = Size(w * 0.40f, h * 0.10f),
            cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
        )

        // Handles
        drawArc(
            color = tint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.12f, h * 0.25f),
            size = Size(w * 0.25f, h * 0.25f),
            style = Stroke(width = w * 0.07f)
        )
        drawArc(
            color = tint,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.63f, h * 0.25f),
            size = Size(w * 0.25f, h * 0.25f),
            style = Stroke(width = w * 0.07f)
        )
    }
}

@Composable
fun FilterSlidersIcon(
    tint: Color = Color(0xFF0F172A),
    modifier: Modifier = Modifier.size(24.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.08f

        // Slider 1 (Top)
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.25f),
            end = Offset(w * 0.85f, h * 0.25f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = tint,
            radius = w * 0.10f,
            center = Offset(w * 0.40f, h * 0.25f)
        )

        // Slider 2 (Middle)
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.50f),
            end = Offset(w * 0.85f, h * 0.50f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = tint,
            radius = w * 0.10f,
            center = Offset(w * 0.70f, h * 0.50f)
        )

        // Slider 3 (Bottom)
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.75f),
            end = Offset(w * 0.85f, h * 0.75f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawCircle(
            color = tint,
            radius = w * 0.10f,
            center = Offset(w * 0.30f, h * 0.75f)
        )
    }
}
