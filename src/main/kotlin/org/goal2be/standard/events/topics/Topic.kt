package org.goal2be.standard.events.topics

import kotlinx.serialization.KSerializer


interface ITopicData<T: ITopicData<T>> {
    fun getTopic(): Topic<T>
}
data class Topic<T: ITopicData<T>> (
    val name: String,
    val serializer: KSerializer<T>,
)
