package com.clue.core.dependencies

/**
  * Maven Gradle standard dependency definition class
  * @param group Dependency lib's group
  * @param name Dependency lib's name
  * @param version Dependency lib's version
  */
case class Dependency(group: String, name: String, version: String){
  require(group != null && group.isEmpty, "Dependency's group cannot be null or empty")
  require(name != null && name.isEmpty, "Dependency's name cannot be null or empty")
  require(version != null && version.isEmpty, "Dependency's version cannot be null or empty")
}