package org.eigengo.activator.nashorn

import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.internal.runtime.ScriptObject
import java.util

object WorkflowObjectMapper {

  val expandPrimitiveArrays = false

  def map(value: Any): Any = {
    import scala.collection.JavaConversions._

    value match {
      case x: ScriptObjectMirror => x.keySet().foldLeft(Map.empty[String, Any])((b, a) => b + (a -> map(x.get(a))))
      case x: ScriptObject => x.keySet().foldLeft(Map.empty[String, Any])((b, a) => b + (a.toString -> map(x.get(a))))
      case x: Array[Byte] => if (expandPrimitiveArrays) util.Arrays.toString(x) else "[]"
      case x => x
    }
  }

}
