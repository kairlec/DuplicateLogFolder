package com.kairlec

import javassist.ClassPool
import mu.KotlinLogging
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain


internal object LogAgent {
    @JvmStatic
    fun premain(agentArgs: String?, inst: Instrumentation) {
        inst.addTransformer(DefineTransformer(), true)
    }
    private val log = KotlinLogging.logger { }

    private val cp = ClassPool.getDefault()

    class DefineTransformer : ClassFileTransformer {
        override fun transform(
            loader: ClassLoader,
            className: String,
            classBeingRedefined: Class<*>?,
            protectionDomain: ProtectionDomain,
            classfileBuffer: ByteArray
        ): ByteArray {
            if (className == "mu/KotlinLogging") {
                val cc = cp[className.replace("/", ".")]
                cc.methods.forEach {
                    println(it)
                }

            }
            return classfileBuffer
        }
    }
}

