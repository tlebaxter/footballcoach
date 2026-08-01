/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package achijones.footballcoach.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Outlined.Leaderboard: ImageVector
    get() {
        if (_outlinedLeaderboard != null) {
            return _outlinedLeaderboard!!
        }
        _outlinedLeaderboard = materialIcon(name = "Outlined.Leaderboard") {
            materialPath {
                moveTo(16.0f, 11.0f)
                verticalLineTo(3.0f)
                horizontalLineTo(8.0f)
                verticalLineToRelative(6.0f)
                horizontalLineTo(2.0f)
                verticalLineToRelative(12.0f)
                horizontalLineToRelative(20.0f)
                verticalLineTo(11.0f)
                horizontalLineTo(16.0f)
                close()
                moveTo(10.0f, 5.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(14.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineTo(5.0f)
                close()
                moveTo(4.0f, 11.0f)
                horizontalLineToRelative(4.0f)
                verticalLineToRelative(8.0f)
                horizontalLineTo(4.0f)
                verticalLineTo(11.0f)
                close()
                moveTo(20.0f, 19.0f)
                horizontalLineToRelative(-4.0f)
                verticalLineToRelative(-6.0f)
                horizontalLineToRelative(4.0f)
                verticalLineTo(19.0f)
                close()
            }
        }
        return _outlinedLeaderboard!!
    }

private var _outlinedLeaderboard: ImageVector? = null
