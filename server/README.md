# Central Server

This is the project for the central server for the messaging service. It handles only assigning user hashes and keeping track of their recent IPs, as well as facilitating P2P connections between clients. It's a standard Java project that doesn't use Maven or Gradle.

This should be modifiable such that anyone can create their own central server and use it separately, given that they port-forward it. In this way, there can still be some level of decentralization that allows any specific group of people to keep connections within their Mercury group private.

## Build Instructions

1. Install VSCode and add the Debugger for Java and Extension Pack for Java extensions.
2. Install Temurin Java 17 LTS.
3. Install MySQL server and create a database with the table described in [the base readme](../README.md).
4. Run the MySQL server.
5. Run App.java.
6. Enter the SQL server credentials.
