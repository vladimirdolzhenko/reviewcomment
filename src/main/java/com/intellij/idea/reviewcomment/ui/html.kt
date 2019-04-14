package com.intellij.idea.reviewcomment.ui

import java.util.ArrayList

open class Tag(val name: String) {
    val children: MutableList<Tag> = ArrayList()
    val attributes: MutableList<Attribute> = ArrayList()

    override fun toString(): String {
        return "<$name" +
                (if (attributes.isEmpty()) "" else attributes.joinToString(separator = "", prefix = " ")) + ">" +
                (if (children.isEmpty()) "" else children.joinToString(separator = "")) +
                "</$name>"
    }
}

class Attribute(val name : String, val value : String) {
    override fun toString() = """$name="$value" """
}

fun <T: Tag> T.set(name: String, value: String?): T {
    if (value != null) {
        attributes.add(Attribute(name, value))
    }
    return this
}

fun <T: Tag> Tag.doInit(tag: T, init: T.() -> Unit): T {
    tag.init()
    children.add(tag)
    return tag
}

class Html: Tag("html")
class Br: Tag("br")
class Hr: Tag("hr")
class Pre: Tag("pre")
class Text(val text: String): Tag("span") {
    override fun toString() = text
}

fun html(init: Html.() -> Unit): Html = Html().apply(init)
fun Html.br(init : Br.() -> Unit) = doInit(Br(), init)
fun Html.hr(init : Hr.() -> Unit) = doInit(Hr(), init)
fun Html.pre(init : Pre.() -> Unit) = doInit(Pre(), init)
fun Tag.text(s : Any?) = doInit(Text(s.toString()), {})
