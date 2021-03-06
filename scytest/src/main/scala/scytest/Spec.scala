package scytest

import java.util.UUID

import cats.MonadError
import cats.data.NonEmptyChain
import scytest.fixture.Fixture

abstract class Spec[F[_]](
    implicit F: MonadError[F, Throwable]
) extends SpecMethods[F]
    with AssertionMethods {
  def name: String = this.getClass.getSimpleName
  protected def tests: List[Test[F]]

  lazy val toSuite =
    new SingleSuite[F](
      Suite.Id(s"$name - ${newId()}"),
      NonEmptyChain
        .fromSeq(tests.map(t => Test.Id(s"${t.name} - ${newId()}") -> t))
        .getOrElse(
          NonEmptyChain
            .one(Test.Id(newId()) -> Test.pass[F](s"Empty suite $name"))
        )
    )

  protected final def test(name: String)(body: => F[Assertion]): Test[F] =
    new FixturelessTest[F](name, body)

  protected final def testWith[R](
      name: String,
      fix: Fixture[F, R]
  )(
      body: R => F[Assertion]
  ): Test[F] =
    new FixtureTest(name, fix)(body)

  protected final implicit def pureAssertion(
      assertion: Assertion
  ): F[Assertion] =
    F.pure(assertion)

  private def newId() = UUID.randomUUID().toString
}

trait SpecMethods[F[_]] {
  protected def all(tests: Test[F]*): List[Test[F]] = tests.toList
}

trait AssertionMethods {
  // TODO source code position macro, replace with expecty, etc
  def assert(cond: Boolean): Assertion =
    if (cond) Verified else FailedAssertion(new AssertionError())
}
