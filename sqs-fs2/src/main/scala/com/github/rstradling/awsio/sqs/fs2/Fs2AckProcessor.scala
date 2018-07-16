package com.github.rstradling.awsio.sqs.fs2

import cats.effect.Async
import cats.implicits._
import com.github.rstradling.awsio.sqs.ReceiveLoop
import com.github.rstradling.awsio.sqs.AckProcessor
import com.github.rstradling.awsio.sqs.MessageOps
import com.github.rstradling.awsio.sqs.MessageOps
import com.github.rstradling.awsio.sqs.AckProcessor
import software.amazon.awssdk.services.sqs.SQSAsyncClient
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest

object Fs2AckProcessor {

  def process[F[_]: Async, B](client: SQSAsyncClient,
                       messageOps: MessageOps[F],
                       queueUrl: String,
                       receiveMessageRequest: ReceiveMessageRequest,
                       handler: Message => Either[Throwable, B])(
      implicit p: AckProcessor[F, Message, B, fs2.Stream]): fs2.Stream[F, B] = {
    p.processAndAck(client,
                    messageOps,
                    queueUrl,
                    receiveMessageRequest,
                    handler)(Fs2ReceiveLoop.receiveLoop[F])
  }

  private def deleteMessage[F[_]: Async](m: Message,
                                         messageOps: MessageOps[F],
                                         queueUrl: String): F[Unit] = {
    val deleteMessageRequest = DeleteMessageRequest
      .builder()
      .queueUrl(queueUrl)
      .receiptHandle(m.receiptHandle())
      .build
    for {
      _ <- implicitly[Async[F]].delay(println(deleteMessageRequest))
      deleteResponse <- messageOps.delete(deleteMessageRequest)
      _ <- implicitly[Async[F]].delay(println(deleteResponse))
    } yield ()
  }
  implicit def ackProcessor[F[_]: Async, B]
    : AckProcessor[F, Message, B, fs2.Stream] =
    new AckProcessor[F, Message, B, fs2.Stream] {
      def processAndAck(client: SQSAsyncClient,
                        messageOps: MessageOps[F],
                        queueUrl: String,
                        receiveMessageRequest: ReceiveMessageRequest,
                        handler: Message => Either[Throwable, B])(
          implicit receiveLoop: ReceiveLoop[F, Message, fs2.Stream])
        : fs2.Stream[F, B] = {
        receiveLoop.receive(client, messageOps, receiveMessageRequest).flatMap {
          msg =>
            handler(msg).fold(
              (t => fs2.Stream.raiseError(t)), { item =>
                val ret = for {
                  del <- deleteMessage(msg, messageOps, queueUrl)
                  res <- implicitly[Async[F]].pure(item)
                } yield res
                fs2.Stream.eval(ret)
              }
            )
        }
      }
    }
}
