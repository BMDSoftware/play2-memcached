package com.github.mumoshu.play2.memcached

import java.util.concurrent.{ TimeUnit, CancellationException }

import net.spy.memcached.transcoders.Transcoder
import play.api.cache.CacheApi
import play.api.{Logger, Configuration, Environment}
import play.api.inject.{ BindingKey, Injector, ApplicationLifecycle, Module }

import javax.inject.{Inject, Singleton}

import scala.concurrent.duration.Duration

import net.spy.memcached.MemcachedClient

import scala.reflect.ClassTag

@Singleton
class MemcachedCacheApi @Inject() (val namespace: String, client: MemcachedClient, configuration: Configuration) extends CacheApi {
  lazy val logger = Logger("memcached.plugin")
  lazy val tc = new CustomSerializing().asInstanceOf[Transcoder[Any]]
  lazy val timeout: Int = configuration.getInt("memcached.timeout").getOrElse(1)

  lazy val timeunit: TimeUnit = {
    configuration.getString("memcached.timeunit").getOrElse("seconds") match {
      case "seconds" => TimeUnit.SECONDS
      case "milliseconds" => TimeUnit.MILLISECONDS
      case "microseconds" => TimeUnit.MICROSECONDS
      case "nanoseconds" => TimeUnit.NANOSECONDS
      case _ => TimeUnit.SECONDS
    }
  }

  def get[T: ClassTag](key: String): Option[T] = {
    if (key.isEmpty) {
      None
    } else {
      try {
        doGet(key)
      } catch {
        case e: CancellationException => doGet(key)
        case t: Throwable =>
          logger.error("Failed to get the value in cache: " + t.getMessage)
          None
      }
    }
  }

  private def doGet[T: ClassTag](key: String): Option[T] = {
    val ct = implicitly[ClassTag[T]]
    val any = client.get(namespace + key, tc)
    Option(
      any match {
        case x if ct.runtimeClass.isInstance(x) => x.asInstanceOf[T]
        case x if ct == ClassTag.Nothing => x.asInstanceOf[T]
        case x => x.asInstanceOf[T]
      }
    )
  }

  def getOrElse[A: ClassTag](key: String, expiration: Duration = Duration.Inf)(orElse: => A): A = {
    get[A](key).getOrElse {
      val value = orElse
      set(key, value, expiration)
      value
    }
  }

  def set(key: String, value: Any, expiration: Duration = Duration.Inf) {
    if (!key.isEmpty) {
      val exp = if (expiration.isFinite()) expiration.toSeconds.toInt else 0
      try {
        client.set(namespace + key, exp, value, tc)
      } catch {
        case t: Throwable =>
          logger.error("Failed to set the value in cache: " + t.getMessage)
      }
    }
  }

  def remove(key: String) {
    if (!key.isEmpty) {
      try {
        client.delete(namespace + key)
      } catch {
        case t: Throwable =>
          logger.error("An error has occured while removing the value from memcached: " + t.getMessage)
      }
    }
  }
}

object MemcachedCacheApi {
  object ValFromJavaObject {
    def unapply(x: AnyRef): Option[AnyVal] = x match {
      case x: java.lang.Byte => Some(x.byteValue())
      case x: java.lang.Short => Some(x.shortValue())
      case x: java.lang.Integer => Some(x.intValue())
      case x: java.lang.Long => Some(x.longValue())
      case x: java.lang.Float => Some(x.floatValue())
      case x: java.lang.Double => Some(x.doubleValue())
      case x: java.lang.Character => Some(x.charValue())
      case x: java.lang.Boolean => Some(x.booleanValue())
      case _ => None
    }
  }
}
