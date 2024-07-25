@file:Suppress("UNUSED")
package org.goal2be.standard.events

import kotlinx.serialization.json.Json
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.goal2be.standard.events.topics.ITopicData
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class KafkaProducerClient(
    private val client: KafkaProducer<String, String>
) {

    companion object {
        fun create(configPath: String? = null, build: (KafkaProducerClientBuilder.() -> Unit)? = null) : KafkaProducerClient {
            return KafkaProducerClientBuilder(configPath).also { build?.invoke(it) }.toProducer()
        }
    }

    fun <T: ITopicData<T>> send(key: String, data: T): Result<RecordMetadata> {
        val topic = data.getTopic()
        return try {
            Result.success(client.send(ProducerRecord(topic.name, key, Json.encodeToString(topic.serializer, data))).get())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class KafkaProducerClientBuilder(configPath: String? = null) {

    private val producerConfig = Properties()
    init {
        if (configPath !== null) {
            if (!Files.exists(Paths.get(configPath))) {
                throw IOException("File $configPath does not exist.")
            }
            producerConfig.load(FileInputStream(configPath))
        }
    }

    fun config(prop: String, value: Any) {
        producerConfig[prop] = value
    }

    fun toProducer(): KafkaProducerClient {
        return KafkaProducerClient(KafkaProducer(producerConfig))
    }
}
