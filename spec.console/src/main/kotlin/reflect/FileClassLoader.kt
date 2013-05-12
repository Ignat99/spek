package org.spek.console.reflect;

import org.reflections.Reflections
import org.spek.impl.TestGivenAction
import org.spek.api.skip
import org.spek.impl.SkippedException
import org.spek.impl.SpekImpl
import java.lang.reflect.Method
import java.lang.reflect.AnnotatedElement
import org.spek.api.annotations.spek
import org.spek.impl.AnnotationsHelper
import org.reflections.util.ConfigurationBuilder
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ClasspathHelper

/**
 * @author hadihariri, jonnyzzz
 */
public object FileClassLoader {
    public fun <T : SpekImpl> getClasses(clazz : Class<T>, packageName : String ) : List<DetectedSpek>{
        val result = arrayListOf<DetectedSpek>()

        var reflectionConfig = ConfigurationBuilder.build(packageName)!!
        reflectionConfig.addScanners(SubTypesScanner())
        reflectionConfig.addScanners(MethodAnnotationsScanner())
        reflectionConfig.useParallelExecutor()
        val reflections = Reflections(reflectionConfig);

        result addAll (reflections.getSubTypesOf(clazz)!! map { ClassSpek(it) })

        val spekClazz = AnnotationsHelper.toAnnotationClazz(javaClass<spek>())!!
        val annotatedMethods = reflections.getMethodsAnnotatedWith(spekClazz)!!

        result addAll (annotatedMethods map { ExtensionFunctionSpek(it)})

        return result
    }
}

public trait DetectedSpek {
    fun allGiven() : List<TestGivenAction>
    fun name() : String
}

private fun AnnotatedElement.checkSkipped() {
    /*
    * TODO: need to be refactored when #KT-3534, KT-3534 got fixed.
    */
    val skip = AnnotationsHelper.getAnnotation(this, javaClass<skip>())
    if (skip != null) throw SkippedException(skip.value() ?: "")
}

public data class ExtensionFunctionSpek(val method : Method) : DetectedSpek {
    override fun name(): String = method.toString()
    override fun allGiven(): List<TestGivenAction> {
        val builder = SpekImpl()

        method.checkSkipped()
        method.invoke(null, builder)

        return builder.allGivens()
    }
}

public data class ClassSpek<T : SpekImpl>(val specificationClass: Class<out T>) : DetectedSpek {
    override fun name(): String = specificationClass.toString()
    override fun allGiven(): List<TestGivenAction> {
        specificationClass.checkSkipped()
        return specificationClass.newInstance().allGivens()
    }
}