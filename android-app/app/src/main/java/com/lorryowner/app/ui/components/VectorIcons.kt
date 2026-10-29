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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun PersonVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Head
        drawCircle(
            color = tint,
            radius = w * 0.20f,
            center = Offset(w * 0.5f, h * 0.32f),
            style = Stroke(width = w * 0.08f)
        )

        // Shoulders curve
        val bodyPath = Path().apply {
            moveTo(w * 0.20f, h * 0.82f)
            cubicTo(
                w * 0.20f, h * 0.58f,
                w * 0.80f, h * 0.58f,
                w * 0.80f, h * 0.82f
            )
        }
        drawPath(
            path = bodyPath,
            color = tint,
            style = Stroke(width = w * 0.08f)
        )
    }
}

@Composable
fun LockVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Shackle
        val shacklePath = Path().apply {
            moveTo(w * 0.30f, h * 0.46f)
            lineTo(w * 0.30f, h * 0.30f)
            cubicTo(
                w * 0.30f, h * 0.12f,
                w * 0.70f, h * 0.12f,
                w * 0.70f, h * 0.30f
            )
            lineTo(w * 0.70f, h * 0.46f)
        }
        drawPath(
            path = shacklePath,
            color = tint,
            style = Stroke(width = w * 0.09f)
        )

        // Lock Body
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.22f, h * 0.44f),
            size = Size(w * 0.56f, h * 0.44f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
            style = Stroke(width = w * 0.09f)
        )
    }
}

@Composable
fun VisibilityVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Eye Outline
        val eyePath = Path().apply {
            moveTo(w * 0.10f, h * 0.50f)
            quadraticTo(w * 0.50f, h * 0.18f, w * 0.90f, h * 0.50f)
            quadraticTo(w * 0.50f, h * 0.82f, w * 0.10f, h * 0.50f)
            close()
        }
        drawPath(
            path = eyePath,
            color = tint,
            style = Stroke(width = w * 0.08f)
        )

        // Pupil
        drawCircle(
            color = tint,
            radius = w * 0.14f,
            center = Offset(w * 0.50f, h * 0.50f)
        )
    }
}

@Composable
fun VisibilityOffVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Eye Outline
        val eyePath = Path().apply {
            moveTo(w * 0.10f, h * 0.50f)
            quadraticTo(w * 0.50f, h * 0.18f, w * 0.90f, h * 0.50f)
            quadraticTo(w * 0.50f, h * 0.82f, w * 0.10f, h * 0.50f)
            close()
        }
        drawPath(
            path = eyePath,
            color = tint,
            style = Stroke(width = w * 0.08f)
        )

        // Slash line
        drawLine(
            color = tint,
            start = Offset(w * 0.20f, h * 0.20f),
            end = Offset(w * 0.80f, h * 0.80f),
            strokeWidth = w * 0.09f
        )
    }
}

@Composable
fun ErrorVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Circle
        drawCircle(
            color = tint,
            radius = w * 0.42f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = w * 0.09f)
        )

        // Exclamation Mark Line
        drawLine(
            color = tint,
            start = Offset(w * 0.5f, h * 0.28f),
            end = Offset(w * 0.5f, h * 0.58f),
            strokeWidth = w * 0.10f
        )

        // Exclamation Dot
        drawCircle(
            color = tint,
            radius = w * 0.05f,
            center = Offset(w * 0.5f, h * 0.72f)
        )
    }
}

@Composable
fun ArrowForwardVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(18.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Arrow line
        drawLine(
            color = tint,
            start = Offset(w * 0.15f, h * 0.50f),
            end = Offset(w * 0.82f, h * 0.50f),
            strokeWidth = h * 0.14f
        )

        // Arrow head
        val headPath = Path().apply {
            moveTo(w * 0.55f, h * 0.22f)
            lineTo(w * 0.85f, h * 0.50f)
            lineTo(w * 0.55f, h * 0.78f)
        }
        drawPath(
            path = headPath,
            color = tint,
            style = Stroke(width = h * 0.14f)
        )
    }
}

@Composable
fun ShieldVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(16.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Shield Outer Outline
        val shieldPath = Path().apply {
            moveTo(w * 0.50f, h * 0.10f)
            lineTo(w * 0.88f, h * 0.24f)
            lineTo(w * 0.88f, h * 0.54f)
            cubicTo(
                w * 0.88f, h * 0.78f,
                w * 0.50f, h * 0.92f,
                w * 0.50f, h * 0.92f
            )
            cubicTo(
                w * 0.50f, h * 0.92f,
                w * 0.12f, h * 0.78f,
                w * 0.12f, h * 0.54f
            )
            lineTo(w * 0.12f, h * 0.24f)
            close()
        }

        drawPath(
            path = shieldPath,
            color = tint,
            style = Stroke(width = w * 0.09f)
        )

        // Checkmark inside shield
        val checkPath = Path().apply {
            moveTo(w * 0.32f, h * 0.48f)
            lineTo(w * 0.46f, h * 0.62f)
            lineTo(w * 0.68f, h * 0.38f)
        }
        drawPath(
            path = checkPath,
            color = tint,
            style = Stroke(width = w * 0.09f)
        )
    }
}


@Composable
fun LogoutVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val doorPath = Path().apply {
            moveTo(w * 0.55f, h * 0.18f)
            lineTo(w * 0.22f, h * 0.18f)
            lineTo(w * 0.22f, h * 0.82f)
            lineTo(w * 0.55f, h * 0.82f)
        }
        drawPath(
            path = doorPath,
            color = tint,
            style = Stroke(width = w * 0.09f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        drawLine(
            color = tint,
            start = Offset(w * 0.42f, h * 0.50f),
            end = Offset(w * 0.82f, h * 0.50f),
            strokeWidth = w * 0.09f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        val arrowHead = Path().apply {
            moveTo(w * 0.66f, h * 0.34f)
            lineTo(w * 0.84f, h * 0.50f)
            lineTo(w * 0.66f, h * 0.66f)
        }
        drawPath(
            path = arrowHead,
            color = tint,
            style = Stroke(width = w * 0.09f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}

@Composable
fun KeyVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = tint,
            radius = w * 0.22f,
            center = Offset(w * 0.32f, h * 0.50f),
            style = Stroke(width = w * 0.08f)
        )

        drawLine(
            color = tint,
            start = Offset(w * 0.54f, h * 0.50f),
            end = Offset(w * 0.85f, h * 0.50f),
            strokeWidth = w * 0.08f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        drawLine(
            color = tint,
            start = Offset(w * 0.72f, h * 0.50f),
            end = Offset(w * 0.72f, h * 0.68f),
            strokeWidth = w * 0.08f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.84f, h * 0.50f),
            end = Offset(w * 0.84f, h * 0.64f),
            strokeWidth = w * 0.08f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun PhoneCallVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(w * 0.28f, h * 0.20f)
            cubicTo(w * 0.24f, h * 0.20f, w * 0.20f, h * 0.24f, w * 0.20f, h * 0.30f)
            cubicTo(w * 0.20f, h * 0.58f, w * 0.42f, h * 0.80f, w * 0.70f, h * 0.80f)
            cubicTo(w * 0.76f, h * 0.80f, w * 0.80f, h * 0.76f, w * 0.80f, h * 0.72f)
            lineTo(w * 0.72f, h * 0.56f)
            cubicTo(w * 0.70f, h * 0.52f, w * 0.64f, h * 0.50f, w * 0.60f, h * 0.54f)
            lineTo(w * 0.54f, h * 0.60f)
            cubicTo(w * 0.44f, h * 0.54f, w * 0.40f, h * 0.48f, w * 0.38f, h * 0.40f)
            lineTo(w * 0.44f, h * 0.36f)
            cubicTo(w * 0.48f, h * 0.32f, w * 0.48f, h * 0.26f, w * 0.44f, h * 0.22f)
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
    }
}

@Composable
fun PlayVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Outer circle
        drawCircle(
            color = tint,
            radius = w * 0.44f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = w * 0.08f)
        )

        // Triangle
        val triPath = Path().apply {
            moveTo(w * 0.42f, h * 0.32f)
            lineTo(w * 0.68f, h * 0.50f)
            lineTo(w * 0.42f, h * 0.68f)
            close()
        }
        drawPath(
            path = triPath,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
    }
}

@Composable
fun GlobeVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = tint,
            radius = w * 0.42f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = w * 0.08f)
        )

        drawLine(
            color = tint,
            start = Offset(w * 0.08f, h * 0.5f),
            end = Offset(w * 0.92f, h * 0.5f),
            strokeWidth = w * 0.07f
        )

        // Vertical ellipse
        drawOval(
            color = tint,
            topLeft = Offset(w * 0.30f, h * 0.08f),
            size = Size(w * 0.40f, h * 0.84f),
            style = Stroke(width = w * 0.07f)
        )
    }
}

@Composable
fun HelpQuestionVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = tint,
            radius = w * 0.42f,
            center = Offset(w * 0.5f, h * 0.5f),
            style = Stroke(width = w * 0.08f)
        )

        // Question mark top curve
        val qPath = Path().apply {
            moveTo(w * 0.36f, h * 0.36f)
            cubicTo(w * 0.36f, h * 0.24f, w * 0.64f, h * 0.24f, w * 0.64f, h * 0.38f)
            cubicTo(w * 0.64f, h * 0.48f, w * 0.50f, h * 0.50f, w * 0.50f, h * 0.60f)
        }
        drawPath(
            path = qPath,
            color = tint,
            style = Stroke(width = w * 0.09f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )

        // Dot
        drawCircle(
            color = tint,
            radius = w * 0.05f,
            center = Offset(w * 0.5f, h * 0.72f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
    }
}

@Composable
fun CommercialTruckHeaderGraphic(
    modifier: Modifier = Modifier.size(110.dp, 80.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Truck Cab (3D Perspective Left to Right)
        val cabBody = Path().apply {
            moveTo(w * 0.48f, h * 0.15f)
            lineTo(w * 0.88f, h * 0.15f)
            cubicTo(w * 0.94f, h * 0.15f, w * 0.98f, h * 0.20f, w * 0.98f, h * 0.28f)
            lineTo(w * 0.98f, h * 0.72f)
            lineTo(w * 0.45f, h * 0.72f)
            lineTo(w * 0.45f, h * 0.18f)
            close()
        }
        drawPath(
            path = cabBody,
            color = Color(0xFF1E88E5), // Blue primary cab
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        // Cab highlight / roof
        val cabRoof = Path().apply {
            moveTo(w * 0.48f, h * 0.15f)
            lineTo(w * 0.88f, h * 0.15f)
            lineTo(w * 0.92f, h * 0.22f)
            lineTo(w * 0.45f, h * 0.22f)
            close()
        }
        drawPath(
            path = cabRoof,
            color = Color(0xFF42A5F5),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        // Windshield
        val windshield = Path().apply {
            moveTo(w * 0.66f, h * 0.24f)
            lineTo(w * 0.95f, h * 0.24f)
            lineTo(w * 0.95f, h * 0.44f)
            lineTo(w * 0.66f, h * 0.44f)
            close()
        }
        drawPath(
            path = windshield,
            color = Color(0xFF0D47A1), // Dark glass
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        // Windshield reflection
        drawLine(
            color = Color(0x80FFFFFF),
            start = Offset(w * 0.88f, h * 0.26f),
            end = Offset(w * 0.74f, h * 0.42f),
            strokeWidth = w * 0.02f
        )

        // Front Grille
        drawRoundRect(
            color = Color(0xFFE0E0E0),
            topLeft = Offset(w * 0.86f, h * 0.50f),
            size = Size(w * 0.12f, h * 0.18f),
            cornerRadius = CornerRadius(w * 0.02f, w * 0.02f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
        // Grille slats
        for (i in 0..2) {
            drawLine(
                color = Color(0xFF424242),
                start = Offset(w * 0.88f, h * (0.54f + i * 0.05f)),
                end = Offset(w * 0.96f, h * (0.54f + i * 0.05f)),
                strokeWidth = w * 0.015f
            )
        }

        // Headlights
        drawRoundRect(
            color = Color(0xFFFFF9C4),
            topLeft = Offset(w * 0.92f, h * 0.66f),
            size = Size(w * 0.06f, h * 0.05f),
            cornerRadius = CornerRadius(w * 0.01f, w * 0.01f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        // Trailer / Container Box (Left side behind cab)
        val trailerBody = Path().apply {
            moveTo(w * 0.05f, h * 0.05f)
            lineTo(w * 0.50f, h * 0.05f)
            lineTo(w * 0.50f, h * 0.72f)
            lineTo(w * 0.05f, h * 0.72f)
            close()
        }
        drawPath(
            path = trailerBody,
            color = Color(0xFF1565C0), // Darker blue container
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        // Container vertical ribs
        for (i in 1..4) {
            val rx = w * (0.05f + i * 0.09f)
            drawLine(
                color = Color(0xFF1976D2),
                start = Offset(rx, h * 0.08f),
                end = Offset(rx, h * 0.68f),
                strokeWidth = w * 0.018f
            )
        }

        // Bumper
        drawRoundRect(
            color = Color(0xFF37474F),
            topLeft = Offset(w * 0.80f, h * 0.70f),
            size = Size(w * 0.19f, h * 0.07f),
            cornerRadius = CornerRadius(w * 0.01f, w * 0.01f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        // Wheels
        // Front Wheel
        drawCircle(
            color = Color(0xFF212121),
            radius = w * 0.08f,
            center = Offset(w * 0.80f, h * 0.76f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
        drawCircle(
            color = Color(0xFFBDBDBD),
            radius = w * 0.04f,
            center = Offset(w * 0.80f, h * 0.76f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        // Rear Wheels (Double)
        drawCircle(
            color = Color(0xFF212121),
            radius = w * 0.08f,
            center = Offset(w * 0.35f, h * 0.76f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
        drawCircle(
            color = Color(0xFFBDBDBD),
            radius = w * 0.04f,
            center = Offset(w * 0.35f, h * 0.76f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )

        drawCircle(
            color = Color(0xFF212121),
            radius = w * 0.08f,
            center = Offset(w * 0.18f, h * 0.76f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
        drawCircle(
            color = Color(0xFFBDBDBD),
            radius = w * 0.04f,
            center = Offset(w * 0.18f, h * 0.76f),
            style = androidx.compose.ui.graphics.drawscope.Fill
        )
    }
}

