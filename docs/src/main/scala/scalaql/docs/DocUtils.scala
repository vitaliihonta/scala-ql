package scalaql.docs

import java.nio.file.{Files, Path}

object DocUtils {
  def printFile(file: Path): Unit =
    println(new String(Files.readAllBytes(file)))
}
