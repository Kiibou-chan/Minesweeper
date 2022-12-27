package space.kiibou.annotations

import com.google.auto.service.AutoService
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic

@AutoService(Processor::class)
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedOptions(AutoLoadProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class AutoLoadProcessor : AbstractProcessor() {

    override fun process(annotations: MutableSet<out TypeElement>, roundEnv: RoundEnvironment): Boolean {
        val generatedSourcesRoot = processingEnv.options[KAPT_KOTLIN_GENERATED_OPTION_NAME].orEmpty()

        if (generatedSourcesRoot.isEmpty()) {
            processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "Can't find the target directory for generated Kotlin files")
            return false
        }

        val elements: Set<Element> = roundEnv.getElementsAnnotatedWith(AutoLoad::class.java)

        elements.filter { it.kind != ElementKind.CLASS }.let { list ->
            list.forEach {
                processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, "@AutoLoad can only be applied to classes", it)
            }

            if (list.isNotEmpty()) {
                return false
            }
        }

        val filer = processingEnv.filer



        return true
    }

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
    }

}