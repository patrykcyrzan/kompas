package dev.cyrzan.skeleton.feature.chat.impl

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** The chat's own icon: a speech bubble with three dots. Tinted by `Icon`, so the fill color does not matter. */
internal val ChatIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Chat",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = addPathNodes(
            // Bubble with a tail at the bottom left, the dots cut out (even-odd).
            "M20,2H4C2.9,2 2,2.9 2,4V22L6,18H20C21.1,18 22,17.1 22,16V4C22,2.9 21.1,2 20,2Z" +
                "M7,11.5A1.5,1.5 0,1 1,7,8.5A1.5,1.5 0,1 1,7,11.5Z" +
                "M12,11.5A1.5,1.5 0,1 1,12,8.5A1.5,1.5 0,1 1,12,11.5Z" +
                "M17,11.5A1.5,1.5 0,1 1,17,8.5A1.5,1.5 0,1 1,17,11.5Z",
        ),
        fill = SolidColor(Color.Black),
        pathFillType = PathFillType.EvenOdd,
    ).build()
}
