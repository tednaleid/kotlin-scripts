#!/usr/bin/env -S kotlinc -script --

// to run:
//     scripts/demoinclude.main.kts
//
// Demonstrates importing another script file and a that comes from that imported file

@file:Import("importme.main.kts")
import Importme_main.Detective

value = value + 1
println("value in demoinclude: $value")

println("imported detective: ${sherlock.name}")

val dirk = Detective(name = "Dirk Gently")
println ("name of detective: ${dirk.name}")
