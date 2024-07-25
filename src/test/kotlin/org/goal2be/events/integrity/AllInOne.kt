package org.goal2be.events.integrity

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.goal2be.standard.events.KafkaConsumerClient
import org.goal2be.standard.events.KafkaProducerClient
//import org.goal2be.standard.events.topics.UserCreated
//import org.goal2be.standard.events.topics.UserCreatedTopic
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

//class AllInOne {
//    val userCreatedData1 = UserCreated(
//        userId = UUID.randomUUID(),
//        email = "tascan@gmail.com",
//        username = "tascan"
//    )
//    @Test
//    fun send() {
//        val producer = KafkaProducerClient.create("./src/test/resources/kafka.servers")
//        producer.send(userCreatedData1.userId.toString(), userCreatedData1)
//            .onSuccess { assertEquals(it.topic(), userCreatedData1.getTopic().name) }
//            .onFailure { throw it }
//
//
//        var fetched = false
//        val consumer = KafkaConsumerClient.create("./src/test/resources/kafka.servers") {
//            config(ConsumerConfig.GROUP_ID_CONFIG, "cool-consumer-group")
//            config(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
//            on(UserCreatedTopic) {
//                fetched = true
//                assertEquals(userCreatedData1, it)
//            }
//        }
//        consumer.runForever()
//        assertTrue(fetched)
//    }
//
//}