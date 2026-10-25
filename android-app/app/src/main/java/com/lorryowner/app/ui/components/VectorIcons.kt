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