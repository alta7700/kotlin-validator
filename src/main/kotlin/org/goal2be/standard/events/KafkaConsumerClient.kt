@file:Suppress("UNUSED")
package org.goal2be.standard.events

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.goal2be.standard.events.topics.ITopicData
import org.goal2be.standard.events.topics.Topic
import java.io.Closeable
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Duration
import java.util.*

typealias KafkaConsumerEventHandler<T> = suspend (T & Any, ConsumerRecord<String, String>) -> Unit

class KafkaConsumerClient (
    private val client: KafkaConsumer<String, String>,
    private val topics: Map<Topic<*>, KafkaConsumerEventHandler<*>>
): Closeable {
    companion object {
        fun create(configPath: String? = null, build: KafkaConsumerClientBuilder.() -> Unit): KafkaConsumerClient {
            return KafkaConsumerClientBuilder(configPath).also { it.build() }.toConsumer()
        }
    }

    init {
        client.subscribe(topics.map { it.key.name })
    }

    fun runForever(duration: Duration = Duration.ofMillis(100)) {
        while(true) {
            runOnce(duration)
        }
    }

    fun runCount(count: Int, duration: Duration = Duration.ofMillis(100)) {
        for (i in 0..count) {
            runOnce(duration)
        }
    }
    fun runOnce(duration: Duration = Duration.ofMillis(100)) {
        val records = client.poll(duration)
        records.forEach { record ->
            val (topic, handler) = topics.entries.find { it.key.name == record.topic() }!!
            val value = Json.decodeFromString(topic.serializer, record.value())
            @Suppress("UNCHECKED_CAST")
            handler as KafkaConsumerEventHandler<Any>
            runBlocking { handler(value, record) }
        }
    }

    override fun close() = client.close()

}

class KafkaConsumerClientBuilder(configPath: String? = null) {
    private val topics = mutableMapOf<Topic<*>, KafkaConsumerEventHandler<*>>()

    private val consumerConfig = Properties()
    init {
        if (configPath !== null) {
            if (!Files.exists(Paths.get(configPath))) {
                throw IOException("File $configPath does not exist.")
            }
            consumerConfig.load(FileInputStream(configPath))
        }
    }

    fun <T: ITopicData<T>> on(topic: Topic<T>, handler: KafkaConsumerEventHandler<T>) {
        topics[topic] = handler
    }

    fun config(prop: String, value: Any) {
        consumerConfig[prop] = value
    }

    fun toConsumer(): KafkaConsumerClient {
        return KafkaConsumerClient(KafkaConsumer<String, String>(consumerConfig), topics.toMap())
    }

}
