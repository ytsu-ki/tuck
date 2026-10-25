package com.example.starbox.ui.main

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.starbox.LocalAppContainer
import com.example.starbox.R
import com.example.starbox.system.PermissionState
import com.example.starbox.ui.inbox.InboxScreen
import com.example.starbox.ui.memos.MemoEditorScreen
import com.example.starbox.ui.memos.MemoEditorViewModel
import com.example.starbox.ui.memos.MemosScreen
import com.example.starbox.ui.rules.RuleDetailScreen
import com.example.starbox.ui.rules.RuleWizardScreen
import com.example.starbox.ui.rules.RulesScreen
import com.example.starbox.ui.settings.SettingsScreen
import com.example.starbox.ui.theme.StarBoxTheme

private enum class Tab(val route: String, @param:StringRes val label: Int, val icon: ImageVector) {
    Inbox("inbox", R.string.tab_inbox, Icons.Filled.Star),
    Memos("memos", R.string.tab_memos, Icons.Filled.Edit),
    Rules("rules", R.string.tab_rules, Icons.AutoMirrored.Filled.List),
    Settings("settings", R.string.tab_settings, Icons.Filled.Settings),
}

private const val RULE_DETAIL_ROUTE = "rule/{id}"
private fun ruleDetailRoute(id: Long) = "rule/$id"
private const val RULE_WIZARD_ROUTE = "rules/new"

private const val MEMO_EDITOR_ROUTE = "memo/{id}?fromMessage={fromMessage}&todo={todo}"
private fun memoEditorRoute(id: Long, fromMessage: Long = 0, todo: Boolean = false) =
    "memo/$id?fromMessage=$fromMessage&todo=$todo"

@Composable
fun MainScaffold(permissions: PermissionState, openMessageId: Long?, onOpenMessageHandled: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val container = LocalAppContainer.current
    val unreadFlow = remember(container) { container.database.messageDao().observeUnreadCount() }
    val unread by unreadFlow.collectAsStateWithLifecycle(initialValue = 0)
    val currentRoute = backStackEntry?.destination?.route
    val onTabScreen = currentRoute == null || Tab.entries.any { it.route == currentRoute }

    fun openTab(tab: Tab) {
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    /** Pops a detail screen once; a double tap on Back or Save must not pop the tab under it. */
    fun closeDetail(route: String) {
        if (navController.currentBackStackEntry?.destination?.route == route) navController.popBackStack()
    }

    // Tapping a StarBox alert brings the inbox forward wherever the user was.
    LaunchedEffect(openMessageId) {
        if (openMessageId != null && currentRoute != Tab.Inbox.route) openTab(Tab.Inbox)
    }

    Scaffold(
        bottomBar = {
            // Detail screens such as the rule editor are full screen; the bar slides away for them.
            AnimatedVisibility(
                visible = onTabScreen,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                Column {
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        Tab.entries.forEach { tab ->
                            val selected = backStackEntry?.destination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { openTab(tab) },
                                icon = {
                                    if (tab == Tab.Inbox && unread > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = StarBoxTheme.star.star,
                                                    contentColor = StarBoxTheme.star.onStar,
                                                ) { Text(if (unread > 99) "99+" else unread.toString()) }
                                            },
                                        ) {
                                            Icon(tab.icon, contentDescription = null)
                                        }
                                    } else {
                                        Icon(tab.icon, contentDescription = null)
                                    }
                                },
                                label = { Text(stringResource(tab.label)) },
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Tab.Inbox.route,
            // Quick, quiet crossfades instead of the slow default.
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(160)) },
        ) {
            composable(Tab.Inbox.route) {
                InboxScreen(
                    contentPadding = padding,
                    permissions = permissions,
                    openMessageId = openMessageId,
                    onOpenMessageHandled = onOpenMessageHandled,
                    onOpenSettings = { openTab(Tab.Settings) },
                    onSaveAsMemo = { messageId ->
                        navController.navigate(memoEditorRoute(MemoEditorViewModel.NEW_MEMO_ID, fromMessage = messageId))
                    },
                )
            }
            composable(Tab.Memos.route) {
                MemosScreen(
                    contentPadding = padding,
                    onOpenMemo = { id -> navController.navigate(memoEditorRoute(id)) },
                    onNewMemo = { asTodo ->
                        navController.navigate(memoEditorRoute(MemoEditorViewModel.NEW_MEMO_ID, todo = asTodo))
                    },
                )
            }
            composable(Tab.Rules.route) {
                RulesScreen(
                    contentPadding = padding,
                    onOpenRule = { id -> navController.navigate(ruleDetailRoute(id)) },
                    onNewRule = { navController.navigate(RULE_WIZARD_ROUTE) },
                )
            }
            composable(Tab.Settings.route) { SettingsScreen(contentPadding = padding, permissions = permissions) }
            composable(
                RULE_DETAIL_ROUTE,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                RuleDetailScreen(
                    ruleId = entry.arguments?.getLong("id") ?: 0L,
                    onClose = { closeDetail(RULE_DETAIL_ROUTE) },
                    // Duplicate: swap this page for the copy's page.
                    onOpenRule = { id ->
                        navController.navigate(ruleDetailRoute(id)) { popUpTo(RULE_DETAIL_ROUTE) { inclusive = true } }
                    },
                )
            }
            composable(RULE_WIZARD_ROUTE) {
                RuleWizardScreen(
                    onClose = { closeDetail(RULE_WIZARD_ROUTE) },
                    // Land on the new rule's page; Back from there returns to the list, not the wizard.
                    onCreated = { id ->
                        navController.navigate(ruleDetailRoute(id)) { popUpTo(RULE_WIZARD_ROUTE) { inclusive = true } }
                    },
                )
            }
            composable(
                MEMO_EDITOR_ROUTE,
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("fromMessage") {
                        type = NavType.LongType
                        defaultValue = 0L
                    },
                    navArgument("todo") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                ),
            ) { entry ->
                MemoEditorScreen(
                    memoId = entry.arguments?.getLong("id") ?: MemoEditorViewModel.NEW_MEMO_ID,
                    fromMessageId = entry.arguments?.getLong("fromMessage") ?: 0L,
                    initialTodo = entry.arguments?.getBoolean("todo") ?: false,
                    onClose = { closeDetail(MEMO_EDITOR_ROUTE) },
                )
            }
        }
    }
}
