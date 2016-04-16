package co.triptailor

package object sandbox {

  implicit class TraversableExtensions[A](val xs: Traversable[A]) extends AnyVal {
    def sumBy[B](f: A => B)(implicit num: Numeric[B]): B = {
      var sum = num.zero
      for (x <- xs) sum = num.plus(sum, f(x))
      sum
    }
  }

}