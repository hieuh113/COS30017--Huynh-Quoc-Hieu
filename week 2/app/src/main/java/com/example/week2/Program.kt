package com.example.week2

fun main() {
    val card = Card(rank = "ACE", suit = "HEARTS")
    card.printDetails()
    card.flip()
    card.printDetails()
}


