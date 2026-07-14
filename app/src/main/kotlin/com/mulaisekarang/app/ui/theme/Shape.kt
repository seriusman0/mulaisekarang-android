package com.mulaisekarang.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Corner radii mirror the prototype's rounded-[Npx] utilities: 16dp on small
// controls/category tiles, 20dp on inputs and buttons, 24dp+ on cards/banners.
val MulaiSekarangShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
