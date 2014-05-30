package org.eigengo.activator.nashorn

import javax.script.ScriptEngineManager

object Simple extends App {

  val engine = new ScriptEngineManager().getEngineByName("nashorn")

  val bindings = engine.createBindings()
  bindings.put("self", this)
  engine.eval("print('Hello, world from ' + self.getClass().getName())", bindings)

}
