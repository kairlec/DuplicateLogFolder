@file:Suppress("UnusedReceiverParameter", "unused")

package com.kairlec

import com.google.protobuf.timestamp
import com.kairlec.FoldMdcKeys.foldFormatKey
import com.kairlec.FoldMdcKeys.foldIdMdcKey
import com.kairlec.FoldMdcKeys.foldTimesMdcKey
import com.kairlec.FolderKLoggerContexts.FolderKLoggerContext
import com.kairlec.log.LogPersistence
import com.kairlec.log.logPersistence
import mu.KotlinLogging
import org.slf4j.MDC
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.*
import java.util.concurrent.TimeUnit.MINUTES
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
import kotlin.io.path.deleteIfExists
import kotlin.math.max

@ExperimentalApi
internal fun FolderKLoggerContext.saveTo(directory: Path) {
    saveAs(directory.resolve(id.toString()))
}

@ExperimentalApi
internal fun FolderKLoggerContext.removeSaveTo(directory: Path) {
    removeSaveAs(directory.resolve(id.toString()))
}

@ExperimentalApi
fun FolderKLoggerContext.saveAs(file: Path) {
    if (Files.notExists(file.parent)) {
        Files.createDirectories(file.parent)
    }
    if (countBuffer == 0 || last.isEmpty()) {
        return
    }
    val bytes =
        logPersistence {
            this.logResults.addAll(this@saveAs.last.map { it.logResult })
            this.countBuffer = this@saveAs.countBuffer
            val now = Instant.now()
            this.timestamp =
                timestamp {
                    this.seconds = now.epochSecond
                    this.nanos = now.nano
                }
        }.toByteArray()
    Files.write(file, bytes)
}

@ExperimentalApi
fun FolderKLoggerContext.removeSaveAs(file: Path) {
    file.deleteIfExists()
}

@OptIn(ExperimentalApi::class)
fun FolderKLoggerContext.save() {
    saveTo(LogPersistenceStrategy.persistenceDir)
}

@ExperimentalApi
fun FolderKLoggerContext.removeSave() {
    removeSaveTo(LogPersistenceStrategy.persistenceDir)
}

interface LogPersistenceStrategy {
    fun FolderKLoggerContext.persistence()

    @ExperimentalApi
    fun FolderKLoggerContext.clearPersistence()

    object Default : LogPersistenceStrategy {
        override fun FolderKLoggerContext.persistence() {
            this.save()
        }

        @ExperimentalApi
        override fun FolderKLoggerContext.clearPersistence() {
            this.removeSave()
        }
    }

    object Async : LogPersistenceStrategy {
        override fun FolderKLoggerContext.persistence() {
            asyncPersistenceExecutor.execute {
                this.save()
            }
        }

        @ExperimentalApi
        override fun FolderKLoggerContext.clearPersistence() {
            asyncPersistenceExecutor.execute {
                this.removeSave()
            }
        }
    }

    companion object {
        private val log = KotlinLogging.logger { }
        private val asyncPersistenceExecutor =
            run {
                val corePoolSize =
                    System.getProperty("klogger.folder.persist.threads.core")?.toInt()
                        ?: (Runtime.getRuntime().availableProcessors() * 2)
                val maxPoolSize =
                    System.getProperty("klogger.folder.persist.threads.max")?.toInt()
                        ?: (max(corePoolSize, 64))
                ThreadPoolExecutor(
                    corePoolSize,
                    maxPoolSize,
                    5,
                    MINUTES,
                    LinkedBlockingQueue(),
                    object : ThreadFactory {
                        private val defaultFactory = Executors.defaultThreadFactory()
                        private val threadNumber = AtomicInteger(1)

                        override fun newThread(r: Runnable): Thread {
                            val thread = defaultFactory.newThread(r)
                            if (!thread.isDaemon) {
                                thread.isDaemon = true
                            }
                            thread.name = "klogger-" + threadNumber.getAndIncrement()
                            return thread
                        }
                    },
                )
            }

        val persistenceDir: Path =
            System.getProperty("klogger.folder.persist.dir")?.let { Path.of(it) }
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
                    val instant =
                        Instant.ofEpochSecond(
                            persistence.timestamp.seconds,
                            persistence.timestamp.nanos.toLong(),
                        )
                    val time = DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneId.systemDefault()).format(instant)
                    log.warn { "log of dump id [${it.fileName}] persist at $time" }
                    MDC.put(foldTimesMdcKey, persistence.countBuffer.toString())
                    MDC.put(foldIdMdcKey, it.fileName.toString())
                    MDC.put(foldFormatKey, " [${it.fileName}, x${persistence.countBuffer}]")
                    try {
                        persistence.logResultsList.forEach { lgr ->
                            log.write(RuntimeLogResultWrapper.from(lgr, null))
                        }
                    } finally {
                        MDC.remove(foldTimesMdcKey)
                        MDC.remove(foldIdMdcKey)
                        MDC.remove(foldFormatKey)
                    }
                    try {
                        it.deleteIfExists()
                    } catch (e: Throwable) {
                        log.error(e) { "delete dump file failed" }
                    }
                }
                log.warn { "flush last persist message finished" }
            }
        }

        @ExperimentalApi
        fun saveAs(file: Path): LogPersistenceStrategy {
            return object : LogPersistenceStrategy {
                override fun FolderKLoggerContext.persistence() {
                    this.saveAs(file)
                }

                @ExperimentalApi
                override fun FolderKLoggerContext.clearPersistence() {
                    this.removeSaveAs(file)
                }
            }
        }

        @ExperimentalApi
        fun saveTo(directory: Path): LogPersistenceStrategy {
            return object : LogPersistenceStrategy {
                override fun FolderKLoggerContext.persistence() {
                    this.saveTo(directory)
                }

                @ExperimentalApi
                override fun FolderKLoggerContext.clearPersistence() {
                    this.removeSaveTo(directory)
                }
            }
        }
    }
}
