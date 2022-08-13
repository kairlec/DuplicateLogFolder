@file:Suppress("UnusedReceiverParameter", "unused")

package com.kairlec

import com.kairlec.FolderKLoggerContext.FolderKLoggerThreadContext
import com.kairlec.log.LogPersistence
import com.kairlec.log.logPersistence
import mu.KotlinLogging
import org.slf4j.MDC
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.deleteIfExists

@ExperimentalApi
internal fun FolderKLoggerThreadContext.saveTo(directory: Path) {
    saveAs(directory.resolve(id))
}

@ExperimentalApi
internal fun FolderKLoggerThreadContext.removeSaveTo(directory: Path) {
    removeSaveAs(directory.resolve(id))
}

@ExperimentalApi
fun FolderKLoggerThreadContext.saveAs(file: Path) {
    if (Files.notExists(file.parent)) {
        Files.createDirectories(file.parent)
    }
    if (countBuffer == 0 || last.isEmpty()) {
        return
    }
    val bytes = logPersistence {
        this.logResults.addAll(this@saveAs.last)
        this.countBuffer = this@saveAs.countBuffer
    }.toByteArray()
    Files.write(file, bytes)
}

@ExperimentalApi
fun FolderKLoggerThreadContext.removeSaveAs(file: Path) {
    file.deleteIfExists()
}

@OptIn(ExperimentalApi::class)
fun FolderKLoggerThreadContext.save() {
    saveTo(LogPersistenceStrategy.persistenceDir)
}

@ExperimentalApi
fun FolderKLoggerThreadContext.removeSave() {
    removeSaveTo(LogPersistenceStrategy.persistenceDir)
}

interface LogPersistenceStrategy {
    fun FolderKLoggerThreadContext.persistence()

    @ExperimentalApi
    fun FolderKLoggerThreadContext.clearPersistence()

    object Default : LogPersistenceStrategy {
        override fun FolderKLoggerThreadContext.persistence() {
            this.save()
        }

        @ExperimentalApi
        override fun FolderKLoggerThreadContext.clearPersistence() {
            this.removeSave()
        }
    }

    companion object {
        private val log = KotlinLogging.logger { }

        val persistenceDir: Path = System.getProperty("klogger.folder.persist.dir")?.let { Path.of(it) }
            ?: Path.of(System.getProperty("java.io.tmpdir"), "klogger_dump")

        init {
            if (Files.notExists(persistenceDir)) {
                Files.createDirectories(persistenceDir)
            }
            val dumps = Files.list(persistenceDir).collect(Collectors.toList())
            if (dumps.isNotEmpty()) {
                log.warn { "flush last persist message starting" }
                dumps.forEach {
                    val persistence = LogPersistence.parseFrom(Files.readAllBytes(it))
                    MDC.put(foldTimesMdcKey, persistence.countBuffer.toString())
                    MDC.put(foldIdMdcKey, it.fileName.toString())
                    try {
                        persistence.logResultsList.forEach { lgr ->
                            log.write(lgr)
                        }
                    } finally {
                        MDC.remove(foldTimesMdcKey)
                        MDC.remove(foldIdMdcKey)
                    }
                    it.deleteIfExists()
                }
                log.warn { "flush last persist message finished" }
            }
        }

        @ExperimentalApi
        fun saveAs(file: Path): LogPersistenceStrategy {
            return object : LogPersistenceStrategy {
                override fun FolderKLoggerThreadContext.persistence() {
                    this.saveAs(file)
                }

                @ExperimentalApi
                override fun FolderKLoggerThreadContext.clearPersistence() {
                    this.removeSaveAs(file)
                }
            }
        }

        @ExperimentalApi
        fun saveTo(directory: Path): LogPersistenceStrategy {
            return object : LogPersistenceStrategy {
                override fun FolderKLoggerThreadContext.persistence() {
                    this.saveTo(directory)
                }

                @ExperimentalApi
                override fun FolderKLoggerThreadContext.clearPersistence() {
                    this.removeSaveTo(directory)
                }
            }
        }
    }
}