package io.scalac.seed.service

import io.scalac.seed.domain.AggregateRoot
import io.scalac.seed.service.CommandAdapter.Command

object CommandAdapter {
   trait Command
}

trait CommandAdapter {
   def adapt(command: Command) : (String, AggregateRoot.Command)
}
