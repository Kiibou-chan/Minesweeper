package space.kiibou.annotations

import com.google.auto.common.MoreElements
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import space.kiibou.annotations.meta.ServiceLoadInfo
import java.io.File
import java.io.IOException
import java.lang.Exception
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.StandardLocation
import kotlin.io.path.toPath

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedOptions(AutoLoadProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
@SupportedAnnotationTypes("*")
class AutoLoadProcessor : AbstractProcessor() {

    private val serviceInfos = mutableSetOf<ServiceLoadInfo>()

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val generatedSourcesRoot = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME].orEmpty()

        if (generatedSourcesRoot.isEmpty()) {
            processingEnv.messager.printMessage(
                Diagnostic.Kind.ERROR,
                "Can't find the target directory for generated Kotlin files"
            )
            return false
        }

        val elements: Set<Element> = roundEnv.getElementsAnnotatedWith(AutoLoad::class.java)

        elements.filter { it.kind != ElementKind.CLASS }.let { list ->
            list.forEach {
                processingEnv.messager.printMessage(
                    Diagnostic.Kind.ERROR,
                    "@AutoLoad can only be applied to classes",
                    it
                )
            }

            if (list.isNotEmpty()) {
                return false
            }
        }

        elements.forEach(::processElement)

        return generateMetaFile()
    }

    @Suppress("UnstableApiUsage")
    private fun processElement(element: Element) {
        processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Processing Element $element")

        val type = MoreElements.asType(element)

        val info = ServiceLoadInfo(type.qualifiedName.toString())

        serviceInfos += info
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun generateMetaFile(): Boolean {
        val filer = processingEnv.filer

        val servicesFile = "META-INF/server/services/Services.json"

        try {
            val fileObject = filer.getResource(StandardLocation.CLASS_OUTPUT, "", servicesFile)

            val oldServices = Json.decodeFromStream<List<ServiceLoadInfo>>(fileObject.openInputStream())

            serviceInfos.addAll(oldServices)
        } catch (ignored: IOException) {
            processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, "Config File did not exist yet")
        }

        try {
            val fileObject = filer.getResource(StandardLocation.CLASS_OUTPUT, "", servicesFile)

            fileObject.toUri().toPath().parent.toFile().apply {
                mkdirs()
            }.resolve("Services.json").apply {
                createNewFile()
            }

            Files.write(
                fileObject.toUri().toPath(),
                Json.encodeToString(serviceInfos).toByteArray(Charsets.UTF_8),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            )
        } catch (ex: Exception) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Could not create file, $ex")
            return false
        }

        return true
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

}