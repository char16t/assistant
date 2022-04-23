package com.manenkov.assistant.domain.trello

trait TrelloRepositoryAlgebra[F[_]] {
  def hello(): F[EntityModel]
}
