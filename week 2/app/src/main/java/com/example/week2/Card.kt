package com.example.week2

data class Card(
    val rank: String,
    val suit: String,
    var flip: Boolean = true
) {
    fun flip() {
        flip = !flip
    }

    fun printDetails() {
        if (flip) {
            println("$rank $suit")
        } else {
            println("----")
        }
    }
}


