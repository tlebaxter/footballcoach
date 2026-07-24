package achijones.footballcoach.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.unit.Velocity

/**
 * Consumes leftover fling velocity so nested scrollables inside a
 * [androidx.compose.material3.ModalBottomSheet] do not kick the sheet's
 * spring (visible as up/down shake at scroll bounds).
 *
 * Does not touch [NestedScrollConnection.onPostScroll], so drag-to-dismiss
 * from the top of a list still works.
 */
@Composable
fun rememberSheetFlingBlocker(): NestedScrollConnection = remember {
    object : NestedScrollConnection {
        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity = available
    }
}
