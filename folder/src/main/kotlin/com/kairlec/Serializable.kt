@file:Suppress("unused")

package com.kairlec

import com.google.protobuf.ByteString
import java.io.*

internal fun Serializable.toByteArray(): ByteArray {
    val b = ByteArrayOutputStream()
    ObjectOutputStream(b).use {
        it.writeObject(this)
    }
    return b.toByteArray()
}

@Suppress("UNCHECKED_CAST")
internal fun <T : Serializable> ByteArray.toObject(): T {
    val b = ByteArrayInputStream(this)
    return ObjectInputStream(b).use {
        it.readObject() as T
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <T : Serializable> ByteString.toObject(): T {
    return ObjectInputStream(this.newInput()).use {
        it.readObject() as T
    }
}


internal fun Serializable.toByteString(): ByteString {
    val b = ByteString.newOutput()
    ObjectOutputStream(b).use {
        it.writeObject(this)
    }
    return b.toByteString()
}
