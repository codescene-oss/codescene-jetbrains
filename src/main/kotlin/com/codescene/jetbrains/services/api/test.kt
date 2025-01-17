package com.codescene.jetbrains.services.api

fun testing(a: String, b: String, c: String, d: String, e: String) {
 
}

//fun testing2(a: String, b: String, c: String, d: String, e: String) {
//
//}

fun testing23(a: String, b: String, c: String, d: String, e: String, list:List<Int>, map:Map<String, String>) {
    if ((list.isNotEmpty() && list[0] > 10) ||
        (map.containsKey("key1") && map["key1"]?.length ?: 0 > 5)) {
        println("Complex condition is satisfied")
    } else {
        println("Condition not satisfied")
    }
}

fun testing2r3(a: String, b: String, c: String, d: String, e: String, list:List<Int>, map:Map<String, String>) {
    if ((list.isNotEmpty() && list[0] > 10) ||
        (map.containsKey("key1") && map["key1"]?.length ?: 0 > 5)) {
        println("Complex condition is satisfied")
    } else {
        println("Condition not satisfied")
    }
}