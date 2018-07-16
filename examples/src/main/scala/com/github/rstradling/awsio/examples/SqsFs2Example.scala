package com.github.rstradling.awsio.examples

import cats.effect.IO
import com.github.rstradling.awsio.sqs.QueueOps
import com.github.rstradling.awsio.sqs.MessageOps
import com.github.rstradling.awsio.sqs.QueueOpsAwsImpl
import com.github.rstradling.awsio.sqs.MessageOpsAwsImpl
import com.github.rstradling.awsio.sqs.AckProcessor
import com.github.rstradling.awsio.sqs.ReceiveLoop
import com.github.rstradling.awsio.sqs.fs2.Fs2AckProcessor
import com.github.rstradling.awsio.sqs.fs2.Fs2ReceiveLoop
import scala.concurrent.duration._
import software.amazon.awssdk.services.sqs.SQSAsyncClient
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest
import software.amazon.awssdk.services.sqs.model.DeleteQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.Message

object SqsFs2Example extends App {
  import com.strad.awsio.util.Transformations._
  val builder = SQSAsyncClient.builder().build
  val sqs: QueueOps[IO] = new QueueOpsAwsImpl[IO](builder)
  val message: MessageOps[IO] = new MessageOpsAwsImpl[IO](builder)

  def publish(implicit processAndAck: AckProcessor[IO, Message, Unit, fs2.Stream],
              receiveLoop:ReceiveLoop[IO, Message, fs2.Stream]): Option[Unit] = {
    val qName = "strad-test-queue"
    val createReq = CreateQueueRequest.builder.queueName(qName).build
    val urlRequest = GetQueueUrlRequest.builder.queueName(qName).build
    val res = for {
      createdResp <- sqs.create(createReq)
      urlResp <- sqs.getUrl(urlRequest)
      deleteRequest = DeleteQueueRequest.builder.queueUrl(urlResp.queueUrl()).build()
      messageRequest = ReceiveMessageRequest.builder().queueUrl(urlResp.queueUrl()).build
      sendMessageRequest = SendMessageRequest.builder().queueUrl(urlResp.queueUrl())
        .messageBody("MyBody")
        .build
      pubMsg <- message.send(sendMessageRequest)
      acker = processAndAck.processAndAck(builder, message, urlResp.queueUrl(), messageRequest, {m : Message =>
        println(m)
        Right(()): Either[Throwable, Unit]
      })
    } yield (())
    res.unsafeRunTimed(10.seconds)

  }
  val res = publish(Fs2AckProcessor.ackProcessor, Fs2ReceiveLoop.receiveLoop)
  res

}
