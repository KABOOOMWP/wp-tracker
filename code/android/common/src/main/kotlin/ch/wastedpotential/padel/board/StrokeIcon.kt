package ch.wastedpotential.padel.board

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private const val GRID = 24f

/** A white line icon on a 24×24 grid, [lineWidth] in grid units; tint it with ColorFilter.tint. */
fun strokeIcon(name: String, lineWidth: Float = 2f, pathData: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(name, GRID.dp, GRID.dp, GRID, GRID).apply {
        path(
            stroke = SolidColor(Color.White),
            strokeLineWidth = lineWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathData,
        )
    }.build()
