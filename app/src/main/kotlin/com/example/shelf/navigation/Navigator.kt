package com.example.shelf.navigation

import androidx.navigation3.runtime.NavKey

/** All navigation goes through here, so the rules live in one tested place. */
class Navigator(private val state: NavigationState) {

    /** Selects a tab when [key] is a tab root, otherwise pushes [key] on the selected tab. */
    fun navigate(key: NavKey) {
        val destination = TopLevelDestination.fromKey(key)
        if (destination != null) {
            select(destination)
        } else {
            state.currentBackStack.add(key)
        }
    }

    /** Selecting the tab that is already selected pops it back to its root. */
    fun select(destination: TopLevelDestination) {
        if (destination == state.currentTopLevel) {
            val stack = state.currentBackStack
            while (stack.size > 1) stack.removeAt(stack.lastIndex)
        } else {
            state.currentTopLevel = destination
        }
    }

    /** Replaces the top screen, for example scanner -> book detail, so Back skips the scanner. */
    fun replaceCurrent(key: NavKey) {
        val stack = state.currentBackStack
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
        stack.add(key)
    }

    fun goBack() {
        val stack = state.currentBackStack
        when {
            stack.size > 1 -> stack.removeAt(stack.lastIndex)
            state.currentTopLevel != TopLevelDestination.START -> state.currentTopLevel = TopLevelDestination.START
            // At the root of the start tab there is nothing to pop; NavDisplay lets the system finish.
        }
    }

    /** Shows a deep link target: selects its tab and replaces that tab's stack. */
    fun open(target: DeepLinkTarget) {
        val stack = state.backStacks.getValue(target.topLevel)
        stack.clear()
        stack.addAll(target.backStack)
        state.currentTopLevel = target.topLevel
    }
}
