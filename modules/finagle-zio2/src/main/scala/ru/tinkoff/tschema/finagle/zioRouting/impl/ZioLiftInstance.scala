package ru.tinkoff.tschema.finagle.zioRouting.impl

import ru.tinkoff.tschema.finagle.LiftHttp
import ru.tinkoff.tschema.finagle.zioRouting.{Fail, ZIOHttp, ZioRouting}
import zio.{Tag, ZEnvironment, ZIO}

private[zioRouting] class ZioLiftInstance[R: Tag, R1: Tag, E, E1](implicit eve: E1 <:< E, evr: R <:< R1)
    extends LiftHttp[ZIOHttp[R, E, *], ZIO[R1, E1, *]] {
  private type F[a] = ZIOHttp[R, E, a]
  def apply[A](fa: ZIO[R1, E1, A]): F[A] =
    fa.mapError(Fail.Other(_): Fail[E]).provideSomeEnvironment[ZioRouting[R]](env => ZEnvironment(env.get.embedded))
}