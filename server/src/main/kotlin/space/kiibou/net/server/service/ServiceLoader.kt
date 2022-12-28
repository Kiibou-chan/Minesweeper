package space.kiibou.net.server.service

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.decodeFromStream
import mu.KotlinLogging
import space.kiibou.annotations.Inject
import space.kiibou.annotations.meta.ServiceLoadInfo
import space.kiibou.net.common.Serial
import space.kiibou.net.reflect.ReflectUtils
import space.kiibou.net.server.Server
import space.kiibou.net.server.Service
import java.util.*

private val logger = KotlinLogging.logger { }

class ServiceLoader(private val server: Server, serviceNames: Array<out String>) {

    private val services: MutableList<Service> = Collections.synchronizedList(ArrayList())
    private val servicesMap: MutableMap<String, Service> = Collections.synchronizedMap(HashMap())

    init {
        for (name in serviceNames) {
            registerService(name)
        }

        for (serviceInfo in getAutoLoadedServices()) {
            registerService(serviceInfo.serviceName)
        }

        for (service in services) {
            injectServices(service)
        }

        for (service in services) {
            service.initialize()
        }
    }

    private fun registerService(name: String) {
        val service = ReflectUtils.createInstance<Service>(name, arrayOf(Server::class.java), server)
        services.add(service)
        servicesMap[name] = service

        logger.info { "Loaded Service $name" }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun getAutoLoadedServices(): Set<ServiceLoadInfo> {
        val classLoader = this::class.java.classLoader

        val resources = classLoader.getResources("META-INF/server/services/Services.json").toList()

        val serviceInfos = mutableSetOf<ServiceLoadInfo>()

        resources.forEach {
            val stream = it.openStream() ?: return@forEach

            serviceInfos.addAll(Serial.json.decodeFromStream<Set<ServiceLoadInfo>>(stream))
        }

        return serviceInfos
    }

    private fun injectServices(service: Service) {
        val fields = ReflectUtils.getAnnotatedFields(service, Inject::class.java)
        for (field in fields) {
            val name = field.type.canonicalName
            val toInject = servicesMap[name]

            logger.info { "Injecting $name into ${service::class.java.canonicalName}" }

            field[service] = toInject
        }
    }

}