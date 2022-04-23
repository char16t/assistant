package com.manenkov.assistant.infrastructure.repository

import cats.effect.Bracket
import cats.syntax.all._
import doobie._
import com.manenkov.assistant.domain.trello.{EntityModel, TrelloRepositoryAlgebra}

class DoobieTrelloRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
  extends TrelloRepositoryAlgebra[F] { self =>

  override def hello(): F[EntityModel] = {
    EntityModel(name = "Hi!").pure[F]
  }
}

object DoobieTrelloRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): DoobieTrelloRepositoryInterpreter[F] =
    new DoobieTrelloRepositoryInterpreter(xa)
}
