package sifive.fpgashells.shell

import chisel3._
import chisel3.reflect.DataMirror
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import org.chipsalliance.cde.config._

case class IOPin(element: Element, index: Int = 0)
{
  private val width = DataMirror.widthOf(element)
  require (width.known)
  require (index >= 0 && index < width.get)

  def name = {
    //replace all [#]'s with _# in the pin base name, then append on the final [#] (pindex) if width > 1
    val pin = element.instanceName.split("\\.").map(_.replaceAll("""\[(\d+)\]""", "_$1")).mkString("_")
    val path = element.parentPathName.split("\\.")
    val pindex = pin + (if (width.get > 1) s"[${index}]" else "")
    (path.drop(1) :+ pindex).mkString("/")
  }

  def sdcPin = {
    val path = name
    if (path.contains("/")) s"[get_pins {${path}}]" else s"[get_ports {${path}}]"
  }

  def sdcClock = s"[get_clocks -of_objects ${sdcPin}]"

  def isOutput = {
    import chisel3.ActualDirection._
    DataMirror.directionOf(element) match {
      case Output => true
      case Input => false
      case Bidirectional(_) => true
      case Unspecified => { require(false); false }
    }
  }

  def isInput = {
    import chisel3.ActualDirection._
    DataMirror.directionOf(element) match {
      case Output => false
      case Input => true
      case Bidirectional(_) => true
      case Unspecified => { require(false); false }
    }
  }
}

object IOPin
{
  def of(x: Data): Seq[IOPin] = {
    val elts = x match {
      case a: Aggregate => getDataElements(a).reverse // because Chisel has it backwards
      case e: Element => Seq(e)
    }
    elts.flatMap { elt =>
      val width = DataMirror.widthOf(elt)
      require (width.known)
      Seq.tabulate(width.get) { i => IOPin(elt, i) }
    }
  }
}

case class IOTiming(
  minInput:  Double = 0,
  maxInput:  Double = 0,
  minOutput: Double = 0,
  maxOutput: Double = 0)

class SDC(val name: String)
{
  private var clocks:  Seq[() => String] = Nil
  private var groups:  Seq[() => String] = Nil
  private var falses:  Seq[() => String] = Nil
  private var timings: Seq[() => String] = Nil

  protected def addRawClock (command: => String) { clocks  = (() => command) +: clocks  }
  protected def addRawGroup (command: => String) { groups  = (() => command) +: groups  }
  def addRawFalse (command: => String) { falses  = (() => command) +: falses  }
  def addRawTiming(command: => String) { timings = (() => command) +: timings }
  addRawGroup("set_clock_groups -asynchronous")

  private def flatten(x: Seq[() => String], sep: String = "\n") = x.map(_()).filter(_ != "").reverse.mkString(sep)
  ElaborationArtefacts.add(name,
    s"""# ------------------------- Base Clocks --------------------
       |${flatten(clocks)}
       |# ------------------------- Clock Groups -------------------
       |${if (groups.size == 1) "" else flatten(groups, " \\\n")}
       |# ------------------------- False Paths --------------------
       |${flatten(falses)}
       |# ------------------------- IO Timings ---------------------
       |${flatten(timings)}
       |""".stripMargin)

  def addClock(name: => String, pin: => IOPin, freqMHz: => Double, jitterNs: => Double = 0.5) {
    addRawClock(s"create_clock -name ${name} -period ${1000/freqMHz} ${pin.sdcPin}")
    addRawClock(s"set_input_jitter ${name} ${jitterNs}")
  }

  def addDerivedClock(name: => String, source: => IOPin, sink: => IOPin, div: => Int = 1) {
    addRawClock(s"create_generated_clock -name ${name} -divide_by ${div} -source ${source.sdcPin} ${sink.sdcPin}")
  }

  def addGroup(clocks: => Seq[String] = Nil, pins: => Seq[IOPin] = Nil) {
    def thunk = {
      val clocksList = clocks
      val (pinsList, portsList) = pins.map(_.name).partition(_.contains("/"))
      val sep = " \\\n      "
      val clocksStr = (" [get_clocks {" +: clocksList).mkString(sep) + " \\\n    }]"
      val pinsStr   = (" [get_clocks -of_objects [get_pins {"  +: pinsList ).mkString(sep) + " \\\n    }]]"
      val portsStr  = (" [get_clocks -of_objects [get_ports {" +: portsList).mkString(sep) + " \\\n    }]]"
      val str = s"  -group [list${if (clocksList.isEmpty) "" else clocksStr}${if (pinsList.isEmpty) "" else pinsStr}${if (portsList.isEmpty) "" else portsStr}]"  
      if (clocksList.isEmpty && pinsList.isEmpty && portsList.isEmpty) "" else str
    }
    addRawGroup(thunk)
  }

  def addAsyncPath(through: => Seq[IOPin]) {
    addRawFalse("set_false_path" + through.map(x => s" -through ${x.sdcPin}").mkString)
  }

  def addInputDelay(port: => IOPin, clock: => String, min: => Double, max: => Double) {
    addRawTiming(f"set_input_delay  -min ${min}% -5.2f -clock ${clock} ${port.sdcPin}")
    addRawTiming(f"set_input_delay  -max ${max}% -5.2f -clock ${clock} ${port.sdcPin}")
  }

  def addOutputDelay(port: => IOPin, clock: => String, min: => Double, max: => Double) {
    addRawTiming(f"set_output_delay -min ${min}% -5.2f -clock ${clock} ${port.sdcPin}")
    addRawTiming(f"set_output_delay -max ${max}% -5.2f -clock ${clock} ${port.sdcPin}")
  }

  def addIOTiming(io: IOPin, clock: => String, timing: => IOTiming) {
    if (io.isInput)  { addInputDelay (io, clock, timing.minInput,  timing.maxInput)  }
    if (io.isOutput) { addOutputDelay(io, clock, timing.minOutput, timing.maxOutput) }
  }
}

// An IOOverlay is an Overlay with a public shell-level IO

trait IOPlacedOverlay[IO <: Data, DesignInput, ShellInput, OverlayOutput] extends PlacedOverlay[DesignInput, ShellInput, OverlayOutput]
{
  def ioFactory: IO
  def shell: IOShell
  def sinkScope: LazyScope = shell

  val io = shell { InModuleBody {
    val port = IO(ioFactory)
    port.suggestName(name)
    port
  } }
}

abstract class IOShell()(implicit p: Parameters) extends Shell
{
  // This can be overriden if a particular vendor needs customized SDC output
  def sdc: SDC
}

/*
   Copyright 2016 SiFive, Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
