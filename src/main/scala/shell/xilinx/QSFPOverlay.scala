package sifive.fpgashells.shell.xilinx

import chisel3._
import freechips.rocketchip.diplomacy._
import sifive.fpgashells.shell._

abstract class QSFPXilinxPlacedOverlay(name: String, di: QSFPDesignInput, si: QSFPShellInput)
  extends QSFPPlacedOverlay(name, di, si)
{
  def shell: XilinxShell

  shell { InModuleBody {
    // UIntToAnalog(qsfpSink.bundle.tx_p, io.tx_p, true.B)
    // UIntToAnalog(qsfpSink.bundle.tx_n, io.tx_n, true.B)

    // qsfpSink.bundle.rx_p := AnalogToUInt(io.rx_p)
    // qsfpSink.bundle.rx_n := AnalogToUInt(io.rx_n)
    // qsfpSink.bundle.mgt_refclk_p := AnalogToUInt(io.mgt_refclk_p).asBool.asClock
    // qsfpSink.bundle.mgt_refclk_n := AnalogToUInt(io.mgt_refclk_n)

    // qsfpSink.bundle.modprsl := AnalogToUInt(io.modprsl)
    // qsfpSink.bundle.intl := AnalogToUInt(io.intl)

    // UIntToAnalog(qsfpSink.bundle.modsell, io.modsell, true.B)
    // UIntToAnalog(qsfpSink.bundle.resetl, io.resetl, true.B)
    // UIntToAnalog(qsfpSink.bundle.lpmode, io.lpmode, true.B)

    io.tx_p := qsfpSink.bundle.tx_p
    io.tx_n := qsfpSink.bundle.tx_n

    qsfpSink.bundle.rx_p := io.rx_p
    qsfpSink.bundle.rx_n := io.rx_n
    qsfpSink.bundle.mgt_refclk_p  := io.mgt_refclk_p
    qsfpSink.bundle.mgt_refclk_n  := io.mgt_refclk_n

    qsfpSink.bundle.modprsl := io.modprsl
    qsfpSink.bundle.intl  := io.intl


    io.modsell := qsfpSink.bundle.modsell
    io.resetl := qsfpSink.bundle.resetl
    io.lpmode := qsfpSink.bundle.lpmode
  } }
}
