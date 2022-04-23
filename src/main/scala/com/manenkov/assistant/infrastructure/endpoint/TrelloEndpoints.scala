package com.manenkov.assistant.infrastructure.endpoint

import cats.effect.{Resource, Sync}
import com.manenkov.assistant.domain.trello.{EntityModel, TrelloService, Webhook}
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s.circe.CirceEntityCodec.circeEntityDecoder
import org.http4s.circe._

class TrelloEndpoints[F[_]: Sync] extends Http4sDsl[F] {

  private def helloEndpoint(trelloService: TrelloService[F]): HttpRoutes[F] = {

    HttpRoutes.of[F] {
      case GET -> Root =>
        // Ad-hoc to fix "unused import" error
        val value = trelloService.hello()
        Ok(if (value.equals(EntityModel("AAA"))) EntityModel("aaa").asJson else EntityModel("bbb").asJson)
      case GET -> Root / "organize_cards" =>
        Resource.eval(trelloService.organizeCards()).use(_ => {
          Ok()
        })
      case HEAD -> Root / "receive_webhook" => {
        Ok()
      }
      case req @ POST -> Root / "receive_webhook" => {
        req.decode[Webhook] { webhook =>
          Resource.eval(trelloService.receiveWebhook(webhook)).use(_ => {
            Ok()
          })
        }
      }
    }
  }
  def endpoints(trelloService: TrelloService[F]): HttpRoutes[F] = {
    helloEndpoint(trelloService)
  }
}

object TrelloEndpoints {
  def endpoints[F[_]: Sync](trelloService: TrelloService[F]): HttpRoutes[F] =
    new TrelloEndpoints[F].endpoints(trelloService)
}
