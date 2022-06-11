package scalaql.csv

trait CsvDecoder[A] { self =>

  @throws[IllegalArgumentException]
  def read(value: Map[String, String]): A

  def map[B](f: A => B): CsvDecoder[B] = new CsvDecoder[B] {
    override def read(value: Map[String, String]): B = f(self.read(value))
  }
}
