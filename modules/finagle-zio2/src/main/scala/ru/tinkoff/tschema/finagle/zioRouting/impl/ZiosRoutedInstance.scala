package ru.tinkoff.tschema.finagle.zioRouting.impl

import cats.Monad
import cats.syntax.monoid._
import com.twitter.finagle.http
import ru.tinkoff.tschema.finagle.zioRouting.{Fail, ZIOH, ZRouting, ZioRouting}
import ru.tinkoff.tschema.finagle.{Rejection, Routed, RoutedPlus}
import zio.{Tag, ZEnvironment, ZIO}

private[zioRouting] class ZiosRoutedInstance[R: Tag, E] extends RoutedPlus[ZIOH[R, E, *]] {
  private type F[a] = ZIOH[R, E, a]
  implicit private[this] val self: RoutedPlus[F] = this
  implicit private[this] val monad: Monad[F]     = zio.interop.catz.monadErrorInstance

  def matched: F[Int] = ZIO.serviceWith[ZRouting](_.matched)

  def withMatched[A](m: Int, fa: F[A]): F[A] =
    fa.provideSomeEnvironment[ZRouting with R](r => (r add r.get[ZioRouting[Any]]))

  def path: F[CharSequence]                 = ZIO.serviceWith[ZRouting](_.path)
  def request: F[http.Request]              = ZIO.serviceWith[ZRouting](_.request)
  def reject[A](rejection: Rejection): F[A] =
    Routed.unmatchedPath[F].flatMap(path => throwRej(rejection withPath path.toString))

  def combineK[A](x: F[A], y: F[A]): F[A] =
    catchRej(x)(xrs => catchRej(y)(yrs => throwRej(xrs |+| yrs)))

  @inline private[this] def catchRej[A](z: F[A])(f: Rejection => F[A]): F[A] =
    z.catchSome { case Fail.Rejected(xrs) => f(xrs) }

  @inline private[this] def throwRej[A](map: Rejection): F[A] = ZIO.fail(Fail.Rejected(map))
}
