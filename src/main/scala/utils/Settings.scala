package utils

import akka.actor.ActorSystem

/**
  * Created by Yannick on 19.05.16.
  */
class Settings(system: ActorSystem) {
  object elasti {
    val host = system.settings.config.getString("akka.elasticsearch.host")
    val port = system.settings.config.getInt("akka.elasticsearch.port")
  }

  object cleaning {
    val host = system.settings.config.getString("akka.cleaning.host")
    val port = system.settings.config.getInt("akka.cleaning.port")
  }
}

object Settings {
  def apply(system: ActorSystem) = new Settings(system)
}
