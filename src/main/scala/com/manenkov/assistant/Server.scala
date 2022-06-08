package com.manenkov.assistant

import cats.effect.{Blocker, ConcurrentEffect, ContextShift, ExitCode, IO, IOApp, Resource, Timer}
import com.manenkov.assistant.config.{AssistantConfig, DatabaseConfig}
import com.manenkov.assistant.domain.trello.TrelloService
import com.manenkov.assistant.infrastructure.endpoint.TrelloEndpoints
import com.manenkov.assistant.infrastructure.repository.DoobieTrelloRepositoryInterpreter
import com.typesafe.config.ConfigFactory
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.server.{Router, Server => H4Server}
import io.circe.config.parser
import org.http4s.implicits._
import doobie._

import java.io.File

object Server extends IOApp {
  def createServer[F[_] : ContextShift : ConcurrentEffect : Timer]: Resource[F, H4Server[F]] =
    for {
      conf <- Resource.eval(parser.decodePathF[F, AssistantConfig](ConfigFactory.parseFile(new File(System.getProperty("assistant.config"))), "assistant"))
      serverEc <- ExecutionContexts.cachedThreadPool[F]
      connEc <- ExecutionContexts.fixedThreadPool[F](conf.db.connections.poolSize)
      txnEc <- ExecutionContexts.cachedThreadPool[F]
      xa <- DatabaseConfig.dbTransactor(conf.db, connEc, Blocker.liftExecutionContext(txnEc))
      trelloRepo = DoobieTrelloRepositoryInterpreter[F](xa)
      trelloService = TrelloService[F](trelloRepo, conf)
      httpApp = Router(
        "/api/trello" -> TrelloEndpoints.endpoints[F](trelloService)
      ).orNotFound
      _ <- Resource.eval(DatabaseConfig.initializeDb(conf.db))
      server <- BlazeServerBuilder[F](serverEc)
        .bindHttp(conf.server.port, conf.server.host)
        .withHttpApp(Logger.httpApp(logHeaders = true, logBody = true)(httpApp))
        .resource
    } yield server

  def run(args: List[String]): IO[ExitCode] = createServer.use(_ => IO.never).as(ExitCode.Success)
}
