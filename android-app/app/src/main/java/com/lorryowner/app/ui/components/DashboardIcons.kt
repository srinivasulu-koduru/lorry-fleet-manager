package com.lorryowner.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.lorryowner.app.ui.theme.BhpNotificationDot

@Composable
fun DashboardTruckIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Cargo Container Box
        drawRoundRect(
            color = tint,
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
            color = tint,
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
            color = tint,
            style = Fill
        )

        // Left Wheel
        drawCircle(
            color = tint,
            radius = w * 0.10f,
            center = Offset(w * 0.26f, h * 0.76f),
            style = Fill
        )

        // Right Wheel
        drawCircle(
            color = tint,
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
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Circle
        drawCircle(
            color = tint,
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
            color = tint,
            style = Stroke(width = w * 0.10f)
        )
    }
}

@Composable
fun ClockTimerIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Circle
        drawCircle(
            color = tint,
            radius = w * 0.44f,
            center = Offset(w * 0.50f, h * 0.50f),
            style = Stroke(width = w * 0.10f)
        )

        // Hands
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.50f),
            end = Offset(w * 0.50f, h * 0.26f),
            strokeWidth = w * 0.09f
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.50f),
            end = Offset(w * 0.68f, h * 0.50f),
            strokeWidth = w * 0.09f
        )
    }
}

@Composable
fun FormatListIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val yCoords = listOf(0.28f, 0.50f, 0.72f)
        for (y in yCoords) {
            // Bullet
            drawCircle(
                color = tint,
                radius = w * 0.05f,
                center = Offset(w * 0.22f, h * y)
            )
            // Line
            drawLine(
                color = tint,
                start = Offset(w * 0.36f, h * y),
                end = Offset(w * 0.82f, h * y),
                strokeWidth = w * 0.09f
            )
        }
    }
}

@Composable
fun LocationPinIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(24.dp)
) {
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
            color = tint,
            style = Stroke(width = w * 0.08f)
        )

        // Center dot
        drawCircle(
            color = tint,
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
    tint: Color,
    modifier: Modifier = Modifier.size(16.dp)
) {
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
            color = tint,
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
