package com.example.shelf.core.ui.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocalLibrary
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Every icon the app uses, so feature modules do not depend on the (large) extended icon set
 * and the visual language stays consistent.
 */
object ShelfIcons {
    val Add: ImageVector = Icons.Filled.Add
    val Account: ImageVector = Icons.Outlined.AccountCircle
    val Back: ImageVector = Icons.AutoMirrored.Filled.ArrowBack
    val Book: ImageVector = Icons.AutoMirrored.Outlined.MenuBook
    val Check: ImageVector = Icons.Filled.Check
    val Clear: ImageVector = Icons.Filled.Clear
    val Close: ImageVector = Icons.Filled.Close
    val Delete: ImageVector = Icons.Outlined.Delete
    val DarkMode: ImageVector = Icons.Outlined.DarkMode
    val Dropdown: ImageVector = Icons.Filled.ArrowDropDown
    val Edit: ImageVector = Icons.Outlined.Edit
    val Export: ImageVector = Icons.Outlined.Download
    val Goal: ImageVector = Icons.Outlined.Flag
    val History: ImageVector = Icons.Outlined.History
    val Keyboard: ImageVector = Icons.Outlined.Keyboard
    val LibraryFilled: ImageVector = Icons.Filled.LocalLibrary
    val LibraryOutlined: ImageVector = Icons.Outlined.LocalLibrary
    val LongestStreak: ImageVector = Icons.Outlined.EmojiEvents
    val Notes: ImageVector = Icons.AutoMirrored.Outlined.Notes
    val Notifications: ImageVector = Icons.Outlined.Notifications
    val Offline: ImageVector = Icons.Outlined.CloudOff
    val Palette: ImageVector = Icons.Outlined.Palette
    val Scan: ImageVector = Icons.Outlined.QrCodeScanner
    val SearchFilled: ImageVector = Icons.Filled.Search
    val SearchOutlined: ImageVector = Icons.Outlined.Search
    val SettingsFilled: ImageVector = Icons.Filled.Settings
    val SettingsOutlined: ImageVector = Icons.Outlined.Settings
    val SignOut: ImageVector = Icons.AutoMirrored.Outlined.Logout
    val StarFilled: ImageVector = Icons.Filled.Star
    val StarOutlined: ImageVector = Icons.Outlined.StarOutline
    val StatsFilled: ImageVector = Icons.Filled.BarChart
    val StatsOutlined: ImageVector = Icons.Outlined.BarChart
    val Streak: ImageVector = Icons.Outlined.LocalFireDepartment
    val Sync: ImageVector = Icons.Outlined.CloudSync
    val Timer: ImageVector = Icons.Outlined.Timer
    val TorchOff: ImageVector = Icons.Filled.FlashOff
    val TorchOn: ImageVector = Icons.Filled.FlashOn
}
