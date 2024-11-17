package com.sebastianneubauer.jsontree

import com.sebastianneubauer.jsontree.JsonTreeElement.Collapsable.Array
import com.sebastianneubauer.jsontree.JsonTreeElement.Collapsable.Object
import com.sebastianneubauer.jsontree.JsonTreeElement.EndBracket
import com.sebastianneubauer.jsontree.JsonTreeElement.Primitive

// Expanding

internal fun JsonTreeElement.expand(
    expansion: Expansion,
): JsonTreeElement {
    return when (this) {
        is Array -> this.copy(
            state = TreeState.EXPANDED,
            children = when (expansion) {
                Expansion.None -> children
                Expansion.All -> children.expandChildren(singleChildrenOnly = false)
                Expansion.SingleOnly -> children.expandChildren(singleChildrenOnly = true)
            }
        )

        is Object -> this.copy(
            state = TreeState.EXPANDED,
            children = when (expansion) {
                Expansion.None -> children
                Expansion.All -> children.expandChildren(singleChildrenOnly = false)
                Expansion.SingleOnly -> children.expandChildren(singleChildrenOnly = true)
            }
        )

        is Primitive,
        is EndBracket -> this
    }
}

internal enum class Expansion {
    None,
    All,
    SingleOnly
}

private fun Map<String, JsonTreeElement>.expandChildren(
    singleChildrenOnly: Boolean
): Map<String, JsonTreeElement> {
    return if (singleChildrenOnly && this.size > 1) {
        this
    } else {
        mapValues {
            when (val child = it.value) {
                is Primitive -> child
                is EndBracket -> child
                is Array -> {
                    if (child.state == TreeState.COLLAPSED) {
                        child.copy(
                            state = TreeState.EXPANDED,
                            children = child.children.expandChildren(singleChildrenOnly)
                        )
                    } else {
                        child
                    }
                }
                is Object -> {
                    if (child.state == TreeState.COLLAPSED) {
                        child.copy(
                            state = TreeState.EXPANDED,
                            children = child.children.expandChildren(singleChildrenOnly)
                        )
                    } else {
                        child
                    }
                }
            }
        }
    }
}

// Collapsing

internal fun JsonTreeElement.collapse(): JsonTreeElement {
    return when (this) {
        is Array -> this.copy(
            state = TreeState.COLLAPSED,
            children = children.collapseChildren()
        )

        is Object -> this.copy(
            state = TreeState.COLLAPSED,
            children = children.collapseChildren()
        )

        is Primitive,
        is EndBracket -> this
    }
}

private fun Map<String, JsonTreeElement>.collapseChildren(): Map<String, JsonTreeElement> {
    return mapValues {
        when (val child = it.value) {
            is Primitive -> child
            is EndBracket -> child
            is Array -> {
                if (child.state != TreeState.COLLAPSED) {
                    child.copy(
                        state = TreeState.COLLAPSED,
                        children = child.children.collapseChildren()
                    )
                } else {
                    child
                }
            }
            is Object -> {
                if (child.state != TreeState.COLLAPSED) {
                    child.copy(
                        state = TreeState.COLLAPSED,
                        children = child.children.collapseChildren()
                    )
                } else {
                    child
                }
            }
        }
    }
}

// toList

internal fun JsonTreeElement.toList(): List<JsonTreeElement> {
    val list = mutableListOf<JsonTreeElement>()

    fun addToList(element: JsonTreeElement) {
        when (element) {
            is EndBracket -> list.add(
                element
            ) // TODO: check if this is correct //error("EndBracket in initial list creation")
            is Primitive -> list.add(element)
            is Array -> {
                list.add(element)
                if (element.state != TreeState.COLLAPSED) {
                    element.children.forEach {
                        addToList(it.value)
                    }
                    list.add(element.endBracket)
                }
            }
            is Object -> {
                list.add(element)
                if (element.state != TreeState.COLLAPSED) {
                    element.children.forEach {
                        addToList(it.value)
                    }
                    list.add(element.endBracket)
                }
            }
        }
    }

    addToList(this)
    return list
}
