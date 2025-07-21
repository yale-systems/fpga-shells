package sifive.fpgashells.shell

import chisel3._
import chisel3.experimental.Analog
import freechips.rocketchip.diplomacy._
import org.chipsalliance.cde.config._

case class QSFPShellInput(index: Int = 0)
case class QSFPDesignInput()(implicit val p: Parameters)
case class QSFPOverlayOutput(qsfp: ModuleValue[FlippedQSFPIO])
case object QSFPOverlayKey extends Field[Seq[DesignPlacer[QSFPDesignInput, QSFPShellInput, QSFPOverlayOutput]]](Nil)
trait QSFPShellPlacer[Shell] extends ShellPlacer[QSFPDesignInput, QSFPShellInput, QSFPOverlayOutput]


class ShellQSFPIO() extends Bundle {
  val tx_p      = Analog(4.W)
  val tx_n      = Analog(4.W)
  val rx_p      = Analog(4.W)
  val rx_n      = Analog(4.W)
  val refclk_p  = Analog(1.W)
  val refclk_n  = Analog(1.W)
  val modsell   = Analog(1.W)
  val resetl    = Analog(1.W)
  val modprsl   = Analog(1.W)
  val intl      = Analog(1.W)
  val lpmode    = Analog(1.W)
}

class FlippedQSFPIO() extends Bundle {
  val tx_p      = Output(UInt(4.W))
  val tx_n      = Output(UInt(4.W))
  val rx_p      = Input(UInt(4.W))
  val rx_n      = Input(UInt(4.W))
  val refclk_p  = Input(Bool())
  val refclk_n  = Input(Bool())
  val modsell   = Output(Bool())
  val resetl    = Output(Bool())
  val modprsl   = Input(Bool())
  val intl      = Input(Bool())
  val lpmode    = Output(Bool())
}

abstract class QSFPPlacedOverlay(
  val name: String, val di: QSFPDesignInput, val si: QSFPShellInput)
    extends IOPlacedOverlay[ShellQSFPIO, QSFPDesignInput, QSFPShellInput, QSFPOverlayOutput]
{
  implicit val p = di.p

  def ioFactory = new ShellQSFPIO()

  val qsfpSource = BundleBridgeSource(() => new FlippedQSFPIO())
  val qsfpSink = sinkScope { qsfpSource.makeSink }
  val qsfpout = InModuleBody { qsfpSource.bundle}

  def overlayOutput = QSFPOverlayOutput(qsfp = qsfpout)
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
